/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

/**
 *
 * @author sanin
 */
public class BeamProfileTest {
    String host = "";
    int count = 0;

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        BeamProfileTest bpt;
        bpt = new BeamProfileTest();

        try {
            if (args.length > 0) {
                bpt.host = args[0];
            }
            if (args.length > 1) {
                bpt.count = Integer.parseInt(args[1]);
            }
        
            bpt.process();
        
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    void process() throws SerialPortException, SerialPortTimeoutException {
        System.out.println("-- Start --");

        String[] ports = SerialPortList.getPortNames();
        String portName = "";

        if (ports.length > 0) {
            portName = ports[0];
            System.out.println("Found ports:");
            for(String port:ports)
                System.out.println(port);
        }
        
        System.out.println("Create " + portName);
        SerialPort newPort = new SerialPort(portName);
        System.out.println("Open " + portName);
	newPort.openPort();
        System.out.println("Set parameters for " + portName);
        newPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        boolean stat = newPort.writeByte((byte)0x0D);
        System.out.println("WriteByte((byte)0x0D) " + stat);

        String str = newPort.readString();
        System.out.println("readString() " + str);

        //byte[] bytes = newPort.readBytes(1, 3000);
        //System.out.println("readBytes(1, 3000) " + bytes.length);
        
        ADAM adam = new ADAM(newPort, 1);
        System.out.println("new ADAM " + adam.name);
        
        System.out.println("-- Finish --");
    }
    
}
