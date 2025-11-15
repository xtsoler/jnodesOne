/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package message;

import java.io.Serializable;


/**
 *
 * @author Administrator
 */
public class mapListData extends javax.swing.table.DefaultTableModel implements Serializable {


    public mapListData(){
        super();
        addColumn("id");
        addColumn("name");
        addColumn("description");
        addColumn("owner");
    }




}
