/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dataManagement;

import java.awt.Color;
import mapElements.Link;
import mapElements.Node;
import message.mapData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class MapDataJsonConverter {

    public static JSONObject nodeToJson(Node node) {
        JSONObject jsonNode = new JSONObject();
        jsonNode.put("nodeID", node.getID());
        jsonNode.put("nodeName", node.getNodeName());
        jsonNode.put("ip", node.getIp());
        if (node.getNodeColor() != null) {
            jsonNode.put("nodeColor", node.getNodeColor().getRGB());
        } else {
            jsonNode.put("nodeColor", Color.GRAY.getRGB());
        }
        //jsonNode.put("community", node.getCommunity());
        if (node.getImagefilename() != null) {
            jsonNode.put("imagefilename", node.getImagefilename());
        }
        
        if (node.getSnmpv3encr() != null) {
            jsonNode.put("snmpv3encr", node.getSnmpv3encr());
        }
        
        if (node.getSnmpv3username() != null) {
            jsonNode.put("snmpv3username", node.getSnmpv3username());
        }
        if (node.getCommunity() != null) {
            jsonNode.put("community", node.getCommunity());
        }
        if (node.getSalt() != null) {
            jsonNode.put("salt", node.getSalt());
        } else if (jnodes3clientse.Main.encryption_password == null) {
            if (node.getSnmpv3auth() != null) {
                jsonNode.put("snmpv3auth", node.getSnmpv3auth());
            }
            if (node.getSnmpv3priv() != null) {
                jsonNode.put("snmpv3priv", node.getSnmpv3priv());
            }
        }

        if (jnodes3clientse.Main.encryption_password != null) {
            if (node.getSalt() == null && (node.getSnmpv3auth() != null || node.getSnmpv3priv() != null)) { // if there's no salt while there's an encryption password specified.. create one but only if we actually have something to encrypt
                node.setSalt(tools.AESUtil.generateSalt(10).get());
                jsonNode.put("salt", node.getSalt());
            }
            //encrypt first!
            if (node.getSnmpv3auth() != null) {
                jsonNode.put("snmpv3auth", encString(jnodes3clientse.Main.encryption_password, node.getSnmpv3auth(), node.getSalt()));
            }
            if (node.getSnmpv3priv() != null) {
                jsonNode.put("snmpv3priv", encString(jnodes3clientse.Main.encryption_password, node.getSnmpv3priv(), node.getSalt()));
            }
        }

        jsonNode.put("x", node.getX());
        jsonNode.put("y", node.getY());
        jsonNode.put("z", node.getZ());
        // width, height, strwidth are calculated, so might not need to save unless needed for exact reconstruction
        // jsonNode.put("width", node.getWidth());
        // jsonNode.put("height", node.getHeight());
        // jsonNode.put("strwidth", node.getStrwidth());

        return jsonNode;
    }

    private static String encString(String encryption_pass, String input, String salt) {
        String algorithm = "AES/CBC/PKCS5Padding";
        byte[] iv = {'5', 't', 'a', 'y', '%', 'J', '!', '0', '0', 'c', 'V', 'F', '_', '(', '1', '8'};
        SecretKey key = null;
        try {
            key = tools.AESUtil.getKeyFromPassword(encryption_pass, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            java.util.logging.Logger.getLogger(MapDataJsonConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        String encryptedText = null;
        try {
            encryptedText = tools.AESUtil.encrypt(algorithm, input, key, new IvParameterSpec(iv));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException ex) {
            java.util.logging.Logger.getLogger(MapDataJsonConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println(plainText);
        return encryptedText;
    }

    private static String decString(String encryption_pass, String input, String salt) {
        String algorithm = "AES/CBC/PKCS5Padding";
        byte[] iv = {'5', 't', 'a', 'y', '%', 'J', '!', '0', '0', 'c', 'V', 'F', '_', '(', '1', '8'};
        SecretKey key = null;
        try {
            key = tools.AESUtil.getKeyFromPassword(encryption_pass, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            java.util.logging.Logger.getLogger(MapDataJsonConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        String plainText = null;
        try {
            plainText = tools.AESUtil.decrypt(algorithm, input, key, new IvParameterSpec(iv));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException ex) {
            java.util.logging.Logger.getLogger(MapDataJsonConverter.class.getName()).log(Level.SEVERE, null, ex);
            tools.ModalMsg.display("<html> **ERROR** Decryption failed, probably the provided decryption key is wrong </html>");
            System.exit(1);
        }
        //System.out.println(plainText);
        return plainText;
    }

    public static Node nodeFromJson(JSONObject jsonNode) {
        Node node = new Node(
                jsonNode.optString("imagefilename", null),
                jsonNode.getString("nodeID"),
                jsonNode.getString("nodeName"),
                jsonNode.getString("ip"),
                jsonNode.getInt("x"),
                jsonNode.getInt("y"),
                jsonNode.getInt("z"),
                jsonNode.optString("community", null),
                false
        );

        node.setSalt(jsonNode.optString("salt", null));
        node.setSnmpv3username(jsonNode.optString("snmpv3username", null));
        node.setSnmpv3encr(jsonNode.optString("snmpv3encr", null));
        if (node.getSalt() == null) {// no salt on node so we assume all strings are unencrypted
            node.setSnmpv3auth(jsonNode.optString("snmpv3auth", null));
            node.setSnmpv3priv(jsonNode.optString("snmpv3priv", null));
        } else {
            if (jnodes3clientse.Main.encryption_password == null || jnodes3clientse.Main.encryption_password.isEmpty()) {
                tools.ModalMsg.display("<html> **ERROR** No encryption key provided, <br>but map contains encrypted values! <br>Cannot continue, please type provide the key or delete your map.json. </html>");
                System.exit(1);
            }
            // decrypt before loading
            String snmpv3auth = jsonNode.optString("snmpv3auth", null);
            if (snmpv3auth != null) {
                node.setSnmpv3auth(decString(jnodes3clientse.Main.encryption_password, snmpv3auth, node.getSalt()));
                //System.out.println(node.getSnmpv3auth());
            }
            String snmpv3priv = jsonNode.optString("snmpv3priv", null);
            if (snmpv3priv != null) {
                node.setSnmpv3priv(decString(jnodes3clientse.Main.encryption_password, snmpv3priv, node.getSalt()));
            }
        }

        Color nodeColor = new Color(jsonNode.optInt("nodeColor", Node.defaultNodeColor.getRGB()));
        node.setNodeColor(nodeColor);
        // width, height, strwidth are typically calculated, so might not need to set here
        // node.setWidth(jsonNode.optInt("width", 0));
        // node.setHeight(jsonNode.optInt("height", 0));
        // node.setStrwidth(jsonNode.optInt("strwidth", 0));
        return node;
    }

    public static JSONObject linkToJson(Link link) {
        JSONObject jsonLink = new JSONObject();
        jsonLink.put("linkID", link.getID());
        jsonLink.put("nodeSrcID", link.getNodeSrc().getID());
        jsonLink.put("nodeDstID", link.getNodeDst().getID());
        jsonLink.put("interfaceName", link.getInterfaceName());
        jsonLink.put("oidIndex", link.getOidIndex());
        jsonLink.put("z", link.getZ());
        jsonLink.put("linkThickness", link.getLinkThickness());
        jsonLink.put("linkColor", link.getLinkColor().getRGB());

        if (link.getCounterInCurrent() != null) {
            jsonLink.put("counterInCurrent", link.getCounterInCurrent());
        }
        if (link.getCounterOutCurrent() != null) {
            jsonLink.put("counterOutCurrent", link.getCounterOutCurrent());
        }
        if (link.getCounterInPrevious() != null) {
            jsonLink.put("counterInPrevious", link.getCounterInPrevious());
        }
        if (link.getCounterOutPrevious() != null) {
            jsonLink.put("counterOutPrevious", link.getCounterOutPrevious());
        }
        if (link.getCurrentTime() != null) {
            jsonLink.put("currentTime", link.getCurrentTime());
        }
        if (link.getPreviousTime() != null) {
            jsonLink.put("previousTime", link.getPreviousTime());
        }
        if (link.getNodeSnmpSrc() != null) {
            jsonLink.put("nodeSnmpID", link.getNodeSnmpSrc().getID());
        }

        return jsonLink;
    }

    public static Link linkFromJson(JSONObject jsonLink, Map<String, Node> nodeMap) {
        String linkID = jsonLink.getString("linkID");
        String nodeSrcID = jsonLink.getString("nodeSrcID");
        String nodeDstID = jsonLink.getString("nodeDstID");
        String nodeSnmpID = jsonLink.optString("nodeSnmpID", null);
        String interfaceName = jsonLink.getString("interfaceName");
        String oidIndex = jsonLink.getString("oidIndex");
        int linkThickness = jsonLink.optInt("linkThickness", 5);
        Color linkColor = new Color(jsonLink.optInt("linkColor", -985));
        int z = jsonLink.getInt("z");

        Node nodeSrc = nodeMap.get(nodeSrcID);
        Node nodeDst = nodeMap.get(nodeDstID);
        Node nodeSnmp = null;
        if (nodeSnmpID != null) {
            nodeSnmp = nodeMap.get(nodeSnmpID);
        }

        if (nodeSrc == null || nodeDst == null) {
            System.err.println("Error: Could not find nodes for link ID: " + linkID);
            return null; // Or handle the error as appropriate
        }

        Link link = new Link(linkID, nodeSrc, nodeDst, interfaceName, oidIndex, z, nodeSnmp);
        link.setLinkThickness(linkThickness);
        link.setLinkColor(linkColor);
        if (jsonLink.has("counterInCurrent")) {
            link.setCounterInCurrent(jsonLink.getLong("counterInCurrent"));
        }
        if (jsonLink.has("counterOutCurrent")) {
            link.setCounterOutCurrent(jsonLink.getLong("counterOutCurrent"));
        }
        if (jsonLink.has("counterInPrevious")) {
            link.setCounterInPrevious(jsonLink.getLong("counterInPrevious"));
        }
        if (jsonLink.has("counterOutPrevious")) {
            link.setCounterOutPrevious(jsonLink.getLong("counterOutPrevious"));
        }
        if (jsonLink.has("currentTime")) {
            link.setCurrentTime(jsonLink.getLong("currentTime"));
        }
        if (jsonLink.has("previousTime")) {
            link.setPreviousTime(jsonLink.getLong("previousTime"));
        }

        return link;
    }

    public static JSONObject mapDataToJson(mapData data) {
        JSONObject jsonMapData = new JSONObject();
        jsonMapData.put("id", data.getID());

        JSONArray nodesArray = new JSONArray();
        if (data.getNodes() != null) {
            for (Node node : data.getNodes()) {
                nodesArray.put(nodeToJson(node));
            }
        }
        jsonMapData.put("nodes", nodesArray);

        JSONArray linksArray = new JSONArray();
        if (data.getLinks() != null) {
            for (Link link : data.getLinks()) {
                linksArray.put(linkToJson(link));
            }
        }
        jsonMapData.put("links", linksArray);

        return jsonMapData;
    }

    public static mapData mapDataFromJson(JSONObject jsonData) {
        String id = jsonData.getString("id");
        mapData data = new mapData(id);
        JSONArray nodesArray = jsonData.optJSONArray("nodes");
        List<Node> nodesList = new ArrayList<>();
        Map<String, Node> nodeMap = new HashMap<>();
        if (nodesArray != null) {
            for (int i = 0; i < nodesArray.length(); i++) {
                JSONObject jsonNode = nodesArray.getJSONObject(i);
                Node node = nodeFromJson(jsonNode);
                nodesList.add(node);
                nodeMap.put(node.getID(), node);
            }
        }
        data.setNodes(nodesList.toArray(new Node[0]));

        JSONArray linksArray = jsonData.optJSONArray("links");
        List<Link> linksList = new ArrayList<>();
        if (linksArray != null) {
            for (int i = 0; i < linksArray.length(); i++) {
                JSONObject jsonLink = linksArray.getJSONObject(i);
                Link link = linkFromJson(jsonLink, nodeMap);
                if (link != null) {
                    linksList.add(link);
                }
            }
        }
        data.setLinks(linksList.toArray(new Link[0]));

        return data;
    }

    public static void writeMapDataToJsonFile(mapData data, String filePath) {
        try (FileWriter file = new FileWriter(filePath)) {
            JSONObject jsonObject = mapDataToJson(data);
            file.write(jsonObject.toString(4)); // Use toString(4) for pretty printing with indentation
            file.flush();
            System.out.println("Successfully wrote map data to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static mapData loadMapDataFromJsonFile(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONObject jsonObject = new JSONObject(content);
            return mapDataFromJson(jsonObject);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
