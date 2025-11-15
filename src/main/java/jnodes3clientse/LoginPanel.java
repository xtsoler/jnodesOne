/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jnodes3clientse;

/**
 *
 * @author nickblame
 */
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

/* PasswordDemo.java requires no other files. */
public class LoginPanel extends JPanel
        implements ActionListener {

    private String OK = "ok";
    private String HELP = "help";
    private JLabel label = new JLabel("Enter the password: ");
    private boolean success = false;
    private JDialog controllingDialog = null; //needed for dialogs
    private JPasswordField passwordField;
    private JButton okButton = new JButton("OK");
    private JButton helpButton = new JButton("Help");
    private static boolean response = false, received = false;

    public LoginPanel() {
        //Use the default FlowLayout.
        //Create everything.
        passwordField = new JPasswordField(10);
        passwordField.setActionCommand(OK);
        passwordField.addActionListener(this);
        controllingDialog = new JDialog();
        label.setLabelFor(passwordField);
        JComponent buttonPane = createButtonPanel();
        //Lay out everything.
        JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(passwordField);
        add(textPane);
        add(buttonPane);
    }

    protected JComponent createButtonPanel() {
        JPanel p = new JPanel(new GridLayout(0, 1));
        okButton.setActionCommand(OK);
        helpButton.setActionCommand(HELP);
        okButton.addActionListener(this);
        helpButton.addActionListener(this);
        p.add(okButton);
        p.add(helpButton);
        return p;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (OK.equals(cmd)) { //Process the password.
            char[] input = passwordField.getPassword();
            for (int i = 0; i < input.length; i++) {
                System.out.println(input[i]);
            }
            label.setText("Authanticating please wait...");
            okButton.setEnabled(false);
            helpButton.setEnabled(false);
            passwordField.setEnabled(false);
            //send password to server
            //Main.con.sendData((byte) 99, tools.ByteArrayUtils.getSerializedBytes(new message.passwordData(input)));
            //while (!received) {
             //   try {
              //      Thread.sleep(400);
               // } catch (InterruptedException ex) {
                //    Logger.getLogger(LoginPanel.class.getName()).log(Level.SEVERE, null, ex);
               // }
            //}
            if (checkPass(input)) {
                JOptionPane.showMessageDialog(controllingDialog,
                        "Success! You typed the right password.");
                success = true;
            } else {
                label.setText("Enter the password:");
                okButton.setEnabled(true);
                helpButton.setEnabled(true);
                passwordField.setEnabled(true);
                JOptionPane.showMessageDialog(controllingDialog,
                        "Invalid password. Try again.",
                        "Error Message",
                        JOptionPane.ERROR_MESSAGE);
            }
            received = false;
            //Zero out the possible password, for security.
            Arrays.fill(input, '0');
            passwordField.selectAll();
            resetFocus();
        } else { //The user has asked for help.
            JOptionPane.showMessageDialog(controllingDialog,
                    "Do not spam the login sequence \n"
                    + "Spam will result in ban. \n"
                    + "Your actions are logged.");
        }
    }
    private boolean checkPass(char[] input){
        boolean isCorrect = true;

                //char[] correctPassword = jnodes3server.Main.maplist.getEntry("7bc1d3d1").getPass();
                char[] correctPassword = { 'o', 'k'};

                if (input.length != correctPassword.length) {
                    isCorrect = false;
                } else {
                    isCorrect = Arrays.equals(input, correctPassword);
                }
                return isCorrect;
    }
    public static void setServerResponse(boolean r) {
        response = r;
        received = true;
    }

    /**
     * Checks the passed-in array against the correct password.
     * After this method returns, you should invoke eraseArray
     * on the passed-in array.
     */
    //Must be called from the event dispatch thread.
    protected void resetFocus() {
        if (!success) {
            passwordField.requestFocusInWindow();
        } else {
            controllingDialog.dispose();
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    public boolean createAndShowGUI() {
        success = false;
        received = false;
        //Create and set up the window.

        controllingDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        controllingDialog.setModal(true);
        controllingDialog.setLocationRelativeTo(null);
        setOpaque(true); //content panes must be opaque
        controllingDialog.setContentPane(this);

        //Make sure the focus goes to the right component
        //whenever the frame is initially given the focus.
        controllingDialog.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                resetFocus();
            }
        });
        //Display the window.
        controllingDialog.pack();
        controllingDialog.setVisible(true);
        return success;
    }
}
