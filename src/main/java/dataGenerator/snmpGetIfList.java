package dataGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

public class snmpGetIfList {

    String[] nameList = {};
    String[] indexList = {};

    // --- Inputs used to decide SNMP mode ---
    // If "thecommunity" is non-empty, we will use SNMPv1 community mode.
    // Otherwise, we will use SNMPv3 USM mode (username/auth/priv).
    String thehost, thecommunity, theusername, theauthPass, theprivPass, theencr;

    /**
     * Existing constructor: SNMPv3 mode.
     * If you pass an empty username, authPass, etc, it will likely fail at runtime.
     */
    public snmpGetIfList(String host, String username, String authPass, String privPass, String encr) {
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
    public snmpGetIfList(String host, String community) {
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
        // ifDescr table: 1.3.6.1.2.1.2.2.1.2
        // This is a standard interface description column.
        String oidValue = "1.3.6.1.2.1.2.2.1.2"; // ifDescr table

        // Build SNMP4J address format
        String addr = "udp:" + thehost + "/161";

        // --- Decision: SNMPv1 if community exists, else SNMPv3 ---
        // NOTE: You asked: "do snmpv1 instead of v3 when the node has community"
        if (thecommunity != null && !thecommunity.trim().isEmpty()) {
            snmpGetTableV1(addr, thecommunity.trim(), oidValue);
        } else {
            snmpGetTableV3(addr, theusername, theauthPass, theprivPass, oidValue, theencr);
        }
    }

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
     * SNMPv1 community-based table walk.
     *
     * - No USM / user / auth / priv.
     * - Uses CommunityTarget and SNMP version1.
     * - Fetches the same ifDescr table.
     */
    public void snmpGetTableV1(String address, String community, String oidValue) {
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

            List<TreeEvent> output = walk(treeUtils, target, new OID(oidValue));

            // Convert walk output into nameList/indexList just like v3
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> indeces = new ArrayList<>();
            for (TreeEvent event : output) {
                if (event == null || event.isError()) {
                    continue;
                }
                if (event.getVariableBindings() == null) {
                    continue;
                }

                for (VariableBinding vb : event.getVariableBindings()) {
                    if (vb == null) {
                        continue;
                    }
                    names.add(vb.getVariable().toString());
                    indeces.add(vb.getOid().getSuffix(new OID(oidValue)).toString());
                }
            }

            nameList = names.toArray(new String[0]);
            indexList = indeces.toArray(new String[0]);

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
     * SNMPv3 USM-based table walk (your existing code path).
     *
     * Notes:
     * - Requires username + authPass + privPass for AUTH_PRIV security level.
     * - If you ever want AUTH_NO_PRIV or NO_AUTH_NO_PRIV, you’d change security level
     *   and user configuration accordingly.
     */
    public void snmpGetTableV3(String address, String username, String authPass, String privPass, String oidValue, String encr) {
        Snmp snmp = null;
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            // USM setup
            USM usm = new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
            SecurityModels.getInstance().addSecurityModel(usm);

            // Default: AES128 privacy
            UsmUser user = new UsmUser(
                    new OctetString(username),
                    AuthSHA.ID, new OctetString(authPass),
                    PrivAES128.ID, new OctetString(privPass));

            // Optional: DES privacy
            if (encr != null && encr.equals("DES")) {
                user = new UsmUser(
                        new OctetString(username),
                        AuthSHA.ID, new OctetString(authPass),
                        PrivDES.ID, new OctetString(privPass));
            }

            // non-deprecated overload
            snmp.getUSM().addUser(user);

            // Target (use typed view for generics)
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            UserTarget<Address> userTarget = new UserTarget<>();
            userTarget.setAddress(GenericAddress.parse(address)); // e.g., "udp:127.0.0.1/161"
            userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
            userTarget.setSecurityName(new OctetString(username));
            userTarget.setRetries(3);
            userTarget.setTimeout(3000);
            userTarget.setVersion(SnmpConstants.version3);

            List<TreeEvent> output = walk(treeUtils, userTarget, new OID(oidValue));

            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> indeces = new ArrayList<>();
            for (TreeEvent event : output) {
                if (event == null || event.isError()) {
                    continue;
                }
                if (event.getVariableBindings() == null) {
                    continue;
                }

                for (VariableBinding vb : event.getVariableBindings()) {
                    if (vb == null) {
                        continue;
                    }
                    names.add(vb.getVariable().toString());
                    indeces.add(vb.getOid().getSuffix(new OID(oidValue)).toString());
                }
            }
            nameList = names.toArray(new String[0]);
            indexList = indeces.toArray(new String[0]);

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

    public String[] getList() {
        return nameList;
    }

    public String[] getIndex() {
        return indexList;
    }

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
