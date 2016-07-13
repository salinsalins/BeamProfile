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
    double to_min = 0.5;
    double to_max = 2.0;
    double to_fp = 2;
    double to_fm = 3;
    Date to_tic = new Date();
    int to_toc = 20;
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
        if ((now.getTime()- to_tic.getTime()) > to_toc) {
            try {
                String oldname = name;
                to_tic = now;
                String newname = read_name();
                if (newname.equals(oldname)) {
                    to_count = 0;
                    return true;
                } else {
                    System.out.println("Module name mismatch.");
                    return false;
                }
            }
            catch (Exception ME) {
                to_susp = true;
                to_tic = now;
            }
        }
    }

    boolean send_command(String command) throws SerialPortException {
        boolean status = false;
        // Send command to ADAM module
        reconnect();
        last_command = command;
        // Clear com port buffer;
        while (port.readString() != null);
        long now = (new Date()).getTime();
        status = port.writeString(command);
        to_w = (new Date()).getTime() - now;
        if (log) {
            System.out.println("COMMAND: //s\n" + command);
        }
        return status;
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
		function [resp, status] = read_response(obj)
			// Read response form GENESIS module
			resp = "";
			status = false;
			obj.msg = {};

			// Perform n reties to read response
			n = obj.retries;
			while n >= 0
				n = n - 1;
				obj.to_r = -1;
				obj.read_fgetl;
				read_error = ~strcmp(obj.last_msg, "");
				if ~read_error
					resp = obj.last_response;
					status = true;
					break
				}
				if obj.read_rest
					obj.read_fgetl;
				}
			}
			// Correct timeout
			obj.correct_timeout;

			if read_error && nargout < 2
				System.out.println("GENESIS read error.");
			}
		}
		
		function read_fgetl(obj)
			if ~obj.to_susp}
				obj.to_tic = tic;
				tic;
				[resp, count, message] = fgetl(obj.port);
				if obj.log
					printl("RESPONSE: //s\n", resp);
				}
				obj.to_r = max(obj.to_r, toc);
				obj.last_response = resp;
				obj.last_count = count;
				obj.last_msg = message;
				if ~strcmpi(message, "")
					if obj.log
						printl("MESSAGE: //s\n", message);
					}
					obj.msg = {obj.msg{:}, message};
					obj.to_count = obj.to_count + 1;
					if obj.to_count > obj.to_countmax
						obj.to_tic = tic;
						obj.to_susp} = true;
					}
				else
					obj.to_count = 0;
					obj.to_susp} = false;
				}
			else
				obj.last_msg = "Susp}ed";
				obj.msg = {obj.msg{:}, obj.last_msg};
				if obj.log
					printl("//s\n", obj.last_msg);
				}
			}
		}

		function correct_timeout(obj)
			// Correct timeout
			if obj.to_ctrl
				dt = max(obj.to_r, obj.to_w);
				if dt >= obj.port.timeout*0.9
					newto = min(obj.to_fp*obj.port.timeout, obj.to_max);
					if obj.port.timeout < newto
						obj.port.timeout = newto;
						obj.msg = {obj.msg{:}, sprintf("Timeout+ //d //d", obj.port.timeout, dt)};
						if obj.log
							printl("Timeout+ //d //d\n", obj.port.timeout, dt);
						}
					}
				else
					newto = max(obj.to_fm*dt, obj.to_min);
					if obj.port.timeout > newto
						obj.port.timeout = newto;
						obj.msg = {obj.msg{:}, sprintf("Timeout- //d //d", obj.port.timeout, dt)};
						if obj.log
							printl("Timeout- //d //d\n", obj.port.timeout, dt);
						}
					}
				}
			}
		}
		
		function status = set_addr(obj)
			status = true;
		}
		
		function [resp, status] = execute(obj, command)
			// S} command and read response form ADAM
			status = false;
			try
				obj.s}_command(command);
				resp = obj.read_response;
				status = true;
			catch ME
				//printl("//s\n", ME.message);
				//printl("//s //i\n", ME.message, nargout);
				if nargout < 2
					rethrow(ME);
				}
			}
		}
		
		function [data, status] = execute_format(obj, fmt)
			// Execute command for ADAM address with format string fmt
			data = "";
			status = false;
			try
				cmd = sprintf(fmt, obj.addr);
				s = obj.execute(cmd);
				[data, status] = obj.isok(s);
			catch ME
			}
		}
		
		function [outstr, status] = isok(obj, instr)
			status = false;
			outstr = "";
			if length(instr) > 3
				if strcmp(instr(1:3), sprintf("!//02X", obj.addr))
					status = true;
					outstr = instr(4:});
				}
			}
		}
		
		function [name, status] = read_name(obj)
			// Read Module Name.  Command: $AAM
			[name, status] = obj.execute_format("$//02XM");
		}
		
		function [sn, status] = read_serial(obj)
			// Read Module Serial Number
			sn = "not implemented";
			status = true;
		}
		
		function [version, status] = read_firmware(obj)
			// Read Module Firware Version.  Command: $AAF
			[version, status] = obj.execute_format("$//02XF");
		}
		
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
    
}
