package dataGenerator;

/*
 * ping.java
 *
 * Created on 10 ��������� 2006, 11:11 ��
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

import mapElements.*;
import java.net.*;
import java.io.*;
import mapElements.NodeInterface;
//import xmlSettings.mapData;

/**
 *
 * @author Administrator
 */
public class nodeMaintainer implements Runnable {

    private Thread me = null;
    private Node[] nodes;
    private snmpGetIfList[] snmplist = null;

    public nodeMaintainer(Node[] nodes) {
        //new instance of ping
        this.nodes = nodes;
        snmplist = new snmpGetIfList[nodes.length];
    }

    public void start() {
        if (me == null) {
            me = new Thread(this, "me");
            me.start();   // start() method in Thread
        }
    }

    public void stop() {
        me = null;
    }

    @Override
    public void run() {
        Thread myThread = Thread.currentThread();
        //little delay at the boot to allow all loading to cool down
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        int cnt = 0;
        while (me == myThread && nodes.length > 0) {
            System.out.println("pinger run #" + cnt);
            for (Node node : nodes) {
                boolean alive = doping(node.getIp());
                if (node.isAlive() != alive) {
                    node.setAlive(alive);
                    System.out.println("[PING INFO] node " + node.getNodeName() + " state changed. reply to ping:" + alive);
                    //mapData.get_instance().saveSettings(nodes);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                //System.out.println(nodes[i].getNodeName() +" (ip="+nodes[i].getIp()+ ") replies to ping: " + alive);
            }

            //if(cnt>4){
            updateNodeDetails();
            //    cnt=0;
            //}
            //cnt++;

            try {
                Thread.sleep(9000);
            } catch (InterruptedException e) {
            }
            cnt++;
        }
    }

    public boolean doping(String whatIp) {
        if (whatIp.isEmpty()) {
            //System.out.println("no ip specified");
            return false;
        }
        boolean reply = false;
        try {
            InetAddress host = InetAddress.getByName(whatIp);
            reply = host.isReachable(10000);
            System.out.println("[PING INFO] " + whatIp + " ping attempted. reply=" + reply);
        } catch (UnknownHostException e) {
            //System.err.println("Unable to lookup ");
            System.out.println("[PING INFO] " + whatIp + " Unable to lookup ");
            reply = false;
        } catch (IOException e) {
            //System.err.println("Unable to reach ");
            System.out.println("[PING INFO] " + whatIp + " Unable to reach ");
            reply = false;
        }
        return reply;
    }

    private void updateNodeDetails() {
        if (snmplist != null) {
            for (int i = 0; i < snmplist.length; i++) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                //interface list of each node
                if (nodes[i].getIp() != null && !nodes[i].getIp().isEmpty() && 
                        nodes[i].getSnmpv3username() != null && !nodes[i].getSnmpv3username().isEmpty() && 
                        nodes[i].getSnmpv3auth() != null && !nodes[i].getSnmpv3auth().isEmpty() && 
                        nodes[i].getSnmpv3priv() != null && !nodes[i].getSnmpv3priv().isEmpty()
                        && nodes[i].getIfList() == null
                        ) {
                    System.out.println("[SNMP INFO:] checking interfaces of node " + nodes[i].getNodeName() + " - " + nodes[i].getID());
                    snmplist[i] = new snmpGetIfList(nodes[i].getIp(), nodes[i].getSnmpv3username(), nodes[i].getSnmpv3auth(), nodes[i].getSnmpv3priv());

                    String[] tmp = snmplist[i].getList();
                    NodeInterface[] iflist = new NodeInterface[tmp.length];
                    String[] index = snmplist[i].getIndex();
                    for (int j = 0; j < tmp.length; j++) {
                        //System.out.print(index[j]);
                        //System.out.println(" " + tmp[j]);
                        iflist[j] = new NodeInterface(tmp[j], tmp[j] + "(" + index[j] + ")", index[j]);
                    }
                    nodes[i].setIfList(iflist);
                }
            }
        }
    }
}
