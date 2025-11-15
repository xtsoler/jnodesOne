/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * NewNode.java
 *
 * Created on 31 Δεκ 2010, 1:52:12 μμ
 */
package jnodes3clientse;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.DefaultListModel;
import javax.swing.JColorChooser;
import mapElements.Node;

/**
 *
 * @author tsol
 */
public class NodeEditor extends javax.swing.JPanel {

    private final DefaultListModel<String> iconModel = new DefaultListModel<>();

    int selected = -1;
    Node theNode;
    Color selectedColor = Node.defaultNodeColor;

    /**
     * Creates new form NewNode
     */
    public NodeEditor() {
        initComponents();
        iconsList.setModel(iconModel);
        // ensure iconsList exists & is wired to the scrollpane
        if (iconsList == null) {
            iconsList = new javax.swing.JList<>();
            jScrollPane1.setViewportView(iconsList);
        }
        if (!(iconsList.getModel() instanceof DefaultListModel)) {
            iconsList.setModel(new DefaultListModel<String>());
        }
        populateList();
    }

    public NodeEditor(Node node) {
        System.out.println(node.getID());
        initComponents();
        iconsList.setModel(iconModel);
        // ensure iconsList exists & is wired to the scrollpane
        if (iconsList == null) {
            iconsList = new javax.swing.JList<>();
            jScrollPane1.setViewportView(iconsList);
        }
        if (!(iconsList.getModel() instanceof DefaultListModel)) {
            iconsList.setModel(new DefaultListModel<>());
        }
        this.theNode = node;
        selectedColor = node.getNodeColor();

        // prefill the name so it’s not lost on save
        nameField.setText(node.getNodeName());

        jButton1.setBackground(node.getNodeColor());

        if (!node.getIp().isEmpty()) {
            ipCheckBox.setSelected(true);
            ipField.setText(node.getIp());
        }
        if (node.getCommunity() != null && !node.getCommunity().isEmpty()) {
            snmpCommunityField.setText(node.getCommunity());
            snmpCommunityCheckBox.setSelected(true);
            jRadioButton1.setSelected(true);
            jRadioButton2.setSelected(false);
        }
        if (node.getImagefilename() != null && !node.getImagefilename().isEmpty()) {
            iconCheckBox.setSelected(true);
        }
        if (node.getSnmpv3username() != null && !node.getSnmpv3username().isEmpty()) {
            snmpUsernameField.setText(node.getSnmpv3username());
            snmpUsernameCheckBox.setSelected(true);
            jRadioButton2.setSelected(true);
            jRadioButton1.setSelected(false);
            snmpCommunityField.setVisible(false);
            snmpCommunityCheckBox.setVisible(false);
            snmpUsernameField.setVisible(true);
            snmpUsernameCheckBox.setVisible(true);
            //snmpAuthField.setVisible(true);
            snmpAuthFieldObf.setVisible(true);
            snmpAuthCheckBox.setVisible(true);
            //snmpPrivField.setVisible(true);
            snmpPrivFieldObf.setVisible(true);
            snmpPrivCheckBox.setVisible(true);
        }
        if (node.getSnmpv3auth() != null && !node.getSnmpv3auth().isEmpty()) {
            //snmpAuthField.setText(node.getSnmpv3auth());
            snmpAuthFieldObf.setText(node.getSnmpv3auth());
            snmpAuthCheckBox.setSelected(true);
            jRadioButton2.setSelected(true);
            jRadioButton1.setSelected(false);
            snmpCommunityField.setVisible(false);
            snmpCommunityCheckBox.setVisible(false);
            snmpUsernameField.setVisible(true);
            snmpUsernameCheckBox.setVisible(true);
            //snmpAuthField.setVisible(true);
            snmpAuthFieldObf.setVisible(true);
            snmpAuthCheckBox.setVisible(true);
            //snmpPrivField.setVisible(true);
            snmpPrivFieldObf.setVisible(true);
            snmpPrivCheckBox.setVisible(true);
        }
        if (node.getSnmpv3priv() != null && !node.getSnmpv3priv().isEmpty()) {
            //snmpPrivField.setText(node.getSnmpv3priv());
            snmpPrivFieldObf.setText(node.getSnmpv3priv());
            snmpPrivCheckBox.setSelected(true);
            jRadioButton2.setSelected(true);
            jRadioButton1.setSelected(false);
            snmpCommunityField.setVisible(false);
            snmpCommunityCheckBox.setVisible(false);
            snmpUsernameField.setVisible(true);
            snmpUsernameCheckBox.setVisible(true);
            //snmpAuthField.setVisible(true);
            snmpAuthFieldObf.setVisible(true);
            snmpAuthCheckBox.setVisible(true);
            //snmpPrivField.setVisible(true);
            snmpPrivFieldObf.setVisible(true);
            snmpPrivCheckBox.setVisible(true);
        }
        populateList();
        if (selected > -1) {
            iconsList.setEnabled(true);
            iconsList.setSelectedIndex(selected);
        }
    }

    private void populateList() {
        iconModel.clear();
        selected = -1;

        File folder = new File("icons");
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles(new PNGFilter());
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    File f = listOfFiles[i];
                    if (f.isFile()) {
                        String name = f.getName();
                        iconModel.addElement(name);
                        if (theNode != null && name.equals(theNode.getImagefilename())) {
                            selected = i;
                        }
                    }
                }
            }
        }

        if (selected > -1) {
            iconsList.setEnabled(true);
            iconsList.setSelectedIndex(selected);
        }
    }

    public Node getSelection() {
        Node output = null;
        if (jRadioButton1.isSelected()) {
            output = new Node(iconCheckBox.isSelected() ? iconsList.getSelectedValue() : null, null, nameField.getText(), ipField.getText() != null ? ipField.getText() : null, null, null, null, snmpCommunityField.getText() != null ? snmpCommunityField.getText() : null, false);
            output.setSnmpv3username(null);
            output.setSnmpv3auth(null);
            output.setSnmpv3priv(null);
        } else {
            output = new Node(iconCheckBox.isSelected() ? (String) iconsList.getSelectedValue() : null, null, nameField.getText(), ipField.getText() != null ? ipField.getText() : null, null, null, null, null, false);
            output.setSnmpv3username(snmpUsernameField.getText() != null ? snmpUsernameField.getText() : null);
            //output.setSnmpv3auth(snmpAuthField.getText() != null ? snmpAuthField.getText() : null);
            //output.setSnmpv3priv(snmpPrivField.getText() != null ? snmpPrivField.getText() : null);
            if (snmpAuthFieldObf.getPassword() != null && snmpAuthFieldObf.getPassword().length > 0) {
                output.setSnmpv3auth(String.valueOf(snmpAuthFieldObf.getPassword()));
            }
            if (snmpPrivFieldObf.getPassword() != null && snmpPrivFieldObf.getPassword().length > 0) {
                output.setSnmpv3priv(String.valueOf(snmpPrivFieldObf.getPassword()));
            }
        }
        output.setNodeColor(selectedColor);
        return output;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        iconCheckBox = new javax.swing.JCheckBox();
        ipField = new javax.swing.JTextField();
        ipCheckBox = new javax.swing.JCheckBox();
        nameField = new javax.swing.JTextField();
        snmpCommunityField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        iconsList = new javax.swing.JList<>();
        snmpCommunityCheckBox = new javax.swing.JCheckBox();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        snmpUsernameCheckBox = new javax.swing.JCheckBox();
        snmpUsernameField = new javax.swing.JTextField();
        snmpAuthCheckBox = new javax.swing.JCheckBox();
        snmpPrivCheckBox = new javax.swing.JCheckBox();
        snmpAuthFieldObf = new javax.swing.JPasswordField();
        snmpPrivFieldObf = new javax.swing.JPasswordField();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        iconCheckBox.setText("Icon");
        iconCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                iconCheckBoxStateChanged(evt);
            }
        });

        ipField.setEnabled(false);

        ipCheckBox.setText("Ip address");
        ipCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ipCheckBoxStateChanged(evt);
            }
        });

        snmpCommunityField.setEnabled(false);
        snmpCommunityField.setVisible(false);

        jLabel1.setText("Name:");

        iconsList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            @Override
            public int getSize() {
                return strings.length;
            }

            @Override
            public String getElementAt(int i) {
                return strings[i];
            }
        });
        iconsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        iconsList.setEnabled(false);
        jScrollPane1.setViewportView(iconsList);

        snmpCommunityCheckBox.setText("community");
        snmpCommunityCheckBox.setVisible(false);
        snmpCommunityCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                snmpCommunityCheckBoxStateChanged(evt);
            }
        });

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText("SNMPv1/2");
        jRadioButton1.setEnabled(false);
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setSelected(true);
        jRadioButton2.setText("SNMPv3");
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        snmpUsernameCheckBox.setText("username");
        snmpUsernameCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                snmpUsernameCheckBoxStateChanged(evt);
            }
        });

        snmpUsernameField.setEnabled(false);

        snmpAuthCheckBox.setText("auth");
        snmpAuthCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                snmpAuthCheckBoxStateChanged(evt);
            }
        });

        snmpPrivCheckBox.setText("priv");
        snmpPrivCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                snmpPrivCheckBoxStateChanged(evt);
            }
        });

        snmpAuthFieldObf.setEnabled(false);

        snmpPrivFieldObf.setEnabled(false);

        jLabel2.setText("color");

        jButton1.setText("change");
        jButton1.setBackground(Node.defaultNodeColor);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(ipCheckBox)
                                                        .addComponent(jLabel1))
                                                .addGap(63, 63, 63)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(nameField, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                                                        .addComponent(ipField, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)))
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jRadioButton1)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jRadioButton2))
                                                        .addComponent(iconCheckBox))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(snmpCommunityCheckBox)
                                                        .addComponent(snmpUsernameCheckBox)
                                                        .addComponent(snmpAuthCheckBox)
                                                        .addComponent(snmpPrivCheckBox)
                                                        .addComponent(jLabel2))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(snmpCommunityField, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                                                        .addComponent(snmpUsernameField, javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(snmpAuthFieldObf)
                                                        .addComponent(snmpPrivFieldObf))))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(8, 8, 8)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(ipCheckBox)
                                        .addComponent(ipField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jRadioButton1)
                                        .addComponent(jRadioButton2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(snmpCommunityCheckBox)
                                        .addComponent(snmpCommunityField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(snmpUsernameCheckBox)
                                        .addComponent(snmpUsernameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(snmpAuthCheckBox)
                                        .addComponent(snmpAuthFieldObf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(snmpPrivCheckBox)
                                        .addComponent(snmpPrivFieldObf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton1)
                                        .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(iconCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(27, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void iconCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_iconCheckBoxStateChanged
        // TODO add your handling code here:
        iconsList.setEnabled(iconCheckBox.isSelected());
    }//GEN-LAST:event_iconCheckBoxStateChanged

    private void ipCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ipCheckBoxStateChanged
        // TODO add your handling code here:
        ipField.setEnabled(ipCheckBox.isSelected());
        if (!ipCheckBox.isSelected()) {
            ipField.setText("");
        }
    }//GEN-LAST:event_ipCheckBoxStateChanged

    private void snmpCommunityCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpCommunityCheckBoxStateChanged
        // TODO add your handling code here:
        snmpCommunityField.setEnabled(snmpCommunityCheckBox.isSelected());
        if (!snmpCommunityCheckBox.isSelected()) {
            snmpCommunityField.setText("");
        }
    }//GEN-LAST:event_snmpCommunityCheckBoxStateChanged

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
        snmpCommunityCheckBox.setVisible(true);
        snmpCommunityField.setVisible(true);
        snmpUsernameCheckBox.setVisible(false);
        snmpUsernameField.setVisible(false);
        snmpAuthCheckBox.setVisible(false);
        //snmpAuthField.setVisible(false);
        snmpAuthFieldObf.setVisible(false);
        snmpPrivCheckBox.setVisible(false);
        //snmpPrivField.setVisible(false);
        snmpPrivFieldObf.setVisible(false);
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        snmpCommunityCheckBox.setVisible(false);
        snmpCommunityField.setVisible(false);
        snmpUsernameCheckBox.setVisible(true);
        snmpUsernameField.setVisible(true);
        snmpAuthCheckBox.setVisible(true);
        //snmpAuthField.setVisible(true);
        snmpAuthFieldObf.setVisible(true);
        snmpPrivCheckBox.setVisible(true);
        //snmpPrivField.setVisible(true);
        snmpPrivFieldObf.setVisible(true);
    }//GEN-LAST:event_jRadioButton2ActionPerformed

    private void snmpUsernameCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpUsernameCheckBoxStateChanged
        snmpUsernameField.setEnabled(snmpUsernameCheckBox.isSelected());
        if (!snmpUsernameCheckBox.isSelected()) {
            snmpUsernameField.setText("");
        }
    }//GEN-LAST:event_snmpUsernameCheckBoxStateChanged

    private void snmpAuthCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpAuthCheckBoxStateChanged
        snmpAuthFieldObf.setEnabled(snmpAuthCheckBox.isSelected());
        if (!snmpAuthCheckBox.isSelected()) {
            //snmpAuthField.setText("");
            snmpAuthFieldObf.setText("");
        }
    }//GEN-LAST:event_snmpAuthCheckBoxStateChanged

    private void snmpPrivCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpPrivCheckBoxStateChanged
        snmpPrivFieldObf.setEnabled(snmpPrivCheckBox.isSelected());
        if (!snmpPrivCheckBox.isSelected()) {
            //snmpPrivField.setText("");
            snmpPrivFieldObf.setText("");
        }
    }//GEN-LAST:event_snmpPrivCheckBoxStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (theNode != null) {
            selectedColor = JColorChooser.showDialog(null, "Choose a color", theNode.getNodeColor());
        } else {
            selectedColor = JColorChooser.showDialog(null, "Choose a color", selectedColor);
        }
        jButton1.setBackground(selectedColor);
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox iconCheckBox;
    private javax.swing.JList<String> iconsList;
    private javax.swing.JCheckBox ipCheckBox;
    private javax.swing.JTextField ipField;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField nameField;
    private javax.swing.JCheckBox snmpAuthCheckBox;
    private javax.swing.JPasswordField snmpAuthFieldObf;
    private javax.swing.JCheckBox snmpCommunityCheckBox;
    private javax.swing.JTextField snmpCommunityField;
    private javax.swing.JCheckBox snmpPrivCheckBox;
    private javax.swing.JPasswordField snmpPrivFieldObf;
    private javax.swing.JCheckBox snmpUsernameCheckBox;
    private javax.swing.JTextField snmpUsernameField;
    // End of variables declaration//GEN-END:variables
}

class PNGFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return (name.endsWith(".png"));
    }
}
