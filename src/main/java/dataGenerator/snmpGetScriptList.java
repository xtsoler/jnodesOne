package dataGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
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
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeListener;
import org.snmp4j.util.TreeUtils;

public class snmpGetScriptList {

    private static final String MIKROTIK_SCRIPT_NAME_OID = "1.3.6.1.4.1.14988.1.1.8.1.1.2";

    private String[] scriptList = {};
    private String[] indexList  = {};

    private final String thehost;
    private final String theusername;
    private final String theauthPass;
    private final String theprivPass;

    public snmpGetScriptList(String host, String username, String authPass, String privPass) {
        this.thehost = host;
        this.theusername = username;
        this.theauthPass = authPass;
        this.theprivPass = privPass;
        update();
    }

    /** Returns the SNMP table of script names and fills scriptList / indexList. */
    private void update() {
        snmpGetTableV3("udp:" + thehost + "/161", theusername, theauthPass, theprivPass, MIKROTIK_SCRIPT_NAME_OID);
    }

    /** Find the MikroTik scripts-table index (OID suffix) by exact script name. */
    public String findIndex(String scriptName) {
        for (int i = 0; i < scriptList.length; i++) {
            if (scriptList[i].equals(scriptName)) {
                return indexList[i]; // return actual table index (OID suffix), not just i+1
            }
        }
        return "";
    }

    public String[] getList() {
        return scriptList;
    }

    public String[] getIndex() {
        return indexList;
    }

    /** Perform an SNMPv3 walk similar to snmpGetIfList and fill arrays. */
    private void snmpGetTableV3(String address, String username, String authPass, String privPass, String oidBase) {
        Snmp snmp = null;
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            // USM (authPriv: SHA/AES128)
            USM usm = new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES128());
            SecurityModels.getInstance().addSecurityModel(usm);

            UsmUser user = new UsmUser(
                    new OctetString(username),
                    AuthSHA.ID,   new OctetString(authPass),
                    PrivAES128.ID,new OctetString(privPass)
            );
            snmp.getUSM().addUser(user);

            // Target + Tree utils
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            UserTarget<Address> userTarget = new UserTarget<>();
            userTarget.setAddress(GenericAddress.parse(address));
            userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
            userTarget.setSecurityName(new OctetString(username));
            userTarget.setRetries(2);
            userTarget.setTimeout(1500);
            userTarget.setVersion(SnmpConstants.version3);

            List<TreeEvent> output = walk(treeUtils, userTarget, new OID(oidBase));

            ArrayList<String> names   = new ArrayList<>();
            ArrayList<String> indices = new ArrayList<>();

            for (TreeEvent event : output) {
                if (event == null || event.isError()) continue;
                if (event.getVariableBindings() == null) continue;

                for (VariableBinding vb : event.getVariableBindings()) {
                    if (vb == null) continue;
                    // Script name
                    names.add(vb.getVariable().toString());
                    // Index = suffix of the row under the base OID
                    indices.add(vb.getOid().getSuffix(new OID(oidBase)).toString());
                }
            }

            scriptList = names.toArray(new String[0]);
            indexList  = indices.toArray(new String[0]);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception ignore) {}
            }
        }
    }

    // Same listener pattern as in snmpGetIfList, kept local for self-containment
    private static List<TreeEvent> walk(TreeUtils treeUtils, Target<Address> targetV3, OID oid) {
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

    private static class InternalTreeListener implements TreeListener {
        private final List<TreeEvent> collectedEvents;
        private final CountDownLatch latch = new CountDownLatch(1);

        InternalTreeListener(List<TreeEvent> out) { this.collectedEvents = out; }

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
