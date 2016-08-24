/*
 */
package binp.nbi.beamprofile;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

/**
 *
 * @author Sanin
 */
public class ADAM {
    //Common class for ADAM4xxx series devices
    //static final Logger LOGGER = Logger.getLogger(ADAM.class.getName());
    static final Logger LOGGER = Logger.getLogger(ADAM.class.getPackage().getName());

    public SerialPort port;
    public int addr = -1;
    
    boolean to_ctrl = true;
    long to_r = 0;
    long to_w = 0;

    String command = "";
    // Reading response buffer, times and statistics
    String response = "";
    static byte[] readBuffer = new byte[256];
    static int readBufferIndex = 0;
    long byteReadTime = 0L;
    double totalByteReadCount = 0.0;
    double averageByteReadTime = 0.0;
    long firstByteReadTime = 0L;
    double firstByteReadCount = 0.0;
    double averageFirstByteReadTime = 0.0;
    
    // Timeouts
    int timeout = 500;
    int to_min = 250;
    int to_max = 2000;
    int to_retries = 3;
    boolean toAuto = true;
    double to_fp = 2.0;
    double to_fm = 0.5;
    int minByteReadTimeout = 2;

    long suspStartTime = 0;
    long suspDuration = 5000;

    String name = "";
    String firmware = "";
		
    public boolean log = true;

    ADAM () {
    }

    ADAM (SerialPort comport, int addr) {
        try {
            setPort(comport);
            setAddr(addr);
            name = readModuleName();
            firmware = readFirmwareVersion();
        }
        catch (SerialPortException | ADAMException ex) {
            if (log) {
                System.out.printf("%s\n", ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public void setPort(SerialPort serialPort) throws SerialPortException {
        port = serialPort;
        if (!serialPort.isOpened()) serialPort.openPort();
    }
    
    public void setAddr(int address) throws ADAMException {
        if (address < 0 || address > 127) {
            throw(new ADAMException("Wrong address " + address));
        }
        addr = address;
    }

    public void delete() {
        //port = null;
        addr = -1;
        name = "";
        firmware = "";
    }

    public boolean detach_addr() {
        addr = -1;
        return true;
    }

    public boolean attach_addr() {
        return true;
    }

    public boolean isvalidport(SerialPort comport) throws SerialPortException {
        if (!comport.isOpened()) return comport.openPort();
        return true;
    }		
    public boolean isvalidport() throws SerialPortException {
        return isvalidport(port);
    }

    public boolean isvalidaddr(int address) {
        if (address < 0 || address > 127) {
            return false;
        }
        return true;
    }
    public boolean isvalidaddr() {
        return isvalidaddr(addr);
    }

    public static boolean isaddrattached(SerialPort port, int addr) {
        // Is address in use on COM port
        return true;
    }
    public boolean isaddrattached() {
        // Is address in use on COM port
        return isaddrattached(port, addr);
    }
		
    public boolean isinitialized() {
        if (name == null || "".equals(name)) {
            return false;
        }
        return true;
    }
		
    public boolean valid() throws SerialPortException {
        isvalidport();
        isvalidaddr();
        isaddrattached();
        isinitialized();
        return true;
    }

    public boolean reconnect() {
        if (isSuspended()) return false;
        try {
            if (!port.isOpened()) port.openPort();
            String newName = readModuleName();
            if (newName.equals(name)) {
                return true;
            } else {
                System.out.println("Module name mismatch during reconnect.");
                return false;
            }
        }
        catch (Exception ME) {
            return false;
        }
    }

    public boolean sendCommand(String cmd) {
        // Send cmd to ADAM module
        if (isSuspended()) return false;

        cmd = cmd.trim();

        boolean status = false;
        command = cmd;
        to_w = -1;

        long start = System.currentTimeMillis();
        try {
            // Clear com port buffer;
            port.readString();
            // Write cmd
            byte[] bytes = cmd.getBytes();
            status = port.writeBytes(bytes);
            if (!status) {
                LOGGER.log(Level.SEVERE, "Error writing bytes");
                return status;
            }
            if (bytes[bytes.length-1] != (byte)0x0D) 
                status = port.writeByte((byte)0x0D);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Command send exception ", ex);
        }
        to_w = System.currentTimeMillis() - start;
        LOGGER.log(Level.FINE, "Command: {0}", cmd);
        if (!status) {
            LOGGER.log(Level.SEVERE, "Error writing bytes");
        }
        return status;
    }

    public boolean isSuspended() {
        long now = System.currentTimeMillis();
        return ((now - suspStartTime) < suspDuration);
    }
            
    public  String readResponse(SerialPort port, int timeout)  
            throws SerialPortException, SerialPortTimeoutException, ADAMException {
        byte[] b;
        StringBuilder sb = new StringBuilder();
        long startTime = System.currentTimeMillis();
        long currentTime = startTime;
        readBufferIndex = 0;
        while ((currentTime  - startTime) <= timeout) {
            int nextByteTimeout = timeout - (int) (currentTime - startTime);
            if (nextByteTimeout < minByteReadTimeout) nextByteTimeout = minByteReadTimeout;
            long byteReadStartTime = System.currentTimeMillis();
            b = port.readBytes(1, nextByteTimeout);
            currentTime = System.currentTimeMillis();
            if (readBufferIndex <= 0) {
                firstByteReadTime = currentTime - byteReadStartTime;
                averageFirstByteReadTime = (averageFirstByteReadTime*firstByteReadCount++ 
                        + firstByteReadTime)/firstByteReadCount;
            }
            else {
                byteReadTime = currentTime - byteReadStartTime;
                averageByteReadTime = (averageByteReadTime*totalByteReadCount++ 
                        + byteReadTime)/totalByteReadCount;
            }
            if (readBufferIndex < readBuffer.length) readBuffer[readBufferIndex++] = b[0];
            if (b[0] == 13 )            // wait for CR = 0x0D = 13. 
                //return sb.toString();
                return new String(readBuffer, 0, readBufferIndex);
            sb.append(b[0]);
            //readResponse = sb.toString();
            response = new String(readBuffer, 0, readBufferIndex);
            currentTime = System.currentTimeMillis();
        }
        //readResponse = sb.toString();
        response = new String(readBuffer, 0, readBufferIndex);
        throw new ADAMException("timeout " + timeout + " ms");
    }

    public String readResponse() {
        // Read response form ADAM module
        response = "";
        // If comport suspended return ""
        if (isSuspended()) return "";

        // Perform n reties to read response
        int n = to_retries;
        while (n-- > 0) {
            to_r = -1;
            try {
                response = ADAM.this.readResponse(port, timeout);
                decreaseTimeout();
                LOGGER.log(Level.FINE, "Response: {0}", response);
                return response;
            }
            catch (SerialPortTimeoutException | ADAMException ex) {
                LOGGER.log(Level.INFO, "Response timeout {0}", timeout);
                if (log) {
                    System.out.printf("Response timeout %d ms\n", timeout);
                }
                increaseTimeout();
            }
            catch (SerialPortException ex) {
                LOGGER.log(Level.WARNING, "Exception reading response ", ex);
                if (log) {
                    System.out.printf("Exception reading response\n");
                }
                increaseTimeout();
            }
        }
        if (log) {
            System.out.printf("No response %d times\n", to_retries);
        }
        suspStartTime = System.currentTimeMillis();
        response = "";
        return response;
    }
    
    public String readResponse(String firstChar) {
        String resp = readResponse();
        if (resp==null || resp.length()<=0) {
            LOGGER.log(Level.INFO, "Null or empty response");
            return "";
        }
        if (!resp.startsWith(firstChar)) {
            LOGGER.log(Level.INFO, "Unexpected response {0}", resp);
            return resp;
        }
        return resp.substring(firstChar.length());
    }
		
    public void increaseTimeout() {
        if (!toAuto) return;
        int newto = (int) (to_fp * timeout);
        timeout = (newto > to_max) ? to_max: newto;
    }
    
    public void decreaseTimeout() {
        if (!toAuto) return;
        int newto = (int) (to_fm * timeout);
        timeout = (newto < to_min) ? to_min: newto;
    }
  
    public String execute(String command) {
        // Send command and read response form ADAM
        try {
            sendCommand(command);
            return readResponse();
        }
        catch (Exception ME) {
            return "";
        }
    }

    public String readModuleName() {
        // Read Module Name.  Command: $AAM
        sendCommand(String.format("$%02XM", addr));
        return readResponse("!");
    }
		
    public String readFirmwareVersion() {
        // Read Module Firware Version.  Command: $AAF
        sendCommand(String.format("$%02XF", addr));
        return readResponse("!");
    }

    public double read(int chan) {
        if ((chan < 0) || (chan > 8)) 
            return -8888.8; //Float.NaN
        try {
            // Compose command to Read One Channel  #AAN
            String cmd = String.format("#%02X%1X", addr, chan);
            String resp = execute(cmd);
            if (!resp.substring(0, 0).equals(">"))
                throw new ADAMException("Wrong reading response.");
            return Float.parseFloat(resp.substring(1));
        }
        catch (ADAMException | NumberFormatException ex) {
            return -8888.8;
        }
    }
		
    public static double[] doubleFromString(String str) {
        double[] data = new double[0];
        try {
            str = str.substring(1);
            String str1 = str.replaceAll("\\+","; +");
            String str2 = str1.replaceAll("-","; -");
            if (str2.startsWith("; ")) str2 = str2.substring(2);
            String[] strarr = str2.split("; ");
            data = new double[strarr.length];
            int j = 0; 
            for (String s : strarr) {
                try {
                    data[j] = Double.parseDouble(s);
                }
                catch (NumberFormatException ex) {
                    data[j] = -8888.8;
                }
                j++;
            }
            return data;
        }
        catch (Exception ex) {
            LOGGER.log(Level.INFO, "ADAM response conversion error.", ex);
            return data;
        }
    }

    public double[] read() {
        double[] data = {-8888.8};
        try {
            // Compose command to Read All Channels  #AA
            String command = String.format("#%02X", addr);
            String resp = execute(command);
            if (!resp.substring(0, 0).equals(">"))
                throw new ADAMException("Wrong reading response.");
            data = doubleFromString(resp);
            if (data.length != 8)
                throw new ADAMException("Wrong reading response.");
            return data;
        }
        catch (ADAMException | NumberFormatException ex) {
            return data;
        }
    }

    public boolean write(String command, int param) throws SerialPortException {
        String cmd = String.format("%s %d", command, param);
        sendCommand(cmd);
        String resp = readResponse();
        if ("OK".equals(resp)) {
            return true;
        }
        else{
            System.out.println("Unexpected response. " + cmd + " -> " + resp);
            return false;
        }
    }

//************************************************************
    public class ADAMException extends Exception {

        public ADAMException(String description, int parameter) {
            super("ADAM exception: " + description + parameter);
        }

        public ADAMException(int timeout) {
            super("ADAM operation timeout " + timeout + " ms.");
        }

        public ADAMException(String str) {
            super("ADAM exception: " + str);
        }

        public ADAMException() {
            super("ADAM exception.");
        }
    }
//************************************************************

}
