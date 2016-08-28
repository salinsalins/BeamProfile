/*
    Common class for ADAM4xxx series devices
 */
package binp.nbi.beamprofile;

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
    static final Logger LOGGER = Logger.getLogger(ADAM.class.getPackage().getName());

    public SerialPort port;
    public int addr = -1;
    
    long to_w = 0;
    // Last command
    String command;
    // Reading response buffer, times and statistics
    String response;
    static byte[] readBuffer = new byte[256];
    static int readBufferIndex = 0;
    long byteReadTime = 0L;
    double totalByteReadCount = 0.0;
    double averageByteReadTime = 0.0;
    long firstByteReadTime = 0L;
    double firstByteReadCount = 0.0;
    double averageFirstByteReadTime = 0.0;
    
    // Timeouts
    boolean autoTimeout = true;
    int timeout = 500;
    int to_min = 250;
    int to_max = 2000;
    int minByteReadTimeout = 2;
    double to_fp = 2.0;
    double to_fm = 0.5;
    int to_retries = 3;

    long suspStartTime = 0;
    long suspDuration = 5000;

    String name = "";
    String firmware = "";
		
    ADAM() {
        //LOGGER.log(Level.FINEST, "Empty ADAM Created");
    }

    ADAM(SerialPort _port, int _addr) throws ADAMException, SerialPortException {
        setPort(_port);
        setAddr(_addr);
        name = readModuleName();
        firmware = readFirmwareVersion();
        LOGGER.log(Level.FINEST, getInfo() + name + " Created");
    }

    public void setPort(SerialPort sp) throws SerialPortException {
        if (!sp.isOpened()) sp.openPort();
        port = sp;
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
		
    public boolean isInitialized() {
        if (name == null || "".equals(name)) {
            return false;
        }
        return true;
    }
		
    public boolean valid() throws SerialPortException {
        isvalidport();
        isvalidaddr();
        isaddrattached();
        isInitialized();
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
                LOGGER.log(Level.SEVERE, getInfo() + "Error writing bytes");
                return status;
            }
            if (bytes[bytes.length-1] != (byte)0x0D) 
                status = port.writeByte((byte)0x0D);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, getInfo() + "Command send exception ", ex);
        }
        to_w = System.currentTimeMillis() - start;
        LOGGER.log(Level.FINE, getInfo() + "Command: {0}", cmd);
        if (!status) {
            LOGGER.log(Level.SEVERE, getInfo() + "Error writing bytes");
        }
        return status;
    }

    public boolean isSuspended() {
        long now = System.currentTimeMillis();
        if ((now - suspStartTime) < suspDuration) {
            LOGGER.log(Level.FINEST, getInfo() + "Reading is suspended");
            return true;
        }
        return false;
    }
            
    public  String readResponse(int timeout)  
            throws SerialPortException, SerialPortTimeoutException, ADAMException {
        byte[] b;
        //StringBuilder sb = new StringBuilder();
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
            //sb.append(new String(b, 0, 1));
            //response = sb.toString();
            response = new String(readBuffer, 0, readBufferIndex);
            currentTime = System.currentTimeMillis();
        }
        //response = sb.toString();
        response = new String(readBuffer, 0, readBufferIndex);
        throw new ADAMException("Read response timeout " + timeout + " ms");
    }

    public String readResponse() {
        // Read response form ADAM module
        response = "";
        // If comport suspended return ""
        if (isSuspended()) {
            return "";
        }

        // Perform n reties to read response
        int n = to_retries;
        while (n-- > 0) {
            try {
                response = ADAM.this.readResponse(timeout);
                decreaseTimeout();
                LOGGER.log(Level.FINE, getInfo() + "Response: {0}", response);
                return response;
            }
            catch (SerialPortTimeoutException | ADAMException ex) {
                LOGGER.log(Level.WARNING, getInfo() + "Response timeout {0}", timeout);
                LOGGER.log(Level.INFO, "Exception info ", ex);
                increaseTimeout();
            }
            catch (SerialPortException ex) {
                LOGGER.log(Level.WARNING, getInfo() + "SerialPortException reading response");
                LOGGER.log(Level.INFO, "Exception info ", ex);
                increaseTimeout();
            }
        }
        LOGGER.log(Level.WARNING, getInfo() + "No response {0} times", to_retries);
        suspStartTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, getInfo() + "Suspend reading for {0} ms", suspDuration);
        response = "";
        return response;
    }
    
    public String readResponse(String cmd, String firstChar) throws ADAMException {
        String resp = readResponse(cmd);
        if (resp==null || resp.length()<=0) {
            LOGGER.log(Level.INFO, getInfo() + "Null or empty response");
            throw new ADAMException("Null or empty response");
        }
        if (!resp.startsWith(firstChar)) {
            LOGGER.log(Level.INFO, getInfo() + "Wrong response {0}", resp);
            throw new ADAMException("Wrong response " + resp);
        }
        return resp;
    }
		
    public String readResponse(String cmd) {
        sendCommand(cmd);
        return readResponse();
    }

    public void increaseTimeout() {
        if (!autoTimeout) return;
        int newto = (int) (to_fp * timeout);
        timeout = (newto > to_max) ? to_max: newto;
        LOGGER.log(Level.FINE, getInfo() + "Timeout increased to {0} ms", timeout);
    }
    
    public void decreaseTimeout() {
        if (!autoTimeout) return;
        int newto = (int) (to_fm * timeout);
        timeout = (newto < to_min) ? to_min: newto;
        LOGGER.log(Level.FINE, getInfo() + "Timeout decreased to {0} ms", timeout);
    }
  
    public String getInfo() {
        return "ADAM " + port.getPortName() + ":" + addr + " ";
    }
    
    public String readModuleName() throws ADAMException {
        // Read Module Name.  Command: $AAM
        String cmd = String.format("$%02XM", addr);
        return readResponse(cmd, "!").substring(1);
    }
		
    public String readFirmwareVersion() throws ADAMException {
        // Read Module Firware Version.  Command: $AAF
        String cmd = String.format("$%02XF", addr);
        return readResponse(cmd, "!").substring(1);
    }

    public double read(int chan) throws ADAMException {
        if ((chan < 0) || (chan > 8)) 
            throw new ADAMException("Wrong channel number " + chan);
        // Compose command to Read One Channel  #AAN
        String cmd = String.format("#%02X%1X", addr, chan);
        String resp = readResponse(cmd, ">");
        double result;
        try {
            result = Double.parseDouble(resp.substring(1));
        } catch (NumberFormatException ex) {
            throw new ADAMException("Wrong response " + resp);
        }
        return result;
    }
		
    public double[] read() throws ADAMException {
        double[] data;
        // Compose command to Read All Channels  #AA
        String cmd = String.format("#%02X", addr);
        String resp = readResponse(cmd, ">");
        data = doubleFromString(resp);
        if (data.length != 8)
            throw new ADAMException("Wrong response " + resp);
        return data;
    }

    public void write(String command, int param, String ok) throws ADAMException {
        String cmd = String.format("%s %d", command, param);
        readResponse(cmd, ok);
    }

    public static double[] doubleFromString(String str) {
        double[] data = new double[8];
        for (int i = 0; i < data.length; i++) {
            data[i] = -8888.8;
        }
        try {
            if (!(str.startsWith(">") || str.startsWith("<"))) 
                return data;
            //str = str.substring(1);
            str = str.replaceAll("\\+","; +");
            str = str.replaceAll("-","; -");
            String[] strarr = str.split("; ");
            if (strarr.length <= 1) return data;
            data = new double[strarr.length - 1];
            for (int i = 1; i < strarr.length; i++) {
                try {
                    data[i-1] = Double.parseDouble(strarr[i]);
                }
                catch (NumberFormatException | NullPointerException ex) {
                    data[i-1] = -8888.8;
                }
            }
            return data;
        }
        catch (Exception ex) {
            LOGGER.log(Level.WARNING, "ADAM response conversion error");
            LOGGER.log(Level.INFO, "Exception info", ex);
            return data;
        }
    }

//************************************************************
    public class ADAMException extends Exception {

        public ADAMException(String description, int parameter) {
            super(getInfo() + description + parameter);
        }

        public ADAMException(int timeout) {
            super(getInfo() + "timeout " + timeout + " ms");
        }

        public ADAMException(String str) {
            super(getInfo() + str);
        }

        public ADAMException() {
            super(getInfo() + "exception");
        }
    }
//************************************************************

}
