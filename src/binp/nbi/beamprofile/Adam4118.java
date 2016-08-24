/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author sanin
 */

public class Adam4118 extends ADAM {
    //static final Logger LOGGER = Logger.getLogger(Adam4118.class.getName());
    //static final Logger LOGGER = Logger.getLogger(Adam4118.class.getPackage().getName());
    static public File file;
    static public BufferedReader reader = null;
    static public String line;
    static public String[] columns;
    static public int index;

    Adam4118(SerialPort comport, int _addr) throws SerialPortException, ADAMException {
        if (reader == null ) {
            setPort(comport);
            setAddr(_addr);
            name = readModuleName();
            firmware = readFirmwareVersion();
            LOGGER.log(Level.FINEST, "Adam4118 created at {0} addr:{1}", 
                    new Object[]{comport.getPortName(), _addr});
        } else {
            //port = null;
            //addr = -1;
            //name = "";
            //firmware = "";
            LOGGER.log(Level.FINEST, "Adam4118 created with reading from file");
        }
    }
    
    public static void openFile() throws FileNotFoundException {
        if (reader == null) {
            if (!file.canRead())
                throw new FileNotFoundException("File is unreadable.");
            reader = new BufferedReader(new FileReader(file));
            LOGGER.log(Level.FINE, "File {0} has been opened.", file.getName());
        }
    }
    public static void openFile(String fileName) throws FileNotFoundException {
        file = new File(fileName);
        openFile();
    }
    public static void openFile(String filePath, String fileName) throws FileNotFoundException {
        file = new File(filePath, fileName);
        openFile();
    }
 
    public static void closeFile() {
        if (reader != null) {
            try {
                reader.close();
                LOGGER.log(Level.FINEST, "Adam4118 input file has been closed.");
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Adam4118 input file close error ", ex);
            }
        }
        reader = null;
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
                        reader.reset();
                        LOGGER.log(Level.FINEST, "Reader reset");
                        line = reader.readLine();
                    } catch (IOException ex1) {
                        LOGGER.log(Level.WARNING, "Line read error ", ex1);
                        return "";
                    }
                }
                if (line == null) {
                    try {
                        closeFile();
                        openFile();
                        LOGGER.log(Level.FINE, "Reopen file.");
                        line = reader.readLine();
                    } catch (IOException ex1) {
                        LOGGER.log(Level.WARNING, "Line read error ", ex1);
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
            return result.toString();
        } 
        else {
            String cmd = String.format("#%02X", addr);
            sendCommand(cmd);
            String resp = readResponse();
            if ( resp != null && resp.length()>0 && resp.substring(0,1).equals(">")) {
                LOGGER.log(Level.FINEST, "4118 readString: ", resp);
                return resp;
            }
            //throw new ADAMException("Wrong reading response.");
            LOGGER.log(Level.WARNING, "4118 readString: Wrong response ", resp);
            return "";
        }
    }
    
    @Override
    public void delete() {
        closeFile();
        super.delete();
    }
    
    public double[] readData() {
        return doubleFromString(readString());
    }




}
