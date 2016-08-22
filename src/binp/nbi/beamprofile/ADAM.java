/*
 */
package binp.nbi.beamprofile;

import java.util.Date;
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
    static final Logger logger = Logger.getLogger(ADAM.class.getName());

    public SerialPort port;
    public int addr = -1;
    
    boolean to_ctrl = true;
    long to_r = 0;
    long to_w = 0;

    String last_command = "";
    int readBufferSize = 256;
    byte[] readBuffer = new byte[readBufferSize];
    int readBufferIndex = 0;
    long readByteCount;
    String readResponse = "";
    
    // Timeouts
    int timeout = 500;
    int to_min = 250;
    int to_max = 2000;
    int to_retries = 3;
    boolean toAuto = true;
    double to_fp = 2.0;
    double to_fm = 0.5;
    int minByteReadTimeout = 2;
    long byteReadTime = 0;
    double byteReadCount = 0;
    double averageByteReadTime = 0;
    long firstByteReadTime = 0;
    double firstByteReadCount = 0;
    double averageFirstByteReadTime = 0;

    boolean to_susp = false;
    long to_susp_start = 0;
    long to_susp_duration = 5000;

    String name = "";
    String firmware = "";
    String serial = "";
		
    public boolean log = true;

    ADAM () {
    }

    ADAM (SerialPort comport, int addr) {

/*
        String[] ports = SerialPortList.getPortNames();
        String portName;
        if(ports.length > 0){
            portName = ports[0];
        }

        SerialPort newPort = new SerialPort("COM6");
        newPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
	newPort.openPort();
*/

        try {
            setPort(comport);
            setAddr(addr);

            name = read_name();
            firmware = read_firmware();
            serial = read_serial();

        }
        catch (Exception ex) {
            if (log) {
                System.out.printf("%s\n", ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    ADAM (String comport, int addr) {

        try {
            setPort(comport);
            setAddr(addr);

            name = read_name();
            firmware = read_firmware();
            serial = read_serial();

        }
        catch (Exception ex) {
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
    
    public void setPort(String portName) throws SerialPortException {
        SerialPort newPort = new SerialPort(portName);
        port = newPort;
        newPort.openPort();
        newPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        setPort(newPort);
    }

    public void setAddr(int address) throws ADAMException {
        addr = -1;
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
        serial = "";
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
            String newName = read_name();
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

    public boolean send_command(String command) {
        // Send command to ADAM module
        if (isSuspended()) return false;

        command = command.trim();
        //if (!command.substring(command.length()-2).equals("\n")) command += "\n";

        boolean status = false;
        last_command = command;
        to_w = -1;

        long start = (new Date()).getTime();
        try {
            // Clear com port buffer;
            port.readString();
            
            // Write command
            status = port.writeString(command);
            status = port.writeByte((byte)0x0D);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        to_w = (new Date()).getTime() - start;
        if (log) {
            System.out.printf("COMMAND: %s\n", command);
        }
        return status;
    }

    public String read_response() {
        // Read response form ADAM module
        String resp = "";
        readResponse = "";
        if (isSuspended()) return resp;

        // Perform n reties to read response
        int n = to_retries;
        while (n-- > 0) {
            to_r = -1;
            try {
                resp = readResponse(port, timeout);
                decreaseTimeout();
                readResponse = resp;
                if (log) {
                    System.out.printf("Response: %s\n", resp);
                }
                return resp;
            }
            catch (Exception ex) {
                if (log) {
                    System.out.printf("Response timeout %d\n", timeout);
                }
                increaseTimeout();
            }
        }
        if (log) {
            System.out.printf("No response %d times\n", to_retries);
        }
        to_susp_start = (new Date()).getTime();
        to_susp = true;
        resp = "";
        readResponse = resp;
        return resp;
    }
    
    public boolean isSuspended() {
        if (!to_susp) return false;
        long now = new Date().getTime();
        if ((now - to_susp_start) >= to_susp_duration) {
            to_susp = false;
            return false;
        }
        return true;
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
            b = port.readBytes(1, nextByteTimeout);
            currentTime = System.currentTimeMillis();
            readByteCount++;
            if (readBufferIndex <= 0) {
                firstByteReadTime = currentTime - startTime;
                averageFirstByteReadTime = (averageFirstByteReadTime*firstByteReadCount++ 
                        + firstByteReadTime)/firstByteReadCount;
            }
            else {
                byteReadTime = currentTime - startTime;
                averageByteReadTime = (averageByteReadTime*byteReadCount++ 
                        + byteReadTime)/byteReadCount;
            }
            if (readBufferIndex < readBufferSize) readBuffer[readBufferIndex++] = b[0];
            if (b[0] == 13 )            // wait for CR = 0x0D = 13. 
                return sb.toString();
            sb.append(b[0]);
            readResponse = sb.toString();
            currentTime = System.currentTimeMillis();
        }
        readResponse = sb.toString();
        throw new ADAMException("timeout " + timeout + " ms");
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
            send_command(command);
            return read_response();
        }
        catch (Exception ME) {
            return "";
        }
    }

    public String read_name() {
        // Read Module Name.  Command: $AAM
        return execute(String.format("$%02XM", addr));
    }
		
    public String read_serial() {
          return "Not implemented";
    }
		
    public String read_firmware() {
        // Read Module Firware Version.  Command: $AAF
        return execute(String.format("$%02XF", addr));
    }

    public double read(int chan) {
        if ((chan < 0) || (chan > 8)) 
            return -8888.8; //Float.NaN
        try {
            // Compose command to Read One Channel  #AAN
            String command = String.format("#%02X%1X", addr, chan);
            String resp = execute(command);
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
                catch (Exception ex) {
                    data[j] = -8888.8;
                }
                j++;
            }
            return data;
        }
        catch (Exception ex) {
            //logger.log(Level.INFO, "ADAM response conversion error.", ex);
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
            send_command(cmd);
            String resp = read_response();
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
