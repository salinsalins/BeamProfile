/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

/**
 *
 * @author Sanin
 */
public class ADAM {
    //Class for ADAM4xxx series devices
//    SerialPort	ports;
    private static final int addr_max = 127;
    private static final int addr_min = 0;
    private static final String MsgIdent = "ADAM";
    SerialPort port;
    int addr = -1;
    
    boolean to_ctrl = true;
    double to_r = 0.0;
    long to_w = 0;

    String response = "";
    String outstr = "";
    int timeout = 200;
    int to_min = 200;
    int to_max = 2000;
    boolean toAuto = true;
    double to_fp = 2.0;
    double to_fm = 1.0/2.0;

    long to_tic = 0;
    long to_toc = 10000;
    int to_count = 0;
    int to_countmax = 2;
    boolean to_susp = false;
		
    boolean set_addr_strict = true;
    int set_addr_retries = 3;

    boolean read_rest = false;
    int retries = 0;

    String name = "";
    String firmware = "";
    String serial = "";
		
    String last_command = "";
    String last_response = "";
    int last_count = 0;
    String last_msg = "";
    List<String> msg = new LinkedList<>();
		
    boolean log = false;

    // Default constructor
    ADAM() {
        last_msg = "Default constructor";
    }
    ADAM(SerialPort comport, int addr) {
        String msgIdent = this.MsgIdent + ":constructor";
        try {
            isvalidport(comport);
            port = comport;
            isvalidaddr(addr);
            if (isaddrattached()) {
                System.out.println(msgIdent + "Address is in use.");
            }
            this.addr = addr;
            attach_addr();
            //obj.set_addr;
            name = read_name();
            firmware = read_firmware();
            serial = read_serial();
            // Check if created object is valid
            valid();
        }
        catch (Exception ME) {
            if (log) {
                System.out.printf("//s\n", ME.getMessage());
            }
            throw(ME);
        }
    }
		
    void delete() {
        try {
            //temp = obj.detach_addr;
            addr = -1;
            last_msg = "Deleted";
            name = "";
            firmware = "";
            serial = "";
        }
        catch (Exception ME) {
            last_msg = "Delete " + ME.getMessage();
        }
    }

    boolean detach_addr() {
        boolean status = false;
        try {
            if (port != null) {
                //boolean index = port.usedAddr.indexOf(addr);
                //port.usedAddr.remove(index);
                status = true;
            }
            else {
                System.out.println("Wrong COM port");
            }
        }
        catch (Exception ME) {
            if (true /*nargout < 1*/) {
                System.out.println("Wrong COM port");
                throw(ME);
            }
        }
        return status;
    }

    boolean attach_addr() {
        boolean status = false;
            try {
                isvalidaddr();
                detach_addr();
                port.usedAddr.add(addr);
                status = true;
            }
            catch (Exception ME) {
                if (true /*nargout < 1*/) {
                    throw(ME);
                }
            }
        return status;
    }

    boolean isvalidport(SerialPort comport) {
        if (comport.isOpened()) {
            last_msg = "Invalid COM port.";
            System.out.println(last_msg);
            return false;
        }
        return true;
    }		
    boolean isvalidport() {
        return isvalidport(port);
    }

    boolean isvalidaddr(int address) {
        if (address < addr_min || address > addr_max) {
            last_msg = "Invalid address.";
            System.out.println(last_msg);
            return false;
        }
        return true;
    }
    boolean isvalidaddr() {
        return isvalidaddr(addr);
    }

    static boolean isaddrattached(SerialPort port, int addr) {
        // Is address in use on COM port
        return true;
    }
    boolean isaddrattached() {
        // Is address in use on COM port
        return isaddrattached(port, addr);
    }
		
    boolean isinitialized() {
        if (name == null || serial == null || "".equals(name) || "".equals(serial)) {
            last_msg = "Module is not initialized.";
            return false;
        }
        return true;
    }
		
    boolean valid() {
        isvalidport();
        isvalidaddr();
        isaddrattached();
        isinitialized();
        return true;
    }

    boolean reconnect() {
        long now = new Date().getTime();
        if ((now - to_tic) > to_toc) {
            try {
                String oldname = name;
                to_tic = now;
                String newname = read_name();
                if (newname.equals(oldname)) {
                    to_count = 0;
                    return true;
                } else {
                    System.out.println("Module name mismatch during reconnect.");
                    return false;
                }
            }
            catch (Exception ME) {
                to_susp = true;
                to_tic = now;
            }
        }
        return false;
    }

    boolean send_command(String command) throws SerialPortException {
        // Send command to ADAM module
        boolean status = false;
//        reconnect();
        last_command = command;
        // Clear com port buffer;
        if (port.getInputBufferBytesCount() > 0)
            port.readBytes();
        long now = (new Date()).getTime();
        status = port.writeString(command + "\n");
        to_w = (new Date()).getTime() - now;
        if (log) {
            System.out.println("COMMAND: " + command);
        }
        return status;
    }

    boolean read_response() {
        // Read response form GENESIS module
        response = "";
        msg.clear();

        // Perform n reties to read response
        int n = retries;
        while (n-- >= 0) {
            to_r = -1;
            try {
                response = readResponse(port, timeout);
                decreaseTimeout();
                return true;
            }
            catch (Exception ex) {
                increaseTimeout();
            }
        }
        increaseTimeout();
        return false;
    }

    private String readResponse(SerialPort port, int timeout)  
            throws SerialPortException, SerialPortTimeoutException, ADAMTimeoutException {
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
        throw new ADAMTimeoutException(timeout);
    }

    void increaseTimeout() {
        if (!toAuto) return;
        int newto = (int) (to_fp * timeout);
        timeout = (newto > to_max) ? to_max: newto;
    }
    
    void decreaseTimeout() {
        if (!toAuto) return;
        int newto = (int) (to_fm * timeout);
        timeout = (newto < to_min) ? to_min: newto;
    }

    boolean set_addr() {
        return true;
    }
    
    boolean execute(String command) {
        // Send command and read response form ADAM
        boolean status = false;
        try {
            send_command(command);
            status = read_response();
            return status;
        }
        catch (Exception ME) {
            //printl("//s\n", ME.message);
            status = false;
        }
        return status;
    }

    boolean execute_format(String fmt) {
        // Execute command for ADAM address with format string fmt
        boolean status = false;
        try {
            String cmd = String.format(fmt, addr);
            execute(cmd);
            status = isok(response);
        }
        catch (Exception ME) {
        }
        return status;
    }
		
    boolean isok(String instr) {
        boolean status = false;
        String outstr = "";
        if (instr.length() > 3) {
            if (instr.substring(0, 2).equals(String.format("!%02X", addr))) {
                status = true;
                outstr = instr.substring(3);
            }
        }
        return status;
    }
    
    boolean read_name() {
        // Read Module Name.  Command: $AAM
        return execute_format("$%02XM");
    }
		
    boolean read_serial() {
          response = "not implemented";
        return true;
    }
		
		function [version, status] = read_firmware(obj)
			// Read Module Firware Version.  Command: $AAF
			[version, status] = obj.execute_format("$//02XF");
		}
		

/*
classdef ADAM < handle
	//{
	a=instrhwinfo("serial");
	ports = instrfind;
	if numel(ports) > 0 
		fclose(ports);
		delete(ports);
	}
	cp = serial("COM6");
	set(cp, "BaudRate", 38400, "DataBits", 8, "StopBits", 1);
	set(cp, "Terminator", "CR");
	set(cp, "Timeout", 1);
	fopen(cp);
	//}
    methods
		
		function outstr = read_str(obj, chan)
			outstr = "";
			if nargin <= 1
				// Compose command to Read All Channels  #AA
				command = sprintf("#//02X", obj.addr);
				outstr = obj.execute(command);
				return
			}
			
			if (chan < 0) || (chan > 8)
				return
			}
			
			// Compose command to Read One Channel  #AAN
			command = sprintf("#//02X//1X", obj.addr, chan);
			outstr = obj.execute(command);
		}

		function [data, n] = read(obj, chan)
			data = [];
			n = 0;
			if nargin <= 1
				outstr = read_str(obj);
			else
				outstr = read_str(obj, chan);
			}
			[data, n] = sscanf(outstr(2:}), "//f");
		}
		
//{		
		function status = write(obj, command, param)
			status = false;
			if nargin >= 3
				if isnumeric(param)
					cmd = sprintf("//s //g", command, param);
				elseif isa(param, "char")
					cmd = [command, " ", param];
				else
					cmd = command;
				}
			else
				cmd = command;
			}
			try
				obj.s}_command(cmd);
				resp = obj.read_response;
				if strcmpi(resp, "OK")
					status = true;
				else
					System.out.println(["Unexpected response. " command, " -> ", resp]);
				}
			catch ME
				//printl("//s\n", ME.message);
				if nargout < 1
					rethrow(ME);
				}
			}
		}
		
		function [value, status] = read_value(obj, command)
			// Read Module Firmware Version
			try
				valuetxt = obj.read(command);
				[value, status] = sscanf(valuetxt, "//g");
				if status == 1
					status = true;
					return
				else
					System.out.println(["Read Value error form ", command, " " , valuetxt]);
				}
			catch ME
				value = [];
				status = false;
				//printl("//s\n", ME.message);
				if nargout < 2
					rethrow(ME);
				}
			}
		}

//}	
	}
}



    */
    


//*************************************
    public class ADAMTimeoutException extends Exception {

        private final int timeoutValue;

        public ADAMTimeoutException(int timeoutValue) {
            super("ADAM operation timeout " + timeoutValue + " ms.");
            this.timeoutValue = timeoutValue;
        }

        public ADAMTimeoutException() {
            super("ADAM operation timeout.");
            this.timeoutValue = 0;
        }

        public int getTimeoutValue(){
            return timeoutValue;
        }
    }

}
