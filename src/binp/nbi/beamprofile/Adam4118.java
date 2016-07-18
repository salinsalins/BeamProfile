/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.io.BufferedReader;
import java.io.File;
import jssc.SerialPort;

/**
 *
 * @author sanin
 */
public class Adam4118 extends ADAM {
    File file;
    BufferedReader reader;
    
    Adam4118(SerialPort comport, int addr) 
    {
        super(comport, addr);
    }
    
}
