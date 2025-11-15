/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 *
 * @author nickblame
 */
public class Logging {

    private static Logger logger;

    public static void log(String input, String filename, String log) {
        if (logger == null) {

            boolean append = true;
            FileHandler fh = null;
            try {
                fh = new FileHandler(filename, append);
            } catch (IOException ex) {
                Logger.getLogger(Logging.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(Logging.class.getName()).log(Level.SEVERE, null, ex);
            }
            //fh.setFormatter(new XMLFormatter());
            fh.setFormatter(new SimpleFormatter());
            logger = Logger.getLogger(log);
            logger.addHandler(fh);

        }

        //logger.severe("my severe message");
        //logger.warning("my warning message");
        logger.info(input);
    }
}
