/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package message;

import mapElements.*;
import java.io.Serializable;

/**
 *
 * @author nickblame
 */
public class mapData implements Serializable {

    private Node[] nodes = null;
    private Link[] links = null;
    private String id = "";

    public mapData(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public void setNodes(Node[] nodes) {
        this.nodes = nodes;
    }

    public void setLinks(Link[] links) {
        this.links = links;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public Link[] getLinks() {
        return links;
    }
    public void debug(){
        System.out.println("[debug] map overview start ---");
        System.out.println("[debug] id="+getID());
        if(nodes!=null){
            for(int i=0;i<nodes.length;i++){
                System.out.println(nodes[i].getNodeName()+" (z="+nodes[i].getZ()+")");
            }
        }
        if(links!=null){
            for(int i=0;i<links.length;i++){
                System.out.println(links[i].getNodeSrc().getNodeName()+"-"+links[i].getNodeDst().getNodeName()+" (z="+links[i].getZ()+")"+" "+links[i].getOidIndex());
            }
        }
        System.out.println("[debug] map overview end ---");
    }
}
