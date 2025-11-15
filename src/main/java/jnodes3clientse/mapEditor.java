/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jnodes3clientse;

import java.util.Random;
import mapElements.*;

import message.*;

/**
 *
 * @author nickblame
 */
public class mapEditor {
    
    public static mapData addNode(mapData map, OptionPaneMultiple newnode, int x, int y) {
        String temp;
        Random rand;
        
        temp = null;
        rand = new Random();
        mapData newmap = new mapData(map.getID());
        Node[] newnodes;
        if (map.getNodes() != null) {
            while (temp == null || nodeIdExists(temp, map.getNodes())) {
                temp = Integer.toHexString(rand.nextInt());
            }
            newmap.setLinks(map.getLinks());
            newnodes = new Node[map.getNodes().length + 1];
            System.arraycopy(map.getNodes(), 0, newnodes, 0, map.getNodes().length);
            //Node nn = newnode.getSelectionNode()new Node(newnode.getSelection()[3], temp, newnode.getSelection()[0], newnode.getSelection()[1], x, y, 0, newnode.getSelection()[2], false);
            Node nn = newnode.getSelectionNode();
            System.out.println("1-------------------------------------------" + nn.getNodeColor().getRGB());
            nn.setID(temp);
            nn.setX(x);
            nn.setY(y);
            nn.setZ(0);
            newnodes[map.getNodes().length] = nn;
            newmap.setNodes(newnodes);
        } else {
            temp = Integer.toHexString(rand.nextInt());
            newmap.setLinks(map.getLinks());
            newnodes = new Node[1];
            //Node nn = new Node(newnode.getSelection()[3], temp, newnode.getSelection()[0], newnode.getSelection()[1], x, y, 0, newnode.getSelection()[2], false);
            Node nn = newnode.getSelectionNode();
            System.out.println("2-------------------------------------------" + nn.getNodeColor().getRGB());
            nn.setID(temp);
            nn.setX(x);
            nn.setY(y);
            nn.setZ(0);
            newnodes[0] = nn;
            newmap.setNodes(newnodes);
        }
        
        return newmap;
    }
    
    public static mapData editNode(mapData map, int index, OptionPaneMultiple newnode, int x, int y) {
        mapData newmap = new mapData(map.getID());
        
        Node[] newnodes = map.getNodes();
        //String oldId = newnodes[index].getID();
        newnodes[index].setNodeName(newnode.getSelectionNode().getNodeName());
        newnodes[index].setIp(newnode.getSelectionNode().getIp());
        
        newnodes[index].setCommunity(newnode.getSelectionNode().getCommunity());
        newnodes[index].setImagefilename(newnode.getSelectionNode().getImagefilename());
        newnodes[index].setSnmpv3auth(newnode.getSelectionNode().getSnmpv3auth());
        newnodes[index].setSnmpv3priv(newnode.getSelectionNode().getSnmpv3priv());
        newnodes[index].setSnmpv3username(newnode.getSelectionNode().getSnmpv3username());
        newnodes[index].setNodeColor(newnode.getSelectionNode().getNodeColor());
        newnodes[index].setX(x);
        newnodes[index].setY(y);
        newnodes[index].setZ(0);

        //newnodes[index] = newnode.getSelectionNode();
        //newnodes[index].setID(oldId);
        //newnodes[index].setX(x);
        //newnodes[index].setY(y);
        //newnodes[index].setZ(0);
        newmap.setNodes(newnodes);
        newmap.setLinks(map.getLinks());
        return newmap;
    }
    
    public static mapData delNode(mapData map, int index) {
        mapData newmap = new mapData(map.getID());
        //delete all emplekomena links
        if (map.getLinks() != null) {
            for (int i = map.getLinks().length-1; i > -1; i--) {
                if (map.getLinks()[i].getNodeDst().getID().equals(map.getNodes()[index].getID())) {
                    System.out.println("deleting link " + map.getLinks()[i].getID());
                    map = delLink(map, i);
                    
                } else if (map.getLinks()[i].getNodeSrc().getID().equals(map.getNodes()[index].getID())) {
                    System.out.println("deleting link " + map.getLinks()[i].getID());
                    map = delLink(map, i);
                    
                }
            }
            
        }
        
        newmap.setLinks(map.getLinks());
        
        Node[] newnodes = new Node[map.getNodes().length - 1];
        int cnt = 0;
        for (int i = 0; i < map.getNodes().length; i++) {
            if (i != index) {
                newnodes[cnt] = map.getNodes()[i];
                cnt++;
            }
        }
        newmap.setNodes(newnodes);
        return newmap;
    }
    
    public static mapData duplicateNode(mapData map, int index) {
        String temp;
        Random rand;
        
        temp = null;
        rand = new Random();
        mapData newmap = new mapData(map.getID());
        Node[] newnodes;
        
        while (temp == null || nodeIdExists(temp, map.getNodes())) {
            temp = Integer.toHexString(rand.nextInt());
        }
        newmap.setLinks(map.getLinks());
        newnodes = new Node[map.getNodes().length + 1];
        System.arraycopy(map.getNodes(), 0, newnodes, 0, map.getNodes().length);
        Node nn = map.getNodes()[index].createClone();
        nn.setID(temp);
        nn.setX(map.getNodes()[index].getX() + 40);
        nn.setY(map.getNodes()[index].getY() + 40);
        nn.setNodeColor(map.getNodes()[index].getNodeColor());
        newnodes[map.getNodes().length] = nn;
        newmap.setNodes(newnodes);
        
        return newmap;
    }
    
    private static boolean nodeIdExists(String id, Node[] nodes) {
        boolean out = false;
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getID().equals(id)) {
                out = true;
            }
        }
        return out;
    }
    
    public static mapData addLink(mapData map, OptionPaneMultiple newlink, int sourcindex, int destindex) {
        String temp;
        Random rand;
        temp = null;
        rand = new Random();
        mapData newmap = new mapData(map.getID());
        newmap.setNodes(map.getNodes());
        if (map.getLinks() != null) {
            while (temp == null || linkIdExists(temp, map.getLinks())) {
                temp = Integer.toHexString(rand.nextInt());
            }
            
            Link nl;
            nl = new Link(temp, map.getNodes()[sourcindex], map.getNodes()[destindex], newlink.getIfName(), newlink.getOidIndex(), map.getLinks().length, newlink.nodeSNMP);
            //if (newlink.getChoice() == 0) {
            //    nl = new Link(temp, map.getNodes()[sourcindex], map.getNodes()[destindex], newlink.getIfName(), newlink.getOidIndex(), map.getLinks().length);
            //} else {
            //    nl = new Link(temp, map.getNodes()[destindex], map.getNodes()[sourcindex], newlink.getIfName(), newlink.getOidIndex(), map.getLinks().length);
            //}
            Link[] newlinks = new Link[map.getLinks().length + 1];
            System.arraycopy(map.getLinks(), 0, newlinks, 0, map.getLinks().length);
            nl.setLinkThickness(newlink.getThickness());
            nl.setLinkColor(newlink.getColor());
            newlinks[map.getLinks().length] = nl;
            newmap.setLinks(newlinks);
        } else {
            temp = Integer.toHexString(rand.nextInt());
            Link nl;
            nl = new Link(temp, map.getNodes()[sourcindex], map.getNodes()[destindex], newlink.getIfName(), newlink.getOidIndex(), 0, newlink.nodeSNMP);
            //if (newlink.getChoice() == 0) {
            //    nl = new Link(temp, map.getNodes()[sourcindex], map.getNodes()[destindex], newlink.getIfName(), newlink.getOidIndex(), 0);
            //} else {
            //    nl = new Link(temp, map.getNodes()[destindex], map.getNodes()[sourcindex], newlink.getIfName(), newlink.getOidIndex(), 0);
            //}
            Link[] newlinks = new Link[1];
            nl.setLinkThickness(newlink.getThickness());
            nl.setLinkColor(newlink.getColor());
            newlinks[0] = nl;
            newmap.setLinks(newlinks);
        }
        
        return newmap;
    }
    
    public static mapData delLink(mapData map, int index) {
        mapData newmap = new mapData(map.getID());
        newmap.setNodes(map.getNodes());
        //System.out.println("del link index="+index);
        Link[] newlinks = new Link[map.getLinks().length - 1];
        int cnt = 0;
        for (int i = 0; i < map.getLinks().length; i++) {
            if (i != index) {
                newlinks[cnt] = map.getLinks()[i];
                cnt++;
            }
        }
        newmap.setLinks(newlinks);
        return newmap;
    }
    
    public static mapData editLink(mapData map, int index, OptionPaneMultiple newlink) {
        mapData newmap = new mapData(map.getID());
        newmap.setNodes(map.getNodes());
        Link[] newlinks = map.getLinks();
        Link nl = new Link(map.getLinks()[index].getID(), map.getLinks()[index].getNodeSrc(), map.getLinks()[index].getNodeDst(), newlink.getIfName(), newlink.getOidIndex(), map.getLinks()[index].getZ(), newlink.nodeSNMP);
        //if (newlink.getChoice() == 0) {
        //    nl = new Link(map.getLinks()[index].getID(), map.getLinks()[index].getNodeSrc(), map.getLinks()[index].getNodeDst(), newlink.getIfName(), newlink.getOidIndex(), map.getLinks()[index].getZ());
        //} else {
        //    nl = new Link(map.getLinks()[index].getID(), map.getLinks()[index].getNodeDst(), map.getLinks()[index].getNodeSrc(), newlink.getIfName(), newlink.getOidIndex(), map.getLinks()[index].getZ());
        //}
        nl.setLinkThickness(newlink.getThickness());
        nl.setLinkColor(newlink.getColor());
        newlinks[index] = nl;
        newmap.setLinks(newlinks);
        return newmap;
    }
    
    private static boolean linkIdExists(String id, Link[] links) {
        boolean out = false;
        for (int i = 0; i < links.length; i++) {
            if (links[i].getID().equals(id)) {
                out = true;
            }
        }
        return out;
    }
}
