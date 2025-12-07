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

    public static dbase<mapManager> maplist = new dbase<>("mapBase");

    public static mapData getMapDataById(String mapId) {
        mapManager mp = maplist.getEntry(mapId);
        return (mp != null) ? mp.getMapData() : null;
    }

    public static String getFirstMapId() {
        Iterator keys = maplist.keySet().iterator();
        if (keys.hasNext()) {
            return (String) keys.next();
        }
        return null;
    }

    public static void updateMap(mapData map) {
        maplist.getEntry(map.getID()).updateMap(map);
        //tools.ByteArrayUtils.writeBytes2File(tools.ByteArrayUtils.getSerializedBytes(map), "tmp.mpd");
        writeMapDataToJsonFile(map, maplist.getEntry(map.getID()).getFilename());
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

    public static String addMap(String name, String descr, String owner, String filename) {
        mapManager m = null;
        String id = maplist.addEntry(m);
        m = new mapManager(id, name, descr, owner, filename);
        m.updateMap(new mapData(id));
        m.start();
        maplist.put(id, m);
        return id;
    }

    public static String addMapFromFile(String filename) {
        String new_id = null;
        message.mapData s = null;

        if ((new File(filename)).exists()) {
            //s = (message.mapData) tools.ByteArrayUtils.getObject(tools.ByteArrayUtils.getBytesFromFile("tmp.mpd"));
            s = loadMapDataFromJsonFile(filename);
            if (s != null) {
                mapManager m = null;
                String id = maplist.addEntry(m);
                m = new mapManager(id, "demo", "demo map description", "admin", filename);
                mapData map1;
                //s.debug();
                map1 = new mapData(id);
                map1.setNodes(s.getNodes());
                map1.setLinks(s.getLinks());
                //map1.debug();
                m.updateMap(map1);
                m.start();
                maplist.put(id, m);
                new_id = id;
            } else {
                tools.ModalMsg.display("Invalid or old versioned map file. Map file was cleared.");
                new_id = addMap("asd", "asd", "asd", filename);
            }
        } else {
            new_id = addMap("asd", "asd", "asd", filename);
        }

        return new_id;
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
