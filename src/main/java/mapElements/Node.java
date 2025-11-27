package mapElements;

import java.awt.*;
import java.io.Serializable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Christos
 */
public class Node extends JComponent implements Serializable {

    private String nodeID, nodeName, community, imagefilename, snmpv3username, snmpv3auth, snmpv3priv;
    @Getter
    @Setter
    private String salt, snmpv3encr, ip;

    private Integer x, y, z, width, height, strwidth;
    private NodeInterface[] iflist = null;
    @Getter
    @Setter
    private boolean alive = false, newLink = false, highlighted = false;
    Font NodefontBold = new Font("LucidaSansDemiBold", Font.BOLD, 12);
    //Font Nodefont = new Font("LucidaSansDemiBold", Font.PLAIN, 12);
    Font current = NodefontBold;
    private ImageIcon icon = null;

    public static Color defaultNodeColor = Color.gray;

    @Getter
    @Setter
    private Color nodeColor = defaultNodeColor;

    public void setID(String nodeID) {
        this.nodeID = nodeID;
    }

    public Node(String imagefilename, String nodeID, String whatName, String whatIp, Integer whatx, Integer whaty, Integer whatz, String whatcom, boolean isAlive) {
        this.nodeID = nodeID;
        z = whatz;
        this.alive = isAlive;
        x = whatx;
        y = whaty;
        nodeName = whatName;
        ip = whatIp;
        community = whatcom;
        this.imagefilename = imagefilename;
        if (imagefilename != null && !imagefilename.isEmpty()) {
            icon = new ImageIcon("icons/" + imagefilename);
        }
        updateSize();

    }

    public void setX(Integer x) {
        this.x = x;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public void setZ(Integer z) {
        this.z = z;
    }

    private void updateSize() {
        FontMetrics fm = this.getFontMetrics(current);
        strwidth = fm.stringWidth(nodeName);
        if (icon != null) {
            width = icon.getIconWidth();
            height = icon.getIconHeight();
        } else {
            width = 16 + strwidth;
            height = 22;
        }
        setSize(width, height);
    }

    public String getCommunity() {
        return this.community;
    }

    public void setIfList(NodeInterface[] iflist) {
        this.iflist = iflist;
    }

    public NodeInterface[] getIfList() {
        return iflist;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getID() {
        return this.nodeID;
    }

    public void moveX(int distance) {
        this.x += distance;
    }

    public void moveY(int distance) {
        this.y += distance;
    }

    public int getZ() {
        return this.z;
    }

    public int getX() {
        return this.x;
    }

    public int getCX() {
        return this.x + width / 2;
    }

    public int getCY() {
        return this.y + height / 2;
    }

    public int getY() {
        return this.y;
    }

    public String getImagefilename() {
        return imagefilename;
    }

    public void setImagefilename(String imagefilename) {
        icon = null;
        if (imagefilename != null && !imagefilename.isEmpty()) {
            icon = new ImageIcon("icons/" + imagefilename);
        }
        this.imagefilename = imagefilename;
        if (icon != null) {
            width = icon.getIconWidth();
            height = icon.getIconHeight();
        } else {
            width = 16 + strwidth;
            height = 22;
        }
    }

    public String getSnmpv3username() {
        return snmpv3username;
    }

    public void setSnmpv3username(String snmpv3username) {
        this.snmpv3username = snmpv3username;
    }

    public String getSnmpv3auth() {
        return snmpv3auth;
    }

    public void setSnmpv3auth(String snmpv3auth) {
        this.snmpv3auth = snmpv3auth;
    }

    public String getSnmpv3priv() {
        return snmpv3priv;
    }

    public void setSnmpv3priv(String snmpv3priv) {
        this.snmpv3priv = snmpv3priv;
    }

    public void decZ() {
        this.z--;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public boolean inBoundsOf(int whatx, int whaty) {
        boolean reply = false;
        if (whatx > this.x && whatx < this.x + width && whaty > this.y && whaty < this.y + height) {
            reply = true;
        }
        return reply;
    }

    public void withinRectangle(int whatx1, int whaty1, int whatx2, int whaty2) {
        int rectX = Math.min(whatx1, whatx2);
        int rectY = Math.min(whaty1, whaty2);
        int rectWidth = Math.abs(whatx1 - whatx2);
        int rectHeight = Math.abs(whaty1 - whaty2);

        highlighted = this.x < rectX + rectWidth
                && this.x + this.width > rectX
                && this.y < rectY + rectHeight
                && this.y + this.height > rectY;
    }

    public void drawNode(Graphics g) {
        g.setFont(NodefontBold);
        current = NodefontBold;
        updateSize();
        if (icon != null) {
            g.drawImage(icon.getImage(), x, y, null);
            g.setColor(Color.red);
            Color bgColor = new Color(255, 255, 255, 70);
            if (alive) {
                g.setColor(Color.green);
                bgColor = new Color(0, 0, 0, 70);
            }
            if (ip.isEmpty()) {
                g.setColor(nodeColor);
            }
            if (highlighted) {
                g.setFont(NodefontBold);
                g.setColor(Color.blue);
                current = NodefontBold;
                updateSize();
                //int text2Width = metrics.stringWidth(name);
            }
            
            drawTextWithBackground(
                    g,
                    nodeName,
                    x + width / 2 - strwidth / 2,
                    y + height + 10,
                    g.getColor(),
                    bgColor
            );

        } else {
            g.setColor(Color.red);
            if (alive) {
                g.setColor(Color.green);
            }
            if (ip.isEmpty()) {
                g.setColor(nodeColor);
            }
            if (highlighted) {
                g.setColor(Color.blue);
                g.setFont(NodefontBold);
                current = NodefontBold;
                updateSize();
            }
            g.fillRoundRect(x, y, width, height, 10, 10);
            g.setColor(Color.black);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(1));
            g.drawRoundRect(x, y, width, height, 10, 10);
            g.drawString(nodeName, x + 9, y + 16);
        }
    }

    @Override
    public String toString() {
        return nodeName;
    }

    public Node createClone() {
        Node output = new Node(imagefilename, nodeID, nodeName, ip, x, y, z, community, alive);
        output.setSnmpv3auth(snmpv3auth);
        output.setSnmpv3priv(snmpv3priv);
        output.setSnmpv3username(snmpv3username);
        return output;
    }

    private void drawTextWithBackground(Graphics g, String text, int x, int y, Color textColor, Color bgColor) {
        Graphics2D g2 = (Graphics2D) g;

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int ascent = fm.getAscent();

        // Padding around the text
        int paddingX = 4;
        int paddingY = 2;

        int bgX = x - paddingX;
        int bgY = y - ascent;
        int bgWidth = textWidth + paddingX * 2;
        int bgHeight = textHeight + paddingY;

        // Draw semi-transparent background
        g2.setColor(bgColor);
        g2.fillRoundRect(bgX, bgY, bgWidth, bgHeight, 6, 6);

        // Draw border (optional)
        //g2.setColor(new Color(0, 0, 0, 160));
        //g2.drawRoundRect(bgX, bgY, bgWidth, bgHeight, 6, 6);
        // Draw the actual text on the foreground
        g2.setColor(textColor);
        g2.drawString(text, x, y);
    }

}
