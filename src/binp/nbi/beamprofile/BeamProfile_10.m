function BeamProfile_10
	clear all;
	delete(gcf);
	closecom;
	
%% Variables

%% Begin of operation
    printl(log_fid, '%s Version %s Started.\n', ProgName, ProgVersion);
    if numel(dir(iniFileName));
        load(iniFileName, '-mat');
    end
	
%% Create GUI

%% Main Figure hFig
%% Start/Stop, Config and other buttons hp6, nBtn1, hBtn4, hBtn5
%% Input select pannel hp1, hLb2, hEd1, hEd4, hEd8, hEd9, hBtn3 
if out_fid > 0
    set(hCbOut, 'Value', get(hCbOut, 'Max'));
else
    set(hCbOut, 'Value', get(hCbOut, 'Min'));
end
out_fid = -1;
%% Log pannel hp3, hTxt1
%% Config pannel hpConf, hCbSplitOut, hCbMax1Prof, hTxt6, hTxt7, hTxt8, hTxt9, hEd6, hEd7
set(hCbSplitOut,'Value', get(hCbSplitOut,'Max'));
set(hCbMax1Prof,'Value', get(hCbMax1Prof,'Max'));
setMax(hCbVoltage);
set(hCbVoltage, 'Enable', 'off');
setMax(hCbFlow);
set(hCbDuration, 'Enable', 'off');

%% Calorimeter plot pannel hp4, hAxes1, hAxes2, hAxes3
pp4 = [pp3(1), pp3(2)+pp3(4)+5, pFig(3)-3, pFig(4)-pp3(2)-pp3(4)-10];
ylabel(hAxes1, 'Temperature, C');
ylabel(hAxes2, 'Temperature, C');
ylabel(hAxes3, 'Beam Current, mA');
%% Targeting plot pannel hp6, hAxes4, hAxes5, hAxes6
ylabel(hAxes4, 'Voltage, V');
ylabel(hAxes5, 'Voltage, V');
ylabel(hAxes6, 'Voltage, V');
grid(hAxes6, 'on');


%%  Initialization before main loop
drawnow;
cbInputPannel;
cbBtn4(hBtn4);

% Add lines of targeting traces
for ii = 1:numel(tpn)
	tph(ii) = line(1:nx, data(:, tpn(ii)), 'Parent', hAxes5, 'Color', tpc(ii));
	tph1(ii) = line(1:2*tpw+1, data(1:2*tpw+1, tpn(ii)), 'Parent', hAxes4, 'Color', tpc(ii));
end

% Add lines of acceleration grid traces
for ii = 1:numel(agn)
    agh(ii) = line(1:nx, data(:, agn(ii)), 'Parent', hAxes6, 'Color', agc(ii));
end

% Add lines of temperature traces
for ii = 1:numel(trn)
    trh(ii) = line(1:nx, data(:, trn(ii)), 'Parent', hAxes2, 'Color', trc(ii));
end

% Add line for beam current
current = (data(:, bctout)-data(:, bctin))*Q*flow/voltage;  %mA
bch = line(1:nx, current, 'Parent', hAxes3, 'Color', 'k', ...
		'ButtonDownFcn', @bdAxes3);

% Add lines of initial profiles
% Vertical profile
prof1h = line(p1x, prof1, 'Parent', hAxes1, 'Color', 'r', 'Linewidth', 2, 'Marker', '.');
% Horizontal profile
prof2h = line(p2x, prof2, 'Parent', hAxes1, 'Color', 'g', 'Linewidth', 2, 'Marker', '.');
% Faded profiles
for ii = 1:numel(fpi)
	color = [0.5 0.5 0.5]*(2*numel(fpi)-1-ii)/(numel(fpi)-1);
	fph(ii) = line(p1x, prof1, 'Parent', hAxes1, 'Color', color, 'Linewidth', 1, 'Marker', '.');
end
% Max profile
prof1maxh = line(p1x, prof1max, 'Parent', hAxes1, 'Color', [1 0 1], 'Linewidth', 2, 'Marker', '.');
% Max1 profile
prof1max1h = line(p1x, prof1max1, 'Parent', hAxes1, 'Color', [0.5 0.5 1], 'Linewidth', 2, 'Marker', '.');

% Max horiz. profile
prof2maxh = line(p2x, prof2max, 'Parent', hAxes1, 'Color', [0 1 1], 'Linewidth', 2, 'Marker', '.');

% Create max beam current annotation
bcmaxh = text(0.75, 0.9, sprintf('%5.1f mA', bcmax), ...
	'Parent', hAxes3, 'Units', 'normalized');
bcch = text(0.75, 0.8, sprintf('%5.1f mA', bcmax), ...
	'Parent', hAxes3, 'Units', 'normalized');

% Create Marker
mw = 100;
mc = 'r';
mi = nx - mw;
mi1 = max(mi - mw, 1);
mi2 = min(mi + mw, nx);
% Marker trace
mh = line(mi1:mi2, (mi1:mi2)*0, 'Parent', hAxes3, 'Color', mc, 'LineWidth', 2);


%% Main loop

%% Quit procedures

save(iniFileName, 'outFileName', 'outFilePath', 'out_fid', ...
	'in_file_name', 'in_file_path', 'in_fid', ...
	'voltage', 'duration', 'flow');

delete(hFig);

status = fclose('all');

DeleteADAMs;

%closecom;

printl('%s Version %s Stopped.\n', ProgName, ProgVersion);

%% Callback functions

	function bdAxes3(~, ~)
		cpoint = get(hAxes3, 'CurrentPoint');
		mi = fix(cpoint(1,1));
		mi1 = mi-mw;
		if mi1 < 1
			mi1 = 1;
		end
		mi2 = mi+mw;
		if mi2 > nx
			mi2 = nx;
		end
		[~, mi] = max(current(mi1:mi2));
		mi = mi + mi1;
	end
	
	function cbBtnOut(~, ~)
		[file_name, file_path] = uiputfile([outFilePath LogFileName()], 'Save Log to File');
		if ~isequal(file_name, 0)
			outFilePath = file_path;
            outFileName = file_name;
			outFile = [outFilePath, outFileName];
			set(hTxtOut,  'String', outFileName);
			flag_out = true;
		end
	end

	function cbInSelect(~, ~)
		[file_name, file_path] = uigetfile([in_file_path in_file_name],'Read from File');
		if ~isequal(file_name, 0)
			in_file_path = file_path;
			in_file_name = file_name;
			in_file = [in_file_path, in_file_name];
			set(hEd1, 'String', in_file_name);
			flag_in = true;
		end
	end

	function cbMax1Prof(hObj, ~)
		if get(hObj, 'Value') == get(hObj, 'Min')
			set(prof1max1h, 'Visible', 'off');
		else
			set(prof1max1h, 'Visible', 'on');
		end
	end
	
	function cbSplitOut(hObj, ~)
		if get(hObj, 'Value') == get(hObj, 'Min')
				flag_hour = false;
		else
				flag_hour = true;
		end
	end
	
	function cbInputPannel(~, ~)
		flag_in = true;
		value = get(hPm1, 'Value');
		st = get(hPm1, 'String');
		if strcmp(st(value), 'FILE');
			set(hEd4, 'Visible', 'off');
			set(hEd5, 'Visible', 'off');
			set(hEd8, 'Visible', 'off');
			set(hEd9, 'Visible', 'off');
			set(hEd1, 'Visible', 'on');
			set(hBtn3, 'Visible', 'on');
		else
			set(hEd4, 'Visible', 'on');
			set(hEd5, 'Visible', 'on');
			set(hEd8, 'Visible', 'on');
			set(hEd9, 'Visible', 'on');
			set(hEd1, 'Visible', 'off');
			set(hBtn3, 'Visible', 'off');
		end
	end

	function cbCbOut(~, ~)
		flag_out = true;
	end
 
	function cbStart(hObj, ~)
		if get(hObj, 'Value') == get(hObj, 'Min')
			set(hObj, 'String', 'Start');
		else
			set(hObj, 'String', 'Stop');
			prof1max(:) = 1;
		end
	end

	function cbBtn4(hObj, ~)
		if get(hObj, 'Value') == get(hObj, 'Min')
			set(hObj, 'String', 'Config');
			set(hp3, 'Visible', 'on');
			set(hpConf, 'Visible', 'off');
		else
			set(hObj, 'String', 'Log');
			set(hp3, 'Visible', 'off');
			set(hpConf, 'Visible', 'on');
		end
	end
	
	function cbTargeting(hObj, ~)
		if get(hObj, 'Value') == get(hObj, 'Min')
			set(hp4, 'Visible', 'on');
			set(hp6, 'Visible', 'off');
			set(hObj, 'String', 'Targeting');
		else
			set(hp4, 'Visible', 'off');
			set(hp6, 'Visible', 'on');
			set(hObj, 'String', 'Calorimeter');
		end
	end
	
	function FigCloseFun(~,~)
		flag_stop = true;
	end

	function cbVoltage(~ ,~)
		if isMax(hCbVoltage)
			[v, n] = sscanf(get(hEdVoltage, 'String'), '%f');
			if n >= 1
				voltage = v(1);
			else
				set(hEdVoltage, 'String', sprintf('%4.1f', voltage));
			end
		end
	end
	
	function cbFlow(~ ,~)
		if isMax(hCbFlow)
			[v, n] = sscanf(get(hEdFlow, 'String'), '%f');
			if n >= 1
				flow = v(1);
			else
				set(hEdFlow, 'String', sprintf('%4.1f', flow));
			end
		end
	end
	
	function cbDuration(h,~)
		if isMax(hCbDuration)
			[v, n] = sscanf(get(hEdDuration, 'String'), '%f');
			if n >= 1
				duration = v(1);
			else
				set(hEdDuration, 'String', sprintf('%4.1f', duration));
			end
		end
	end
	
	%% Local functions

	function DeleteADAMs
            try
                n = numel(adams);
                if n > 0
                    for ida=1:n
                        try
                            delete(adams(ida));
                        catch
                        end
                    end
                end
            catch
            end
	end

	function result = CreateADAMs
            % Create ADAM objects
            si = get(hPm1, 'Value');
            st = get(hPm1, 'String');
            portname = char(st(si));
            addr(1) = sscanf(get(hEd4, 'String'),'%d');
            addr(2) = sscanf(get(hEd5, 'String'),'%d');
            addr(3) = sscanf(get(hEd8, 'String'),'%d');
            addr(4) = sscanf(get(hEd9, 'String'),'%d');

            result(1:numel(addr)) = ADAM;
            %  Attach to com ports
            for ica=1:numel(addr)
                if strncmpi(portname, 'COM', 3)
                    try
                        ports = findopencom(portname);
                        % If COM port does not exist
                        if isempty(ports)
                            % Create new COM port
                            cp = serial(portname);
                            set(cp, 'BaudRate', 38400, 'DataBits', 8, 'StopBits', 1);
                            set(cp, 'Terminator', 'CR');
                            set(cp, 'Timeout', 1);
                        else
                            cp = ports(1);
                            if get(cp, 'BaudRate') ~= 38400 || get(cp, 'DataBits') ~= 8 || get(cp, 'StopBits') ~= 1 
                                error('COM port incompatible configuration');
                            end
                        end
                        % Open COM port
                        if ~strcmpi(cp.status, 'open')
                            fopen(cp);
                        end
                        % If open is sucessfull, create and attach ADAM
                        if strcmp(cp.status, 'open')
                            result(ica) = ADAM(cp, addr(ica));
                            result(ica).valid;
                            printl('ADAM has been created %s addr %i\n', portname, addr(ica));
                            cp_open = true;
                        else
                            cp_open = false;
                            % Find FILE in combo box list
                            for si = 1:numel(st)
                                if strncmpi(st(val), 'FILE', 4)
                                    set(hPm1,'Value', si);
                                end
                            end
                            cbInputPannel(hPm1);
                            % Swith to stop state
                            set(hBtn1, 'Value', get(hBtn1, 'Min'));
                            cbStart(hBtn1);

                        error('ADAM creation error %s addr %i', portname, addr(ica));
                    end
                    catch ME
                        printl('%s\n', ME.message);
                    end
                end
                    if strncmpi(portname, 'FILE', 4)
                            % Open input file
                            in_file = [in_file_path, in_file_name];
                            in_fid = fopen(in_file);
                            if in_fid > 2
                                    set(hEd1, 'String', in_file_name);
                                    printl('Input file %s has been opened\n', in_file);
                                    break
                            else
                                    in_fid = -1;
                                    printl('Input file open error\n');
                                    set(hBtn1, 'Value', get(hBtn1, 'Min'));
                                    cbStart(hBtn1);
                            end
                    end
            end
	end

	function fidout = CloseFile(fidin)
		% Close file if if was opened
		if fidin > 0
			status = fclose(fidin);
			if status == 0
				printl('File %d has been closed.\n', fopen(fidin));
			end
			fidout = -1;
		end
	end
	
	function [t, ai] = ADAM4118_read(cp_obj, adr)
		ai = ADAM4118_readstr(cp_obj, adr);
		[t, n] = sscanf(ai(2:end), '%f');
		if n < 8
			t(1:8) = 0;
			printl('ADAM %02X %s\n', adr, ai);
		end
		t(t == 888888) = 0;
	end
		
	function result = ADAM4118_readstr(cp_obj, adr)
		result = '';
		if (adr < 0) || (adr > 255) 
			return
		end
		
		if in_fid > 2
			result = ReadFromFile(in_fid);
			return;
		else
			if cp_open
				result = ReadFromCOM(cp_obj, adr);
			end
		end
	end
		
	function result = ReadFromFile(fid)
			persistent rffs;
			persistent rffn;
			persistent rffd;
			if isempty(rffn)
				rffn = 0;
			end
			result = '';
			if fid < 0
				return
			end
			if rffn <= 0
				rffs = fgetl(fid);
				n = strfind(rffs, ';');
				[rffd, rffn] = sscanf(rffs((n(1)+2):end), '%f; ');
				cr1 = datevec([rffs(1:n(1)-1) 0], 'HH:MM:SS.FFF');
				cr(3:6) = cr1(3:6);
				if rffn < 24
					rffd((rffn+1):24) = 0;
				end
				rffn = 1;
			end
			result = ['<' sprintf('%+07.3f', rffd(rffn:rffn+7))];
			rffn = rffn + 8;
			if rffn > 24
				rffn = 0;
			end
			if feof(fid)
				frewind(fid);
			end
			%pause(0.01);
		end
		
	function result = ReadFromCOM(cp, adr)
			to_ctrl = true;
			to_min = 0.5;
			to_max = 2;
			to_fp = 2;
			to_fm = 3;
			read_rest = true;
			retries = 0;
			
			if (adr < 0) || (adr > 255)
				return
			end
		
			% Compose command Read All Channels  #AA
			command = ['#', sprintf('%02X', adr)];
			
			% Send command to ADAM4118
			tic;
			fprintf(cp, '%s\n', command);
			dt1 = toc;
			
			% Read response form ADAM4118
			while retries > -1
				retries = retries - 1;
				tic;
				[result, ~, msg] = fgetl(cp);
				dt2 = toc;
				read_error = ~strcmp(msg,  '');
				if ~read_error
					break
				end
				printl('ADAM Read error %d  "%s" %s\n', retries, result, msg);
				if read_rest
					[result1, ~, msg1] = fgetl(cp);
					printl('ADAM Read rest  "%s" %s\n', result1, msg1);
				end
			end
			
			% Correct timeout
			dt = max(dt1, dt2);
			if to_ctrl
				if read_error
					cp.timeout = min(to_fp*cp.timeout, to_max);
					printl('ADAM Timeout+ %4.2f %4.2f\n', cp.timeout, dt);
				else
					if cp.timeout > to_min && cp.timeout > to_fm*dt
						cp.timeout = max(to_fm*dt, to_min);
						printl('ADAM Timeout- %4.2f %4.2f\n', cp.timeout, dt);
					end
				end
			end
		end
		
	function scroll_log(h, instr)
		s = get(h, 'String');
		for i=2:numel(s)
			s{i-1} = s{i};
		end
		s{numel(s)} = instr;
		set(h, 'String', s);
	end
	
	function v = getVal(hObj)
		v = get(hObj, 'Value');
	end
	
    function setMax(hObj)
		set(hObj, 'Value', get(hObj, 'Max'));
	end
	
    function setMin(hObj)
		set(hObj, 'Value', get(hObj, 'Min'));
	end
	
    function v = isMax(hObj)
		v = (get(hObj, 'Value') == get(hObj, 'Max'));
	end
	
    function v = isMin(hObj)
		v = (get(hObj, 'Value') == get(hObj, 'Min'));
	end
	
	function v = isVal(hObj, val)
		v = (get(hObj, 'Value') == val);
	end
	
    function p = In(hObj, par)
		p0 = get(hObj, 'Position');
		if nargin < 2
			par = [5, 5, p0(3)-10, p0(4)-10];
		end
		if numel(par) < 4
			p = [par(1), par(1), p0(3)-2*par(1), p0(4)-2*par(1)];
		else
			if par(3) == 0 
				par(3) = p0(3);
			end
			if par(4) == 0 
				par(4) = p0(4);
			end
			p = [par(1), par(2), par(3)-2*par(1), par(4)-2*par(2)];
		end
	end
	
	function p = Right(hObj, par)
		p0 = get(hObj, 'Position');
		if nargin < 2
			par = [5, 0, p0(3), p0(4)];
		end
		if numel(par) ~= 4
			p = [p0(1)+p0(3)+5, p0(2), par(1), p0(4)];
		else
			if par(3) == 0 
				par(3) = p0(3);
			end
			if par(4) == 0 
				par(4) = p0(4);
			end
			p = [p0(1)+p0(3)+par(1), p0(2)+par(2), par(3), par(4)];
		end
	end
	
	function p = Top(hObj, par)
		p0 = get(hObj, 'Position');
		if nargin < 2
			par = [0, 5, p0(3), p0(4)];
		end
		if numel(par) ~= 4
			p = [p0(1), p0(2)+p0(4)+5, p0(3), par(1)];
		else
			if par(3) == 0 
				par(3) = p0(3);
			end
			if par(4) == 0 
				par(4) = p0(4);
			end
			p = [p0(1)+par(1), p0(2)+p0(4)+par(2), par(3), par(4)];
		end
	end
	
end

%% External functions


