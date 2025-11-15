/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author nickblame
 */
public class ByteArrayUtils {
    // from evnafets' reply

    public static byte[] intToByteArray(final int integer) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(integer);
            dos.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return bos.toByteArray();
    }

    // My code for inversed process.
    public static int byteArrayToInt(final byte[] byteArray) {
        int tmp = 0;
        try {
            ByteArrayInputStream bos = new ByteArrayInputStream(byteArray);
            DataInputStream dos = new DataInputStream(bos);
            tmp = dos.readInt();

        } catch (IOException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return tmp;
    }

    public static int byteArrayToIntFromIndex(final byte[] byteArray, int start) {
        int temp = 0;
        try {
            byte[] tmp = new byte[4];
            System.arraycopy(byteArray, start, tmp, 0, 4);
            ByteArrayInputStream bos = new ByteArrayInputStream(tmp);
            DataInputStream dos = new DataInputStream(bos);
            temp = dos.readInt();
        } catch (IOException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("bytearraytoint:"+temp);
        return temp;
    }

    public static boolean getBitFromByte(byte thebyte, int a) {
        boolean[] bits = convertByte2Bits(thebyte);
        return bits[a];
    }

    public static byte setBitOnByte(byte thebyte, int a, boolean what) {
        boolean[] bits = convertByte2Bits(thebyte);

        bits[a] = what;

        return convertBits2Byte(bits);
    }

    public static boolean[] convertByte2Bits(byte b) {
        boolean[] bits = new boolean[8];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = ((b & (1 << i)) != 0);
        }
        return bits;
    }

    public static byte convertBits2Byte(boolean[] bits) {
        int value = 0;
        for (int i = 0; i < 8; i++) {
            if (bits[i] == true) {
                value = value | (1 << i);
            }
        }

        return (byte) value;
    }

    public static byte zeroByte(byte thebyte) {

        for (int i = 0; i < 8; i++) {
            thebyte = setBitOnByte(thebyte, i, false);
        }
        return thebyte;
    }

    public static void debugByte(byte opcode) {
        System.out.print("debug byte: ");
        for (int j = 0; j < 8; j++) {
            if (getBitFromByte(opcode, j)) {
                System.out.print("1");
            } else {
                System.out.print("0");
            }

        }
        System.out.println();
    }

    public static byte[] getBytesFromFile(String filename) throws IOException {
        File file=new File(filename);
        InputStream is = new FileInputStream(new File(filename));

        // Get the size of the file
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    public static void writeBytes2File(byte[] bytes, String filename) {
        try {
            FileOutputStream filewriter = new FileOutputStream(filename);
            try {
                filewriter.write(bytes);
                filewriter.close();
            } catch (IOException ex) {
                Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static byte[] mergeByteArrays(byte[] first, byte[] second) {
        byte[] total = new byte[first.length + second.length];
        for (int i = 0; i < first.length; i++) {
            total[i] = first[i];
        }
        for (int i = 0; i < second.length; i++) {
            total[i + first.length] = second[i];
        }
        return total;
    }

    public static String[] mergeStringArrays(String[] first, String[] second) {
        String[] total = new String[first.length + second.length];
        for (int i = 0; i < first.length; i++) {
            total[i] = first[i];
        }
        for (int i = 0; i < second.length; i++) {
            total[i + first.length] = second[i];
        }
        return total;
    }

    public static Serializable getObject(byte[] input) {
        Serializable objj = null;
        //mainAnim p = (mainAnim) TableViewer.p;
        ByteArrayInputStream baiss = new ByteArrayInputStream(input);
        ObjectInputStream oiss = null;
        try {
            oiss = new ObjectInputStream(baiss);
        } catch (IOException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            objj = (Serializable) oiss.readObject();
        } catch (IOException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return objj;
    }

    public static byte[] getSerializedBytes(Serializable mySerializableObj) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
        } catch (IOException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            oos.writeObject(mySerializableObj);
        } catch (IOException ex) {
            Logger.getLogger(ByteArrayUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return baos.toByteArray();
    }

    public static byte[] getPartOfByteArray(byte[] thebyte, int start, int len) {
        byte[] b = new byte[len];
        System.arraycopy(thebyte, start, b, 0, len);
        return b;
    }

    //string byte array tools
    public static byte[] string2byteArrayFixedWidth(String s, int width) {
        byte[] data = new byte[0];
        data = mergeByteArrays(data, intToByteArray(s.length()));
        String str = s;
        for (int i = 0; i < width - s.length(); i++) {
            str = str + "0";
        }
        data = mergeByteArrays(data, str.getBytes());

        return data;
    }

    public static String readStringFromByteFixedWidth(byte[] thebyte, int offset) {
        String s = "";
        byte[] a = {thebyte[offset], thebyte[offset + 1], thebyte[offset + 2], thebyte[offset + 3]};
        int strlength = byteArrayToInt(a);
        byte[] str = new byte[14];
        for (int i = 0; i < 14; i++) {
            str[i] = thebyte[i + offset + 4];
        }
        s = new String(str).substring(0, strlength);

        return s;
    }

    public static boolean compareByteArrays(byte[] a, byte[] b) {
        //returns tru if same, false if different
        if (a != null && b != null) {
            if (a.length == b.length) {
                for (int i = 0; i < a.length; i++) {
                    if (a[i] != b[i]) {
                        return false;
                    }

                }
                return true;
            }
        }
        return false;
    }
}
