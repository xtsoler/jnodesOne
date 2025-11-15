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
public class passwordData implements Serializable {

    private char[] p;

    public passwordData(char[] p) {
        this.p = p;
    }

    public char[] getPass() {
        return p;
    }
}
