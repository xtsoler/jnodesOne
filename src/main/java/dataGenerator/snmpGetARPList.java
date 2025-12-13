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
import org.snmp4j.CommunityTarget;                 // <-- ADDED for SNMPv1
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

public class snmpGetARPList {

    // --- ARP table (ipNetToMediaTable) & IF-MIB OIDs ---
    private static final String OID_IP_NET_TO_MEDIA_IFINDEX   = "1.3.6.1.2.1.4.22.1.1";
    private static final String OID_IP_NET_TO_MEDIA_PHYSADDR  = "1.3.6.1.2.1.4.22.1.2";
    private static final String OID_IP_NET_TO_MEDIA_NETADDR   = "1.3.6.1.2.1.4.22.1.3";
    private static final String OID_IP_NET_TO_MEDIA_TYPE      = "1.3.6.1.2.1.4.22.1.4";
    private static final String OID_IF_NAME                   = "1.3.6.1.2.1.31.1.1.1.1";

    // --- ADDED ---
    // SNMPv1-friendly interface label OID (MIB-II ifDescr, generally available on v1 agents)
    private static final String OID_IF_DESCR                  = "1.3.6.1.2.1.2.2.1.2";

    // Old fields kept only for backward compatibility (now derived from arpList)
    String[] nameList = {};
    String[] indexList = {};

    // --- Existing v3 fields ---
    String thehost, theusername, theauthPass, theprivPass, theencr;

    // --- ADDED v1 field ---
    String thecommunity; // when set, we use SNMPv1 instead of v3

    private List<ArpEntry> arpList = new ArrayList<>();

    /**
     * Represents one ARP-table entry
     */
    public static class ArpEntry {
        public final String ip;      // e.g. "192.168.1.10"
        public final String mac;     // e.g. "AA:BB:CC:DD:EE:FF"
        public final int ifIndex;    // ifIndex from IF-MIB
        public final String ifName;  // interface name
        public final int type;       // ipNetToMediaType (1=other,2=invalid,3=dynamic,4=static)

        public ArpEntry(String ip, String mac, int ifIndex, String ifName, int type) {
            this.ip = ip;
            this.mac = mac;
            this.ifIndex = ifIndex;
            this.ifName = ifName;
            this.type = type;
        }

        @Override
        public String toString() {
            return "ArpEntry{" +
                    "ip='" + ip + '\'' +
                    ", mac='" + mac + '\'' +
                    ", ifIndex=" + ifIndex +
                    ", ifName='" + ifName + '\'' +
                    ", type=" + type +
                    '}';
        }
    }

    /**
     * Existing SNMPv3 constructor (unchanged behavior).
     */
    public snmpGetARPList(String host, String username, String authPass, String privPass, String encr) {
        thehost = host;
        theusername = username;
        theauthPass = authPass;
        theprivPass = privPass;
        theencr = encr;

        // IMPORTANT: keep community null so update() selects v3 path
        thecommunity = null;

        update();
    }

    /**
     * ADDED: SNMPv1 constructor.
     * If you pass a community, update() will use SNMPv1 queries.
     */
    public snmpGetARPList(String host, String community) {
        thehost = host;
        thecommunity = community;

        // v3 credentials not used in v1 mode (left null intentionally)
        theusername = null;
        theauthPass = null;
        theprivPass = null;
        theencr = null;

        update();
    }

    private void update() {
        // Decide protocol based on whether community is set.
        // This matches your “use v1 when the node has community” requirement.
        if (thecommunity != null && !thecommunity.isEmpty()) {
            // SNMPv1 mode
            snmpGetArpTableV1("udp:" + thehost + "/161", thecommunity);
        } else {
            // SNMPv3 mode (original behavior)
            snmpGetArpTableV3("udp:" + thehost + "/161", theusername, theauthPass, theprivPass, theencr);
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

    static List<TreeEvent> walk(TreeUtils treeUtils, Target<Address> targetV3, OID oid) {
        List<TreeEvent> walkResult = new LinkedList<>();
        InternalTreeListener treeListener = new InternalTreeListener(walkResult);

        treeUtils.getSubtree(targetV3, oid, null, treeListener);
        try {
            treeListener.waitForResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return walkResult;
    }

    /**
     * Builds ARP table list using ipNetToMediaTable and IF-MIB.
     */
    public void snmpGetArpTableV3(String address, String username, String authPass, String privPass, String encr) {
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

            // --- 1) ifIndex -> ifName (IF-MIB::ifName) ---
            Map<Integer, String> ifIndexToName = new HashMap<>();
            OID ifNameOid = new OID(OID_IF_NAME);

            for (TreeEvent event : walk(treeUtils, userTarget, ifNameOid)) {
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

            // --- 2) ARP table (ipNetToMediaTable) ---
            buildArpList(treeUtils, userTarget, ifIndexToName);

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
     * ADDED: SNMPv1 version of the same ARP-table fetch.
     *
     * Note:
     * - Many SNMPv1 agents do NOT implement IF-MIB::ifName (ifXTable), so we use ifDescr (MIB-II).
     * - ARP table itself (ipNetToMediaTable) is in MIB-II and is commonly available in v1.
     */
    public void snmpGetArpTableV1(String address, String community) {
        Snmp snmp = null;
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            // --- Target & TreeUtils ---
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());

            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setAddress(GenericAddress.parse(address));
            target.setCommunity(new OctetString(community));
            target.setRetries(3);
            target.setTimeout(3000);
            target.setVersion(SnmpConstants.version1);

            // --- 1) ifIndex -> ifDescr (MIB-II) ---
            // We store it into the same map variable name "ifIndexToName" to reuse the common builder.
            Map<Integer, String> ifIndexToName = new HashMap<>();
            OID ifDescrOid = new OID(OID_IF_DESCR);

            for (TreeEvent event : walk(treeUtils, target, ifDescrOid)) {
                if (event == null || event.isError()) continue;
                if (event.getVariableBindings() == null) continue;

                for (VariableBinding vb : event.getVariableBindings()) {
                    if (vb == null) continue;
                    String suffix = vb.getOid().getSuffix(ifDescrOid).toString();
                    try {
                        int ifIndex = Integer.parseInt(suffix);
                        String ifDescr = vb.getVariable().toString();
                        ifIndexToName.put(ifIndex, ifDescr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // --- 2) ARP table (ipNetToMediaTable) ---
            buildArpList(treeUtils, target, ifIndexToName);

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
     * ADDED: Shared ARP-table building logic used by both SNMPv3 and SNMPv1.
     *
     * Why:
     * - Your existing code duplicated the ARP-table walk and entry creation.
     * - This keeps behavior identical while allowing different Targets (v1 vs v3).
     */
    private void buildArpList(TreeUtils treeUtils, Target<Address> target, Map<Integer, String> ifIndexToName) {

        // --- 2) ARP table (ipNetToMediaTable) ---
        Map<String, Integer> ifIndexByKey = new HashMap<>();
        Map<String, String> macByKey = new HashMap<>();
        Map<String, String> ipByKey = new HashMap<>();
        Map<String, Integer> typeByKey = new HashMap<>();

        OID ipNetToMediaIfIndexOid  = new OID(OID_IP_NET_TO_MEDIA_IFINDEX);
        OID ipNetToMediaPhysOid     = new OID(OID_IP_NET_TO_MEDIA_PHYSADDR);
        OID ipNetToMediaNetAddrOid  = new OID(OID_IP_NET_TO_MEDIA_NETADDR);
        OID ipNetToMediaTypeOid     = new OID(OID_IP_NET_TO_MEDIA_TYPE);

        // 2a) ipNetToMediaIfIndex
        for (TreeEvent event : walk(treeUtils, target, ipNetToMediaIfIndexOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(ipNetToMediaIfIndexOid).toString();
                int ifIndex = vb.getVariable().toInt();
                ifIndexByKey.put(key, ifIndex);
            }
        }

        // 2b) ipNetToMediaPhysAddress (MAC)
        for (TreeEvent event : walk(treeUtils, target, ipNetToMediaPhysOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(ipNetToMediaPhysOid).toString();

                // Defensive: some agents might return non-OctetString; handle gracefully.
                String macStr = null;
                if (vb.getVariable() instanceof OctetString) {
                    macStr = octetStringToMac((OctetString) vb.getVariable());
                } else {
                    macStr = vb.getVariable().toString();
                }

                macByKey.put(key, macStr);
            }
        }

        // 2c) ipNetToMediaNetAddress (IP)
        for (TreeEvent event : walk(treeUtils, target, ipNetToMediaNetAddrOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(ipNetToMediaNetAddrOid).toString();
                String ipStr = vb.getVariable().toString();  // IpAddress -> "x.x.x.x"
                ipByKey.put(key, ipStr);
            }
        }

        // 2d) ipNetToMediaType
        for (TreeEvent event : walk(treeUtils, target, ipNetToMediaTypeOid)) {
            if (event == null || event.isError()) continue;
            if (event.getVariableBindings() == null) continue;

            for (VariableBinding vb : event.getVariableBindings()) {
                if (vb == null) continue;
                String key = vb.getOid().getSuffix(ipNetToMediaTypeOid).toString();
                int type = vb.getVariable().toInt();
                typeByKey.put(key, type);
            }
        }

        // --- 3) Build ArpEntry list ---
        List<ArpEntry> result = new ArrayList<>();

        for (Map.Entry<String, String> e : ipByKey.entrySet()) {
            String key = e.getKey();
            String ip = e.getValue();

            // IfIndex is required to know interface; if missing, skip
            Integer ifIndexObj = ifIndexByKey.get(key);
            if (ifIndexObj == null) {
                continue;
            }
            int ifIndex = ifIndexObj;

            String mac = macByKey.get(key);  // may be null if incomplete
            int type = typeByKey.getOrDefault(key, 0);
            String ifName = ifIndexToName.get(ifIndex);

            ArpEntry entry = new ArpEntry(ip, mac, ifIndex, ifName, type);
            result.add(entry);
        }

        arpList = result;

        // --- keep old arrays loosely in sync for compatibility ---
        ArrayList<String> ipNames = new ArrayList<>();
        ArrayList<String> idxStrings = new ArrayList<>();
        for (ArpEntry n : arpList) {
            ipNames.add(n.ip);
            idxStrings.add(Integer.toString(n.ifIndex));
        }
        nameList = ipNames.toArray(new String[0]);
        indexList = idxStrings.toArray(new String[0]);
    }

    /**
     * Convert an OctetString (e.g. 6 bytes) to "AA:BB:CC:DD:EE:FF".
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

    // --- New main getter: full ARP info ---
    public List<ArpEntry> getNeighbors() {
        // kept name for parity with snmpGetMACList
        return new ArrayList<>(arpList);
    }

    // --- Old getters, now just derived from arpList (for compatibility) ---

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
