package tools;

import java.io.IOException;

import mapElements.Link;

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.UserTarget;
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

public class Snmp {

    private final static boolean show_info    = false;
    private final static boolean debug_timing = false; // leave off; timing is in linkMaintainer

    /**
     * Polls a single link and updates its counters/admin/oper state.
     *
     * Tries 64-bit HC counters first (IF-MIB::ifHCInOctets/ifHCOutOctets).
     * Falls back to classic 32-bit counters (MIB-II::ifInOctets/ifOutOctets)
     * if HC counters are not supported or return noSuchInstance.
     *
     * @return true  if we got a valid PDU with counters (and no SNMP errorStatus)
     *         false on timeout, SNMP errorStatus, or incomplete VBs
     */
    public static boolean snmpLinkRateUpdate(Link lnk) {

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
        oids[0] = ".1.3.6.1.2.1.31.1.1.1.6."  + idx; // ifHCInOctets
        oids[1] = ".1.3.6.1.2.1.31.1.1.1.10." + idx; // ifHCOutOctets
        oids[2] = ".1.3.6.1.2.1.2.2.1.10."    + idx; // ifInOctets
        oids[3] = ".1.3.6.1.2.1.2.2.1.16."    + idx; // ifOutOctets
        oids[4] = ".1.3.6.1.2.1.2.2.1.7."     + idx; // ifAdminStatus
        oids[5] = ".1.3.6.1.2.1.2.2.1.8."     + idx; // ifOperStatus

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
                String errText  = responsePDU.getErrorStatusText();
                int    errIndex = responsePDU.getErrorIndex();
                System.out.println("[SNMP WARN] link id:" + lnk.getID() +
                        " oid index:" + lnk.getOidIndex() +
                        " node:" + lnk.getNodeSnmpSrc().getNodeName() +
                        " SNMP errorStatus=" + errStatus +
                        " (" + errText + "), errorIndex=" + errIndex);
                return false;
            }

            int vbCount = responsePDU.getVariableBindings().size();

            // admin / oper: need at least entries for index 4 and 5
            if (vbCount >= 6) {
                Variable adminVar = responsePDU.getVariableBindings().get(4).getVariable();
                Variable operVar  = responsePDU.getVariableBindings().get(5).getVariable();

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
                Variable hcInVar  = responsePDU.getVariableBindings().get(0).getVariable();
                Variable hcOutVar = responsePDU.getVariableBindings().get(1).getVariable();
                Variable in32Var  = responsePDU.getVariableBindings().get(2).getVariable();
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
     * Helper: true if the variable is an SNMP exception (noSuchObject, noSuchInstance, etc.).
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
