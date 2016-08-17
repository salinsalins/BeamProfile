/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.util.logging.Logger;
import jssc.SerialPort;
import static jssc.SerialPort.FLOWCONTROL_NONE;
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
//    static final Logger logger = Logger.getLogger(BeamProfile.class.getPackage().getName());

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
        try {            
            String[] ports = SerialPortList.getPortNames();
            String portName = "COM12";
            boolean exists = false;
            
            if (ports.length > 0) {
                System.out.println("Found ports:");
                for (String port : ports) {
                    System.out.print(port);
                    if (port.equals(portName)) {
                        System.out.println(" - exists!");
                        exists = true;
                    }
                    else
                        System.out.println("");
                }
            }
            if (!exists) 
                portName = ports[0];
            
            System.out.println("Create " + portName);
            SerialPort serialPort = new SerialPort(portName);
            System.out.println(serialPort.getPortName());
            System.out.println("Open " + portName);
            serialPort.openPort();
            System.out.println("Is opened = " + serialPort.isOpened());
            System.out.println("Set parameters for " + portName);
            serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(FLOWCONTROL_NONE);
            
            boolean stat = serialPort.writeString("$08M");
            System.out.println("WriteString $08M " + stat);
            String str = serialPort.readString();
            System.out.println("readString() " + str);

            stat = serialPort.writeByte((byte) 0x0A);
            System.out.println("WriteByte((byte)0x0A) " + stat);
            
            str = serialPort.readString();
            System.out.println("readString() " + str);

            //byte[] bytes = serialPort.readBytes(1, 3000);
            //System.out.println("readBytes(1, 3000) " + bytes.length);
            System.out.println("Creating Adam4118");
            Adam4118 adam = new Adam4118(serialPort, 8);
            System.out.println("new Adam4118 " + adam.name);

            System.out.println("Reading data");
            double[] data = adam.readData();
            for(int i = 0; i <data.length; i++) {
                System.out.println("" + i + " data: " + data[i]);
            }
            
        }
        catch(Exception ex) {
            ex.printStackTrace();
        } 
        System.out.println("-- Finish --");
    }
    
}
