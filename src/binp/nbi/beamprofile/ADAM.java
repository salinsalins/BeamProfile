/*
 */
package binp.nbi.beamprofile;

import java.util.Date;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

/**
 *
 * @author Sanin
 */
public class ADAM {
    //Class for ADAM4xxx series devices

    public SerialPort port;
    public int addr = -1;
    
    boolean to_ctrl = true;
    long to_r = 0;
    long to_w = 0;

    String last_command = "";
    String last_response = "";

    int timeout = 500;
    int to_min = 200;
    int to_max = 2000;
    int to_retries = 3;
    boolean toAuto = true;
    double to_fp = 2.0;
    double to_fm = 1.0/2.0;

    boolean to_susp = false;
    long to_susp_start = 0;
    long to_susp_duration = 5000;

    String name = "";
    String firmware = "";
    String serial = "";
		
    public boolean log = true;

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

    public void setBaudRate(int baud) throws SerialPortException {
        port.setParams(baud, SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
    }
    
    public void setAddr(int address) throws ADAMException {
        addr = -1;
        if (address < 0 || address > 127) {
            throw(new ADAMException("Wrong address " + address));
        }
        addr = address;
    }

    public void delete() {
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
        if (!command.substring(command.length()-2).equals("\n")) command += "\n";

        boolean status = false;
        last_command = command;
        to_w = -1;

        long start = (new Date()).getTime();
        try {
            // Clear com port buffer;
            port.readString();
            
            // Write command
            status = port.writeString(command);
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
        last_response = "";
        if (isSuspended()) return resp;

        // Perform n reties to read response
        int n = to_retries;
        while (n-- > 0) {
            to_r = -1;
            try {
                resp = readResponse(port, timeout);
                decreaseTimeout();
                last_response = resp;
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
        last_response = resp;
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
        long currentTime;
        while (((currentTime = System.currentTimeMillis()) - startTime) < timeout) {
            b = port.readBytes(1, timeout - (int) (currentTime - startTime));
            if (b[0] == 13 )            // wait for CR = 0x0D = 13. 
                return sb.toString();
            sb.append(b[0]);
        }
        throw new ADAMException(timeout);
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
            if (!resp.substring(0,1).equals(">"))
                throw new ADAMException("Wrong reading response.");
            return Float.parseFloat(resp.substring(1));
        }
        catch (ADAMException | NumberFormatException ex) {
            return -8888.8;
        }
    }
		
    public double[] read() {
        double[] data = {-8888.8};
        try {
            // Compose command to Read All Channels  #AA
            String command = String.format("#%02X", addr);
            String resp = execute(command);
            if (!resp.substring(0,1).equals(">"))
                throw new ADAMException("Wrong reading response.");

            int n = resp.length();
            if (n < 8)
                throw new ADAMException("Wrong reading response.");
            data = new double[(n-1)/7];
            for (int i = 1; i < data.length; i++) {
                data[i] = -8888.8;
            }
            int j = 0; 
            for (int i = 1; i < n; i+=7) {
                String str = resp.substring(i, i+6);
                data[j++] = Float.parseFloat(str);
            }
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

        public ADAMException(int timeout) {
            super("ADAM operation timeout " + timeout + " ms.");
        }

        public ADAMException(String str) {
            super("ADAM exception: " + str + ".");
        }

        public ADAMException() {
            super("ADAM exception.");
        }
    }
//************************************************************

}
