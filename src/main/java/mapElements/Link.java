/*
 * Link.java
 *
 * Created on 11 ��������� 2006, 7:02 ��
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package mapElements;

import java.awt.*;
import java.awt.geom.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;

public class Link implements Serializable {

    @Getter
    @Setter
    Node nodeSrc, nodeDst, nodeSnmpSrc;

    @Getter
    @Setter
    private String oidIndex = null, interfaceName = null;
    @Getter
    private final String ID;

    @Getter
    @Setter
    private int labelX = 0, labelY = 0, z, labelWidth = 0, linkThickness = 5;
    private final int widthvelaki = 20;

    Font Linkfont = new Font("Courier", Font.PLAIN, 10);
    @Getter
    @Setter
    Long counterInCurrent = null, counterOutCurrent = null, counterInPrevious = null, counterOutPrevious = null, currentTime, previousTime;
    @Getter
    @Setter
    private Boolean adminEnabled = true, operEnabled = true, isSelected = false;

    @Getter
    @Setter
    private Color linkColor = new Color(255, 252, 39);

    @Getter
    @Setter
    Object counterTypeRx = null, counterTypeTx = null;

    /**
     * Creates a new instance of Link
     *
     * @param linkID
     * @param nodeA
     * @param nodeB
     * @param interfaceName
     * @param oidIndex
     * @param z
     * @param nodeSNMP
     */
    public Link(String linkID, Node nodeA, Node nodeB, String interfaceName, String oidIndex, int z, Node nodeSNMP) {
        //by default first node of link is SNMP data source for the link
        nodeSrc = nodeA;
        nodeDst = nodeB;
        this.interfaceName = interfaceName;
        this.z = z;
        this.ID = linkID;
        this.oidIndex = oidIndex;
        this.nodeSnmpSrc = nodeSNMP;
    }

    public void highlight() {
        this.isSelected = true;
    }

    public void lowlight() {
        this.isSelected = false;
    }

    public void decZ() {
        this.z--;
    }

    public String getRx() {
        if (getCurrentTime() != null && getPreviousTime() != null && getCounterInCurrent() != null && getCounterInPrevious() != null && counterTypeRx != null) {
            long max = 0;
            if (counterTypeRx instanceof Counter32) {
                max = 4294967295L;
            }

            Long diffTime = getCurrentTime() - getPreviousTime();
            if (getCounterInCurrent() < getCounterInPrevious()) {
                System.out.println(counterTypeRx.getClass() + " max=" + max + " getCounterInCurrent()=" + getCounterInCurrent() + " getCounterInPrevious()=" + getCounterInPrevious());
                return "" + (8 * (max - getCounterInPrevious() + getCounterInCurrent())) / diffTime;
            } else {
                return "" + (8 * (getCounterInCurrent() - getCounterInPrevious())) / diffTime;
            }
        }
        return null;
    }

    public String getTx() {
        if (getCurrentTime() != null && getPreviousTime() != null && getCounterOutCurrent() != null && getCounterOutPrevious() != null && counterTypeTx != null) {
            long max = 0;
            if (counterTypeTx instanceof Counter32) {
                max = 4294967295L;
            }
            Long diffTime = getCurrentTime() - getPreviousTime();
            if (getCounterOutCurrent() < getCounterOutPrevious()) {
                System.out.println("max=" + max + " getCounterOutCurrent()=" + getCounterOutCurrent() + " getCounterOutPrevious()=" + getCounterOutPrevious());
                return "" + (8 * (max - getCounterOutPrevious() + getCounterOutCurrent())) / diffTime;
            } else {
                return "" + (8 * (getCounterOutCurrent() - getCounterOutPrevious())) / diffTime;
            }
        }
        return null;
    }

    public boolean inBoundsOf(int whatx, int whaty) {
        if (whatx > this.labelX && whatx < this.labelX + labelWidth && whaty > this.labelY && whaty < this.labelY + 27) {
            return true;
        }
        Point2D p1 = new Point2D.Double(nodeSrc.getX() + nodeSrc.getWidth() / 2, nodeSrc.getY() + nodeSrc.getHeight() / 2);
        Point2D p2 = new Point2D.Double(nodeDst.getX() + nodeDst.getWidth() / 2, nodeDst.getY() + nodeDst.getHeight() / 2);
        Point2D userClick = new Point2D.Double(whatx, whaty);
        ArrayList<Point2D> vertices = findRectanglePoints(p1, p2, linkThickness);
        if (isPointInsideRectangle(vertices, userClick)) {
            return true;
        }
        return false;
    }

    public void drawLink(Graphics g) {
        String Rx = getRx(), Tx = getTx();

        g.setFont(Linkfont);
        FontMetrics metrics = g.getFontMetrics();
        if (interfaceName != null && !interfaceName.isEmpty() && oidIndex != null && !oidIndex.isEmpty()) {
            int green = 252;
            int blue = 39;
            if (Rx != null && Tx != null) {
                labelWidth = metrics.stringWidth(RxTxFooter(Rx));
                if (labelWidth < metrics.stringWidth(RxTxFooter(Tx))) {
                    labelWidth = metrics.stringWidth(RxTxFooter(Tx));
                }
                labelWidth += widthvelaki;
                //kokkinisma analoga me to bandwidth  ----------------------
                int traffic = 0;
                if (Integer.parseInt(Rx) > 0) {
                    traffic += Integer.parseInt(Rx);
                }
                if (Integer.parseInt(Tx) > 0) {
                    traffic += Integer.parseInt(Tx);
                }
                if (traffic > 25000) {
                    green = 165;
                }
                if (traffic > 4 * 25000) {
                    green = 105;
                }
                if (traffic > 4 * 4 * 25000) {
                    green = 0;
                    blue = 0;
                }
                //----------------------------------------------------------
            }
            g.setColor(new Color(255, green, blue));
        } else {
            g.setColor(linkColor);
        }

        if (Rx == null) {
            labelWidth = metrics.stringWidth(" \\ ");
        } else if (!adminEnabled) {
            g.setColor(Color.gray);
            labelWidth = metrics.stringWidth(" disabled ");
        } else if (!operEnabled) {
            labelWidth = metrics.stringWidth(" disconnected ");
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(linkThickness));
        if (Rx != null) {
            if (!adminEnabled) {
                g2d.setStroke(new BasicStroke(2));
            }
        }
        int x = nodeSrc.getX() + (nodeSrc.getCX() - nodeSrc.getX()) / 2;
        int y = nodeSrc.getY() + (nodeSrc.getCY() - nodeSrc.getY()) / 2;
        int x2 = nodeDst.getX() + (nodeDst.getCX() - nodeDst.getX()) / 2;
        int y2 = nodeDst.getY() + (nodeDst.getCY() - nodeDst.getY()) / 2;
        g.drawLine(nodeSrc.getCX(), nodeSrc.getCY(), nodeDst.getCX(), nodeDst.getCY());

        if (interfaceName != null && !interfaceName.isEmpty() && oidIndex != null && !oidIndex.isEmpty()) {
            //System.out.println("interfaceName=" + interfaceName);
            //System.out.println("oidIndex=" + oidIndex);
            g2d.setStroke(new BasicStroke(1));

            if (isSelected) {
                g.setColor(Color.GRAY);
            } else {
                g.setColor(Color.white);
            }
            g.fillRect(nodeSrc.getCX() - (labelWidth / 2) + (nodeDst.getCX() - nodeSrc.getCX()) / 2, nodeSrc.getCY() - 15 + (nodeDst.getCY() - nodeSrc.getCY()) / 2, labelWidth, 29);
            g.setColor(Color.black);
            labelX = nodeSrc.getCX() - (labelWidth / 2) + (nodeDst.getCX() - nodeSrc.getCX()) / 2;
            labelY = nodeSrc.getCY() - 15 + (nodeDst.getCY() - nodeSrc.getCY()) / 2;
            g.drawRect(labelX, labelY, labelWidth, 29);
            int rectx = nodeSrc.getCX() - (labelWidth / 2) + 1 + (nodeDst.getCX() - nodeSrc.getCX()) / 2;
            int recty = nodeSrc.getCY() - 4 + (nodeDst.getCY() - nodeSrc.getCY()) / 2;
            //edw typonetai to string pou exei to bandwidth
            if (getCurrentTime() == null || (System.currentTimeMillis() - getCurrentTime()) > 240000) { // the rates are very old, don't display them
                g.drawString(" // ", rectx, recty + 4);
            } else if (getCurrentTime() - getPreviousTime() > 30000L) {
                g.drawString(" .. ", rectx, recty + 4);
            } else {
                if (!adminEnabled) {
                    g.drawString(" disabled ", rectx, recty + 4);
                } else if (!operEnabled) {
                    g.drawString(" disconnected ", rectx, recty + 4);
                } else if (Rx != null && Tx != null) {
                    g.drawString(RxTxFooter(Rx), widthvelaki + rectx, recty);
                    g.setColor(new Color(179, 17, 0));
                    velaki2(g2d, x, y, rectx, recty);
                    g.setColor(new Color(0, 122, 0));
                    velaki2(g2d, x2, y2, rectx, recty + 13);
                    g.setColor(Color.black);
                    g.drawString(RxTxFooter(Tx), widthvelaki + nodeSrc.getCX() - (labelWidth / 2) + 1 + (nodeDst.getCX() - nodeSrc.getCX()) / 2, nodeSrc.getCY() + 11 + (nodeDst.getCY() - nodeSrc.getCY()) / 2);

                }
            }

            //g2d.setStroke(new BasicStroke(3));
            //g2d.setStroke(new BasicStroke(1));
        }

        if (false) { // debug of drawing the links manually so that we can check if the user clicked on a link line
            g2d.setStroke(new BasicStroke(1));
            g.setColor(Color.ORANGE);
            Point2D p1 = new Point2D.Double(nodeSrc.getX() + nodeSrc.getWidth() / 2, nodeSrc.getY() + nodeSrc.getHeight() / 2);
            Point2D p2 = new Point2D.Double(nodeDst.getX() + nodeDst.getWidth() / 2, nodeDst.getY() + nodeDst.getHeight() / 2);
            ArrayList<Point2D> vertices = findRectanglePoints(p1, p2, linkThickness);
            g2d.drawLine((int) vertices.get(0).getX(), (int) vertices.get(0).getY(), (int) vertices.get(1).getX(), (int) vertices.get(1).getY());
            g2d.drawLine((int) vertices.get(1).getX(), (int) vertices.get(1).getY(), (int) vertices.get(2).getX(), (int) vertices.get(2).getY());
            g2d.drawLine((int) vertices.get(2).getX(), (int) vertices.get(2).getY(), (int) vertices.get(3).getX(), (int) vertices.get(3).getY());
            g2d.drawLine((int) vertices.get(3).getX(), (int) vertices.get(3).getY(), (int) vertices.get(0).getX(), (int) vertices.get(0).getY());
        }
    }

    private void velaki2(Graphics2D g2d, int x, int y, int rectx, int recty) {
        Point src, dst;
        src = new Point(x, y);
        dst = new Point(rectx + 10, recty - 3);
        GeneralPath arrow;
        arrow = new GeneralPath();
        arrow.reset();
        double dy = dst.y - src.y;
        double dx = dst.x - src.x;
        double theta = Math.atan2(dy, dx);
        Point2D.Double p0 = new Point2D.Double();
        p0.x = dst.x - 6 * Math.cos(theta);
        p0.y = dst.y - 6 * Math.sin(theta);

        Point2D.Double p1 = new Point2D.Double();
        p1.x = dst.x + 6 * Math.cos(theta);
        p1.y = dst.y + 6 * Math.sin(theta);

        arrow.append(new Line2D.Double(p0, p1), false);

        double xi1 = p0.getX() + 6 * Math.cos(theta - Math.toRadians(20));
        double yi1 = p0.getY() + 6 * Math.sin(theta - Math.toRadians(20));
        arrow.append(new Line2D.Double(p0.getX(), p0.getY(), xi1, yi1), false);
        double xi2 = p0.getX() + 6 * Math.cos(theta + Math.toRadians(20));
        double yi2 = p0.getY() + 6 * Math.sin(theta + Math.toRadians(20));
        arrow.append(new Line2D.Double(p0.getX(), p0.getY(), xi2, yi2), false);

        //Graphics2D g2 = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g2d.setPaint(Color.blue);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(arrow);
    }

    public static String RxTxFooter(String RxTx) {
        int bw = Integer.parseInt(RxTx);
        String out;
        if (bw == -99999) {
            out = " -- ";
        } else if (bw == -99998) {
            out = " ?? ";
        } else if (bw < 0) {
            out = " .. ";
        } else if (bw < 1000) {
            out = RxTx + " kbps ";
        } else if (bw < 100000) {
            int scale = 1;
            BigDecimal num1 = new BigDecimal(bw);
            BigDecimal num2 = new BigDecimal(1000);
            out = num1.divide(num2, scale, RoundingMode.HALF_UP).toString() + " Mbps ";
        } else if (bw < 1000000) {
            int scale = 0;
            BigDecimal num1 = new BigDecimal(bw);
            BigDecimal num2 = new BigDecimal(1000);
            out = num1.divide(num2, scale, RoundingMode.HALF_UP).toString() + " Mbps ";
        } else {
            int scale = 2;
            BigDecimal num1 = new BigDecimal(bw);
            BigDecimal num2 = new BigDecimal(1000000);
            out = num1.divide(num2, scale, RoundingMode.HALF_UP).toString() + " Gbps ";
        }
        return out;
    }

    /**
     * Determines the four points of a rectangle given the centers of two
     * opposite sides and the thickness of the rectangle (which is the length of
     * the sides whose centers are provided).
     *
     * @param center1 The center point of the first side.
     * @param center2 The center point of the opposite side.
     * @param thickness The thickness (length) of the sides whose centers are
     * provided.
     * @return A list of four Point2D objects representing the corners of the
     * rectangle.
     */
    public static ArrayList<Point2D> findRectanglePoints(Point2D center1, Point2D center2, Integer thickness) {
        ArrayList<Point2D> vertices = new ArrayList<>();

        double x1 = center1.getX();
        double y1 = center1.getY();
        double x2 = center2.getX();
        double y2 = center2.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;

        double lengthBetweenCenters = Math.sqrt(dx * dx + dy * dy);

        if (lengthBetweenCenters == 0) {
            System.out.println("The two center points are the same.");
            return vertices;
        }

        // Center of the rectangle
        double centerX = (x1 + x2) / 2.0;
        double centerY = (y1 + y2) / 2.0;
        //Point2D center = new Point2D.Double(centerX, centerY);

        // Unit vector along the line connecting the centers
        double unitVecX = dx / lengthBetweenCenters;
        double unitVecY = dy / lengthBetweenCenters;

        // Perpendicular unit vector
        double perpUnitVecX = -unitVecY;
        double perpUnitVecY = unitVecX;

        // Half-length of the sides
        double halfLength1 = lengthBetweenCenters / 2.0; // Half the length of the sides perpendicular to the line of centers
        double halfLength2 = thickness / 2.0; // Half the length of the sides whose centers are given

        // Calculate the four vertices
        vertices.add(new Point2D.Double(centerX + halfLength1 * unitVecX + halfLength2 * perpUnitVecX,
                centerY + halfLength1 * unitVecY + halfLength2 * perpUnitVecY));
        vertices.add(new Point2D.Double(centerX + halfLength1 * unitVecX - halfLength2 * perpUnitVecX,
                centerY + halfLength1 * unitVecY - halfLength2 * perpUnitVecY));
        vertices.add(new Point2D.Double(centerX - halfLength1 * unitVecX - halfLength2 * perpUnitVecX,
                centerY - halfLength1 * unitVecY - halfLength2 * perpUnitVecY));
        vertices.add(new Point2D.Double(centerX - halfLength1 * unitVecX + halfLength2 * perpUnitVecX,
                centerY - halfLength1 * unitVecY + halfLength2 * perpUnitVecY));

        return vertices;
    }

    public static boolean isPointInsideRectangle(ArrayList<Point2D> rectangleVertices, Point2D pointToCheck) {
        if (rectangleVertices == null || rectangleVertices.size() != 4 || pointToCheck == null) {
            System.err.println("Invalid input: rectangleVertices should have 4 points and pointToCheck should not be null.");
            return false;
        }

        Point2D p = pointToCheck;
        Point2D v1 = rectangleVertices.get(0);
        Point2D v2 = rectangleVertices.get(1);
        Point2D v3 = rectangleVertices.get(2);
        Point2D v4 = rectangleVertices.get(3);

        return isInside(v1, v2, v3, v4, p);
    }

    private static boolean isInside(Point2D a, Point2D b, Point2D c, Point2D d, Point2D p) {
        return sameSide(p, a, b, c) && sameSide(p, b, c, d) && sameSide(p, c, d, a) && sameSide(p, d, a, b);
    }

    private static boolean sameSide(Point2D p1, Point2D a, Point2D b, Point2D c) {
        double cp1 = crossProduct(b.getX() - a.getX(), b.getY() - a.getY(), p1.getX() - a.getX(), p1.getY() - a.getY());
        double cp2 = crossProduct(b.getX() - a.getX(), b.getY() - a.getY(), c.getX() - a.getX(), c.getY() - a.getY());
        return (cp1 >= 0 && cp2 >= 0) || (cp1 <= 0 && cp2 <= 0);
    }

    private static double crossProduct(double x1, double y1, double x2, double y2) {
        return x1 * y2 - y1 * x2;
    }

}
