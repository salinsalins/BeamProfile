classdef GENESIS < handle
	%GENESIS Class for TDK-Lambda GENESIS Series Power Supplies
	
	%{
	a=instrhwinfo('serial');
	ports = instrfind;
	if numel(ports) > 0 
		fclose(ports);
		delete(ports);
	end
	
	cp = serial('COM7');
	set(cp, 'BaudRate', 19200, 'DataBits', 8, 'StopBits', 1);
	set(cp, 'Terminator', 'CR');
	set(cp, 'Timeout', 1);
	fopen(cp);
	g = GENESIS(cp,6)
	
	%}
	
	properties(Constant = true)
		addr_max = 30;
		addr_min = 0;
		MsgIdent = 'GENESIS';
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
		
		 set_addr_strict = true;
		 set_addr_retries = 3;

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
		function obj = GENESIS(comport, addr)
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
				obj.set_addr;
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
		
%{		
		function set.port(obj, comport)
		    obj.isvalidport(comport);
		    obj.port = comport;
		end
		
		function set.addr(obj, address)
		    obj.isvalidaddr(address);
		    if obj.isaddrattached(address)
		         error('Address is in use.');
		    end
		    obj.addr = address;
		    obj.attach_addr;
		end
%}

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
		
		function status = reconnect(obj)
			status = false;
			if obj.to_suspend
				if toc(obj.to_tic) > obj.to_toc
					obj.to_suspend = false;
					try
						oldserial = obj.serial;
						obj.to_tic = tic;
						obj.set_addr;
						newserial = obj.read_serial;
						if strcmpi(newserial, oldserial)
							obj.to_count = 0;
							obj.read('RCL');
							status = true;
						else
							error('Serial Number mismatch.');
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
		
		function [resp, status] = read(obj, command)
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
		
		function status = set_addr(obj)
			status = false;
			tempretries = obj.retries;
			obj.retries = obj.set_addr_retries;
			try
				obj.write('ADR', obj.addr);
				obj.retries = tempretries;
				if obj.set_addr_strict && ~strcmpi('', obj.serial)
					if ~strcmpi(obj.serial, obj.read_serial)
						error('GENESIS:SetAddress','GENESIS Wrong Serial Number.');
					end
				end
				status = true;
			catch ME
				obj.retries = tempretries;
				if nargout < 1
					error('GENESIS:SetAddress', 'GENESIS Set Address Error.');
				end
			end
		end
		
%{
		function status = set_addr(obj)
			status = false;
			n = max(obj.set_addr_retries, 0);
			while n >= 0
				if obj.write('ADR', obj.addr);
					status = true;
					return
				end
				n = n - 1;
			end
			if nargout < 1
				error('GENESIS Set Address Error.');
			end
		end
%}		
		
		function [name, status] = read_name(obj)
			% Read GENESIS Module Name
			[name, status] = obj.read('IDN?');
		end
		
		function [sn, status] = read_serial(obj)
			% Read GENESIS Module Serial Number
			[sn, status] = obj.read('SN?');
		end
		
		function [fversion, status] = read_firmware(obj)
			% Read GENESIS Module Firmware Version
			[fversion, status] = obj.read('REV?');
		end
		
		function [value, status] = read_value(obj, command)
			% Read numerical value from GENESIS module			
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
	
		function [resp, stat] = Read(obj, command)
			obj.valid;
			obj.set_addr;
			[resp, stat] = obj.read_value(command);
		end
		
		function stat = Write(obj, command, param)
			obj.valid;
			obj.set_addr;
			stat = obj.write(command, param);
		end
		
	end
end

