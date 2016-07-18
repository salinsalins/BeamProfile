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
        super(comport, addr);
    }
    
    Adam4118(String comport, int addr) {
        super(comport, addr);
    }
    
    public void openFile() {
        if (reader != null) {
            try {
                if (!file.canRead())
                    throw new FileNotFoundException("File is unreadable.");
                reader = new BufferedReader(new FileReader(file));
                System.out.printf("Input file %s has been opened\n", file.getName());
            } catch (FileNotFoundException ex) {
                reader = null;
                Logger.getLogger(Adam4118.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    public void openFile(String fileName) {
        file = new File(fileName);
        openFile();
    }
    public void openFile(String filePath, String fileName) {
        file = new File(filePath, fileName);
        openFile();
    }
 
    public void closeFile() {
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
                str = columns[index++].trim();
                result.append("+");
                result.append(str);
                if (str.length() < 6) 
                {
                    result.append("000000".substring(0,6-str.length()));
                }
            }
            if (index > 24)
                index = 0;
            //pause(0.01);
            return result.toString();
        } 
        else {
            String command = String.format("#%02X", addr);
            String resp = execute(command);
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
