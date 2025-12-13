package dataGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeListener;
import org.snmp4j.util.TreeUtils;

public class snmpGetMACList {

    // --- BRIDGE-MIB & IF-MIB OIDs ---
    private static final String OID_DOT1D_TP_FDB_ADDRESS  = "1.3.6.1.2.1.17.4.3.1.1";
    private static final String OID_DOT1D_TP_FDB_PORT     = "1.3.6.1.2.1.17.4.3.1.2";
    private static final String OID_DOT1D_TP_FDB_STATUS   = "1.3.6.1.2.1.17.4.3.1.3";
    private static final String OID_DOT1D_BASE_PORT_IFIDX = "1.3.6.1.2.1.17.1.4.1.2";
    private static final String OID_IF_NAME               = "1.3.6.1.2.1.31.1.1.1.1";

    // Old fields kept only for backward compatibility (now derived from neighborList)
    String[] nameList = {};
    String[] indexList = {};

    // --- Inputs used to decide SNMP mode ---
    // If "thecommunity" is non-empty, we will use SNMPv1 community mode.
    // Otherwise, we will use SNMPv3 USM mode (username/auth/priv).
    String thehost, thecommunity, theusername, theauthPass, theprivPass, theencr;

    private List<NeighborEntry> neighborList = new ArrayList<>();

    /**
     * Represents one MAC-table neighbor entry
     */
    public static class NeighborEntry {
        public final String mac;       // e.g. "AA:BB:CC:DD:EE:FF"
        public final int bridgePort;   // dot1dBasePort
        public final int ifIndex;      // ifIndex from IF-MIB
        public final String ifName;    // interface name
        public final int fdbStatus;    // dot1dTpFdbStatus

        public NeighborEntry(String mac, int bridgePort, int ifIndex, String ifName, int fdbStatus) {
            this.mac = mac;
            this.bridgePort = bridgePort;
            this.ifIndex = ifIndex;
            this.ifName = ifName;
            this.fdbStatus = fdbStatus;
        }

        @Override
        public String toString() {
            return "NeighborEntry{" +
                    "mac='" + mac + '\'' +
                    ", bridgePort=" + bridgePort +
                    ", ifIndex=" + ifIndex +
                    ", ifName='" + ifName + '\'' +
                    ", fdbStatus=" + fdbStatus +
                    '}';
        }
    }

    /**
     * Existing constructor: SNMPv3 mode.
     * If you pass an empty username, authPass, etc, it will likely fail at runtime.
     */
    public snmpGetMACList(String host, String username, String authPass, String privPass, String encr) {
        thehost = host;
        theusername = username;
        theauthPass = authPass;
        theprivPass = privPass;
        theencr = encr;

        // Ensure community is null/empty so update() chooses SNMPv3 path
        thecommunity = null;

        update();
    }

    /**
     * New constructor: SNMPv1 mode (community string).
     * This is what you want when the node has "community".
     */
    public snmpGetMACList(String host, String community) {
        thehost = host;
        thecommunity = community;

        // Ensure v3 fields are empty so update() chooses SNMPv1 path
        theusername = null;
        theauthPass = null;
        theprivPass = null;
        theencr = null;

        update();
    }

    private void update() {
        // Build SNMP4J address format
        String addr = "udp:" + thehost + "/161";

        // --- Decision: SNMPv1 if community exists, else SNMPv3 ---
        // NOTE: You asked: "do snmpv1 instead of v3 when the node has community"
        if (thecommunity != null && !thecommunity.trim().isEmpty()) {
            snmpGetMacNeighborsV1(addr, thecommunity.trim());
        } else {
            snmpGetMacNeighborsV3(addr, theusername, theauthPass, theprivPass, theencr);
        }
    }

    /**
     * Original helper, left as-is (GUI-related)
     */
    public static List<Object> convertTreeEventToList(TreeSelectionEvent event) {
        List<Object> dataList = new ArrayList<>();
        JTree tree = (JTree) event.getSource();
        TreePath[] selectedPaths = tree.getSelectionPaths();

        if (selectedPaths != null) {
            for (TreePath path : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node != null) {
                    dataList.add(node.getUserObject());
                }
            }
        }
        return dataList;
    }

    /**
     * Common "walk" helper used by both SNMPv1 and SNMPv3.
     * It uses TreeUtils.getSubtree() and collects the resulting TreeEvents.
     */
    static List<TreeEvent> walk(TreeUtils treeUtils, Target<Address> target, OID oid) {
        List<TreeEvent> walkResult = new LinkedList<>();
        InternalTreeListener treeListener = new InternalTreeListener(walkResult);

        treeUtils.getSubtree(target, oid, null, treeListener);
        try {
            treeListener.waitForResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return walkResult;
    }

    /**
     * New version: builds MAC neighbor list using BRIDGE-MIB and IF-MIB.
     * SNMPv3 variant (your existing logic).
     */
    public void snmpGetMacNeighborsV3(String address, String username, String authPass, String privPass, String encr) {
        Snmp snmp = null;
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            // --- USM setup ---
            USM usm = new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
            SecurityModels.getInstance().addSecurityModel(usm);

            UsmUser user = new UsmUser(
                    new OctetString(username),
                    AuthSHA.ID, new OctetString(authPass),
                    PrivAES128.ID, new OctetString(privPass));

            if (encr != null && encr.equals("DES")) {
                user = new UsmUser(
                        new OctetString(username),
                        AuthSHA.ID, new OctetString(authPass),
                        PrivDES.ID, new OctetString(privPass));
            }

            snmp.getUSM().addUser(user);

            // --- Target & TreeUtils ---
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            UserTarget<Address> userTarget = new UserTarget<>();
            userTarget.setAddress(GenericAddress.parse(address));
            userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
            userTarget.setSecurityName(new OctetString(username));
            userTarget.setRetries(3);
            userTarget.setTimeout(3000);
            userTarget.setVersion(SnmpConstants.version3);

            // Same core logic for building neighborList
            buildNeighborList(treeUtils, userTarget);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * SNMPv1 community-based MAC-table walk.
     *
     * - No USM / user / auth / priv.
     * - Uses CommunityTarget and SNMP version1.
     * - Fetches the same BRIDGE-MIB/IF-MIB tables.
     */
    public void snmpGetMacNeighborsV1(String address, String community) {
        Snmp snmp = null;
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            // TreeUtils works fine for community targets as well
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());

            // Community Target (SNMPv1)
            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setAddress(GenericAddress.parse(address)); // e.g., "udp:127.0.0.1/161"
            target.setCommunity(new OctetString(community));
            target.setRetries(3);
            target.setTimeout(3000);

            // You explicitly asked for SNMPv1
            target.setVersion(SnmpConstants.version1);

            // Same core logic for building neighborList
            buildNeighborList(treeUtils, target);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Shared logic that actually fetches:
     *  1) bridgePort -> ifIndex (dot1dBasePortIfIndex)
     *  2) ifIndex -> ifName     (IF-MIB::ifName)
     *  3) MAC table rows        (dot1dTpFdbAddress/Port/Status)
     *
     * This is extracted so SNMPv1 and SNMPv3 call the same code with different Target types.
     */
    private void buildNeighborList(TreeUtils treeUtils, Target<Address> target) {

        // --- 1) bridgePort -> ifIndex (dot1dBasePortIfIndex) ---
        Map<Integer, Integer> bridgePortToIfIndex = new HashMap<>();
        OID basePortIfIndexOid = new OID(OID_DOT1D_BASE_PORT_IFIDX);

        for (TreeEvent event : walk(treeUtils, target, basePortIfIndexOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String suffix = vb.getOid().getSuffix(basePortIfIndexOid).toString();
                try {
                    int bridgePort = Integer.parseInt(suffix);
                    int ifIndex = vb.getVariable().toInt();
                    bridgePortToIfIndex.put(bridgePort, ifIndex);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // --- 2) ifIndex -> ifName (ifName) ---
        Map<Integer, String> ifIndexToName = new HashMap<>();
        OID ifNameOid = new OID(OID_IF_NAME);

        for (TreeEvent event : walk(treeUtils, target, ifNameOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String suffix = vb.getOid().getSuffix(ifNameOid).toString();
                try {
                    int ifIndex = Integer.parseInt(suffix);
                    String ifName = vb.getVariable().toString();
                    ifIndexToName.put(ifIndex, ifName);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // --- 3) MAC table: MAC, bridgePort, status ---
        Map<String, String> macByKey = new HashMap<>();
        Map<String, Integer> portByKey = new HashMap<>();
        Map<String, Integer> statusByKey = new HashMap<>();

        OID fdbAddrOid = new OID(OID_DOT1D_TP_FDB_ADDRESS);
        OID fdbPortOid = new OID(OID_DOT1D_TP_FDB_PORT);
        OID fdbStatusOid = new OID(OID_DOT1D_TP_FDB_STATUS);

        // 3a) MAC addresses
        for (TreeEvent event : walk(treeUtils, target, fdbAddrOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(fdbAddrOid).toString();

                // dot1dTpFdbAddress is an OCTET STRING (6 bytes)
                String macStr = octetStringToMac((OctetString) vb.getVariable());
                macByKey.put(key, macStr);
            }
        }

        // 3b) bridge ports
        for (TreeEvent event : walk(treeUtils, target, fdbPortOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(fdbPortOid).toString();
                int port = vb.getVariable().toInt();
                portByKey.put(key, port);
            }
        }

        // 3c) FDB status
        for (TreeEvent event : walk(treeUtils, target, fdbStatusOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(fdbStatusOid).toString();
                int status = vb.getVariable().toInt();
                statusByKey.put(key, status);
            }
        }

        // --- 4) Build NeighborEntry list ---
        List<NeighborEntry> result = new ArrayList<>();

        for (Map.Entry<String, String> e : macByKey.entrySet()) {
            String key = e.getKey();
            String mac = e.getValue();

            Integer bridgePortObj = portByKey.get(key);
            if (bridgePortObj == null) {
                // incomplete row, skip
                continue;
            }
            int bridgePort = bridgePortObj;

            int fdbStatus = statusByKey.getOrDefault(key, 0);
            int ifIndex = bridgePortToIfIndex.getOrDefault(bridgePort, -1);
            String ifName = ifIndexToName.get(ifIndex);

            NeighborEntry entry = new NeighborEntry(mac, bridgePort, ifIndex, ifName, fdbStatus);
            result.add(entry);
        }

        neighborList = result;

        // --- keep old arrays loosely in sync for compatibility ---
        ArrayList<String> macNames = new ArrayList<>();
        ArrayList<String> idxStrings = new ArrayList<>();
        for (NeighborEntry n : neighborList) {
            macNames.add(n.mac);
            idxStrings.add(Integer.toString(n.ifIndex));
        }
        nameList = macNames.toArray(new String[0]);
        indexList = idxStrings.toArray(new String[0]);
    }

    /**
     * Convert an OctetString (6 bytes) to "AA:BB:CC:DD:EE:FF".
     */
    private static String octetStringToMac(OctetString os) {
        byte[] b = os.getValue();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02X", b[i] & 0xFF));
            if (i < b.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    // --- New main getter: full neighbor info ---
    public List<NeighborEntry> getNeighbors() {
        return new ArrayList<>(neighborList);
    }

    // --- Old getters, now just derived from neighborList (for compatibility) ---

    @Deprecated
    public String[] getList() {
        return nameList;
    }

    @Deprecated
    public String[] getIndex() {
        return indexList;
    }

    // --- Internal listener unchanged ---

    static class InternalTreeListener implements TreeListener {

        private final List<TreeEvent> collectedEvents;
        private final CountDownLatch latch = new CountDownLatch(1);

        public InternalTreeListener(List<TreeEvent> eventList) {
            collectedEvents = eventList;
        }

        @Override
        public synchronized boolean next(TreeEvent event) {
            collectedEvents.add(event);
            return true;
        }

        @Override
        public synchronized void finished(TreeEvent event) {
            collectedEvents.add(event);
            latch.countDown();
        }

        @Override
        public boolean isFinished() {
            return latch.getCount() == 0;
        }

        void waitForResult() throws InterruptedException {
            latch.await();
        }
    }
}
