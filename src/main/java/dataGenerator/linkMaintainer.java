package dataGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import mapElements.Link;
import mapElements.Node;
import tools.Snmp;

public class linkMaintainer {

    private static final Logger LOG = Logger.getLogger(linkMaintainer.class.getName());

    private static final long POLL_PERIOD_MS = 7000L; // period per node
    private static final int  MAX_CONSECUTIVE_FAILS = 3;
    private static final long COOLDOWN_MS           = 60_000L; // 1 minute

    private final ConcurrentHashMap<Link, Integer> failCounts    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Link, Long>   cooldownUntil = new ConcurrentHashMap<>();

    // *** Global concurrency limit (for node polls) ***
    private static final int MAX_CONCURRENT_SNMP = 2;
    private static final Semaphore SNMP_PERMITS  = new Semaphore(MAX_CONCURRENT_SNMP);

    private final Link[] links;
    private final java.util.Map<Node, List<Link>> linksByNode = new java.util.LinkedHashMap<>();
    private final List<Node> nodes = new ArrayList<>();

    private final ScheduledThreadPoolExecutor scheduler;
    private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();
    private volatile boolean alive = true;

    // ----------------------------------------------------------------------
    // NEW: SNMP mode resolution (supports SNMPv3 + SNMPv1 via community)
    // ----------------------------------------------------------------------
    // We keep existing logic intact; we only add a small routing layer so links
    // can be polled using either v3 creds or v1 community.
    private enum SnmpMode { V1, V3, NONE }

    // ----------------------------------------------------------------------
    // NEW: In-memory cache for HC support (SNMPv1 only)
    // ----------------------------------------------------------------------
    // Why this exists:
    // - SNMPv1 may fail an entire PDU with errorStatus=noSuchName if HC OIDs are unsupported.
    // - tools.Snmp.snmpLinkRateUpdateV1 currently does "try HC first, then fallback to 32-bit".
    // - Without caching, we pay an extra HC request every poll even for devices that never support HC.
    //
    // Behavior:
    // - Cache is per SOURCE NODE and lives until pollers are restarted (exactly as requested).
    // - null/absent = UNKNOWN -> we allow trying HC once; tools.Snmp reports what it used.
    // - TRUE = supports HC -> we keep trying HC (fast path).
    // - FALSE = does not support HC -> we skip HC and go straight to 32-bit next polls.
    private final ConcurrentHashMap<Node, Boolean> hcSupportByNode = new ConcurrentHashMap<>();

    public linkMaintainer(Link[] links) {
        this.links = Objects.requireNonNull(links, "links");
        System.out.println("[SNMP ATTENTION!] per-node poller starting with " + links.length + " links");

        groupLinksByNode();

        this.scheduler = new ScheduledThreadPoolExecutor(
                Math.min(nodes.size(), 4),
                new NamedThreadFactory("node-poller")
        );
        this.scheduler.setRemoveOnCancelPolicy(true);

        schedulePerNodePolling();
    }

    // ----------------------------------------------------------------------
    // Group links by their source node
    // ----------------------------------------------------------------------
    private void groupLinksByNode() {
        for (Link l : links) {
            if (l == null || l.getNodeSnmpSrc() == null) {
                continue;
            }
            Node src = l.getNodeSnmpSrc();
            linksByNode.computeIfAbsent(src, k -> new ArrayList<>()).add(l);
        }
        nodes.addAll(linksByNode.keySet());

        System.out.println("[SNMP INFO] total SNMP nodes: " + nodes.size());
        for (Node n : nodes) {
            List<Link> ls = linksByNode.get(n);
            System.out.println("  node " + safeNodeName(n) + " has " + (ls != null ? ls.size() : 0) + " links");
        }
    }

    // ----------------------------------------------------------------------
    // Schedule one polling task per node
    // ----------------------------------------------------------------------
    private void schedulePerNodePolling() {
        final int n = nodes.size();

        for (int i = 0; i < n; i++) {
            final Node node = nodes.get(i);
            final List<Link> nodeLinks = linksByNode.get(node);
            if (nodeLinks == null || nodeLinks.isEmpty()) {
                continue;
            }

            final long initialDelayMs = (n > 0) ? (i * POLL_PERIOD_MS) / n : 0L;

            Runnable task = () -> {
                if (!alive) {
                    return;
                }

                try {
                    // Node-level quick check: if none of the links have valid SNMP config, skip
                    boolean hasValid = nodeLinks.stream().anyMatch(this::isSnmpConfigValid);
                    if (!hasValid) {
                        LOG.fine(() -> "[SNMP] Skipping node (no valid SNMP links): " + safeNodeName(node));
                        return;
                    }

                    // Check if all links for this node are cooling down; if yes, skip
                    long now = System.currentTimeMillis();
                    boolean allCooling = true;
                    for (Link l : nodeLinks) {
                        Long until = cooldownUntil.get(l);
                        if (until == null || now >= until) {
                            allCooling = false;
                            break;
                        }
                    }
                    if (allCooling) {
                        return;
                    }

                    // Acquire permit for this node's poll cycle
                    SNMP_PERMITS.acquire();
                    try {
                        if (!alive) {
                            return;
                        }

                        long nodeStart = System.nanoTime();

                        for (Link link : nodeLinks) {
                            if (!isSnmpConfigValid(link)) {
                                continue;
                            }

                            long tNow = System.currentTimeMillis();
                            Long until = cooldownUntil.get(link);
                            if (until != null && tNow < until) {
                                // link still in cooldown, skip
                                continue;
                            }

                            // NEW: decide whether this link should be polled via SNMPv3 or SNMPv1
                            SnmpMode mode = resolveSnmpMode(link);
                            if (mode == SnmpMode.NONE) {
                                // Should not happen because isSnmpConfigValid() already checked,
                                // but keep it defensive and logic-neutral.
                                continue;
                            }

                            long t0 = System.nanoTime();
                            boolean ok;
                            try {
                                // NEW: route to v3 or v1 polling without changing the scheduler/cooldown logic
                                if (mode == SnmpMode.V3) {
                                    // NOTE: This requires tools.Snmp to expose a v3-specific method.
                                    // If you only have a single method today, map this call inside tools.Snmp.
                                    ok = Snmp.snmpLinkRateUpdateV3(link);
                                } else {
                                    // SNMPv1/v2c community path
                                    //
                                    // NEW: HC support caching
                                    // - If we already learned HC support for this node, we tell Snmp whether to try HC.
                                    // - If unknown, we allow trying HC (Snmp will report what happened).
                                    Boolean cached = hcSupportByNode.get(node); // null => unknown
                                    boolean allowTryHC = (cached == null) ? true : cached.booleanValue();

                                    Snmp.V1PollResult res = Snmp.snmpLinkRateUpdateV1(link, allowTryHC);
                                    ok = res.ok;

                                    // NEW: update cache ONLY when we have a definitive result about HC usage/support.
                                    // - If SNMP succeeded and usedHC=true -> HC supported for this node.
                                    // - If SNMP succeeded and usedHC=false AND we had allowed HC -> HC not supported (or not usable),
                                    //   so we can skip HC next time for this node.
                                    if (res.ok) {
                                        if (res.usedHC) {
                                            // Set TRUE if new or changed
                                            Boolean prev = hcSupportByNode.put(node, Boolean.TRUE);
                                            if (prev == null || !prev) {
                                                System.out.println("[SNMP INFO] HC counters supported for node "
                                                        + safeNodeName(node) + " (community) - caching TRUE");
                                            }
                                        } else if (allowTryHC) {
                                            // We tried HC (because allowTryHC=true) but ended up using 32-bit.
                                            // That implies HC OIDs did not work for this node (at least for this ifIndex).
                                            Boolean prev = hcSupportByNode.put(node, Boolean.FALSE);
                                            if (prev == null || prev) {
                                                System.out.println("[SNMP INFO] HC counters NOT supported for node "
                                                        + safeNodeName(node) + " (community) - caching FALSE");
                                            }
                                        }
                                        // else: allowTryHC was false, so we intentionally skipped HC; do not flip cache here.
                                    }
                                }
                            } catch (Exception e) {
                                ok = false;
                                LOG.log(Level.WARNING, "[SNMP] Exception polling " + safeLinkDesc(link) + " (mode=" + mode + ")", e);
                            }
                            long dtMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

                            System.out.println("[SNMP DEBUG] node="
                                    + safeNodeName(node)
                                    + " oid=" + link.getOidIndex()
                                    + " took " + dtMs + " ms (ok=" + ok + ")");

                            if (ok) {
                                failCounts.remove(link);
                                cooldownUntil.remove(link);
                            } else {
                                int fails = failCounts.merge(link, 1, Integer::sum);
                                if (fails >= MAX_CONSECUTIVE_FAILS) {
                                    long untilTs = System.currentTimeMillis() + COOLDOWN_MS;
                                    cooldownUntil.put(link, untilTs);
                                    System.out.println("[SNMP INFO] link " + safeLinkDesc(link)
                                            + " entered cooldown after " + fails
                                            + " consecutive failures, until " + untilTs);
                                }
                            }
                        }

                        long nodeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nodeStart);
                        System.out.println("[SNMP DEBUG] node poll finished for "
                                + safeNodeName(node) + " in " + nodeMs + " ms");

                    } finally {
                        SNMP_PERMITS.release();
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.log(Level.FINE,
                            "[SNMP] Node poll interrupted for " + safeNodeName(node),
                            ie);
                } catch (Throwable t) {
                    LOG.log(Level.WARNING,
                            "[SNMP] Unexpected error in node poll for " + safeNodeName(node),
                            t);
                }
            };

            ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(
                    task,
                    initialDelayMs,
                    POLL_PERIOD_MS,
                    TimeUnit.MILLISECONDS
            );
            scheduledTasks.add(f);
        }
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    // NEW: Determine which SNMP mode applies for this link.
    // - Prefer v3 if it looks configured.
    // - Otherwise, allow community-based (v1/v2c) mode.
    private SnmpMode resolveSnmpMode(Link l) {
        if (l == null) return SnmpMode.NONE;
        if (l.getNodeSnmpSrc() == null) return SnmpMode.NONE;

        var src = l.getNodeSnmpSrc();

        // SNMPv3 config (existing fields used by your current implementation)
        boolean v3 =
                notEmpty(src.getIp())
                        && notEmpty(src.getSnmpv3auth())
                        && notEmpty(src.getSnmpv3priv())
                        && notEmpty(src.getSnmpv3username())
                        && notEmpty(l.getOidIndex());

        if (v3) return SnmpMode.V3;

        // SNMPv1/v2c config: IP + community + OID index
        // IMPORTANT: This assumes Node has getCommunity() (common in your codebase).
        boolean v1 =
                notEmpty(src.getIp())
                        && notEmpty(src.getCommunity())
                        && notEmpty(l.getOidIndex());

        if (v1) return SnmpMode.V1;

        return SnmpMode.NONE;
    }

    private boolean isSnmpConfigValid(Link l) {
        // Existing validation logic was v3-only.
        // NEW: accept either v3 or v1/v2c config as "valid".
        return resolveSnmpMode(l) != SnmpMode.NONE;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private static String safeNodeName(Node n) {
        try {
            return (n != null && n.getNodeName() != null)
                    ? n.getNodeName()
                    : "null-node";
        } catch (Throwable t) {
            return "node[desc-error]";
        }
    }

    private static String safeLinkDesc(Link l) {
        try {
            String ip  = (l != null && l.getNodeSnmpSrc() != null) ? l.getNodeSnmpSrc().getIp() : "null-ip";
            String idx = (l != null) ? l.getOidIndex() : "null-oid";
            return "ip=" + ip + ", oidIndex=" + idx;
        } catch (Throwable t) {
            return "link[desc-error]";
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------
    public void kill() {
        alive = false;
        System.out.println("[SNMP ATTENTION!] stopping per-node poller…");

        for (ScheduledFuture<?> f : scheduledTasks) {
            if (f != null && !f.isCancelled()) {
                f.cancel(true);
            }
        }

        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("[SNMP ATTENTION!] Scheduler did not terminate in time, forcing shutdown.");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[SNMP ATTENTION!] per-node poller stopped.");
    }

    public boolean isAlive() {
        return alive && !scheduler.isShutdown();
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final String base;
        private volatile int count = 0;

        NamedThreadFactory(String base) {
            this.base = base;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, base + "-" + (++count));
            t.setDaemon(true);
            return t;
        }
    }
}
