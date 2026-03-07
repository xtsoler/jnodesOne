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
import java.util.concurrent.ConcurrentHashMap;

/**
 * SNMPv3 SET for MikroTik Script Run column:
 * OID base: 1.3.6.1.4.1.14988.1.1.8.1.1.3.<index>  (set to 1 to run)
 */
public class snmpSet {

    private static final String MIKROTIK_SCRIPT_RUN_OID_BASE = "1.3.6.1.4.1.14988.1.1.8.1.1.3";
    private static final Object USM_LOCK = new Object();
    private static volatile boolean USM_READY = false;
    private static final ConcurrentHashMap<String, OctetString> ENGINE_ID_BY_ADDRESS = new ConcurrentHashMap<>();

    private static void ensureUsmInitialized() {
        if (USM_READY) return;
        synchronized (USM_LOCK) {
            if (USM_READY) return;
            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES128());
            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivDES());
            USM usm = new USM(SecurityProtocols.getInstance(),
                    new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(usm);
            USM_READY = true;
        }
    }

    private static OctetString getOrDiscoverEngineId(Snmp snmp, String host) {
        if (host == null || host.isEmpty()) return null;
        String address = "udp:" + host + "/161";
        OctetString cached = ENGINE_ID_BY_ADDRESS.get(address);
        if (cached != null) return cached;
        try {
            Address addr = GenericAddress.parse(address);
            byte[] engineIdBytes = snmp.discoverAuthoritativeEngineID(addr, 2500);
            if (engineIdBytes == null || engineIdBytes.length == 0) return null;
            OctetString engineId = new OctetString(engineIdBytes);
            ENGINE_ID_BY_ADDRESS.put(address, engineId);
            return engineId;
        } catch (Exception e) {
            return null;
        }
    }

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

            ensureUsmInitialized();

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
            OctetString secName = new OctetString(user);
            OctetString engineId = getOrDiscoverEngineId(snmp, host);
            if (engineId == null) {
                setConsole(console, "<html><font color=red>{ERROR}</font> Engine ID discovery failed.</html>");
                System.err.println("SNMPv3 SET skipped, engine ID discovery failed. Host: " + host);
                return;
            }
            snmp.getUSM().addUser(secName, engineId, usmUser);

            // Target
            UserTarget<Address> target = new UserTarget<>();
            target.setAddress(GenericAddress.parse("udp:" + host + "/161"));
            target.setVersion(SnmpConstants.version3);
            target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
            target.setSecurityName(secName);
            target.setTimeout(2000);
            target.setRetries(2);
            target.setAuthoritativeEngineID(engineId.getValue());

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
