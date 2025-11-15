/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author Administrator
 */
public class Time {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss:SS";

    public static String now() {
        long timeInMillis = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
}
