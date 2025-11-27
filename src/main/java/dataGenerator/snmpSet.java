package dataGenerator;

import javax.swing.JLabel;

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
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
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * SNMPv3 SET for MikroTik Script Run column:
 * OID base: 1.3.6.1.4.1.14988.1.1.8.1.1.3.<index>  (set to 1 to run)
 */
public class snmpSet {

    private static final String MIKROTIK_SCRIPT_RUN_OID_BASE = "1.3.6.1.4.1.14988.1.1.8.1.1.3";

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void setConsole(JLabel console, String html) {
        if (console != null) console.setText(html);
    }

    /**
     * New way: SNMPv3 authPriv (SHA/AES128).
     *
     * @param host        device IP/host
     * @param user        snmpv3 username
     * @param authPass    snmpv3 auth password
     * @param privPass    snmpv3 priv password
     * @param scriptIndex the scripts-table index (OID suffix) for the row to run
     * @param console     optional JLabel to show status (can be null)
     */
    public static void execute(String host,
                               String user,
                               String authPass,
                               String privPass,
                               String encr,
                               String scriptIndex,
                               JLabel console) {
        // Quiet no-op when creds are missing (your request)
        if (isBlank(host) || isBlank(user) || isBlank(authPass) || isBlank(privPass) || isBlank(scriptIndex)) {
            setConsole(console, "<html><font color=red>{INFO}</font> Missing SNMPv3 credentials or script index.</html>");
            return;
        }

        Snmp snmp = null;
        try {
            // Transport & SNMP
            DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            // USM (engine, protocols)
            USM usm = new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES128());
            SecurityModels.getInstance().addSecurityModel(usm);

            // User
            UsmUser usmUser = new UsmUser(
                    new OctetString(user),
                    AuthSHA.ID,    new OctetString(authPass),
                    PrivAES128.ID, new OctetString(privPass)
            );
            if(encr!=null && encr.equals("DES")){
                usmUser = new UsmUser(
                    new OctetString(user),
                    AuthSHA.ID,    new OctetString(authPass),
                    PrivDES.ID, new OctetString(privPass)
            );
            }
            snmp.getUSM().addUser(usmUser);

            // Target
            UserTarget<Address> target = new UserTarget<>();
            target.setAddress(GenericAddress.parse("udp:" + host + "/161"));
            target.setVersion(SnmpConstants.version3);
            target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
            target.setSecurityName(new OctetString(user));
            target.setTimeout(2000);
            target.setRetries(2);

            // Build SET PDU
            String oid = MIKROTIK_SCRIPT_RUN_OID_BASE + "." + scriptIndex;
            ScopedPDU pdu = new ScopedPDU();
            pdu.setType(PDU.SET);

            // Most MikroTik MIBs expect INTEGER (1) for "run".
            // If your device expects a string "1", swap to: new OctetString("1")
            pdu.add(new VariableBinding(new OID(oid), new Integer32(1)));

            // Send
            ResponseEvent<?> responseEvent = snmp.set(pdu, target);
            PDU response = responseEvent != null ? (PDU) responseEvent.getResponse() : null;

            if (response == null) {
                setConsole(console, "<html><font color=red>{ERROR}</font> No SNMP response (timeout).</html>");
                System.err.println("SNMPv3 SET timeout. Host: " + host);
                return;
            }

            if (response.getErrorStatus() == PDU.noError) {
                setConsole(console, "<html><font color=green>{SUCCESS}</font> SNMP SET delivered!</html>");
                System.out.println("SNMPv3 SET OK - Host=" + host + " OID=" + oid);
            } else {
                String err = "ErrorStatus=" + response.getErrorStatus() +
                             " (" + response.getErrorStatusText() + ") at index " + response.getErrorIndex();
                setConsole(console, "<html><font color=red>{ERROR}</font> SNMP SET failed!</html>");
                System.err.println("SNMPv3 SET failed - Host=" + host + " OID=" + oid + " :: " + err);
            }
        } catch (Exception e) {
            setConsole(console, "<html><font color=red>{ERROR}</font> SNMP SET exception.</html>");
            e.printStackTrace();
        } finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Legacy signature (v2c). Kept only to avoid breaking old callers.
     * It now no-ops and warns; prefer the SNMPv3 overload above.
     */
    @Deprecated
    public static void execute(String host, String community, String scriptIndex, JLabel console) {
        setConsole(console, "<html><font color=red>{INFO}</font> SNMPv2 method deprecated. Use SNMPv3 execute(host,user,auth,priv,idx,...).</html>");
        System.err.println("snmpSet.execute(v2c) is deprecated. Please call the SNMPv3 overload.");
    }
}
