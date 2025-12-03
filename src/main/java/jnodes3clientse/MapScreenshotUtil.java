package jnodes3clientse;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;

import message.mapData;

public class MapScreenshotUtil {

    public static BufferedImage renderMapToImage(mapData map) {
        if (map == null) {
            throw new IllegalArgumentException("map is null");
        }

        JLabel dummyConsole = new JLabel();
        mainAnim component = new mainAnim("web-snapshot", dummyConsole);

        // φορτώνουμε το map
        component.updateMap(map);

        // χρησιμοποίησε το preferred size που υπολογίζει το mainAnim
        Dimension pref = component.getPreferredSize();
        int w = Math.max(pref.width, 200);
        int h = Math.max(pref.height, 200);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            component.setSize(w, h);
            component.doLayout();
            component.printAll(g2);  // ή component.paint(g2);
        } finally {
            g2.dispose();
        }

        return img;
    }
}
