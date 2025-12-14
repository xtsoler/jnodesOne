package tools;

import java.io.IOException;

import mapElements.Link;

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.UserTarget;
import org.snmp4j.CommunityTarget; // NEW: for SNMPv1 community target
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.TransportMapping;
import org.snmp4j.security.PrivDES;

public class Snmp {

    private final static boolean show_info = false;
    private final static boolean debug_timing = false; // leave off; timing is in linkMaintainer

    // ----------------------------------------------------------------------
    // NEW: result type for SNMPv1 polling
    // ----------------------------------------------------------------------
    // Purpose:
    // - linkMaintainer wants to cache whether a community-based node supports HC counters.
    // - We need to report whether this poll used HC or fell back to 32-bit.
    // - This stays in-memory only and does not change any Link/Node data models.
    public static final class V1PollResult {
        public final boolean ok;
        public final boolean usedHC;

        public V1PollResult(boolean ok, boolean usedHC) {
            this.ok = ok;
            this.usedHC = usedHC;
        }
    }

    /**
     * Polls a single link and updates its counters/admin/oper state.
     *
     * Tries 64-bit HC counters first (IF-MIB::ifHCInOctets/ifHCOutOctets).
     * Falls back to classic 32-bit counters (MIB-II::ifInOctets/ifOutOctets) if
     * HC counters are not supported or return noSuchInstance.
     *
     * @return true if we got a valid PDU with counters (and no SNMP
     * errorStatus) false on timeout, SNMP errorStatus, or incomplete VBs
     */
    public static boolean snmpLinkRateUpdate(Link lnk) {
        // IMPORTANT:
        // Keep this method for backward compatibility. Historically it was v3-only.
        // linkMaintainer can now call snmpLinkRateUpdateV3/V1 explicitly, but any old
        // code that still calls snmpLinkRateUpdate(...) will continue to work (v3).
        return snmpLinkRateUpdateV3(lnk);
    }

    /**
     * SNMPv3 polling path (this is your original snmpLinkRateUpdate logic).
     *
     * @return true if we got a valid PDU with counters (and no SNMP
     * errorStatus) false on timeout, SNMP errorStatus, or incomplete VBs
     */
    public static boolean snmpLinkRateUpdateV3(Link lnk) {

        if (lnk == null || lnk.getNodeSnmpSrc() == null || lnk.getOidIndex() == null) {
            return false;
        }

        // OID layout:
        //  0: ifHCInOctets   (64-bit)   1.3.6.1.2.1.31.1.1.1.6.ifIndex
        //  1: ifHCOutOctets  (64-bit)   1.3.6.1.2.1.31.1.1.1.10.ifIndex
        //  2: ifInOctets     (32-bit)   1.3.6.1.2.1.2.2.1.10.ifIndex
        //  3: ifOutOctets    (32-bit)   1.3.6.1.2.1.2.2.1.16.ifIndex
        //  4: ifAdminStatus             1.3.6.1.2.1.2.2.1.7.ifIndex
        //  5: ifOperStatus              1.3.6.1.2.1.2.2.1.8.ifIndex
        String idx = lnk.getOidIndex();

        String[] oids = new String[6];
        oids[0] = ".1.3.6.1.2.1.31.1.1.1.6." + idx; // ifHCInOctets
        oids[1] = ".1.3.6.1.2.1.31.1.1.1.10." + idx; // ifHCOutOctets
        oids[2] = ".1.3.6.1.2.1.2.2.1.10." + idx; // ifInOctets
        oids[3] = ".1.3.6.1.2.1.2.2.1.16." + idx; // ifOutOctets
        oids[4] = ".1.3.6.1.2.1.2.2.1.7." + idx; // ifAdminStatus
        oids[5] = ".1.3.6.1.2.1.2.2.1.8." + idx; // ifOperStatus

        org.snmp4j.Snmp snmp = null;
        long t0 = System.nanoTime();

        try {
            // Transport + listener
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new org.snmp4j.Snmp(transport);
            transport.listen();

            // USM/Security
            USM usm = new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
            SecurityModels.getInstance().addSecurityModel(usm);

            UsmUser user = new UsmUser(
                    new OctetString(lnk.getNodeSnmpSrc().getSnmpv3username()),
                    AuthSHA.ID, new OctetString(lnk.getNodeSnmpSrc().getSnmpv3auth()),
                    PrivAES128.ID, new OctetString(lnk.getNodeSnmpSrc().getSnmpv3priv())
            );
            if (lnk.getNodeSnmpSrc().getSnmpv3encr() != null && lnk.getNodeSnmpSrc().getSnmpv3encr().equals("DES")) {
                user = new UsmUser(
                        new OctetString(lnk.getNodeSnmpSrc().getSnmpv3username()),
                        AuthSHA.ID, new OctetString(lnk.getNodeSnmpSrc().getSnmpv3auth()),
                        PrivDES.ID, new OctetString(lnk.getNodeSnmpSrc().getSnmpv3priv())
                );
            }

            snmp.getUSM().addUser(user);

            // Target
            UserTarget<Address> userTarget = new UserTarget<>();
            userTarget.setAddress((Address) GenericAddress.parse(
                    "udp:" + lnk.getNodeSnmpSrc().getIp() + "/161"));
            userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
            userTarget.setSecurityName(new OctetString(lnk.getNodeSnmpSrc().getSnmpv3username()));

            userTarget.setRetries(1);
            userTarget.setTimeout(2500);
            userTarget.setVersion(SnmpConstants.version3);

            // PDU
            ScopedPDU pdu = new ScopedPDU();
            for (String s : oids) {
                pdu.add(new VariableBinding(new OID(s)));
            }
            pdu.setType(PDU.GET);

            // Send
            ResponseEvent<Address> response = snmp.send(pdu, userTarget);

            if (debug_timing) {
                long dtMs = java.util.concurrent.TimeUnit.NANOSECONDS
                        .toMillis(System.nanoTime() - t0);
                System.out.println("[SNMP DEBUG] node="
                        + lnk.getNodeSnmpSrc().getNodeName()
                        + " oidIndex=" + lnk.getOidIndex()
                        + " took " + dtMs + " ms");
            }

            if (response == null || response.getResponse() == null) {
                System.out.println("[SNMP ERROR] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": Timeout or null response.");
                return false;
            }

            PDU responsePDU = response.getResponse();

            // Check SNMP errorStatus first
            int errStatus = responsePDU.getErrorStatus();
            if (errStatus != PDU.noError) {
                String errText = responsePDU.getErrorStatusText();
                int errIndex = responsePDU.getErrorIndex();
                System.out.println("[SNMP WARN] link id:" + lnk.getID()
                        + " oid index:" + lnk.getOidIndex()
                        + " node:" + lnk.getNodeSnmpSrc().getNodeName()
                        + " SNMP errorStatus=" + errStatus
                        + " (" + errText + "), errorIndex=" + errIndex);
                return false;
            }

            int vbCount = responsePDU.getVariableBindings().size();

            // admin / oper: need at least entries for index 4 and 5
            if (vbCount >= 6) {
                Variable adminVar = responsePDU.getVariableBindings().get(4).getVariable();
                Variable operVar = responsePDU.getVariableBindings().get(5).getVariable();

                // ifAdminStatus / ifOperStatus: 1=up, 2=down, 3=testing
                // treat "2" as disabled/down
                if (!isException(adminVar)) {
                    lnk.setAdminEnabled(adminVar.toInt() != 2);
                } else if (lnk.getAdminEnabled() == null) {
                    lnk.setAdminEnabled(Boolean.TRUE);
                }

                if (!isException(operVar)) {
                    lnk.setOperEnabled(operVar.toInt() != 2);
                } else if (lnk.getOperEnabled() == null) {
                    lnk.setOperEnabled(Boolean.TRUE);
                }
            } else {
                if (lnk.getAdminEnabled() == null) {
                    lnk.setAdminEnabled(Boolean.TRUE);
                }
                if (lnk.getOperEnabled() == null) {
                    lnk.setOperEnabled(Boolean.TRUE);
                }
            }

            if (!lnk.getAdminEnabled() && show_info) {
                System.out.println("[SNMP INFO] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": disabled");
            } else if (!lnk.getOperEnabled() && show_info) {
                System.out.println("[SNMP INFO] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": disconnected");
            }

            // counters: we try HC (0,1) first, then fallback to classic (2,3)
            if (vbCount >= 4) {
                Variable hcInVar = responsePDU.getVariableBindings().get(0).getVariable();
                Variable hcOutVar = responsePDU.getVariableBindings().get(1).getVariable();
                Variable in32Var = responsePDU.getVariableBindings().get(2).getVariable();
                Variable out32Var = responsePDU.getVariableBindings().get(3).getVariable();

                Variable variableRx;
                Variable variableTx;
                boolean usingHC = false;

                if (!isException(hcInVar) && !isException(hcOutVar)) {
                    // Use 64-bit HC counters
                    variableRx = hcInVar;
                    variableTx = hcOutVar;
                    usingHC = true;
                } else if (!isException(in32Var) && !isException(out32Var)) {
                    // Fallback: use 32-bit counters
                    variableRx = in32Var;
                    variableTx = out32Var;
                    usingHC = false;
                } else {
                    System.out.println("[SNMP WARN] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                            + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                            + ": No usable octet counters (HC/32-bit both invalid).");
                    return false;
                }

                lnk.setCounterInPrevious(lnk.getCounterInCurrent());
                lnk.setCounterOutPrevious(lnk.getCounterOutCurrent());

                lnk.setCounterInCurrent(variableRx.toLong());
                lnk.setCounterTypeRx(variableRx);

                lnk.setCounterOutCurrent(variableTx.toLong());
                lnk.setCounterTypeTx(variableTx);

                lnk.setPreviousTime(lnk.getCurrentTime());
                lnk.setCurrentTime(System.currentTimeMillis());

                if (show_info) {
                    String typeStr = usingHC ? "HC(64-bit)" : "32-bit";
                    System.out.println("[SNMP INFO] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                            + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                            + " Rx:" + lnk.getRx() + " Tx:" + lnk.getTx()
                            + " using " + typeStr + " counters");
                }
                return true;
            } else {
                System.out.println("[SNMP WARN] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": Incomplete response (VB count=" + vbCount + ").");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * NEW: SNMPv1 polling path (community-based).
     *
     * IMPORTANT SNMPv1 NOTE:
     * - In SNMPv1, requesting an unsupported OID inside a GET can cause the agent
     *   to respond with errorStatus=noSuchName, which FAILS the ENTIRE PDU.
     * - That means the "single GET with both HC and 32-bit counters" approach that
     *   works fine on v2c/v3 (because they return per-VB exceptions) may fail on v1.
     *
     * Therefore, in v1 we do:
     *  1) Try HC counters in a dedicated GET (2 OIDs)      (OPTIONAL; controlled by tryHC)
     *  2) If that fails, fallback to 32-bit counters in a dedicated GET (2 OIDs)
     *  3) Admin/Oper in a dedicated GET (2 OIDs)
     *
     * We keep your link update logic and your console messages consistent with your existing style.
     *
     * @return V1PollResult that includes:
     *   - ok: true if we got valid counters and updated the link
     *   - usedHC: true if HC counters were successfully used in this poll
     */
    public static V1PollResult snmpLinkRateUpdateV1(Link lnk, boolean tryHC) {

        if (lnk == null || lnk.getNodeSnmpSrc() == null || lnk.getOidIndex() == null) {
            return new V1PollResult(false, false);
        }

        // SNMPv1 requires community string
        if (lnk.getNodeSnmpSrc().getCommunity() == null || lnk.getNodeSnmpSrc().getCommunity().isEmpty()) {
            return new V1PollResult(false, false);
        }

        String idx = lnk.getOidIndex();

        // OID layout (same meaning as v3, but we fetch in multiple PDUs for v1 robustness):
        //
        // HC counters (64-bit):
        //   ifHCInOctets   1.3.6.1.2.1.31.1.1.1.6.ifIndex
        //   ifHCOutOctets  1.3.6.1.2.1.31.1.1.1.10.ifIndex
        //
        // Classic counters (32-bit):
        //   ifInOctets     1.3.6.1.2.1.2.2.1.10.ifIndex
        //   ifOutOctets    1.3.6.1.2.1.2.2.1.16.ifIndex
        //
        // Status:
        //   ifAdminStatus  1.3.6.1.2.1.2.2.1.7.ifIndex
        //   ifOperStatus   1.3.6.1.2.1.2.2.1.8.ifIndex

        String hcInOid   = ".1.3.6.1.2.1.31.1.1.1.6." + idx;   // ifHCInOctets
        String hcOutOid  = ".1.3.6.1.2.1.31.1.1.1.10." + idx;  // ifHCOutOctets
        String in32Oid   = ".1.3.6.1.2.1.2.2.1.10." + idx;     // ifInOctets
        String out32Oid  = ".1.3.6.1.2.1.2.2.1.16." + idx;     // ifOutOctets
        String adminOid  = ".1.3.6.1.2.1.2.2.1.7." + idx;      // ifAdminStatus
        String operOid   = ".1.3.6.1.2.1.2.2.1.8." + idx;      // ifOperStatus

        org.snmp4j.Snmp snmp = null;
        long t0 = System.nanoTime();

        try {
            // Transport + listener
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new org.snmp4j.Snmp(transport);
            transport.listen();

            // Target (community-based)
            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(lnk.getNodeSnmpSrc().getCommunity()));
            target.setAddress((Address) GenericAddress.parse("udp:" + lnk.getNodeSnmpSrc().getIp() + "/161"));
            target.setRetries(1);
            target.setTimeout(2500);

            // User asked for SNMPv1 specifically (not v2c)
            target.setVersion(SnmpConstants.version1);

            // -------------------------------------------------------------
            // 1) Try HC counters first (2 OIDs only) - optional via tryHC
            // -------------------------------------------------------------
            boolean haveCounters = false;
            boolean usingHC = false;

            Variable variableRx = null;
            Variable variableTx = null;

            if (tryHC) {
                PDU pdu = new PDU();
                pdu.add(new VariableBinding(new OID(hcInOid)));
                pdu.add(new VariableBinding(new OID(hcOutOid)));
                pdu.setType(PDU.GET);

                ResponseEvent<Address> response = snmp.send(pdu, target);

                // Note: We intentionally do NOT treat HC failure as fatal in v1.
                // Many SNMPv1 agents will return errorStatus=noSuchName for unsupported HC OIDs.
                if (response != null && response.getResponse() != null) {
                    PDU responsePDU = response.getResponse();
                    int errStatus = responsePDU.getErrorStatus();

                    if (errStatus == PDU.noError) {
                        int vbCount = responsePDU.getVariableBindings().size();
                        if (vbCount >= 2) {
                            Variable hcInVar  = responsePDU.getVariableBindings().get(0).getVariable();
                            Variable hcOutVar = responsePDU.getVariableBindings().get(1).getVariable();

                            // For SNMPv1, we still protect against exception values in the response
                            if (!isException(hcInVar) && !isException(hcOutVar)) {
                                variableRx = hcInVar;
                                variableTx = hcOutVar;
                                haveCounters = true;
                                usingHC = true;
                            }
                        }
                    }
                    // else: SNMP errorStatus (including noSuchName) -> fallback to 32-bit counters below
                }
                // else: timeout/null -> fallback to 32-bit counters below
            }

            // -------------------------------------------------------------
            // 2) Fallback to classic 32-bit counters if HC is unavailable
            // -------------------------------------------------------------
            if (!haveCounters) {
                PDU pdu = new PDU();
                pdu.add(new VariableBinding(new OID(in32Oid)));
                pdu.add(new VariableBinding(new OID(out32Oid)));
                pdu.setType(PDU.GET);

                ResponseEvent<Address> response = snmp.send(pdu, target);

                if (response == null || response.getResponse() == null) {
                    System.out.println("[SNMP ERROR] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                            + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                            + ": Timeout or null response.");
                    return new V1PollResult(false, false);
                }

                PDU responsePDU = response.getResponse();

                // Check SNMP errorStatus first
                int errStatus = responsePDU.getErrorStatus();
                if (errStatus != PDU.noError) {
                    String errText = responsePDU.getErrorStatusText();
                    int errIndex = responsePDU.getErrorIndex();
                    System.out.println("[SNMP WARN] link id:" + lnk.getID()
                            + " oid index:" + lnk.getOidIndex()
                            + " node:" + lnk.getNodeSnmpSrc().getNodeName()
                            + " SNMP errorStatus=" + errStatus
                            + " (" + errText + "), errorIndex=" + errIndex);
                    return new V1PollResult(false, false);
                }

                int vbCount = responsePDU.getVariableBindings().size();
                if (vbCount >= 2) {
                    Variable in32Var  = responsePDU.getVariableBindings().get(0).getVariable();
                    Variable out32Var = responsePDU.getVariableBindings().get(1).getVariable();

                    if (!isException(in32Var) && !isException(out32Var)) {
                        variableRx = in32Var;
                        variableTx = out32Var;
                        haveCounters = true;
                        usingHC = false;
                    } else {
                        System.out.println("[SNMP WARN] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                                + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                                + ": No usable octet counters (32-bit invalid).");
                        return new V1PollResult(false, false);
                    }
                } else {
                    System.out.println("[SNMP WARN] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                            + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                            + ": Incomplete response (VB count=" + vbCount + ").");
                    return new V1PollResult(false, false);
                }
            }

            // If we still don't have counters here, we must fail
            if (!haveCounters || variableRx == null || variableTx == null) {
                System.out.println("[SNMP WARN] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": No usable octet counters (HC/32-bit both failed).");
                return new V1PollResult(false, false);
            }

            // -------------------------------------------------------------
            // 3) Admin/Oper status in a dedicated GET (2 OIDs)
            // -------------------------------------------------------------
            {
                PDU pdu = new PDU();
                pdu.add(new VariableBinding(new OID(adminOid)));
                pdu.add(new VariableBinding(new OID(operOid)));
                pdu.setType(PDU.GET);

                ResponseEvent<Address> response = snmp.send(pdu, target);

                // We do not fail hard if admin/oper cannot be fetched; match your existing “default TRUE”
                if (response != null && response.getResponse() != null) {
                    PDU responsePDU = response.getResponse();

                    int errStatus = responsePDU.getErrorStatus();
                    if (errStatus == PDU.noError) {
                        int vbCount = responsePDU.getVariableBindings().size();
                        if (vbCount >= 2) {
                            Variable adminVar = responsePDU.getVariableBindings().get(0).getVariable();
                            Variable operVar  = responsePDU.getVariableBindings().get(1).getVariable();

                            // ifAdminStatus / ifOperStatus: 1=up, 2=down, 3=testing
                            // treat "2" as disabled/down
                            if (!isException(adminVar)) {
                                lnk.setAdminEnabled(adminVar.toInt() != 2);
                            } else if (lnk.getAdminEnabled() == null) {
                                lnk.setAdminEnabled(Boolean.TRUE);
                            }

                            if (!isException(operVar)) {
                                lnk.setOperEnabled(operVar.toInt() != 2);
                            } else if (lnk.getOperEnabled() == null) {
                                lnk.setOperEnabled(Boolean.TRUE);
                            }
                        } else {
                            // default behavior if VB count is too small
                            if (lnk.getAdminEnabled() == null) {
                                lnk.setAdminEnabled(Boolean.TRUE);
                            }
                            if (lnk.getOperEnabled() == null) {
                                lnk.setOperEnabled(Boolean.TRUE);
                            }
                        }
                    } else {
                        // default behavior if SNMP errorStatus non-zero
                        if (lnk.getAdminEnabled() == null) {
                            lnk.setAdminEnabled(Boolean.TRUE);
                        }
                        if (lnk.getOperEnabled() == null) {
                            lnk.setOperEnabled(Boolean.TRUE);
                        }
                    }
                } else {
                    // default behavior if timeout/null response
                    if (lnk.getAdminEnabled() == null) {
                        lnk.setAdminEnabled(Boolean.TRUE);
                    }
                    if (lnk.getOperEnabled() == null) {
                        lnk.setOperEnabled(Boolean.TRUE);
                    }
                }
            }

            if (debug_timing) {
                long dtMs = java.util.concurrent.TimeUnit.NANOSECONDS
                        .toMillis(System.nanoTime() - t0);
                System.out.println("[SNMP DEBUG] node="
                        + lnk.getNodeSnmpSrc().getNodeName()
                        + " oidIndex=" + lnk.getOidIndex()
                        + " took " + dtMs + " ms");
            }

            if (!lnk.getAdminEnabled() && show_info) {
                System.out.println("[SNMP INFO] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": disabled");
            } else if (!lnk.getOperEnabled() && show_info) {
                System.out.println("[SNMP INFO] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + ": disconnected");
            }

            // -------------------------------------------------------------
            // 4) Update counters/timestamps exactly as your existing logic
            // -------------------------------------------------------------
            lnk.setCounterInPrevious(lnk.getCounterInCurrent());
            lnk.setCounterOutPrevious(lnk.getCounterOutCurrent());

            lnk.setCounterInCurrent(variableRx.toLong());
            lnk.setCounterTypeRx(variableRx);

            lnk.setCounterOutCurrent(variableTx.toLong());
            lnk.setCounterTypeTx(variableTx);

            lnk.setPreviousTime(lnk.getCurrentTime());
            lnk.setCurrentTime(System.currentTimeMillis());

            if (show_info) {
                String typeStr = usingHC ? "HC(64-bit)" : "32-bit";
                System.out.println("[SNMP INFO] link id:" + lnk.getID() + " oid index:" + lnk.getOidIndex()
                        + " " + lnk.getNodeSnmpSrc().getNodeName() + " to " + lnk.getNodeDst().getNodeName()
                        + " Rx:" + lnk.getRx() + " Tx:" + lnk.getTx()
                        + " using " + typeStr + " counters");
            }

            // NEW: return whether HC was used so linkMaintainer can cache capability
            return new V1PollResult(true, usingHC);

        } catch (IOException e) {
            e.printStackTrace();
            return new V1PollResult(false, false);
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Backward-compatible wrapper (keeps existing call sites working).
     *
     * NOTE:
     * - The old signature returned boolean only.
     * - We keep it, and default to tryHC=true (original behavior).
     * - linkMaintainer now uses the richer overload to enable caching.
     */
    public static boolean snmpLinkRateUpdateV1(Link lnk) {
        return snmpLinkRateUpdateV1(lnk, true).ok;
    }

    /**
     * Helper: true if the variable is an SNMP exception (noSuchObject,
     * noSuchInstance, etc.).
     */
    private static boolean isException(Variable v) {
        if (v == null) {
            return true;
        }
        if (v instanceof Null) {
            return ((Null) v).isException();
        }
        return v.isException();
    }

    // snmpGetV3 can stay as you had it (not shown)
}
