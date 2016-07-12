/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.util.LinkedList;
import java.util.List;
import jssc.SerialPort;

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
    double to_w = 0.0;
    double to_min = 0.5;
    double to_max = 2.0;
    double to_fp = 2;
    double to_fm = 3;
    long to_tic = 0;
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
    ADAM(SerialPort comport) {
        this();
        System.out.println(msgIdent + " Wrong nargin.");
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
        boolean status = false;
        try {
            if (nargin < 2) {
                comport = port;
            }
            if (~isa(comport, "serial") || ~strcmpi(comport.status, "open")) {
                last_msg = "Invalid COM port.";
                error(last_msg);
            }
            else {
                status = true;
            }
        }
        catch (Exception ME) {
            if (true /*nargout < 1*/) {
                throw(ME);
            }
        }
        return status;
    }		

    boolean isvalidaddr(int address) {
        boolean status = false;
        if (true /*nargin < 2*/) {
            address = addr;
        }
        try {
            if (address < addr_min || address > addr_max) {
                last_msg = "Invalid address.";
                error(last_msg);
            }
            else {
                status = true;
            }
        }
        catch (Exception ME) {
            if (nargout < 1) {
                throw(ME);
            }
        }
        return status;
    }

    boolean isaddrattached(int addr) {
        // Is address in use on com port
        boolean status = false;
        int nargin = 1;
        int nargout = 0;
        try {
            if (nargin < 2) {
                this.addr = addr;
            }
            if (isa(obj, "GENESIS")) 
                comport = port;
            else if (isa(obj, "serial")) 
                comport = obj;
            else
                error("Wron object.");
            if (isempty(find(comport.userdata == addr, 1))) 
                if (isa(obj, "GENESIS")) {
                    last_msg = "Address is not attched.";
                    error("Address is not attched.");
                }
            else 
                status = true;
        }
        catch (Exception ME) {
            if (nargout < 1) {
                throw(ME);
            }
        }
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
		
		function status = isinitialized(obj)
			try
				status = false;
				if isempty(obj.name) || isempty(obj.serial)
					obj.last_msg = "Module is not initialized.";
					error(obj.last_msg);
				else
					status = true;
				}
			catch ME
				if nargout < 1
					rethrow(ME);
				}
			}
		}
		
		function status = valid(obj)
			status = false;
			try
				obj.isvalidport;
				obj.isvalidaddr;
				obj.isaddrattached;
				obj.isinitialized;
				status = true;
			catch ME
				if nargout < 1
					rethrow(ME);
				}
			}
		}
		
		function status = reconnect(obj)
			status = false;
			if obj.to_susp}
				if toc(obj.to_tic) > obj.to_toc
					obj.to_susp} = false;
					try
						oldname = obj.name;
						obj.to_tic = tic;
						newname = obj.read_name;
						if strcmpi(newname, oldname)
							obj.to_count = 0;
							status = true;
						else
							error("Module name mismatch.");
						}
					catch ME
						obj.to_susp} = true;
						obj.to_tic = tic;
						if nargout < 1
							rethrow(ME);
						}
					}
				}
			}
		}
		
		function status = s}_command(obj, command)
			// S} command to GENESIS module
			status = false;
			temp = obj.reconnect;
			if ~obj.to_susp}
				obj.last_command = command;
				obj.to_w = -1;
				tic;
				try
					if obj.port.BytesAvailable >0
						fread(obj.port, obj.port.BytesAvailable);
					}
					fprintf(obj.port, "//s\n", command);
					obj.to_w = toc;
					status = true;
					if obj.log
						printl("COMMAND: //s\n", command);
					}
				catch ME
					if nargout < 1
						rethrow(ME);
					}
				}
			}
		}
		
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
				error("GENESIS read error.");
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
					error(["Unexpected response. " command, " -> ", resp]);
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
					error(["Read Value error form ", command, " " , valuetxt]);
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
