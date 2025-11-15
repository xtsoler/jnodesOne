/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package message;

import java.io.Serializable;

/**
 *
 * @author nickblame
 */
public class newMapData implements Serializable{

    private String[] s = new String[4];
    private char[] p;

    public newMapData(String a, String b, String c, char[] p) {
        s[0]=a;
        s[1]=b;
        s[2]=c;
        this.p=p;
    }

    public String[] getData() {
        return s;
    }
    public char[] getPass() {
        return p;
    }
}
