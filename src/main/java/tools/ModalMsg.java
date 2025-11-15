/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tools;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author nickblame
 */
public class ModalMsg {
    public static void display(String msg){
        final JDialog a = new JDialog((JFrame) null, "Error");
            a.setModal(true);
            JButton button = new JButton("ok");
            button.setActionCommand("done");
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("done")) {
                        a.dispose();
                    }
                }
            });
            JLabel text = new JLabel(msg);
            JPanel mple = new JPanel();
            mple.add(button);
            a.getContentPane().add(mple, BorderLayout.PAGE_END);
            a.getContentPane().add(text, BorderLayout.PAGE_START);
            a.pack();
            a.setLocationRelativeTo(null);
            a.setVisible(true);
    }
}
