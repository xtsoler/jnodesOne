/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataManagement;

import dataGenerator.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import mapElements.*;
import message.*;

/**
 *
 * @author nickblame
 */
public class mapManager extends Thread {

    private Node[] nodes;
    private Link[] links;
    private String id = "", name = "", descr = "", owner = "", filename = "";
    private boolean alive = true;
    private nodeMaintainer pinger = null;
    private linkMaintainer snmperLink;

    public mapManager(String id, String name, String description, String owner, String filename) {
        super("mapManagerThread(" + id + ")");
        this.id = id;
        this.name = name;
        descr = description;
        this.owner = owner;
        this.filename = filename;
    }

    public String getID() {
        return id;
    }

    public String getMapName() {
        return name;
    }

    public String getDescr() {
        return descr;
    }

    public String getOwner() {
        return owner;
    }
    
    public String getFilename(){
        return filename;
    }
    
    public void updateMap(mapData newmap) {
        
        
        nodes = newmap.getNodes();
        links = newmap.getLinks();

        resetPing();
        //start snmp save thread
        resetSnmp();
    }

    public void resetPing(){
        if (pinger != null) {
            pinger.stop();
            pinger = null;
        }
        if (nodes != null) {
            pinger = new nodeMaintainer(nodes);
            pinger.start();
        }
    }

    public void resetSnmp(){
        if (snmperLink != null) {
            snmperLink.kill();
            snmperLink = null;
        }
        if (links != null) {
            snmperLink = new linkMaintainer(links);
            //snmperLink.start();
        }
    }

    public void setLinks(Link[] links) {
        this.links = links;
    }

    public mapData getMapData() {
        mapData m = new mapData(id);
        m.setLinks(links);
        m.setNodes(nodes);
        return m;
    }

    @Override
    public void run() {
        while (alive) {
            try {
                sleep(1500);
            } catch (InterruptedException ex) {
                Logger.getLogger(mapManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            //userBase.broadcastConnected((byte) 57, id, tools.ByteArrayUtils.getSerializedBytes(getMapData()));
            jnodes3clientse.Main.updateMapMonitor(getMapData());
        }
        System.out.println("mapManager: exitted while()");
    }

    public void kill() {
        alive = false;
        pinger.stop();
        snmperLink.kill();
    }
}
