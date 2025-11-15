/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapElements;

import java.io.Serializable;

/**
 *
 * @author nickblame
 */
public class NodeInterface implements Serializable {

    private String name, index, label;

    public NodeInterface(String name,String label, String index) {
        this.name = name;
        this.index = index;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getIndex() {
        return index;
    }

    public String correspondingOidRx() {
        return ".1.3.6.1.2.1.2.2.1.10." + index;
    }

    public String correspondingOidTx() {
        return ".1.3.6.1.2.1.2.2.1.16." + index;
    }
}
