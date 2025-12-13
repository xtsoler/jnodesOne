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

        populateList();
        v3enabled(jRadioButton2.isSelected());

    }

    public NodeEditor(Node node) {
        System.out.println(node.getID());
        initComponents();
        iconsList.setModel(iconModel);

        this.theNode = node;
        selectedColor = node.getNodeColor();

        // prefill the name so it’s not lost on save
        nameField.setText(node.getNodeName());

        jButton1.setBackground(node.getNodeColor());

        if (node.getIp() != null && !node.getIp().isEmpty()) {
            ipCheckBox.setSelected(true);
            ipField.setText(node.getIp());
        }
        if (node.getCommunity() != null && !node.getCommunity().isEmpty()) {
            snmpCommunityField.setText(node.getCommunity());
            snmpCommunityCheckBox.setSelected(true);
            jRadioButton1.setSelected(true);
            jRadioButton2.setSelected(false);
            v3enabled(false);
        }
        if (node.getImagefilename() != null && !node.getImagefilename().isEmpty()) {
            iconCheckBox.setSelected(true);
        }
        if (node.getSnmpv3username() != null && !node.getSnmpv3username().isEmpty()) {
            snmpUsernameField.setText(node.getSnmpv3username());
            snmpUsernameCheckBox.setSelected(true);
            jRadioButton2.setSelected(true);
            jRadioButton1.setSelected(false);
            v3enabled(true);
        }
        if (node.getSnmpv3encr() != null && !node.getSnmpv3encr().isEmpty()) {
            if (node.getSnmpv3encr().equals("DES")) {
                snmpEncr.setSelectedIndex(1);
            }
        }
        if (node.getSnmpv3auth() != null && !node.getSnmpv3auth().isEmpty()) {
            //snmpAuthField.setText(node.getSnmpv3auth());
            snmpAuthFieldObf.setText(node.getSnmpv3auth());
            snmpAuthCheckBox.setSelected(true);
            jRadioButton2.setSelected(true);
            jRadioButton1.setSelected(false);
            v3enabled(true);
        }
        if (node.getSnmpv3priv() != null && !node.getSnmpv3priv().isEmpty()) {
            //snmpPrivField.setText(node.getSnmpv3priv());
            snmpPrivFieldObf.setText(node.getSnmpv3priv());
            snmpPrivCheckBox.setSelected(true);
            jRadioButton2.setSelected(true);
            jRadioButton1.setSelected(false);
            v3enabled(true);
        }
        populateList();
        if (selected > -1) {
            iconsList.setEnabled(true);
            iconsList.setSelectedIndex(selected);
        }

        v3enabled(jRadioButton2.isSelected());

    }

    private void populateList() {
        iconModel.clear();
        selected = -1;

        File folder = new File("icons");
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles(new PNGFilter());
            if (listOfFiles != null) {

                java.util.Arrays.sort(listOfFiles, (a, b)
                        -> a.getName().compareToIgnoreCase(b.getName())
                );

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

        iconsList.setEnabled(iconCheckBox.isSelected());

        if (selected > -1) {
            iconsList.setSelectedIndex(selected);
        } else {
            iconsList.clearSelection();
        }
    }

    public Node getSelection() {
        // --- ICON (fix #2 + #3) ---
        String icon = null;
        if (iconCheckBox.isSelected()) {
            icon = iconsList.getSelectedValue();   // may be null, that's OK
            // If you want to force selection when Icon is checked, you could validate here.
        }

        // --- IP (fix #3) ---
        String ip = "";
        if (ipCheckBox.isSelected()) {
            ip = ipField.getText();
            if (ip == null) {
                ip = "";
            } else {
                ip = ip.trim();
            }
        }

        Node output;

        if (jRadioButton1.isSelected()) {
            // SNMPv1

            String community = null;
            if (snmpCommunityCheckBox.isSelected()) {
                community = snmpCommunityField.getText();
                if (community != null) {
                    community = community.trim();
                    if (community.isEmpty()) {
                        community = null;
                    }
                }
            }

            output = new Node(
                    icon,
                    null,
                    nameField.getText(),
                    ip,
                    null, null, null,
                    community,
                    false
            );

            // Ensure v3 is cleared
            output.setSnmpv3username(null);
            output.setSnmpv3auth(null);
            output.setSnmpv3priv(null);
            output.setSnmpv3encr(null);

        } else {
            // SNMPv3

            String user = null;
            if (snmpUsernameCheckBox.isSelected()) {
                user = snmpUsernameField.getText();
                if (user != null) {
                    user = user.trim();
                    if (user.isEmpty()) {
                        user = null;
                    }
                }
            }

            String auth = null;
            if (snmpAuthCheckBox.isSelected() && snmpAuthFieldObf.getPassword() != null && snmpAuthFieldObf.getPassword().length > 0) {
                auth = String.valueOf(snmpAuthFieldObf.getPassword());
            }

            String priv = null;
            if (snmpPrivCheckBox.isSelected() && snmpPrivFieldObf.getPassword() != null && snmpPrivFieldObf.getPassword().length > 0) {
                priv = String.valueOf(snmpPrivFieldObf.getPassword());
            }

            // Only set encryption if priv is being used (otherwise it’s meaningless)
            String encr = null;
            if (snmpPrivCheckBox.isSelected()) {
                Object sel = snmpEncr.getSelectedItem();
                encr = (sel != null) ? sel.toString() : null;
            }

            output = new Node(
                    icon,
                    null,
                    nameField.getText(),
                    ip,
                    null, null, null,
                    null,
                    false
            );

            output.setSnmpv3username(user);
            output.setSnmpv3auth(auth);
            output.setSnmpv3priv(priv);
            output.setSnmpv3encr(encr);
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
        snmpEncr = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        iconsList = new javax.swing.JList<>();

        iconCheckBox.setText("Icon");
        iconCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                iconCheckBoxStateChanged(evt);
            }
        });

        ipField.setEnabled(false);
        ipField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ipFieldActionPerformed(evt);
            }
        });

        ipCheckBox.setText("Ip address");
        ipCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ipCheckBoxStateChanged(evt);
            }
        });

        snmpCommunityField.setEnabled(false);

        jLabel1.setText("Name:");

        snmpCommunityCheckBox.setText("community");
        snmpCommunityCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                snmpCommunityCheckBoxStateChanged(evt);
            }
        });

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText("SNMPv1");
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

        snmpEncr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AES128", "DES" }));

        iconsList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(iconsList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ipCheckBox)
                            .addComponent(jLabel1))
                        .addGap(63, 63, 63)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameField, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                            .addComponent(ipField, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)))
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
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(snmpPrivFieldObf)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(snmpEncr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jRadioButton1)
                                .addGap(18, 18, 18)
                                .addComponent(jRadioButton2))
                            .addComponent(iconCheckBox))
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                    .addComponent(snmpPrivFieldObf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(snmpEncr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iconCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void iconCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_iconCheckBoxStateChanged
        // TODO add your handling code here:
        iconsList.setEnabled(iconCheckBox.isSelected());
        if (!iconCheckBox.isSelected()) {
            iconsList.clearSelection();
        }
    }//GEN-LAST:event_iconCheckBoxStateChanged

    private void ipCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ipCheckBoxStateChanged
        // TODO add your handling code here:
        syncCheckBoxAndField(ipCheckBox, ipField);
    }//GEN-LAST:event_ipCheckBoxStateChanged

    private void snmpCommunityCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpCommunityCheckBoxStateChanged
        // TODO add your handling code here:
        syncCheckBoxAndField(snmpCommunityCheckBox, snmpCommunityField);
    }//GEN-LAST:event_snmpCommunityCheckBoxStateChanged

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
        v3enabled(false);
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        v3enabled(true);
    }//GEN-LAST:event_jRadioButton2ActionPerformed

    private void snmpUsernameCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpUsernameCheckBoxStateChanged
        syncCheckBoxAndField(snmpUsernameCheckBox, snmpUsernameField);
    }//GEN-LAST:event_snmpUsernameCheckBoxStateChanged

    private void snmpAuthCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpAuthCheckBoxStateChanged
        syncCheckBoxAndField(snmpAuthCheckBox, snmpAuthFieldObf);
    }//GEN-LAST:event_snmpAuthCheckBoxStateChanged

    private void snmpPrivCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_snmpPrivCheckBoxStateChanged
        syncPrivControls();
    }//GEN-LAST:event_snmpPrivCheckBoxStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Color base = (theNode != null) ? theNode.getNodeColor() : selectedColor;
        Color c = JColorChooser.showDialog(null, "Choose a color", base);

        if (c != null) { // user didn't cancel
            selectedColor = c;
            jButton1.setBackground(selectedColor);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void ipFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ipFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ipFieldActionPerformed

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
    private javax.swing.JComboBox<String> snmpEncr;
    private javax.swing.JCheckBox snmpPrivCheckBox;
    private javax.swing.JPasswordField snmpPrivFieldObf;
    private javax.swing.JCheckBox snmpUsernameCheckBox;
    private javax.swing.JTextField snmpUsernameField;
    // End of variables declaration//GEN-END:variables

    private void v3enabled(boolean v3) {

        // --- SNMPv1 ---
        snmpCommunityCheckBox.setEnabled(!v3);
        if (v3) {
            snmpCommunityCheckBox.setSelected(false);
        }
        syncCheckBoxAndField(snmpCommunityCheckBox, snmpCommunityField);

        // --- SNMPv3 ---
        snmpUsernameCheckBox.setEnabled(v3);
        snmpAuthCheckBox.setEnabled(v3);
        snmpPrivCheckBox.setEnabled(v3);

        if (!v3) {
            snmpUsernameCheckBox.setSelected(false);
            snmpAuthCheckBox.setSelected(false);
            snmpPrivCheckBox.setSelected(false);
        }

        syncCheckBoxAndField(snmpUsernameCheckBox, snmpUsernameField);
        syncCheckBoxAndField(snmpAuthCheckBox, snmpAuthFieldObf);
        syncPrivControls();
    }

    private void syncCheckBoxAndField(javax.swing.JCheckBox cb, javax.swing.JComponent field) {
        boolean enabled = cb.isSelected() && cb.isEnabled();
        field.setEnabled(enabled);

        if (!enabled) {
            if (field instanceof javax.swing.JTextField) {
                ((javax.swing.JTextField) field).setText("");
            } else if (field instanceof javax.swing.JPasswordField) {
                ((javax.swing.JPasswordField) field).setText("");
            }
        }
    }

    private void syncPrivControls() {
        boolean enabled = snmpPrivCheckBox.isSelected() && snmpPrivCheckBox.isEnabled();
        snmpPrivFieldObf.setEnabled(enabled);
        snmpEncr.setEnabled(enabled);

        if (!enabled) {
            snmpPrivFieldObf.setText("");
        }
    }

}

class PNGFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String name) {
        return name != null && name.toLowerCase().endsWith(".png");
    }
}
