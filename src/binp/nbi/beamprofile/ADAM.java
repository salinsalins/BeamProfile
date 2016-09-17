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

    static final int CHAN_MIN = 0;
    static final int CHAN_MAX = 7;

    public SerialPort port;
    public int addr = -1;
    String name = "";
    String firmware = "";
    
    // Write command
    long writeStartTime;
    long writeDuration;
    String command; //Last command

    // Read response - times and statistics
    long readStartTime;
    long readDuration;
    StringBuilder response = new StringBuilder();
    long byteReadTime = 0L;
    double totalByteReadCount = 0.0;
    double averageByteReadTime = 0.0;
    long firstByteReadTime = 0L;
    double firstByteReadCount = 0.0;
    double averageFirstByteReadTime = 0.0;
    int readRetries = 3;
    // Reading Timeouts
    boolean autoTimeout = true;
    int timeout = 500;  // current timeout
    int minTimeout = 250;
    int maxTimeout = 2000;
    int minByteReadTimeout = 2;
    double increaseTimeoutFactor = 2.0;
    double decreseTimeoutFactor = 0.5;
    // Reading suspension
    long suspStartTime = 0;
    long suspDuration = 5000;

    ADAM() {
        //LOGGER.log(Level.FINEST, "Empty ADAM Created");
    }

    ADAM(SerialPort _port, int _addr) throws ADAMException, SerialPortException {
        setPort(_port);
        setAddr(_addr);
        name = readModuleName();
        firmware = readFirmwareVersion();
        LOGGER.log(Level.FINE, getInfo() + name + " Created");
    }

    public void setPort(SerialPort sp) throws SerialPortException {
        if (!sp.isOpened()) sp.openPort();
        port = sp;
        LOGGER.log(Level.FINEST, getInfo() + "setPort");
    }
    
    public void setAddr(int address) throws ADAMException {
        if (address < 0 || address > 127) {
            throw(new ADAMException("Wrong address " + address));
        }
        addr = address;
        LOGGER.log(Level.FINEST, getInfo() + "setAddr");
    }

    public void delete() {
        //port = null;
        addr = -1;
        name = "";
        firmware = "";
        LOGGER.log(Level.FINEST, getInfo() + "delete");
    }

    public boolean reconnect() {
        if (isSuspended()) return false;
        try {
            if (!port.isOpened()) port.openPort();
            String newName = readModuleName();
            if (newName.equals(name)) {
                LOGGER.log(Level.FINEST, getInfo() + "reconnected");
                return true;
            } else {
                LOGGER.log(Level.SEVERE, getInfo() + "Module name mismatch during reconnect");
                return false;
            }
        }
        catch (SerialPortException | ADAMException ex) {
            LOGGER.log(Level.SEVERE, getInfo() + "Error during reconnect");
            LOGGER.log(Level.INFO, "Exception info ", ex);
            return false;
        }
    }

    public boolean sendCommand(String cmd) {
        // Send cmd to ADAM module
        if (isSuspended()) return false;

        boolean status = false;
        command = cmd.trim();
        LOGGER.log(Level.FINE, getInfo() + "sendCommand: {0}", command);
        writeDuration = -1;

        writeStartTime = System.currentTimeMillis();
        try {
            // Clear COM port buffer;
            port.readString();
            // Write command
            byte[] bytes = command.getBytes();
            status = port.writeBytes(bytes);
            if (!status) {
                LOGGER.log(Level.SEVERE, getInfo() + "Error writing bytes");
                writeDuration = System.currentTimeMillis() - writeStartTime;
                return status;
            }
            if (bytes[bytes.length-1] != (byte)0x0D) 
                status = port.writeByte((byte)0x0D);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, getInfo() + "Send command exception");
            LOGGER.log(Level.INFO, "Exception info ", ex);
        }
        if (!status) {
            LOGGER.log(Level.SEVERE, getInfo() + "Error writing bytes");
        }
        writeDuration = System.currentTimeMillis() - writeStartTime;
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
            
    public String readResponse(int timeout)  
            throws SerialPortException, SerialPortTimeoutException, ADAMException {
        readStartTime = System.currentTimeMillis();
        readDuration = -1;
        byte[] b;
        long currentTime = readStartTime;
        long byteReadStartTime;
        int nextByteTimeout;
        if(response != null) 
            response.delete(0, response.length());
        else 
            response = new StringBuilder();
        while ((currentTime  - readStartTime) <= timeout) {
            nextByteTimeout = timeout - (int) (currentTime - readStartTime);
            if (nextByteTimeout < minByteReadTimeout) nextByteTimeout = minByteReadTimeout;
            byteReadStartTime = System.currentTimeMillis();
            b = port.readBytes(1, nextByteTimeout);
            currentTime = System.currentTimeMillis();
            if (response.length() <= 0) {
                firstByteReadTime = currentTime - byteReadStartTime;
                averageFirstByteReadTime = (averageFirstByteReadTime*firstByteReadCount++ 
                        + firstByteReadTime)/firstByteReadCount;
            }
            else {
                byteReadTime = currentTime - byteReadStartTime;
                averageByteReadTime = (averageByteReadTime*totalByteReadCount++ 
                        + byteReadTime)/totalByteReadCount;
            }
            if (b[0] == 13 ) {           // wait for CR = 0x0D = 13. 
                readDuration = System.currentTimeMillis() - readStartTime;
                return response.toString();
            }
            response.append(new String(b, 0, 1));
            currentTime = System.currentTimeMillis();
        }
        throw new ADAMException(getInfo() + "Read response timeout " + timeout + " ms");
    }

    public String readResponse() {
        // Read response form ADAM module

        readDuration = -1;
        readStartTime = System.currentTimeMillis();
        // If comport suspended return ""
        if (isSuspended()) {
            return "";
        }

        // Perform n reties to read response
        int n = readRetries;
        while (n-- > 0) {
            try {
                String rsp = ADAM.this.readResponse(timeout);
                decreaseTimeout();
                LOGGER.log(Level.FINE, getInfo() + "Response: {0}", response.toString());
                readDuration = System.currentTimeMillis() - readStartTime;
                return rsp;
            }
            catch (SerialPortTimeoutException | ADAMException ex) {
                LOGGER.log(Level.SEVERE, getInfo() + "Response timeout {0}", timeout);
                LOGGER.log(Level.INFO, "Exception info ", ex);
                increaseTimeout();
                sendCommand(command);
            }
            catch (SerialPortException ex) {
                LOGGER.log(Level.SEVERE, getInfo() + "SerialPortException reading response");
                LOGGER.log(Level.INFO, "Exception info ", ex);
                increaseTimeout();
                sendCommand(command);
            }
        }
        LOGGER.log(Level.SEVERE, getInfo() + "No response {0} times", readRetries);
        suspStartTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, getInfo() + "Reading is suspended for {0} ms", suspDuration);
        return "";
    }
    
    public String readResponse(String cmd, String firstChar) throws ADAMException {
        String resp = readResponse(cmd);
        if (resp==null || resp.length()<=0) {
            String msg = getInfo() + "Null or empty response";
            LOGGER.log(Level.SEVERE, msg);
            throw new ADAMException(msg);
        }
        if (!resp.startsWith(firstChar)) {
            String msg = getInfo() + "Wrong response " + resp;
            LOGGER.log(Level.SEVERE, msg);
            throw new ADAMException(msg);
        }
        return resp;
    }
		
    public String readResponse(String cmd) {
        sendCommand(cmd);
        return readResponse();
    }

    public void increaseTimeout() {
        if (!autoTimeout) return;
        int newto = (int) (increaseTimeoutFactor * timeout);
        timeout = (newto > maxTimeout) ? maxTimeout: newto;
        LOGGER.log(Level.INFO, getInfo() + "Timeout increased to {0} ms", timeout);
    }
    
    public void decreaseTimeout() {
        if (!autoTimeout) return;
        int newto = (int) (decreseTimeoutFactor * timeout);
        timeout = (newto < minTimeout) ? minTimeout: newto;
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
        // Read One Channel chan. Command  #AAN
        if ((chan < CHAN_MIN) || (chan > CHAN_MAX )) {
            String str = getInfo() + "Wrong channel number " + chan;
            LOGGER.log(Level.WARNING, str);
            throw new ADAMException(str);
        }
        // Compose command to Read One Channel  #AAN
        String cmd = String.format("#%02X%1X", addr, chan);
        String resp = readResponse(cmd, ">");
        double result;
        try {
            result = Double.parseDouble(resp.substring(1));
        } catch (NumberFormatException ex) {
            String str = getInfo() + "Wrong response " + resp;
            LOGGER.log(Level.SEVERE, str);
            throw new ADAMException(str);
        }
        return result;
    }
		
    public double[] read() throws ADAMException {
        // Read All Channels. Command  #AA
        double[] data;
        // Compose command to Read All Channels  #AA
        String cmd = String.format("#%02X", addr);
        String resp = readResponse(cmd, ">");
        data = doubleFromString(resp);
        return data;
    }

    public void write(String command, int param, String ok) throws ADAMException {
        String cmd = String.format("%s %d", command, param);
        readResponse(cmd, ok);
    }

    public static double[] doubleFromString(String str) {
        double[] data = new double[CHAN_MAX - CHAN_MIN + 1];
        for (int i = 0; i < data.length; i++) {
            data[i] = -8888.8;
        }
        try {
            if (!(str.startsWith(">") || str.startsWith("<"))) 
                return data;
            if (!str.contains("; ")) {
                str = str.replaceAll("\\+","; +");
                str = str.replaceAll("-","; -");
            }
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
