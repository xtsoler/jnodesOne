package jnodes3clientse;

//import dataGenerator.snmpGetScriptList;
//import dataGenerator.snmpSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import mapElements.Link;
import mapElements.Node;
import message.mapData;

/**
 *
 * @author nickblame
 */
public class mainAnim extends JComponent implements MouseListener, MouseMotionListener, KeyListener {

    private Node[] nodes = null;
    private Link[] links = null;
    //gui funcz
    private int lastclickX = 0, lastclickY = 0, nodeSelected = -1, nodeDragged = -1, nodeOver = -1, newLinkSource = -1, linkHighlighted = -1, linkSelected = -1;
    private static int mx, my;
    private static int mx2, my2;
    private boolean newLink = false, nodeDraggedMultiple = false;
    private final JLabel console;
    private final JLabel ipinf = new JLabel("");
    private final JPopupMenu jPopupMenu1;
    private final JPopupMenu jPopupMenu2;
    private final javax.swing.JMenuItem addLink;
    private final javax.swing.JMenuItem copyIp;
    private final javax.swing.JMenuItem addNode;
    private final javax.swing.JMenuItem clearMap;
    private final javax.swing.JMenuItem delLink;
    private final javax.swing.JMenuItem delNode;
    private final javax.swing.JMenuItem editLink;
    private final javax.swing.JMenuItem editNode;
    private final javax.swing.JMenuItem duplicateNode;
    private final javax.swing.JMenuItem rescanInterfaces;
    private final javax.swing.JMenuItem infoNode;
    private final javax.swing.JMenuItem scriptList;
    private final javax.swing.JMenuItem sendCommandEnable;
    private final javax.swing.JMenuItem sendCommandDisable;
    private final javax.swing.JMenuItem lineManage;
    private mapData map;
    private boolean PosChangesMade = false;
    // Rectangle drawing variables
    private boolean drawingRectangle = false;
    private int rectStartX, rectStartY, rectEndX, rectEndY;
    //----

    public mainAnim(String id, JLabel console) {
        this.console = console;
        setSize(200, 200);
        setPreferredSize(new Dimension(200, 200));
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        setFocusable(true);
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jPopupMenu2 = new javax.swing.JPopupMenu();
        jPopupMenu2.add(ipinf);
        addNode = new javax.swing.JMenuItem();
        delNode = new javax.swing.JMenuItem();
        editNode = new javax.swing.JMenuItem();
        duplicateNode = new javax.swing.JMenuItem();
        rescanInterfaces = new javax.swing.JMenuItem();
        addLink = new javax.swing.JMenuItem();
        copyIp = new javax.swing.JMenuItem();
        delLink = new javax.swing.JMenuItem();
        editLink = new javax.swing.JMenuItem();
        clearMap = new javax.swing.JMenuItem();
        infoNode = new javax.swing.JMenuItem();
        scriptList = new javax.swing.JMenuItem();
        sendCommandEnable = new javax.swing.JMenuItem();
        sendCommandDisable = new javax.swing.JMenuItem();
        lineManage = new javax.swing.JMenuItem();

        copyIp.setText("Copy Ip");
        copyIp.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(copyIp);

        addNode.setText("Add Node");
        addNode.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(addNode);

        delNode.setText("Delete Node");
        delNode.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(delNode);

        editNode.setText("Edit Node");
        editNode.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(editNode);

        duplicateNode.setText("Duplicate Node");
        duplicateNode.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(duplicateNode);

        rescanInterfaces.setText("Rescan interfaces");
        rescanInterfaces.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(rescanInterfaces);

        addLink.setText("Add Link");
        addLink.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(addLink);

        delLink.setText("Delete Link");
        delLink.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(delLink);

        editLink.setText("Edit Link");
        editLink.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(editLink);

        clearMap.setText("Clear Map");
        clearMap.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(clearMap);

        infoNode.setText("Node Info");
        infoNode.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(infoNode);

        scriptList.setText("Script List");
        scriptList.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        jPopupMenu1.add(scriptList);

        sendCommandEnable.setText("Enable");
        sendCommandEnable.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        //jPopupMenu1.add(sendCommandEnable);

        sendCommandDisable.setText("Disable");
        sendCommandDisable.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        //jPopupMenu1.add(sendCommandDisable);

        lineManage.setText("Line Management");
        lineManage.addActionListener((java.awt.event.ActionEvent evt) -> {
            PopUpActionPerformed(evt);
        });
        //jPopupMenu1.add(lineManage);
    }

    public void updateMap(mapData map) {
        if (nodeDragged != -1) {
            System.out.println("update grounded (node dragged=" + nodeDragged + ")");
        } else {
            this.map = map;
            if (map != null) {
                nodes = (map.getNodes() != null) ? map.getNodes() : new Node[0];
                links = (map.getLinks() != null) ? map.getLinks() : new Link[0];
            } else {
                nodes = new Node[0];
                links = new Link[0];
            }
            int maxx = 0;
            int maxwidth = 0;
            int maxy = 0;
            int maxheight = 0;
            if (map != null) {
                if (map.getNodes() != null) {
                    //for (Node node1 : nodes) {
                    for (Node node : nodes) {
                        if (node.getX() > maxx) {
                            maxx = node.getX();
                            maxwidth = node.getWidth();
                        }
                        if (node.getY() > maxy) {
                            maxy = node.getY();
                            maxheight = node.getHeight();
                        }
                    }
                    //}
                }
            }
            setSize(maxx + maxwidth + 100, maxy + maxheight + 100);
            setPreferredSize(new Dimension(maxx + maxwidth + 100, maxy + maxheight + 100));
            repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        drawScene(g);
        //System.out.println("paint called");
    }

    private void drawScene(Graphics g) {
        Dimension d = getSize();
        g.setColor(new Color(191, 191, 191));
        g.fillRect(0, 0, d.width, d.height);
        g.setColor(Color.black);
        //g.drawLine(0, 0, 0, getPreferredSize().height);
        //g.drawLine(0, 0, getPreferredSize().width, 0);
        //g.drawLine(getPreferredSize().width, getPreferredSize().height, 0, getPreferredSize().height);
        //g.drawLine(getPreferredSize().width, getPreferredSize().height, getPreferredSize().width, 0);
        if (map != null) {

            if (nodes != null) {
                if (links != null) {
                    //draw links by first sorting by z
                    List<Link> sortedLinks = new ArrayList<>(Arrays.asList(links));
                    sortedLinks.sort(Comparator.comparingInt(Link::getZ));
                    for (Link link : sortedLinks) {
                        link.drawLink(g);
                    }
                }
                //draw nodes
                for (int i = 0; i < nodes.length; i++) {
                    for (int y = 0; y < nodes.length; y++) {
                        //System.out.println("i:"+i+" z:"+mapNodes[y].getZ());
                        if (nodes[y].getZ() == i) {
                            nodes[y].drawNode(g);

                        }
                    }
                }
                //draw line new komvou
                if (newLink && newLinkSource != -1 && mx2 != -1 && my2 != -1) {
                    g.drawLine(nodes[newLinkSource].getCX(), nodes[newLinkSource].getCY(), mx2, my2);
                }
            }
            //mouse over stous komvous
            if (Main.windowActive) {
                if (!jPopupMenu1.isVisible()) {
                    if (nodeOver != -1) {
                        ipinf.setText(nodes[nodeOver].getIp());
                        if (!ipinf.getText().isEmpty()) {
                            if (!jPopupMenu2.isVisible()) {
                                jPopupMenu2.setVisible(true);
                            }
                            jPopupMenu2.show(this, mx2 + 15, my2 + 20);
                        }
                        //g.setColor(Color.WHITE);
                        //g.fillRect(mx2 + 15, my2 + 20, 50, 40);
                        //g.setColor(Color.BLACK);
                        //g.drawString(nodes[nodeOver].getNodeName(), mx2 + 15, my2 + 20);
                    } else {
                        jPopupMenu2.setVisible(false);
                    }
                } else {
                    nodeOver = -1;
                    jPopupMenu2.setVisible(false);
                }
            } else {
                nodeOver = -1;
                jPopupMenu2.setVisible(false);
            }
            // Draw the rectangle if drawing
            if (drawingRectangle) {
                g.setColor(Color.BLACK);
                g.drawRect(Math.min(rectStartX, rectEndX), Math.min(rectStartY, rectEndY),
                        Math.abs(rectEndX - rectStartX), Math.abs(rectEndY - rectStartY)
                );
            }

        } else {

            g.setColor(Color.WHITE);
            g.drawLine(0, 0, d.width, d.height);
            g.drawString("please wait while loading..", 40, 40);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (Main.authedAdmin) {
            if (!MapViewer.lockMap.isSelected()) {
                int new_mx = e.getX();
                int new_my = e.getY();
                // ensure valid index when using nodes[nodeDragged]
                if (nodes == null) {
                    // still allow rectangle to update visually without touching nodes
                    if (drawingRectangle) {
                        rectEndX = new_mx;
                        rectEndY = new_my;
                        repaint();
                        e.consume();
                    }
                    return;
                }
                if (nodeDragged < -1 || nodeDragged >= nodes.length) {
                    nodeDragged = -1;
                }
                if (nodeDragged != -1) {
                    nodes[nodeDragged].moveX(new_mx - mx);
                    nodes[nodeDragged].moveY(new_my - my);
                    mx = new_mx;
                    my = new_my;
                    nodeOver = -1;
                    repaint();
                    e.consume();
                    PosChangesMade = true;
                    console.setText("Positions successfully updated.");
                } else if (nodeDraggedMultiple) {
                    if (nodes.length > 0) {
                        for (Node n : nodes) {
                            if (n.isHighlighted()) {
                                n.moveX(new_mx - mx);
                                n.moveY(new_my - my);
                            }
                        }
                    }
                    mx = new_mx;
                    my = new_my;
                    nodeOver = -1;
                    repaint();
                    e.consume();
                    PosChangesMade = true;
                    console.setText("Positions successfully updated.");
                } else if (drawingRectangle) {
                    rectEndX = new_mx;
                    rectEndY = new_my;
                    repaint();
                    e.consume();
                    if (nodes.length > 0) {
                        for (Node n : nodes) {
                            n.withinRectangle(rectStartX, rectStartY, rectEndX, rectEndY);
                        }
                    }
                }
            } else {
                console.setText("<html><font color=red>" + "{ERROR}" + "</font> Please uncheck lock map option.</html>");
            }

        } else {
            console.setText("<html><font color=red>" + "{ERROR}" + "</font> You must be admin to modify the map.</html>");
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (map != null) {
            mx2 = e.getX();
            my2 = e.getY();
            if (newLink) {
                repaint();
            } else {
                if (nodeDragged == -1) {
                    if (nodeOver != -1) {
                        nodeOver = -1;
                        repaint();
                    }
                    if (nodes != null) {
                        for (int i = 0; i < nodes.length; i++) {
                            if (nodes[i].inBoundsOf(mx2, my2)) {
                                if (nodeOver != -1) {
                                    if (nodes[i].getZ() > nodes[nodeOver].getZ()) {
                                        nodeOver = i;
                                        repaint();
                                    }
                                } else {
                                    nodeOver = i;
                                    repaint();
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        nodeDragged = -1;
        drawingRectangle = false;
        nodeDraggedMultiple = false;
        mx = e.getX();
        my = e.getY();
        boolean mapElementClicked = false;
        //check if the user clicked on a node
        if (nodes != null) {
            //first check if the user already multi-selected nodes and now they clicked on a preselected node
            boolean clickedSelectedNode = false;
            int selectedNodes = 0;
            for (Node n : nodes) {
                if (n.isHighlighted()) {
                    selectedNodes++;
                    if (n.inBoundsOf(mx, my)) {
                        clickedSelectedNode = true;
                    }
                }
            }
            if (clickedSelectedNode && selectedNodes > 1) {
                nodeDraggedMultiple = true;
            } else {
                for (int i = 0; i < nodes.length; i++) {
                    nodes[i].setHighlighted(false);
                    if (nodes[i].inBoundsOf(mx, my)) {
                        mapElementClicked = true;
                        if (nodeDragged == -1) {
                            nodes[i].setHighlighted(true);
                            nodeSelected = i;
                            //System.out.println("Node coordinates x:" + nodes[i].getX() + " y:" + nodes[i].getY() + " z:" + nodes[i].getZ());
                            nodeDragged = i;
                        } else if (nodes[i].getZ() > nodes[nodeDragged].getZ()) {
                            nodeDragged = i;
                            nodes[i].setHighlighted(true);
                            nodeSelected = nodeDragged;
                        }
                    }
                    for (Node node : nodes) {
                        node.setNewLink(false);
                    }
                    repaint();
                }
            }

        }
        //check if the user clicked on a link
        if (links != null && !nodeDraggedMultiple) {
            for (int i = 0; i < links.length; i++) {
                links[i].lowlight();
                if (nodeDragged == -1) {//(if there's no node in front of the link)
                    if (links[i].inBoundsOf(mx, my)) {
                        mapElementClicked = true;
                        if (linkHighlighted == -1) {
                            links[i].highlight();
                            linkHighlighted = i;
                            //System.out.println("Node coordinates x:" + nodes[i].getX() + " y:" + nodes[i].getY() + " z:" + nodes[i].getZ());
                            linkSelected = i;

                        } else if (links[i].getZ() > links[linkHighlighted].getZ()) {
                            links[linkHighlighted].lowlight();
                            linkSelected = i;
                            linkHighlighted = i;
                            links[linkHighlighted].highlight();

                        }
                        //System.out.println("pressed on a link");
                    }
                }
            }
        }

        //check for new link creation (destination selection)
        if (newLink) {
            nodeDragged = -1;
            console.setText("Link creation started.");
            if (newLinkSource != -1 && nodeSelected != -1) {
                if (nodes[newLinkSource] != nodes[nodeSelected]) {
                    //System.out.println("source:" + nodes[newLinkSource].getNodeName() + " dest:" + nodes[nodeSelected].getNodeName());
                    OptionPaneMultiple opt = new OptionPaneMultiple(this);
                    opt.newLinkEditLink(nodes[newLinkSource], nodes[nodeSelected], null, nodes);
                    if (opt.completed()) {
                        //adlink
                        //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.addLink(map, opt, newLinkSource, nodeSelected)));
                        dataManagement.storage.updateMap(mapEditor.addLink(map, opt, newLinkSource, nodeSelected));
                    }
                }
            }
            newLink = false;
            console.setText("Link creation ended.");
        } else {
            if (!mapElementClicked) {
                // User clicked on blank space, start drawing rectangle
                rectStartX = mx;
                rectStartY = my;
                rectEndX = mx;
                rectEndY = my;
                drawingRectangle = true;
            }
        }
        //bubblesort z gia ta nodes
        if (nodes != null && Main.authedAdmin) {
            if (nodes.length > 0) {
                bublesortNodes(nodeSelected);
            }
        }
        //bubblesort z gia ta links
        if (links != null && Main.authedAdmin) {
            if (links.length > 0) {
                bublesortLinks(linkHighlighted);
            }
        }
        repaint();
        e.consume();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        drawingRectangle = false;
        if (map != null) {
            if (nodeDraggedMultiple) {
                //do nothing in this scenario for now, maybe later a delete selected nodes option can be added here
            } else if (nodeDragged != -1) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    delNode.setVisible(true);
                    editNode.setVisible(true);
                    duplicateNode.setVisible(true);
                    rescanInterfaces.setVisible(true);
                    lineManage.setVisible(true);
                    infoNode.setVisible(true);
                    scriptList.setVisible(true);
                    sendCommandEnable.setVisible(false);
                    sendCommandDisable.setVisible(false);
                    addNode.setVisible(false);
                    addLink.setVisible(true);
                    copyIp.setVisible(true);
                    delLink.setVisible(false);
                    clearMap.setVisible(false);
                    editLink.setVisible(false);
                    jPopupMenu1.show(this, e.getX(), e.getY());
                    nodeSelected = nodeDragged;
                    lastclickX = e.getX();
                    lastclickY = e.getY();

                }
                if (PosChangesMade) {
                    if (Main.authedAdmin) {
                        dataManagement.storage.updateMap(map);
                        PosChangesMade = false;
                    } else {
                        console.setText("<html><font color=red>" + "{ERROR}" + "</font> You must be admin to modify the map.</html>");
                    }
                }
                nodeDragged = -1;
            } else {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    if (linkHighlighted != -1) {
                        delNode.setVisible(false);
                        editNode.setVisible(false);
                        duplicateNode.setVisible(false);
                        rescanInterfaces.setVisible(false);
                        lineManage.setVisible(false);
                        infoNode.setVisible(false);
                        scriptList.setVisible(false);
                        sendCommandEnable.setVisible(true);
                        sendCommandDisable.setVisible(true);
                        addNode.setVisible(false);
                        addLink.setVisible(false);
                        copyIp.setVisible(false);
                        delLink.setVisible(true);
                        editLink.setVisible(true);
                        clearMap.setVisible(false);
                    } else {
                        delNode.setVisible(false);
                        editNode.setVisible(false);
                        duplicateNode.setVisible(false);
                        rescanInterfaces.setVisible(false);
                        lineManage.setVisible(false);
                        infoNode.setVisible(false);
                        scriptList.setVisible(false);
                        sendCommandEnable.setVisible(false);
                        sendCommandDisable.setVisible(false);
                        addNode.setVisible(true);
                        addLink.setVisible(false);
                        copyIp.setVisible(false);
                        delLink.setVisible(false);
                        editLink.setVisible(false);
                        if (nodes != null && nodes.length > 0) {
                            clearMap.setVisible(true);
                        } else {
                            clearMap.setVisible(false);
                        }
                    }
                    jPopupMenu1.show(this, e.getX(), e.getY());

                    lastclickX = e.getX();
                    lastclickY = e.getY();

                }
            }
            if (linkHighlighted != -1) {
                //mapData.get_instance().sendXML(false);
                linkHighlighted = -1;
            }
            repaint();
        }

        e.consume();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    private void bublesortLinks(int pulled) {
        //bubblesort z gia ta links
        if (pulled != -1) {
            for (int b = 0; b < links.length; b++) {
                if (b != pulled) {
                    if (links[b].getZ() > links[pulled].getZ()) {
                        links[b].decZ();
                    }
                }
            }
            links[pulled].setZ(links.length - 1);
        }
    }

    private void bublesortNodes(int pulled) {
        //bubblesort z gia ta links
        if (pulled != -1) {
            for (int b = 0; b < nodes.length; b++) {
                if (b != pulled) {
                    if (nodes[b].getZ() > nodes[pulled].getZ()) {
                        nodes[b].decZ();
                    }
                }
            }
            nodes[pulled].setZ(nodes.length - 1);
        }
    }

    private void PopUpActionPerformed(java.awt.event.ActionEvent evt) {
        if (Main.authedAdmin) {
            nodeDragged = -1;
            if (!MapViewer.lockMap.isSelected()) {
                if (evt.getSource() == addNode) {
                    console.setText("Adding new node...");
                    OptionPaneMultiple opt = new OptionPaneMultiple(this);
                    opt.newNode();
                    if (opt.completed()) {
                        //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.addNode(map, opt, lastclickX, lastclickY)));
                        dataManagement.storage.updateMap(mapEditor.addNode(map, opt, lastclickX, lastclickY));
                        console.setText("Node added successfully.");
                    }
                } else if (evt.getSource() == delNode) {
                    console.setText("Removing node...");
                    //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.delNode(map, nodeSelected)));
                    dataManagement.storage.updateMap(mapEditor.delNode(map, nodeSelected));
                    nodeSelected = -1;
                    console.setText("Node removed.");
                } else if (evt.getSource() == editNode) {
                    console.setText("Editing node...");
                    OptionPaneMultiple opt = new OptionPaneMultiple(this);
                    opt.editNode(nodes[nodeSelected]);
                    if (opt.completed()) {
                        //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.editNode(map, nodeSelected, opt, lastclickX, lastclickY)));
                        dataManagement.storage.updateMap(mapEditor.editNode(map, nodeSelected, opt, nodes[nodeSelected].getX(), nodes[nodeSelected].getY()));
                        console.setText("Node edited successfully.");
                    }
                } else if (evt.getSource() == duplicateNode) {
                    console.setText("Duplicating node...");
                    dataManagement.storage.updateMap(mapEditor.duplicateNode(map, nodeSelected));
                    console.setText("Node edited successfully.");

                } else if (evt.getSource() == clearMap) {
                    console.setText("Clearing map...");
                    //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(new mapData(map.getID())));
                    dataManagement.storage.updateMap(new mapData(map.getID()));
                    nodeSelected = -1;
                    console.setText("map cleared.");
                } else if (evt.getSource() == addLink) {
                    console.setText("Please select destination node");
                    nodes[nodeSelected].setNewLink(true);
                    newLink = true;
                    newLinkSource = nodeSelected;
                } else if (evt.getSource() == delLink) {
                    console.setText("Removing link..");
                    //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.delLink(map, linkSelected)));
                    dataManagement.storage.updateMap(mapEditor.delLink(map, linkSelected));
                    linkSelected = -1;
                    console.setText("Link removed.");
                } else if (evt.getSource() == editLink) {
                    OptionPaneMultiple opt = new OptionPaneMultiple(this);
                    opt.newLinkEditLink(links[linkSelected].getNodeSrc(), links[linkSelected].getNodeDst(), links[linkSelected], nodes);
                    if (opt.completed()) {
                        console.setText("Editing link..");
                        //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.editLink(map, linkSelected, opt)));
                        dataManagement.storage.updateMap(mapEditor.editLink(map, linkSelected, opt));
                        linkSelected = -1;
                        console.setText("Link edited.");
                    }
                }
            } else {
                console.setText("<html><font color=red>" + "{ERROR}" + "</font> Please uncheck lock map option.</html>");
            }

            if (evt.getSource() == sendCommandEnable) {
                //snmpGetScriptList sl = new snmpGetScriptList(links[linkSelected].getNodeSrc().getIp(), links[linkSelected].getNodeSrc().getCommunity());
                //snmpSet.execute(links[linkSelected].getNodeSrc().getIp(), links[linkSelected].getNodeSrc().getCommunity(), sl.findIndex(links[linkSelected].getInterfaceName() + "-enable"), console);
                dataManagement.storage.restartPollersforMap(map);
                //System.out.println(links[linkSelected].getInterfaceName());
                //System.out.println(sl.findIndex(links[linkSelected].getInterfaceName()));
            } else if (evt.getSource() == sendCommandDisable) {
                //snmpGetScriptList sl = new snmpGetScriptList(links[linkSelected].getNodeSrc().getIp(), links[linkSelected].getNodeSrc().getCommunity());
                //snmpSet.execute(links[linkSelected].getNodeSrc().getIp(), links[linkSelected].getNodeSrc().getCommunity(), sl.findIndex(links[linkSelected].getInterfaceName() + "-disable"), console);
                dataManagement.storage.restartPollersforMap(map);
                //System.out.println(links[linkSelected].getInterfaceName());
                //System.out.println(sl.findIndex(links[linkSelected].getInterfaceName()));
            } else if (evt.getSource() == lineManage) {
                console.setText("Line managing...");
                OptionPaneMultiple opt = new OptionPaneMultiple(this);
                opt.linkManagement(nodes[nodeSelected]);
                console.setText("Line management completed");

            } else if (evt.getSource() == infoNode) {
                OptionPaneMultiple opt = new OptionPaneMultiple(this);
                opt.nodeInfo(nodes[nodeSelected]);
            } else if (evt.getSource() == scriptList) {
                OptionPaneMultiple opt = new OptionPaneMultiple(this);
                opt.nodeScripts(nodes[nodeSelected]);
            } else if (evt.getSource() == rescanInterfaces) {
                console.setText("Rescan for Node via SNMP will be performed...");
                //Main.con.sendData((byte) 4, tools.ByteArrayUtils.getSerializedBytes(mapEditor.delNode(map, nodeSelected)));
                nodes[nodeSelected].setIfList(null);
            }
            repaint();
        } else {
            console.setText("<html><font color=red>" + "{ERROR}" + "</font> You must be admin to modify the map.</html>");
        }
        if (evt.getSource() == copyIp) {
            if (!nodes[nodeSelected].getIp().isEmpty()) {
                console.setText("Ip address copied to clipboard");
                StringSelection data = new StringSelection(nodes[nodeSelected].getIp());
                Clipboard clipboard
                        = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(data, data);
            } else {
                console.setText("<html><font color=red>" + "{ERROR}" + "</font> The node selected has no IP specified!</html>");
            }
        }
    }
}
