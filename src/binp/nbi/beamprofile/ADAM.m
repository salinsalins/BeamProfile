classdef ADAM < handle
	%ADAM Class for ADAM4xxx series devices
	%   Detailed explanation goes here

	%{
	a=instrhwinfo('serial');
	ports = instrfind;
	if numel(ports) > 0 
		fclose(ports);
		delete(ports);
	end
	
	cp = serial('COM6');
	set(cp, 'BaudRate', 38400, 'DataBits', 8, 'StopBits', 1);
	set(cp, 'Terminator', 'CR');
	set(cp, 'Timeout', 1);
	fopen(cp);
	
	%}
	
	properties(Constant = true)
		addr_max = 127;
		addr_min = 0;
		MsgIdent = 'ADAM';
	end
	
	properties
		port = -1;
		addr = -1;

		to_ctrl = true;
		to_r = 0;
		to_w = 0;
		to_min = 0.5;
		to_max = 2;
		to_fp = 2;
		to_fm = 3;
		to_tic = uint64(0);
		to_toc = 20;
		to_count = 0;
		to_countmax = 2;
		to_suspend = false;
		
		%set_addr_strict = true;
		%set_addr_retries = 3;

		read_rest = false;
		retries = 0;

		name = '';
		firmware = '';
		serial = '';
		
		last_command = '';
		last_response = '';
		last_count = 0;
		last_msg = '';
		msg = {};
		
		log = false;
	end
	
	methods
		function obj = ADAM(comport, addr)
			try
				msgIdent = [obj.MsgIdent, ':constructor'];

				% Default constructor
				if nargin == 0
					obj.last_msg = 'Default constructor';
					return
				end
				
				if nargin < 2
					error(msgIdent, 'Wrong nargin.');
				end
				obj.isvalidport(comport);
				obj.port = comport;
				obj.isvalidaddr(addr);
				if obj.isaddrattached
					error(msgIdent, 'Address is in use.');
				end
				obj.addr = addr;
				obj.attach_addr;
				%obj.set_addr;
				obj.name = obj.read_name;
				obj.firmware = obj.read_firmware;
				obj.serial = obj.read_serial;
				% Check if created object is valid
				obj.valid;
			catch ME
				if obj.log
					printl('%s\n', ME.message);
				end
				rethrow(ME);
			end
		end
		
		function delete(obj)
			try
				temp = obj.detach_addr;
				obj.addr = -1;
				obj.last_msg = 'Deleted';
				obj.name = '';
				obj.firmware = '';
				obj.serial = '';
			catch ME
				obj.last_msg = ['Delete ', ME.message];
			end
		end

		function status = detach_addr(obj)
			status = false;
			try
				if isa(obj.port, 'serial')
					index = obj.port.userdata ~= obj.addr;
					obj.port.userdata = obj.port.userdata(index);
					status = true;
				else
					error('Wrong COM port');
				end
			catch ME
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
		function status = attach_addr(obj)
			status = false;
			try
				obj.isvalidaddr;
				temp = obj.detach_addr;
				obj.port.userdata = [obj.port.userdata; obj.addr];
				status = true;
			catch ME
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
		function status = isvalidport(obj, comport)
			try
				status = false;
				if nargin < 2
					comport = obj.port;
				end
				if ~isa(comport, 'serial') || ~strcmpi(comport.status, 'open')
					obj.last_msg = 'Invalid COM port.';
					error(obj.last_msg);
				else
					status = true;
				end
			catch ME
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
		function status = isvalidaddr(obj, address)
			status = false;
			if nargin < 2
				address = obj.addr;
			end
			try
				if address < obj.addr_min || address > obj.addr_max
					obj.last_msg = 'Invalid address.';
					error(obj.last_msg);
				else
					status = true;
				end
			catch ME
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
		function status = isaddrattached(obj, addr)
			% Is address in use on com port
			try
				status = false;
				if nargin < 2
					addr = obj.addr;
				end
				if isa(obj, 'GENESIS')
					comport = obj.port;
				elseif isa(obj, 'serial')
					comport = obj;
				else
					error('Wron object.');
				end
				if isempty(find(comport.userdata == addr, 1))
					if isa(obj, 'GENESIS')
						obj.last_msg = 'Address is not attched.';
					end
					error('Address is not attched.');
				else
					status = true;
				end
			catch ME
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
		function status = isinitialized(obj)
			try
				status = false;
				if isempty(obj.name) || isempty(obj.serial)
					obj.last_msg = 'Module is not initialized.';
					error(obj.last_msg);
				else
					status = true;
				end
			catch ME
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
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
				end
			end
		end
		
		function status = reconnect(obj)
			status = false;
			if obj.to_suspend
				if toc(obj.to_tic) > obj.to_toc
					obj.to_suspend = false;
					try
						oldname = obj.name;
						obj.to_tic = tic;
						newname = obj.read_name;
						if strcmpi(newname, oldname)
							obj.to_count = 0;
							status = true;
						else
							error('Module name mismatch.');
						end
					catch ME
						obj.to_suspend = true;
						obj.to_tic = tic;
						if nargout < 1
							rethrow(ME);
						end
					end
				end
			end
		end
		
		function status = send_command(obj, command)
			% Send command to GENESIS module
			status = false;
			temp = obj.reconnect;
			if ~obj.to_suspend
				obj.last_command = command;
				obj.to_w = -1;
				tic;
				try
					if obj.port.BytesAvailable >0
						fread(obj.port, obj.port.BytesAvailable);
					end
					fprintf(obj.port, '%s\n', command);
					obj.to_w = toc;
					status = true;
					if obj.log
						printl('COMMAND: %s\n', command);
					end
				catch ME
					if nargout < 1
						rethrow(ME);
					end
				end
			end
		end
		
		function [resp, status] = read_response(obj)
			% Read response form GENESIS module
			resp = '';
			status = false;
			obj.msg = {};

			% Perform n reties to read response
			n = obj.retries;
			while n >= 0
				n = n - 1;
				obj.to_r = -1;
				obj.read_fgetl;
				read_error = ~strcmp(obj.last_msg, '');
				if ~read_error
					resp = obj.last_response;
					status = true;
					break
				end
				if obj.read_rest
					obj.read_fgetl;
				end
			end
			% Correct timeout
			obj.correct_timeout;

			if read_error && nargout < 2
				error('GENESIS read error.');
			end
		end
		
		function read_fgetl(obj)
			if ~obj.to_suspend
				obj.to_tic = tic;
				tic;
				[resp, count, message] = fgetl(obj.port);
				if obj.log
					printl('RESPONSE: %s\n', resp);
				end
				obj.to_r = max(obj.to_r, toc);
				obj.last_response = resp;
				obj.last_count = count;
				obj.last_msg = message;
				if ~strcmpi(message, '')
					if obj.log
						printl('MESSAGE: %s\n', message);
					end
					obj.msg = {obj.msg{:}, message};
					obj.to_count = obj.to_count + 1;
					if obj.to_count > obj.to_countmax
						obj.to_tic = tic;
						obj.to_suspend = true;
					end
				else
					obj.to_count = 0;
					obj.to_suspend = false;
				end
			else
				obj.last_msg = 'Suspended';
				obj.msg = {obj.msg{:}, obj.last_msg};
				if obj.log
					printl('%s\n', obj.last_msg);
				end
			end
		end

		function correct_timeout(obj)
			% Correct timeout
			if obj.to_ctrl
				dt = max(obj.to_r, obj.to_w);
				if dt >= obj.port.timeout*0.9
					newto = min(obj.to_fp*obj.port.timeout, obj.to_max);
					if obj.port.timeout < newto
						obj.port.timeout = newto;
						obj.msg = {obj.msg{:}, sprintf('Timeout+ %d %d', obj.port.timeout, dt)};
						if obj.log
							printl('Timeout+ %d %d\n', obj.port.timeout, dt);
						end
					end
				else
					newto = max(obj.to_fm*dt, obj.to_min);
					if obj.port.timeout > newto
						obj.port.timeout = newto;
						obj.msg = {obj.msg{:}, sprintf('Timeout- %d %d', obj.port.timeout, dt)};
						if obj.log
							printl('Timeout- %d %d\n', obj.port.timeout, dt);
						end
					end
				end
			end
		end
		
		function status = set_addr(obj)
			status = true;
		end
		
		function [resp, status] = execute(obj, command)
			% Send command and read response form ADAM
			status = false;
			try
				obj.send_command(command);
				resp = obj.read_response;
				status = true;
			catch ME
				%printl('%s\n', ME.message);
				%printl('%s %i\n', ME.message, nargout);
				if nargout < 2
					rethrow(ME);
				end
			end
		end
		
		function [data, status] = execute_format(obj, fmt)
			% Execute command for ADAM address with format string fmt
			data = '';
			status = false;
			try
				cmd = sprintf(fmt, obj.addr);
				s = obj.execute(cmd);
				[data, status] = obj.isok(s);
			catch ME
			end
		end
		
		function [outstr, status] = isok(obj, instr)
			status = false;
			outstr = '';
			if length(instr) > 3
				if strcmp(instr(1:3), sprintf('!%02X', obj.addr))
					status = true;
					outstr = instr(4:end);
				end
			end
		end
		
		function [name, status] = read_name(obj)
			% Read Module Name.  Command: $AAM
			[name, status] = obj.execute_format('$%02XM');
		end
		
		function [sn, status] = read_serial(obj)
			% Read Module Serial Number
			sn = 'not implemented';
			status = true;
		end
		
		function [version, status] = read_firmware(obj)
			% Read Module Firware Version.  Command: $AAF
			[version, status] = obj.execute_format('$%02XF');
		end
		
		function outstr = read_str(obj, chan)
			outstr = '';
			if nargin <= 1
				% Compose command to Read All Channels  #AA
				command = sprintf('#%02X', obj.addr);
				outstr = obj.execute(command);
				return
			end
			
			if (chan < 0) || (chan > 8)
				return
			end
			
			% Compose command to Read One Channel  #AAN
			command = sprintf('#%02X%1X', obj.addr, chan);
			outstr = obj.execute(command);
		end

		function [data, n] = read(obj, chan)
			data = [];
			n = 0;
			if nargin <= 1
				outstr = read_str(obj);
			else
				outstr = read_str(obj, chan);
			end
			[data, n] = sscanf(outstr(2:end), '%f');
		end
		
%{		
		function status = write(obj, command, param)
			status = false;
			if nargin >= 3
				if isnumeric(param)
					cmd = sprintf('%s %g', command, param);
				elseif isa(param, 'char')
					cmd = [command, ' ', param];
				else
					cmd = command;
				end
			else
				cmd = command;
			end
			try
				obj.send_command(cmd);
				resp = obj.read_response;
				if strcmpi(resp, 'OK')
					status = true;
				else
					error(['Unexpected response. ' command, ' -> ', resp]);
				end
			catch ME
				%printl('%s\n', ME.message);
				if nargout < 1
					rethrow(ME);
				end
			end
		end
		
		function [value, status] = read_value(obj, command)
			% Read Module Firmware Version
			try
				valuetxt = obj.read(command);
				[value, status] = sscanf(valuetxt, '%g');
				if status == 1
					status = true;
					return
				else
					error(['Read Value error form ', command, ' ' , valuetxt]);
				end
			catch ME
				value = [];
				status = false;
				%printl('%s\n', ME.message);
				if nargout < 2
					rethrow(ME);
				end
			end
		end

%}	
	end
end

