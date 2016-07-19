/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import static binp.nbi.beamprofile.BeamProfile.logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;

/**
 *
 * @author sanin
 */

public class Adam4118 extends ADAM {
    static public File file;
    static public BufferedReader reader;
    static public String line;
    static public String[] columns;
    static public int index;

    Adam4118(SerialPort comport, int addr) {
        if (reader == null ) {
            try {
                setPort(comport);
                setAddr(addr);

                name = read_name();
                firmware = read_firmware();
                serial = read_serial();

            }
            catch (Exception ex) {
                if (log) {
                    System.out.printf("%s\n", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }
    
    Adam4118(String comport, int addr) {
        if (reader == null ) {
            try {
                setPort(comport);
                setAddr(addr);

                name = read_name();
                firmware = read_firmware();
                serial = read_serial();

            }
            catch (Exception ex) {
                if (log) {
                    System.out.printf("%s\n", ex.getMessage());
                    ex.printStackTrace();
                }

            }
        }
    }
    
    public static void openFile() {
        if (reader == null) {
            try {
                if (!file.canRead())
                    throw new FileNotFoundException("File is unreadable.");
                reader = new BufferedReader(new FileReader(file));
                logger.log(Level.FINE, "File {0} has been opened.", file.getName());
            } catch (FileNotFoundException ex) {
                reader = null;
                logger.log(Level.WARNING, "File {0} not found.", file.getName());
            }
        }
    }
    public static void openFile(String fileName) {
        file = new File(fileName);
        openFile();
    }
    public static void openFile(String filePath, String fileName) {
        file = new File(filePath, fileName);
        openFile();
    }
 
    public static void closeFile() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(Adam4118.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        reader = null;
    }

    public String readString() {
        if (reader != null) {
            if (index <= 0) {
                // Read line from file
                try {
                    line = reader.readLine();
                } catch (IOException ex) {
                    try {
                        reader.reset();
                        line = reader.readLine();
                    } catch (IOException ex1) {
                        return "";
                    }
                }
                if (line == null)
                    return "";

                columns = line.split(";");
                index = 1;
            }
        
            StringBuilder result = new StringBuilder();
            result.append("<");
            String str;
            for (int i = 0; i < 8; i++)
            {
                if (index >= columns.length) 
                    str = "+00.000";
                else
                    str = columns[index].trim();
                index++;
                if (!"+".equals(str.substring(0, 1))) result.append("+");
                result.append(str);
                if (str.length() < 6) 
                {
                    result.append("000000".substring(0,6-str.length()));
                }
            }
            if (index > 24)
                index = 0;
            //pause(0.01);
            logger.log(Level.FINE, "File read: {0}", result.toString());
            return result.toString();
        } 
        else {
            String command = String.format("#%02X", addr);
            String resp = execute(command);
            logger.log(Level.INFO, "4118 Read: ", resp);
            if (resp.substring(0,1).equals(">"))
                return resp;
            //throw new ADAMException("Wrong reading response.");
            return "";
        }
    }
    
    public double[] readData() {
        return doubleFromString(readString());
    }




}
