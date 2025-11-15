/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 *
 * @author Administrator
 */
public class TextSize {

    public static int getTextWidthInPixels(Graphics2D g2, String s, Font font) {
        FontMetrics metrics = g2.getFontMetrics(font);
        int adv = metrics.stringWidth(s);
        return adv;
    }

    public static int getTextHeightInPixels(Graphics2D g2, Font font) {
        FontMetrics metrics = g2.getFontMetrics(font);
        int hgt = metrics.getHeight();
        return hgt;
    }

    public static Font createProperFont(Graphics2D g2, String s, int desiredTextWidth,int desiredTextHeight, int PLAINorBOLD, String fontcode) {
        //Font output = null;
        int fontsize = calcFontSize(g2, s, desiredTextWidth,PLAINorBOLD);
        double affY=calcAffineForHeight(g2,desiredTextHeight,PLAINorBOLD,fontcode,fontsize);

        //System.out.println(affY);
        return new Font(fontcode, PLAINorBOLD, fontsize).deriveFont(AffineTransform.getScaleInstance(1.0, affY));
    }
    public static double calcAffineForHeight(Graphics2D g2, int desiredTextHeight, int PLAINorBOLD, String fontcode,int fontsize) {
        //Font output = null;
        double affineY=0.0;
        while(getTextHeightInPixels(g2,fontsize,1.0,affineY+0.05)<desiredTextHeight){
            //System.out.println(getTextHeightInPixels(g2,fontsize,1.0,affineY)<desiredTextHeight);
            affineY+=0.05;
        }

        return affineY;
    }
    private static int getTextWidthInPixels(Graphics2D g2, String s, int fontSize,int plainORbold) {
        FontMetrics metrics = g2.getFontMetrics(new Font("u00e5 = \u00e5", fontSize,plainORbold));
        // get the advance of my text in this font and render context
        int adv = metrics.stringWidth(s);
        return adv;
    }
    private static int getTextHeightInPixels(Graphics2D g2, int fontSize, double scaleX, double scaleY) {
        FontMetrics metrics = g2.getFontMetrics(new Font("u00e5 = \u00e5", Font.PLAIN, fontSize).deriveFont(AffineTransform.getScaleInstance(scaleX, scaleY)));
        // get the height of a line of text in this font and render context
        int hgt = metrics.getHeight();
        return hgt;
    }

    private static int calcFontSize(Graphics2D g2, String s, int desiredTextWidth, int PlainOrBold) {
        int fontSize = 1;
        while (getTextWidthInPixels(g2, s,PlainOrBold , fontSize + 1) < desiredTextWidth) {
            fontSize++;
        }
        return fontSize;
    }
}
