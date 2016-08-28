/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import binp.nbi.beamprofile.BeamProfile.AdamReader;
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
    // Inherits LOGGER from ADAM
    AdamReader reader;

    Adam4118(AdamReader _reader) {
        reader = _reader ;
        LOGGER.log(Level.FINEST, "Adam4118: {0} created", reader.file.getName()); 
    }
    
    Adam4118(SerialPort _port, int _addr) throws SerialPortException, ADAMException {
        super(_port, _addr);
    }

    public String readString() throws ADAMException {
        String resp;
        try {
            resp = reader.readString();
            return resp;
        } catch (Exception ex) {
            String cmd = String.format("#%02X", addr);
            resp = readResponse(cmd, ">");
            return resp;
        }
    }
    
    public double[] readData() {
        try {
            return ADAM.doubleFromString(readString());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Adam4118 response conversion error");
            LOGGER.log(Level.INFO, "Exception info", ex);
            return new double[8];
        }
    }
}
