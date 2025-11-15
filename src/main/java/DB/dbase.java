/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DB;

import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author nickblame
 */
public class dbase<Type> extends HashMap<String, Type> {

    String name;

    public dbase(String name) {
        super();
        this.name = name;
    }

    public synchronized String addEntry(Type obj) {
        String id = getUniqueId();
        put(id, obj);
        System.out.println("[dbase:" + name + "] Added new entry with ID: " + id);
        return id;
    }

    private synchronized String getUniqueId() {
        String temp;
        Random rand;
        temp = null;
        rand = new Random();

        while (temp == null || containsKey(temp)) {
            temp = Integer.toHexString(rand.nextInt());
        }
        return temp;
    }

    public synchronized Type getEntry(String identifier) {
        return get(identifier);
    }

    public synchronized int getObjectCount() {
        return size();
    }

    public synchronized void deleteEntry(String identifier) {
        if (containsKey(identifier)) {
            remove(identifier);
            System.out.println("[dbase:" + name + "] Removed entry with ID: " + identifier);
        } else {
            System.out.println("[dbase:" + name + "] there is no entry with id: " + identifier);
        }
    }
}
