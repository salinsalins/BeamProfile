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
    // Uses superclass logger
    //static final Logger LOGGER = Logger.getLogger(Adam4118.class.getName());
    //static final Logger LOGGER = Logger.getLogger(Adam4118.class.getPackage().getName());
    static public File file;
    static public BufferedReader reader = null;
    static public String line;
    static public String[] columns;
    static public int index;

    Adam4118(File _file) throws FileNotFoundException {
        if (reader == null ) {
            openFile(_file);
            LOGGER.log(Level.FINEST, "Adam4118: " + file.getName() + " created"); 
        } else {
            LOGGER.log(Level.WARNING, "Adam4118: Input file was not closed");
        }
    }
    
    Adam4118(SerialPort _port, int _addr) throws SerialPortException, ADAMException {
        super(_port, _addr);
    }
    
    public static void openFile(File _file) throws FileNotFoundException {
        if (reader == null) {
            if (!_file.canRead())
                throw new FileNotFoundException("File is unreadable");
            file = _file;
            reader = new BufferedReader(new FileReader(file));
            LOGGER.log(Level.FINEST, "Adam4118: File {0} has been opened", file.getName());
        }
    }
    
    public static void openFile(String fileName) throws FileNotFoundException {
        openFile(new File(fileName));
    }
    
    public static void openFile(String filePath, String fileName) throws FileNotFoundException {
        openFile(new File(filePath, fileName));
    }
 
    public static void closeFile() {
        if (reader != null) {
            try {
                reader.close();
                LOGGER.log(Level.FINEST, "Adam4118 input file has been closed");
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Adam4118 input file closing error ", ex);
            }
        } 
        reader = null;
    }

    public String readString() throws ADAMException, FileNotFoundException, IOException {
        if (reader != null) {
            // Reading next line from reader
            if (index <= 0) {
                // Read next line
                line = reader.readLine();
                // Reopen file if EOF
                if (line == null) {
                    closeFile();
                    openFile(file);
                    line = reader.readLine();
                }
                columns = line.split(";");
                // Skip fist value = time
                index = 1;
            }
            StringBuilder result = new StringBuilder("<");
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
                    d = -8888.8;
                }
                str = String.format("%+07f", d);
                str = str.replaceAll(",", ".");
                result.append(str);
            }
            if (index >= 24)
                index = 0;
            return result.toString();
        } 
        else {
            String cmd = String.format("#%02X", addr);
            String resp = readResponse(cmd, ">");
            return ">" + resp;
        }
    }
    
    @Override
    public void delete() {
        closeFile();
        super.delete();
    }
    
    public double[] readData() {
        try {
            return doubleFromString(readString());
        } catch (Exception ex) {
            return new double[8];
        }
    }
}
