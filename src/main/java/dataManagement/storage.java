/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataManagement;

import DB.dbase;
import static dataManagement.MapDataJsonConverter.loadMapDataFromJsonFile;
import static dataManagement.MapDataJsonConverter.writeMapDataToJsonFile;
import java.io.File;
import java.util.Iterator;
import message.mapData;

/**
 *
 * @author nickblame
 */
public class storage {

    public static dbase<mapManager> maplist = new dbase<mapManager>("mapBase");

    public static void updateMap(mapData map) {
        maplist.getEntry(map.getID()).updateMap(map);
        //tools.ByteArrayUtils.writeBytes2File(tools.ByteArrayUtils.getSerializedBytes(map), "tmp.mpd");
        writeMapDataToJsonFile(map, "map.json");
    }

    public static void restartPollersforMap(mapData map) {
        maplist.getEntry(map.getID()).resetPing();
        maplist.getEntry(map.getID()).resetSnmp();
        //tools.ByteArrayUtils.writeBytes2File(tools.ByteArrayUtils.getSerializedBytes(map), "tmp.mpd");
    }

    public static void deleteMap(String mapId) {
        mapManager mi = maplist.getEntry(mapId);
        mi.kill();
        maplist.deleteEntry(mapId);
    }

    public static void addMap(String name, String descr, String owner, char[] pass) {
        mapManager m = null;
        String id = maplist.addEntry(m);
        m = new mapManager(id, name, descr, owner, pass);
        m.updateMap(new mapData(id));
        m.start();
        maplist.put(id, m);
    }

    public static boolean addMapFromFile(String filename) {
        boolean succeeded = true;
        message.mapData s = null;
        
        if ((new File(filename)).exists()) {
            //s = (message.mapData) tools.ByteArrayUtils.getObject(tools.ByteArrayUtils.getBytesFromFile("tmp.mpd"));
            s = loadMapDataFromJsonFile(filename);
            if (s != null) {
                char[] p = {'d', 'e', 'm', 'o'};
                mapManager m = null;
                String id = maplist.addEntry(m);
                m = new mapManager(id, "demo", "admin pass=demo", "admin", p);
                mapData map1;
                //s.debug();
                map1 = new mapData(id);
                map1.setNodes(s.getNodes());
                map1.setLinks(s.getLinks());
                //map1.debug();
                m.updateMap(map1);
                m.start();
                maplist.put(id, m);
            } else {
                tools.ModalMsg.display("Invalid or old versioned map file. Map file was cleared.");
                succeeded = false;
                char[] a = {'o', 'k'};
                addMap("asd", "asd", "asd", a);
            }
        } else {
            succeeded = false;
            char[] a = {'o', 'k'};
            addMap("asd", "asd", "asd", a);
        }

        return succeeded;
    }

    public static byte[] getMapList() {
        Iterator keys = maplist.keySet().iterator();
        int i = 0;
        message.mapListData a = new message.mapListData();
        while (keys.hasNext()) {
            mapManager mp = (mapManager) maplist.get((String) keys.next());

            String[] arrayA = {mp.getID(), mp.getMapName(), mp.getDescr(), mp.getOwner()};
            a.addRow(arrayA);

            i++;
        }
        return tools.ByteArrayUtils.getSerializedBytes(a);
    }
}
