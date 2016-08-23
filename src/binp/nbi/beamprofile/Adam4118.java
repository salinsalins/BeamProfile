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
    static final Logger logger = Logger.getLogger(Adam4118.class.getName());
    static public File file;
    static public BufferedReader reader = null;
    static public String line;
    static public String[] columns;
    static public int index;

    Adam4118(SerialPort comport, int addr) {
        if (reader == null ) {
            try {
                setPort(comport);
                setAddr(addr);

                name = readModuleName();
                firmware = readFirmwareVersion();
                serial = readSerialNumber();
                logger.log(Level.FINE, "Adam4118 created at " + comport + " addr:" + addr);
            }
            catch (Exception ex) {
                logger.log(Level.WARNING, "Adam4118 creation exception ", ex);
                //System.out.printf("%s\n", ex.getMessage());
                //ex.printStackTrace();
            }
        } else {
            logger.log(Level.FINE, "Adam4118 created");
        }
    }
    
    Adam4118(String comport, int addr) {
        if (reader == null ) {
            try {
                setPort(comport);
                setAddr(addr);
                name = readModuleName();
                firmware = readFirmwareVersion();
                serial = readSerialNumber();
                logger.log(Level.FINE, "Adam4118 created at " + comport + " addr:" + addr);
            }
            catch (Exception ex) {
                logger.log(Level.WARNING, "Adam4118 creation exception ", ex);
            }
        } else {
            logger.log(Level.FINE, "Adam4118 created");
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
                logger.log(Level.WARNING, "File {0} not found.", file.getName());
                closeFile();
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
                logger.log(Level.WARNING, "Adam4118 input file close error ", ex);
            }
        }
        reader = null;
        logger.log(Level.FINE, "Adam4118 input file has been closed.");
    }

    public String readString() {
        if (reader != null) {
            // Reading from BufferedReader reader
            if (index <= 0) {
                // Read next line
                try {
                    line = reader.readLine();
                } catch (IOException ex) {
                    try {
                        logger.log(Level.FINE, "Reset reader.");
                        reader.reset();
                        line = reader.readLine();
                    } catch (IOException ex1) {
                        logger.log(Level.INFO, "Line read error.");
                        return "";
                    }
                }
                if (line == null) {
                    try {
                        closeFile();
                        openFile();
                        logger.log(Level.FINE, "Reopen file.");
                        line = reader.readLine();
                    } catch (IOException ex1) {
                        logger.log(Level.INFO, "Line read error.");
                        return "";
                    }
                    if (line == null) 
                        return "";
                }
                columns = line.split(";");
                index = 1;
            }
        
            StringBuilder result = new StringBuilder();
            result.append("<");
            String str;
            for (int i = 0; i < 8; i++)
            {
                if (index >= columns.length) 
                    str = "+000.00";
                else
                    str = columns[index].trim();
                index++;
                
                double d;
                try {
                    d = Double.parseDouble(str);
                }
                catch (NumberFormatException | NullPointerException ex) {
                    d = -888.88;
                }
                str = String.format("%+07.2f", d);
                str = str.replaceAll(",", ".");
                result.append(str);
            }
            if (index >= 24)
                index = 0;
            //pause(0.01);
            //logger.log(Level.FINEST, "File read: {0}", result.toString());
            return result.toString();
        } 
        else {
            String command = String.format("#%02X", addr);
            String resp = execute(command);
            if ( resp != null && resp.length()>0 && resp.substring(0,1).equals(">")) {
                logger.log(Level.INFO, "4118 readString: ", resp);
                return resp;
            }
            //throw new ADAMException("Wrong reading response.");
            logger.log(Level.INFO, "4118 readString: Wrong response ", resp);
            return "";
        }
    }
    
    public double[] readData() {
        return doubleFromString(readString());
    }




}
