package jnodes3clientse;

/**
 *
 * @author nickblame
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import lombok.Getter;
import lombok.Setter;
import mapElements.Link;
import mapElements.Node;
import mapElements.NodeInterface;

public class OptionPaneMultiple {

    JTextField input1, input2, input3;
    JPasswordField passwordField;
    private boolean completed = false;

    // Typed arrays
    String[] itemsType = new String[]{"Simple", "SNMP", "SNMP other nodes"};
    String[] thicknessOptions = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

    JLabel labelType = new JLabel("Link type");
    JLabel labelSourceNode = new JLabel("Get SNMP data from:");
    JLabel labelColor = new JLabel("Color:");
    JButton buttonColor = new JButton("change");
    JLabel labelInterface = new JLabel("Interface:");
    JLabel labelThickness = new JLabel("Thickness:");
    JLabel labelSourceSnmpNode = new JLabel("");

    Color selectedColor = new Color(255, 252, 39);

    @Getter
    @Setter
    Node nodeSNMP = null;

    // Typed Swing widgets
    private JComboBox<Node> comboSourceNode = new JComboBox<>();
    private JComboBox<String> comboInterface = new JComboBox<>();
    private JComboBox<String> comboType = new JComboBox<>(itemsType);
    private JComboBox<String> comboThickness = new JComboBox<>(thicknessOptions);

    private NodeInterface[] iflist = {};
    private final Component parentComponent;
    private NodeEditor ne;
    private NodeInfo ni;

    public OptionPaneMultiple(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    // ---- edit node ----
    public void editNode(Node node) {
        ne = new NodeEditor(node);
        Object[] options = {"Ok", "Cancel"};
        JOptionPane op = new JOptionPane(
                ne,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, "Edit node " + node.getNodeName());
        dialog.setVisible(true);
        Object selectedValue = op.getValue();

        if (selectedValue != null && selectedValue.equals("Ok")) {
            completed = true;
        }
    }

    public void linkManagement(Node node) {
        lineManagement ll = new lineManagement(node);
        Object[] options = {"Done"};
        JOptionPane op = new JOptionPane(
                ll,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, "Line Management on " + node.getNodeName());
        dialog.setResizable(true);
        dialog.setVisible(true);
    }

    // ---- node info ----
    public void nodeInfo2(Node node) {
        ni = new NodeInfo(node);
        Object[] options = {"Ok"};
        JOptionPane op = new JOptionPane(
                ni,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, node.getNodeName() + " node details ");
        dialog.setResizable(true);   // <-- allow resizing
        dialog.pack();               // optional: recompute preferred size
        dialog.setVisible(true);
        Object selectedValue = op.getValue();

        if (selectedValue != null && selectedValue.equals("Ok")) {
            completed = true;
        }
    }

    // ---- node scripts (styled like nodeInfo) ----
    public void nodeScripts(Node node) {
        // 1) Gather data (only if SNMPv3 creds exist)
        String[] names = new String[0];
        String[] idx = new String[0];
        String ip = null;

        if (node.getIp() != null && !node.getIp().isEmpty()
                && node.getSnmpv3username() != null && !node.getSnmpv3username().isEmpty()
                && node.getSnmpv3auth() != null && !node.getSnmpv3auth().isEmpty()
                && node.getSnmpv3priv() != null && !node.getSnmpv3priv().isEmpty()) {

            ip = node.getIp();
            String snmpUser = node.getSnmpv3username();
            String authPass = node.getSnmpv3auth();
            String privPass = node.getSnmpv3priv();
            String encr = node.getSnmpv3encr();

            try {
                dataGenerator.snmpGetScriptList sl
                        = new dataGenerator.snmpGetScriptList(ip, snmpUser, authPass, privPass, encr);
                names = sl.getList();
                idx = sl.getIndex();
            } catch (Exception ignore) {
                names = new String[0];
                idx = new String[0];
            }
        }

        // 2) Build UI pieces to match nodeInfo's look
        JTextField tfName = new JTextField(5);
        tfName.setText(node.getNodeName());
        tfName.setEditable(false);

        JTextField tfIp = new JTextField(5);
        tfIp.setText(ip != null ? ip : "");
        tfIp.setEditable(false);

        String[] display = (names == null || names.length == 0)
                ? new String[]{"(no scripts found)"}
                : names;

        final String[] namesRef = names;
        final String[] idxRef = idx;
        final String[] displayRef = display;
        final String ipRef = ip;
        final String userRef = node.getSnmpv3username();
        final String authRef = node.getSnmpv3auth();
        final String encrRef = node.getSnmpv3encr();
        final String privRef = node.getSnmpv3priv();

        JList<String> list = new JList<>(displayRef);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new java.awt.Dimension(260, 160));

        JButton btnRun = new JButton("Run");
        btnRun.setEnabled(false);

        // Enable Run only when a real script is selected
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int sel = list.getSelectedIndex();
                boolean enable
                        = (ipRef != null)
                        && sel >= 0
                        && namesRef != null
                        && idxRef != null
                        && namesRef.length == idxRef.length
                        && sel < idxRef.length
                        && displayRef.length > 0
                        && !displayRef[0].startsWith("(");
                btnRun.setEnabled(enable);
            }
        });

        btnRun.addActionListener(ev -> {
            int sel = list.getSelectedIndex();
            if (sel >= 0 && idxRef != null && sel < idxRef.length) {
                // Run script via SNMPv3
                dataGenerator.snmpSet.execute(ipRef, userRef, authRef, privRef, encrRef, idxRef[sel], null);
                // Optionally give a quick toast-like confirmation
                JOptionPane.showMessageDialog(
                        parentComponent,
                        "Triggered script: " + namesRef[sel],
                        "SNMP",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // 3) Match nodeInfo layout using the same JOptionPane "msg" structure
        Object[] msg = {
            "scripts", scrollPane,
            btnRun
        };
        Object[] options = {"Close"};

        JOptionPane op = new JOptionPane(
                msg,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options
        );
        JDialog dialog = op.createDialog(parentComponent, node.getNodeName() + " node scripts ");
        dialog.setResizable(false); // matches the compact look of nodeInfo
        dialog.setVisible(true);
    }

    private void updateCombos(final Node source, final Node dest, Node[] nodes, int comboTypeSelectedIndex) {
        if (comboType.getSelectedIndex() == 0) { // Simple
            nodeSNMP = null;
            comboSourceNode.setEnabled(false);
            comboSourceNode.setSelectedIndex(-1);
            labelSourceNode.setEnabled(false);

            comboInterface.setEnabled(false);
            comboInterface.setSelectedIndex(-1);
            labelInterface.setEnabled(false);

            labelColor.setEnabled(true);
            buttonColor.setEnabled(true);
        } else { // SNMP and SNMP other nodes
            comboSourceNode.setEnabled(true);
            labelSourceNode.setEnabled(true);
            comboInterface.setEnabled(true);
            labelInterface.setEnabled(true);
            labelColor.setEnabled(false);
            buttonColor.setEnabled(false);
            populateNodeCombo(source, dest, nodes, comboTypeSelectedIndex);
        }
        if (comboType.getSelectedIndex() == 2) {
            labelSourceSnmpNode.setText("Source node (pointed by red arrow): " + source.getNodeName());
        } else {
            labelSourceSnmpNode.setText(" ");
        }
    }

    // ---- new/edit link ----
    public void newLinkEditLink(final Node source, final Node dest, Link oldlink, Node[] nodes) {

        comboType.addItemListener(evt -> {
            if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                updateCombos(source, dest, nodes, comboType.getSelectedIndex());
            }
        });

        comboSourceNode = new JComboBox<>(); // keep typed
        comboSourceNode.addItemListener(evt -> {
            if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                nodeSNMP = (Node) comboSourceNode.getSelectedItem();
                populateInterfaceCombo(source, dest, nodes);
            }
        });

        populateNodeCombo(source, dest, nodes, comboType.getSelectedIndex());
        updateCombos(source, dest, nodes, comboType.getSelectedIndex());
        populateInterfaceCombo(source, dest, nodes);

        buttonColor.setBackground(selectedColor);

        // Edit mode: reflect existing values
        if (oldlink != null) {
            if (!oldlink.getOidIndex().isEmpty()) {
                if (oldlink.getNodeSnmpSrc() != null) {
                    if (oldlink.getNodeSnmpSrc().getID().equals(source.getID())) {
                        comboType.setSelectedIndex(1);
                        populateNodeCombo(source, dest, nodes, comboType.getSelectedIndex());
                        comboSourceNode.setSelectedIndex(0);
                    } else if (oldlink.getNodeSnmpSrc().getID().equals(dest.getID())) {
                        comboType.setSelectedIndex(1);
                        populateNodeCombo(source, dest, nodes, comboType.getSelectedIndex());
                        comboSourceNode.setSelectedIndex(1);
                    } else {
                        comboType.setSelectedIndex(2);
                        populateNodeCombo(source, dest, nodes, comboType.getSelectedIndex());
                        comboSourceNode.setSelectedItem(oldlink.getNodeSnmpSrc());
                    }
                }
                if (iflist != null) {
                    for (int i = 0; i < iflist.length; i++) {
                        if (iflist[i].getIndex().equals(oldlink.getOidIndex())) {
                            comboInterface.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } else {
                comboType.setSelectedIndex(0);
                updateCombos(source, dest, nodes, comboType.getSelectedIndex());
            }
            comboThickness.setSelectedIndex(oldlink.getLinkThickness() - 1);
            selectedColor = oldlink.getLinkColor();
            buttonColor.setBackground(selectedColor);
        } else {
            comboThickness.setSelectedIndex(4);
        }

        buttonColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (oldlink != null && oldlink.getLinkColor() != null) {
                    selectedColor = JColorChooser.showDialog(null, "Choose a color", oldlink.getLinkColor());
                } else {
                    selectedColor = JColorChooser.showDialog(null, "Choose a color", selectedColor);
                }
                buttonColor.setBackground(selectedColor);
            }
        });

        Object[] msg = {
            labelType, comboType,
            labelSourceNode, comboSourceNode,
            labelInterface, comboInterface,
            labelColor, buttonColor,
            labelThickness, comboThickness,
            labelSourceSnmpNode
        };
        Object[] options = {"Ok", "Cancel"};

        JOptionPane op = new JOptionPane(
                msg,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, "New link ");
        dialog.setVisible(true);
        Object selectedValue = op.getValue();
        if (selectedValue != null && selectedValue.equals("Ok")) {
            completed = true;
        }
    }

    // ---- new node ----
    public void newNode() {
        input1 = new JTextField(5);
        input2 = new JTextField(5);
        input3 = new JTextField(5);

        ne = new NodeEditor();
        Object[] options = {"Ok", "Cancel"};
        JOptionPane op = new JOptionPane(
                ne,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, "Create new node");
        dialog.setVisible(true);
        Object selectedValue = op.getValue();

        if (selectedValue != null && selectedValue.equals("Ok")) {
            completed = true;
        }
    }

    // ---- new map ----
    public void newMap() {
        input1 = new JTextField(5);
        input2 = new JTextField(5);
        input3 = new JTextField(5);
        passwordField = new JPasswordField(10);

        Object[] msg = {"Name:", input1, "Description:", input2, "Owner:", input3, "Password", passwordField};
        Object[] options = {"Ok", "Cancel"};

        JOptionPane op = new JOptionPane(
                msg,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, "Create new map");
        dialog.setVisible(true);
        Object selectedValue = op.getValue();

        if (selectedValue != null && selectedValue.equals("Ok")) {
            completed = true;
        }
    }

    public String[] get3Input() {
        return new String[]{input1.getText(), input2.getText(), input3.getText()};
    }

    public Node getSelectionNode() {
        return ne.getSelection();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public int getThickness() {
        return comboThickness.getSelectedIndex() + 1;
    }

    public Color getColor() {
        return selectedColor;
    }

    public String getOidIndex() {
        String oidIndex = "";
        if (comboInterface.getSelectedIndex() != -1 && iflist.length > 0) {
            oidIndex = iflist[comboInterface.getSelectedIndex()].getIndex();
        }
        return oidIndex;
    }

    public String getIfName() {
        String name = "";
        if (comboInterface.getSelectedIndex() != -1 && iflist.length > 0) {
            name = iflist[comboInterface.getSelectedIndex()].getName();
        }
        return name;
    }

    public boolean completed() {
        return completed;
    }

    private void populateInterfaceCombo(Node source, Node dest, Node[] nodes) {
        iflist = null;

        // Figure out which node's interfaces to show
        if (comboType.getSelectedIndex() == 1) {                 // SNMP (source/dest)
            if (comboSourceNode.getSelectedIndex() == 0) {
                iflist = source.getIfList();
            } else {
                iflist = dest.getIfList();
            }
        } else if (comboType.getSelectedIndex() == 2) {          // SNMP other nodes
            Node selected = (Node) comboSourceNode.getSelectedItem();
            if (selected != null) {
                for (Node n : nodes) {
                    if (n.getID().equals(selected.getID())) {
                        iflist = n.getIfList();
                        break;
                    }
                }
            }
        }

        comboInterface.removeAllItems();
        if (iflist != null) {
            for (NodeInterface nif : iflist) {
                comboInterface.addItem(nif.getLabel());
            }
        }
    }

    private void populateNodeCombo(final Node source, final Node dest, Node[] nodes, int comboTypeSelectedIndex) {
        nodeSNMP = null;
        comboSourceNode.removeAllItems();

        if (comboTypeSelectedIndex == 1) {
            comboSourceNode.addItem(source);
            comboSourceNode.addItem(dest);
            nodeSNMP = source;
        } else if (comboTypeSelectedIndex == 2) {
            for (Node n : nodes) {
                if (!n.getID().equals(source.getID()) && !n.getID().equals(dest.getID())) {
                    comboSourceNode.addItem(n);
                    if (nodeSNMP == null) {
                        nodeSNMP = n;
                    }
                }
            }
        }
        populateInterfaceCombo(source, dest, nodes);
    }
}
