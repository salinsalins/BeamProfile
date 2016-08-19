/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import jssc.SerialPort;
import static jssc.SerialPort.FLOWCONTROL_NONE;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
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
    public SerialPort serialPort;
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
            
            boolean stat;
            System.out.println("Create " + portName);
            serialPort = new SerialPort(portName);
            System.out.println("Opening " + serialPort.getPortName());
            stat = serialPort.openPort();
            System.out.println("Is opened " + serialPort.isOpened());
            stat = serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            System.out.println("Set parameters " + stat);
            stat = serialPort.setFlowControlMode(FLOWCONTROL_NONE);
            System.out.println("setFlowControlMode NONE " + stat);
            stat = serialPort.setRTS(false);
            System.out.println("setRTS false " + stat);
            //stat = serialPort.setDTR(true);
            stat = serialPort.setDTR(false);
            System.out.println("setDTR false " + stat);
/*
            serialPort.addEventListener(new Reader(), SerialPort.MASK_RXCHAR |
                                                      SerialPort.MASK_RXFLAG |
                                                      SerialPort.MASK_CTS |
                                                      SerialPort.MASK_DSR |
                                                      SerialPort.MASK_RLSD);
*/            
            
            // Clear input buffer
            serialPort.readString();
            // write command ReadAllChannels for ADAM at addr 08
            String str = "#08";
            stat = writeStringWithCR(str);
            System.out.println("writeStringWithCR " + str + " = " + stat);
            System.out.println("Output count = " + serialPort.getOutputBufferBytesCount());
            System.out.println("Input count =  " + serialPort.getInputBufferBytesCount());
            //str = serialPort.readString(1, 1000);
            //System.out.println("readString() " + str);
            // Read string until CR = 0x0D with timeout 1000ms
            str = readStringToCR(1000);
            System.out.println("readStringToCR() " + str);
            
            //stat = serialPort.writeBytes(bytes);
            //System.out.println("writeBytes " + stat);
            //System.out.println(serialPort.getInputBufferBytesCount());
            //System.out.println(serialPort.getOutputBufferBytesCount());
            //bytes = serialPort.readBytes(15, 1000);
            //System.out.println("readBytes " + bytes.length);

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
    boolean writeStringWithCR(String str) throws SerialPortException {
        if (str == null || str.length() <= 0) return true;
        byte[] bytes = new byte[str.length()+1];
        for (int i=0; i < str.length(); i++) {
            bytes[i] = str.substring(i, i+1).getBytes()[0];
        }
        bytes[bytes.length-1] = 0x0D;
        return serialPort.writeBytes(bytes);
    }
    String readStringToCR(int timeout) throws SerialPortTimeoutException, SerialPortException  {
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = serialPort.readBytes(1, timeout)[0];
            if (bytes[i] == 0x0D)
                return new String(bytes, 0, i);
        }
        throw new SerialPortTimeoutException(serialPort.getPortName(), "readStringToCR", bytes.length);
    }

    private class Reader implements SerialPortEventListener {

        public String str = "";

        @Override
        public void serialEvent(SerialPortEvent spe) {
            if(spe.isRXCHAR() || spe.isRXFLAG()){
                if(spe.getEventValue() > 0){
                    try {
                        str = "";
                        byte[] buffer = serialPort.readBytes(spe.getEventValue());
                        System.out.println("Reader: " + buffer.length + " bytes");
                        str = new String(buffer);
                        System.out.println("Reader: " + str);
                    }
                    catch (Exception ex) {
                        //Do nothing
                    }
                }
            }
            else if(spe.isCTS()){
                if(spe.getEventValue() == 1){
                    System.out.println("Reader: CTS == 1");
                }
                else {
                    System.out.println("Reader: CTS != 1");
                }
            }
            else if(spe.isDSR()){
                if(spe.getEventValue() == 1){
                    System.out.println("Reader: DSR == 1");
                }
                else {
                    System.out.println("Reader: DSR != 1");
                }
            }
            else if(spe.isRLSD()){
                if(spe.getEventValue() == 1){
                    System.out.println("Reader: RLSD == 1");
                }
                else {
                    System.out.println("Reader: RLSD != 1");
                }
            }
        }
    }

}
