function MagnetPS_4
	
	pause on;
	clear all;
	delete(gcf);
	closecom;
	
%% Variables
	
% Program Name and Version
	ProgName = 'Magnet Power Supply Control';
	ProgNameShort = 'MagnetPS_';
	ProgVersion = '4';
	iniFileName = [ProgNameShort, ProgVersion, '.ini'];

% Output	
	% Output file
	outFileName = LogFileName(ProgNameShort, 'txt');
	outFilePath = 'D:\';
	outFile = [outFilePath, outFileName];
	out_fid = -1;

% Logical flags
	flag_stop = false;
	flag_hour = true;
	flag_out = true;
	flag_in = true;
	
% Error logging file
	logFileName = LogFileName(['D:\', ProgNameShort, ProgVersion], 'log');
	%log_fid = fopen(logFileName, 'at+', 'n', 'windows-1251');
	log_fid = 1;
	
% Colors
	cWHITE = [1, 1, 1];

%% Initialization

	printl(log_fid, '%s Version %s Started.\n', ProgName, ProgVersion);
	
	% Load saved configuration
	if numel(dir(iniFileName));
		load(iniFileName, '-mat');
	end
	
%% Create GUI

%% Main Figure hFig
pFig = [50, 50, 400, 600];
hFig = figure('Position', pFig);
set(hFig, 'Color', get(0,'defaultUicontrolBackgroundColor'), 'DockControls', 'off');
set(hFig, 'Name' , 'Magnet PS', 'NumberTitle', 'off', 'MenuBar', 'none');
set(hFig, 'Resize' , 'off', 'CloseRequestFcn', @FigCloseFun);

%% Button pannel: Start/Stop button hpStart, hCbStart, hBtnStart
ppStart = [3, 2, pFig(3)-3, 30];
hpStart = uipanel(hFig, 'Title', '', 'Units', 'pixels', ...
	'Visible', 'off', ...
	'Position', ppStart);
hCbStart = uicontrol(hpStart, 'Style', 'checkbox', 'String', '', ...
	'Position', [5, 2, 20, 25], ...
	'FontSize', 10, ...
	'Enable', 'off', ...
	'HorizontalAlignment', 'left');
hBtnStart = uicontrol(hpStart, 'Style', 'togglebutton', 'String', 'Start', ...
	'Position', Right(hCbStart, 130), ...
	'Callback', @cbStart);

setMax(hBtnStart);

%% Output select pannel hpOut, hCbOut, hTxtOut, hBtnOut
hpOut = uipanel(hFig, 'Title', 'Output', 'Units', 'pixels', ...
	'Position', Top(hpStart, 40));
hCbOut = uicontrol(hpOut, 'Style', 'checkbox', 'String', 'Write', ...
	'Position', [5, 1, 60, 25], ...
	'FontSize', 10, ...
	'HorizontalAlignment', 'right', ...
	'Callback', @cbCbOut);
hTxtOut = uicontrol(hpOut, 'Style', 'text',  'String', outFileName, ...
	'Position', [70, 5, pFig(3)-110, 21], ...
	'BackgroundColor', [1, 1, 1], ...
	'FontSize', 10, ...
	'HorizontalAlignment', 'center');
hBtnOut = uicontrol(hpOut, 'Style', 'pushbutton', 'String', '...', ...
	'Position', [pFig(3)-35, 3, 25, 25], ...
	'Callback', @cbBtnOut);

if out_fid > 0
	setMax(hCbOut);
else
	setMin(hCbOut);
end
out_fid = -1;

%% Config Table hTabConfig 
hTabConfig = uitable(hFig, 'Position', Top(hpOut, 100), ...
	'ColumnName', {'Use', 'Port', 'Address', 'U', 'I'}, ...
	'ColumnEditable', [true, true, true, true, true], ...
	'RowStriping', 'off', ...
	'FontSize', 10, ...
	'CellEditCallback', @cbTabConfig, ...
	'ColumnWidth', {50, 90, 50, 50, 50});
try 
	TableConfig;
catch
	TableConfig = { ...
		false, 'COM8', 6, 'r', 'g'; ...
		false, 'COM7', 6, '', ''; ...
		false, 'COM7', 7, '', ''};
end
set(hTabConfig, 'Data', TableConfig);

% Define a context menu; it is not attached to anything
hcmConfig = uicontextmenu;
% Define the context menu items and install their callbacks
itemAdd = uimenu(hcmConfig,'Label','Add New PS','Callback', @TabAddItem);
itemDel = uimenu(hcmConfig,'Label','Delete Last','Callback', @TabDeleteItem);
% Attach the context menu
set(hTabConfig, 'uicontextmenu', hcmConfig);

%% Indicaton and Operation Tables hTabInd, hTanOper
hTabOper = uitable(hFig, 'Position', Top(hTabConfig, [0, 5, 225, 150]), ...
	'ColumnName', {'ON', 'Set Voltage', 'Set Current'}, ...
	'FontSize', 16, ...
	'ColumnEditable', [true, true, true], ...
	'RowStriping', 'off', ...
	'CellEditCallback', @cbTabOper, ...
	'ColumnWidth', {30, 80, 80});
try
	TableOper;
catch
	TableOper = {false, 0, 0'; ...
		         false, 0, 0; ...
		         false, 0, 0};
end
set(hTabOper, 'Data', TableOper);

hTabInd = uitable(hFig, 'Position', Right(hTabOper, [0, 0, 172, 0]), ...
	'ColumnName', {'Read Voltage', 'Read Current'}, ...
	'FontSize', 16, ...
	'ColumnEditable', [false, false], ...
	'RowStriping', 'off', ...
	'RowName', [], ...
	'ColumnWidth', {80, 80});
try
	TableInd;
catch
	TableInd = {' ---.--', ' ---.--'; ...
		        ' ---.--', ' ---.--'; ...
		        ' ---.--', ' ---.--'};
end
set(hTabInd, 'Data', TableInd);

%% Plot pannel hpPlot, hAxesLeft, hAxesRight
ppPlot = Top(hTabOper, [0, 0, 395, 250]);
hpPlot = uipanel(hFig, 'Title', 'Power Supply Output', 'Units', 'pixels', ...
	'Position', ppPlot);
pAxesLeft = [50, 25, ppPlot(3)-95, ppPlot(4)-50];
hAxesLeft = axes('Parent', hpPlot, 'Unit', 'pixels', ...
	'ButtonDownFcn', @bdAxes, ...
	'XGrid', 'off', ...
	'Position', pAxesLeft);
ylabel(hAxesLeft, 'Voltage, V');
grid(hAxesLeft, 'on');
hAxesRight = axes('Parent', hpPlot, 'Unit', 'pixels', ...
	'Position', pAxesLeft, ...
	'YAxisLocation', 'right', ...
	'ButtonDownFcn', @bdAxes, ...
     'Color', 'none');
ylabel(hAxesRight, 'Current, A');

%%  Initialization before main loop

drawnow;

% Clocks
c0 = clock;
c1 = c0;
c2 = c0;

%% Main loop

while ~flag_stop
	% If input source was changed
	if flag_in
		flag_in = false;
		flag_out = true;

		%oldTableConfig = TableConfig;
		%[oldmPS, ~] = size(oldTableConfig);

		TableConfig = get(hTabConfig, 'Data');
		[mPS, ~] = size(TableConfig);
		
		% Delete GENESIS objects
		try 
			n = 0;
			n = numel(ps);
			if n > 0
				for i8=1:n
					try
						delete(ps(i8));
					catch
					end
				end
			end
		catch
		end
		
		% Create GENESIS objects
		ps(1:mPS) = GENESIS;
		
		%  Attach to com ports
		for ii=1:mPS
			portname = TableConfig{ii, 2};
			if TableConfig{ii, 1} && strcmpi(portname(1:3), 'COM')
				try
					ports = findopencom(portname);
					% If COM port does not exist
					if isempty(ports)
						% Create new COM port
						cp = serial(portname);
						set(cp, 'BaudRate', 19200, 'DataBits', 8, 'StopBits', 1);
						set(cp, 'Terminator', 'CR');
						set(cp, 'Timeout', 1);
					else
						cp = ports(1);
						if get(cp, 'BaudRate') ~= 19200 || get(cp, 'DataBits') ~= 8 || get(cp, 'StopBits') ~= 1 
							error('COM port incompatible configuration');
						end
					end
					% Open COM port
					if ~strcmpi(cp.status, 'open')
						fopen(cp);
					end
					% If open is sucessfull, create and attach GEN PS
					if strcmp(cp.status, 'open')
						% Create and attach GEN PS to COM port
						ps(ii) = GENESIS(cp, TableConfig{ii,3});
						ps(ii).valid;
						ps(ii).read('RCL');
						%ps(ii).log = true;
						printl('GENESIS PS has been created %s addr %i\n', portname, TableConfig{ii,3});
					else
						error('Error opening COM port');
					end
				catch ME
					printl('%s\n', ME.message);
					printl('GENESIS PS creation error %s addr %i\n', portname, TableConfig{ii,3});
					TableConfig{ii, 1} = false;
				end
			end
		end
		
		set(hTabConfig, 'Data', TableConfig);
		
		% Create data arrays for traces
		nx = 2000;      % number of trace points
		ny = 2*mPS+1;   % number of registered values + time
		% Resize existing data array or create new
		try
			[nx1, ny1] = size(data);
			if nx1 <= nx
				data = data(1:nx1, :);
			else
				data(nx1, end) = 0;
				data(nx+1:nx1, :) = 0;
			end
			if ny1 <= ny
				data = data(:, 1:ny1);
			else
				data(end, ny1) = 0;
				data(:, ny+1:ny1) = 0;
			end
		catch
			% Allocate array
			data = zeros(nx, ny);
		end
		
		% Reset zoom
		z0 = nx/2;
		z1 = 1;
		z2 = nx;
		zi = z1:z2;
		set(hAxesLeft, 'XLimMode', 'manual', 'XLim', [z1, z2]);
		set(hAxesRight, 'XLimMode', 'manual', 'XLim', [z1, z2]);

		
		% Delete previous traces
		try
			delete(trvh);
		catch
		end
		try
			delete(trch);
		catch
		end
		trvh = [];
		trch = [];
		set(hAxesLeft, 'Visible', 'off');
		set(hAxesRight, 'Visible', 'off');

		% Create new traces
		for ii=1:mPS
			if TableConfig{ii,1} 
				if ~isempty(TableConfig{ii,4})
					trvh(end+1) = line(zi, data(zi, ii*2), 'Parent', hAxesLeft, ...
						'ButtonDownFcn', @bdAxes, ...
						'UserData', ii*2, ...
						'Color', TableConfig{ii,4});
					set(hAxesLeft, 'Visible', 'on');
				end
				if ~isempty(TableConfig{ii,5})
					trch(end+1) = line(zi, data(zi, ii*2+1), 'Parent', hAxesRight, ...
						'ButtonDownFcn', @bdAxes, ...
						'UserData', ii*2+1, ...
						'Color', TableConfig{ii,5});
					set(hAxesRight, 'Visible', 'on');
				end
			end
		end
		
		% Create TableInd and TableOper
		TableInd = {};
		TableInd{mPS, 2} = '   ---.--';  
		TableInd(:) = {'   ---.--'};  
		TableOper = {};
		TableOper{mPS, 3} = [];  
		TableOper(:, 1) = {false};  
		TableOper(:, 2) = {0};  
		TableOper(:, 3) = {0};  

		% Connect existing PS to TableInd
		readSettings;
		readValues;
		set(hTabOper, 'Data', TableOper);
		set(hTabInd, 'Data', TableInd);
		writeSettings;
		drawnow

	end
	
	% If output was changed
	if flag_out
		flag_out = false;

		% Close output file
		if out_fid >= 0
			status = fclose(out_fid);
			out_fid = -1;
			printl('Output file has been closed\n');
		end
			
		% If writing to output is enabled
		if get(hCbOut,'Value') == get(hCbOut,'Max')
			% Open new output file
			outFileName = LogFileName(ProgNameShort, 'txt');
			outFile = [outFilePath, outFileName];
			out_fid = fopen(outFile, 'at+', 'n', 'windows-1251');
			if out_fid < 0
				printl('Output file %s open error\n', outFileName);
				% Disable output writing
				set(hCbOut,'Value', get(hCbOut,'Min'));
			else
				set(hTxtOut,  'String', outFileName);
				printl('Output file %s has been opened\n', outFile);
			end
		end
	end
	
	% If Start was pressed
	if true || get(hBtnStart, 'Value') == get(hBtnStart, 'Max')
		c1 = clock;

		% Change output file every hour
		if flag_hour && c1(4) ~= c0(4)
			c0 = c1;
			flag_out = true;
		end
			
		% Read data 
		c2 = clock;
		temp = data(nx, :);
		temp(1) = datenum(c2);
		readValues;
		set(hTabInd, 'Data', TableInd);
		
		for i7 = 1:mPS
			if TableConfig{i7,1} 
				try
					temp(i7*2) = TableInd{i7, 1};
					temp(i7*2+1) = TableInd{i7, 2};
				catch ME
					temp(i7*2) = 0;
					temp(i7*2+1) = 0;
				end
			end
		end
		
		% Save line with data to output file if writing is enabled
		if get(hCbOut, 'Value') == get(hCbOut, 'Max') && out_fid > 0
			% Separator is "; "
			sep = '; ';
			% Write time HH:MM:SS.SS
			fprintf(out_fid, ['%02.0f:%02.0f:%05.2f' sep], c2(4), c2(5), c2(6));
			% Data output format
			fmt = '%+09.4f';
			% Write data array
			fprintf(out_fid, [fmt sep], temp(2:end-1));
			% Write last value with NL instead of sepearator
			fprintf(out_fid, [fmt '\n'], temp(end));
		end
		
		% Shift data array
		data(1:nx-1, :) = data(2:nx, :);
		% Fill last data point
		data(nx, :) = temp;
		
		% Shift zoom
		z0 = z0-1;
		if z0 < 1
			z0 = fix((nx+1)/2);
			z1 = 1;
			z2 = nx;
		else
			if z1 > 1
				z1 = z1-1;
				z2 = z2-1;
			end
		end
		set(hAxesLeft, 'XLimMode', 'manual', 'XLim', [z1, z2]);
		set(hAxesRight, 'XLimMode', 'manual', 'XLim', [z1, z2]);

		zi = z1:z2;

		% Refresh data traces for current and voltage
		if numel(trvh) > 0
			for ii=1:numel(trvh)
				index = get(trvh(ii), 'UserData');
				set(trvh(ii), 'Ydata', data(zi, index));
				set(trvh(ii), 'Xdata', zi);
			end
		end
		if numel(trch) > 0
			for ii=1:numel(trch)
				index = get(trch(ii), 'UserData');
				set(trch(ii), 'Ydata', data(zi, index));
				set(trch(ii), 'Xdata', zi);
			end
		end
		
		% Refresh Figure
		drawnow
	else
		% Refresh Figure
		drawnow
	end
	
end
 
%% Quit procedures

save(iniFileName, 'outFileName', 'outFilePath', 'out_fid', ...
	'TableConfig', 'TableInd', 'TableOper', 'logFileName');

delete(hFig);

status = fclose('all');

% Delete all PS
try
	n = 0;
	n = numel(ps);
	if n > 0
		for i8=1:n
			try
				delete(ps(i8));
			catch
			end
		end
	end
catch
end
	
%closecom;

printl('%s Version %s Stopped.\n', ProgName, ProgVersion);

%% Callback functions

	function bdAxes(h, ~)
		cpoint = get(hAxesRight, 'CurrentPoint');
		button = get(hFig, 'SelectionType');
		x = cpoint(1, 1);
		y = cpoint(1, 2);
		%fprintf(1, 'x=%4.0f   y=%6.2f\n', x, y);
		dz = z2 - z1;
		switch button
			case 'normal'
				z0 = fix(x);
				dz = max(fix(dz/2), 30);
			case 'alt'
				dz = min(fix(dz*2), nx);
			case 'open'
				z0 = fix(nx/2);
				dz = nx;
		end
		z1 = max(fix(z0 - dz/2), 1);
		z2 = min(fix(z0 + dz/2), nx);
		
		% Find nearest data point
	end
	
	function cbTabConfig(~, ~)
		%if isempty(eventdata.Error)
		%	row = eventdata.Indices(1);
		%	col = eventdata.Indices(2);
		%end
		flag_in = true;
	end

	function cbTabOper(~, eventdata)
		TableOper =  get(hTabOper, 'Data');
		if isempty(eventdata.Error)
			row = eventdata.Indices(1);
			col = eventdata.Indices(2);
			if TableConfig{row, 1}
				try
					ps(row).set_addr;
					if col == 1
						if TableOper{row, col}
							ps(row).write('OUT', 1);
						else
							ps(row).write('OUT', 0);
						end
					elseif col == 2
						ps(row).write('PV', TableOper{row , col});
					elseif col == 3
						ps(row).write('PC', TableOper{row, col});
					end
					ps(row).read('SAV');
				catch ME
					printl('%s\n', ME.message);
					rethrow(ME);
				end
			end
		end
	end
	
	function TabAddItem(~, ~)
		TableConfig = get(hTabConfig, 'Data');
		[mPS, ~] = size(TableConfig);
		TableConfig(mPS+1,:) = TableConfig(mPS,:);
		TableConfig{mPS+1,1} = false;
		set(hTabConfig, 'Data', TableConfig);
		flag_in = true;
end
	
	function TabDeleteItem(~, ~)
		TableConfig = get(hTabConfig, 'Data');
		[mPS, ~] = size(TableConfig);
		if mPS > 1
			TableConfig = TableConfig(1:mPS-1,:);
			set(hTabConfig, 'Data', TableConfig);
			flag_in = true;
		end
	end
	
	function cbBtnOut(~, ~)
		[file_name, file_path] = uiputfile([outFilePath LogFileName(ProgNameShort)], 'Save to File');
		if ~isequal(file_name, 0)
			outFilePath = file_path;
            outFileName = file_name;
			outFile = [outFilePath, outFileName];
			set(hTxtOut,  'String', outFileName);
			flag_out = true;
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
		end
	end

	function FigCloseFun(~,~)
		flag_stop = true;
	end
	
%% Local functions
    function readSettings
		for i3 = 1:mPS
			if TableConfig{i3,1}
				try
					ps(i3).valid;
					ps(i3).set_addr;
					TableOper{i3, 2} = ps(i3).read_value('PV?');
					TableOper{i3, 3} = ps(i3).read_value('PC?');
					v = ps(i3).read('OUT?');
					if strcmpi(v,  'ON')
						TableOper{i3, 1} = true;
					elseif strcmpi(v,  'OFF')
						TableOper{i3, 1} = false;
					end
				catch ME
					TableOper{i3, 2} = '  ***.**';
					TableOper{i3, 3} = '  ***.**';
					printl('%s\n', ME.message);
				end
			end
		end
	end
	
	function writeSettings
		TableOper = get(hTabOper, 'Data');
		[m1, ~] = size(TableOper);
		for i2 = 1:m1
			if TableConfig{i2, 1}
				try
					ps(i2).valid;
					ps(i2).set_addr;
					ps(i2).write('PV', TableOper{i2 ,2});
					ps(i2).write('PC', TableOper{i2, 3});
					if TableOper{i2, 1}
						ps(i2).write('OUT', 1);
					else
						ps(i2).write('OUT', 0);
					end
					ps(i2).read('SAV');
				catch ME
					printl('%s\n', ME.message);
				end
			end
		end
	end

    function readValues
		for ii = 1:mPS
			if TableConfig{ii,1} 
				try
					ps(ii).valid;
					ps(ii).set_addr;
					TableInd{ii, 1} = ps(ii).read_value('MV?');
					TableInd{ii, 2} = ps(ii).read_value('MC?');
					%printl('%g %g %g\n', ps(ii).to_r, ps(ii).to_w, ps(ii).port.timeout);
				catch ME
					TableInd{ii, 1} = '  ***.**';
					TableInd{ii, 2} = '  ***.**';
					printl('%s\n', ME.message);
					printl('%s\n', ps(ii).last_msg);
					%printl('%g %g %g\n', ps(ii).to_r, ps(ii).to_w, ps(ii).port.timeout);
				end
			end
		end
	end

end

%% External functions
%% GUI object support functions	
	
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
		if numel(par) < 2
			p = [par(1), par(1), p0(3)-2*par(1), p0(4)-2*par(1)];
		elseif numel(par) < 4
			p = [par(1), par(2), p0(3)-2*par(1), p0(4)-2*par(2)];
		else
			if par(3) <= 0
				par(3) = p0(3)-2*par(1);
			end
			if par(3) > p0(3) 
				par(3) = p0(3)-2*par(1);
			end
			if par(4) <= 0
				par(4) = p0(4)-2*par(2);
			end
			if par(4) > p0(4)
				par(4) = p0(4)-2*par(2);
			end
			p = [par(1), par(2), par(3), par(4)];
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


