function BeamProfile
	pause on;
	clear all;
	delete(gcf);
	closecom;
	
%% Variables
	
	% Programm Name and Version
	ProgName = 'Calorimeter Beam Profile';
	ProgVersion = '9';
	iniFileName = ['BeamProfile' ProgVersion, '.ini'];

% Output	
	% Output file
	out_file_name = LogFileName();
	out_file_path = 'D:\';
	out_file = [out_file_path, out_file_name];
	out_fid = -1;

% Input
	% COM Port
	cp_obj = -1;
	cp_open = false;
	% Adresses of ADAMs
	addr1 = 3;
	addr2 = 4;
	addr3 = 2;
	addr4 = 5;
	% Input file
	in_file_name = 'ADAMTempLog_2014-12-30-13-00-00.txt';
	in_file_path = '.\2014-12-30\';
	in_file = [in_file_path, in_file_name];
	in_fid = -1;
	
% Logical flags
	flag_stop = false;
	flag_hour = true;
	flag_out = true;
	flag_in = false;
	
% Data arrays for traces
	nx = 10*60*3;    % number of trace points
	ny = 25;              % number of registered temperatures + time
	% Preallocate arrays
	data = zeros(nx, ny);        % traces
	dmin = zeros(1, ny);         % minimal temperatures
	dmax = zeros(1, ny);        % maximal temperatures
	
% Profile arrays and their plot handles
	p1range = [2:7, 10:14];      % Channels for vertical profile
	p1x = [1,3:11,13];               % X values for prof1
	p2range = [16, 7, 15];       % Channels for horizontal profile
	p2x = [3, 7, 11];                 % X values for prof2
	prof1  = data(nx, p1range);    % Vertical profile and handle
	prof1h = 0;
	prof2  = data(nx, p2range);   % Horizontal profile
	prof2h = 0;
	prof1max  = prof1;            % Maximal vertical profile (over the plot)
	prof1max(:)  = 1;
	prof1maxh = 0;                 % Maximal vertical profile handle
	prof1max1  = prof1max;   % Maximal vertical profile (from the program start)
	prof1max1h = 0;                % Handle
	prof2max  = prof2;           % Maximal horizontal profile (ofer the plot)
	prof2max(:)  = 1;
	prof2maxh = 0;
	% Faded profiles
	fpn = 10;             % Number of faded pofiles
	fpi(1:fpn) = nx;   % Faded pofiles indexes
	fph = fpi*0;         % Faded pofile plot handles
	fpdt = 0.5;           % Faded pofile time inteval [s]

% Traces to plot
	trn = [7, 3, 11];    % Chaneel numbers of traces
	trc = ['r';'g';'b'];  % Colors of traces
	trh = trn*0;         % Handles of traces
	
% Beam current calculations and plot
	voltage = 80.0;  % keV Particles energy
	duration = 2;      % s Beam duration
	flow = 12.5;        % gpm Cooling water flow (gallons per minute) 
	bctin = 9;            % Input water temperature channel number
	bctout = 8;         % Output water temperature channel number
	% Current[mA] =	folw[gpm]*(OutputTemperature-InputTemperature)*Q/voltage
	Q = 4.3*0.0639*1000; % Coeff to convert 
	bch = 0;         % Handle for beam current plot
	bcmax = 0;    % Max beam current on the screen
	bcmax1 = 0;  % MaxMax beam current
	bcmaxh = 0;   % Handle of max current text
	bcflowchan = 22;  % Channel number for flowmeter output
	bcv2flow = 12;       % V/gpm Conversion coefficienf for flowmeter 
	
% Acceleration electrode voltage and current
	agvn = 23;
	agcn = 24;
	agn = [agvn, agcn];
	agc = ['r';'g'];      % Colors of traces
	agh = agc*0;       % Handles of traces

% Targeting plots
	tpt = 18;
	tpb = 19;
	tpl = 20;
	tpr = 21;
	tpn = [tpt, tpb, tpl, tpr];   % Channel numbers of traces
	tpc = ['r'; 'g'; 'b'; 'm'];        % Colors of traces
	tph = tpn*0;                        % Handles of traces
	tph1 = tpn*0;                      % Handles of traces zoom
	tpw = 30;                             % +- Zoom window halfwidth
	
% Error logging file
	elog_file = 'BeamProfileLog.txt';
	elog_fid = 1;
	%elog_fid = fopen(elog_file, 'at+', 'n', 'windows-1251');
	if elog_fid < 0
		elog_fid = 1;
	end
	
% Colors
	WHITE = [1, 1, 1];

%% Begin of operation

	printl(elog_fid, '%s Version %s Started.\n', ProgName, ProgVersion);
	
	if numel(dir(iniFileName));
		load(iniFileName, '-mat');
	end
	
%% Create GUI

%% Main Figure hFig
pFig = [50, 50, 400, 600];
hFig = figure('Position', pFig);
set(hFig, 'Color', get(0,'defaultUicontrolBackgroundColor'), 'DockControls', 'off');
set(hFig, 'Name' , 'Calorimeter Profile', 'NumberTitle', 'off', 'MenuBar', 'none');
set(hFig, 'Resize' , 'off', 'CloseRequestFcn', @FigCloseFun);

%% Start/Stop, Config and other buttons hp6, nBtn1, hBtn4, hBtn5
pp6 = [3, 2, pFig(3)-3, 31];
hp6 = uipanel(hFig, 'Title', '', 'Units', 'pixels', ...
	'Position', pp6);
hBtn1 = uicontrol(hp6, 'Style', 'togglebutton', 'String', 'Start', ...
	'Position', [10, 2, 150, 25], ...
	'Callback', @cbStart);
hBtn4 = uicontrol(hp6, 'Style', 'togglebutton', 'String', 'Config', ...
	'Position', [170, 2, 100, 25], ...
	'Callback', @cbBtn4);
set(hBtn4,'Value', get(hBtn4,'Max'));
hBtn5 = uicontrol(hp6, 'Style', 'togglebutton', 'String', 'Targeting', ...
	'Position', Right(hBtn4, 100), ...
	'Callback', @cbTargeting);

%% Input select pannel hp1, hLb2, hEd1, hEd4, hEd8, hEd9, hBtn3 
pp1 = [pp6(1), pp6(2)+pp6(4)+5, pFig(3)-3, 40];
hp1 = uipanel(hFig, 'Title', 'Input', 'Units', 'pixels', ...
	'Position', pp1);

hPm1 = uicontrol(hp1, 'Style', 'popupmenu', 'String', {'COM6', 'FILE', 'COM1', 'COM2',...
	'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9', 'COM10' }, ...
	'Callback', @cbInputPannel, ...
	'Position', [5, 1, 60, 25]);
if in_fid > 0
	set(hPm1, 'Value', 2);
	in_fid = -1;
end

ped4 = Right(hPm1, [5, 2, 30, 25]);
hEd4 = uicontrol(hp1, 'Style', 'edit', 'String', '03', ...
	'Position', ped4, ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbInputPannel, ...
	'HorizontalAlignment', 'center');
hEd5 = uicontrol(hp1, 'Style', 'edit', 'String', '04', ...
	'Position', Right(hEd4), ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbInputPannel, ...
	'HorizontalAlignment', 'center');
hEd8 = uicontrol(hp1, 'Style', 'edit', 'String', '02', ...
	'Position', Right(hEd5), ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbInputPannel, ...
	'HorizontalAlignment', 'center');
hEd9 = uicontrol(hp1, 'Style', 'edit', 'String', '05', ...
	'Position', Right(hEd8), ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbInputPannel, ...
	'HorizontalAlignment', 'center');

hEd1 = uicontrol(hp1, 'Style', 'text', 'String', in_file_name, ...
	'Position', [70, 5, pFig(3)-110, 21], ...
	'BackgroundColor', WHITE, ...
	'HorizontalAlignment', 'center', ...
	'Callback', @cbInputPannel);
hBtn3 = uicontrol(hp1, 'Style', 'pushbutton', 'String', '...', ...
	'Position', [pFig(3)-35, 3, 25, 25], ...
	'Callback', @cbInSelect);

%% Output select pannel hp2, hCb2, hTxt2, hBtn2
pp2 = [pp1(1), pp1(2)+pp1(4)+5, pFig(3)-3, 40];
hp2 = uipanel(hFig, 'Title', 'Output', 'Units', 'pixels', ...
	'Position', pp2);

hCb2 = uicontrol(hp2, 'Style', 'checkbox', 'String', 'Write', ...
	'Position', [5, 1, 60, 25], ...
	'HorizontalAlignment', 'right', ...
	'Callback', @cbCb2);
if out_fid > 0
	set(hCb2, 'Value', get(hCb2, 'Max'));
else
	set(hCb2, 'Value', get(hCb2, 'Min'));
end
out_fid = -1;

hTxt2 = uicontrol(hp2, 'Style', 'text',  'String', out_file_name, ...
	'Position', [70, 5, pFig(3)-110, 21], ...
	'BackgroundColor', WHITE, ...
	'HorizontalAlignment', 'center');
hBtn2 = uicontrol(hp2, 'Style', 'pushbutton', 'String', '...', ...
	'Position', [pFig(3)-35, 3, 25, 25], ...
	'Callback', @cbOutSelect);

%% Log pannel hp3, hTxt1
pp3 = [pp2(1), pp2(2)+pp2(4)+5, pFig(3)-3, 90];
hp3 = uipanel(hFig, 'Title', 'Log', 'Units', 'pixels', ...
	'Visible', 'off', ...
	'Position', pp3);
hTxt1 = uicontrol(hp3, 'Style', 'text', 'String', {'1', '2', '3', '4'}, ...
	'Position', [5, 5, pp3(3)-10, pp3(4)-25], ...
	'BackgroundColor', WHITE, ...
	'HorizontalAlignment', 'left');

%% Config pannel hpConf, hCbSplitOut, hCbMax1Prof, hTxt6, hTxt7, hTxt8, hTxt9, hEd6, hEd7
hpConf = uipanel(hFig, 'Title', 'Config', 'Units', 'pixels', ...
	'Visible', 'on', ...
	'Position', pp3);
hCbSplitOut = uicontrol(hpConf, 'Style', 'checkbox', 'String', 'Split Output', ...
	'Position', [5, 2, 80, 25], ...
	'Callback', @cbSplitOut, ...
	'HorizontalAlignment', 'left');
set(hCbSplitOut,'Value', get(hCbSplitOut,'Max'));
hCbMax1Prof = uicontrol(hpConf, 'Style', 'checkbox', 'String', 'Max Prof', ...
	'Position', Top(hCbSplitOut), ...
	'Callback', @cbMax1Prof, ...
	'HorizontalAlignment', 'left');
set(hCbMax1Prof,'Value', get(hCbMax1Prof,'Max'));

hTxtVoltage = uicontrol(hpConf, 'Style', 'text', 'String', 'Voltage [kV]:', ...
	'HorizontalAlignment', 'right', ...
	'Position', Right(hCbSplitOut, [5, 0, 70, 18]));
hEdVoltage = uicontrol(hpConf, 'Style', 'edit', 'String', '80', ...
	'Position', Right(hTxtVoltage, 40), ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbVoltage, ...
	'HorizontalAlignment', 'center');
hCbVoltage = uicontrol(hpConf, 'Style', 'checkbox', 'String', '', ...
	'HorizontalAlignment', 'right', ...
	'Position', Right(hEdVoltage, [5, 0, 40, 18]));
setMax(hCbVoltage);
set(hCbVoltage, 'Enable', 'off');

hTxtFlow = uicontrol(hpConf, 'Style', 'text', 'String', 'Flow: [gmp]', ...
	'HorizontalAlignment', 'right', ...
	'Position', Top(hTxtVoltage));
hEdFlow = uicontrol(hpConf, 'Style', 'edit', 'String', '12.5', ...
	'Position', Right(hTxtFlow, 40), ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbFlow, ...
	'HorizontalAlignment', 'center');
hCbFlow = uicontrol(hpConf, 'Style', 'checkbox', 'String', '', ...
	'HorizontalAlignment', 'right', ...
	'Position', Right(hEdFlow));
setMax(hCbFlow);

hTxtDuration = uicontrol(hpConf, 'Style', 'text', 'String', 'Duration [s]:', ...
	'HorizontalAlignment', 'right', ...
	'Position', Top(hTxtFlow));
hEdDuration = uicontrol(hpConf, 'Style', 'edit', 'String', '2.0', ...
	'Position', Right(hTxtDuration, 40), ...
	'BackgroundColor', WHITE, ...
	'Callback', @cbDuration, ...
	'HorizontalAlignment', 'center');
hCbDuration = uicontrol(hpConf, 'Style', 'checkbox', 'String', '', ...
	'HorizontalAlignment', 'right', ...
	'Position', Right(hEdDuration));
setMax(hCbDuration);
set(hCbDuration, 'Enable', 'off');

hTxtCurrent = uicontrol(hpConf, 'Style', 'text', 'String', 'Current:    N/A ', ...
	'HorizontalAlignment', 'left', ...
	'Position', Right(hCbVoltage, 100));

%% Calorimeter plot pannel hp4, hAxes1, hAxes2, hAxes3
pp4 = [pp3(1), pp3(2)+pp3(4)+5, pFig(3)-3, pFig(4)-pp3(2)-pp3(4)-10];
hp4 = uipanel(hFig, 'Title', 'Temperatures', 'Units', 'pixels', ...
	'Position', pp4);
pAxes1 = [50, 25, pp4(3)-95, (pp4(4)-75)/2];
hAxes1 = axes('Parent', hp4, 'Unit', 'pixels', ...
	'Position', pAxes1);
ylabel(hAxes1, 'Temperature, C');
grid(hAxes1, 'on');
pAxes2 = [pAxes1(1), pAxes1(2)+pAxes1(4)+25, pAxes1(3), pAxes1(4)];
hAxes2 = axes('Parent', hp4, 'Unit', 'pixels', ...
	'YAxisLocation','left', ...
	'Position', pAxes2);
ylabel(hAxes2, 'Temperature, C');
hAxes3 = axes('Parent', hp4, 'Unit', 'pixels', ...
	'Position', pAxes2, ...
	'YAxisLocation', 'right', ...
	'ButtonDownFcn', @bdAxes3, ...
	'GridLineStyle', ':', ...
     'Color','none');
grid(hAxes3, 'on');
ylabel(hAxes3, 'Beam Current, mA');

%% Targeting plot pannel hp6, hAxes4, hAxes5, hAxes6
pp6 = [pp3(1), pp3(2)+pp3(4)+5, pFig(3)-3, pFig(4)-pp3(2)-pp3(4)-10];
hp6 = uipanel(hFig, 'Title', 'Targeting', 'Units', 'pixels', ...
	'Visible', 'off', ...
	'Position', pp6);
pAxes4 = [50, 25, pp4(3)-95, (pp4(4)-75)/2];
hAxes4 = axes('Parent', hp6, 'Unit', 'pixels', ...
	'Position', pAxes4, ...
	'YGrid', 'on', ...
	'XGrid', 'off');
ylabel(hAxes4, 'Voltage, V');
pAxes5 = [pAxes4(1), pAxes4(2)+pAxes4(4)+25, pAxes4(3), pAxes4(4)];
hAxes5 = axes('Parent', hp6, 'Unit', 'pixels', ...
	'YAxisLocation','left', ...
	'Position', pAxes5);
ylabel(hAxes5, 'Voltage, V');
hAxes6 = axes('Parent', hp6, 'Unit', 'pixels', ...
	'Position', pAxes5, ...
	'YAxisLocation','right', ...
	'ButtonDownFcn', @bdAxes3, ...
	'Color','none');
ylabel(hAxes6, 'Voltage, V');
grid(hAxes6, 'on');
 		  
%%  Initialization before main loop

drawnow;

%inspect(hFig);

cbInputPannel;
cbBtn4(hBtn4);

c0 = clock;
c1 = c0;

% Add lines of targeting traces
for ii = 1:numel(tpn)
	tph(ii) = line(1:nx, data(:, tpn(ii)), 'Parent', hAxes5, 'Color', tpc(ii));
end
for ii = 1:numel(tpn)
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
%bcmaxh = annotation('textbox', [0.7,0.8,0.05,0.05], 'String', sprintf('%3.0f',bcmax));
bcmaxh = text(0.75, 0.9, sprintf('%5.1f mA',bcmax), ...
	'Parent', hAxes3, 'Units', 'normalized');
bcch = text(0.75, 0.8, sprintf('%5.1f mA',bcmax), ...
	'Parent', hAxes3, 'Units', 'normalized');

% Marker
mw = 50;
mi = nx;
mi1 = mi-mw;
if mi1 < 1
	mi1 = 1;
end
mi2 = mi+mw;
if mi2 > nx
	mi2 = nx;
end
mh = line(mi1:mi2, (mi1:mi2)*0, 'Parent', hAxes3, 'Color', 'r', 'LineWidth', 2);

%% Main loop

while ~flag_stop
	% If input was changed
	if flag_in
		flag_in = false;

		% Close input file if if was opened
		if in_fid > 0
			status = fclose(in_fid);
			if status
				printl('Input file close error\n');
			else
				printl('Input file has been closed\n');
			end
			in_fid = -1;
		end
		
		% Close input COM port if if was opened
		if cp_open
			try
				fclose(cp_obj);
				printl('Input port has been closed\n');
			catch
				printl('Input port close error\n');
			end
			try
				delete(cp_obj);
				%clear cp_obj
			catch
				printl('Input port delete error\n');
			end
			cp_open = false;
		end
		
		% Open input
		val = get(hPm1, 'Value');
		s1 = get(hPm1, 'String');
		if ~strcmp(s1(val), 'FILE')
			% Open COM port
			cpname = char(s1(val));
			try
				cp_obj = serial(cpname);
				set(cp_obj, 'BaudRate', 38400, 'DataBits', 8, 'StopBits', 1);
				set(cp_obj, 'Terminator', 'CR');
				set(cp_obj, 'Timeout', 1);
				fopen(cp_obj);
				if strcmp(cp_obj.status, 'open')
					addr1 = sscanf(get(hEd4, 'String'),'%d');
					addr2 = sscanf(get(hEd5, 'String'),'%d');
					addr3 = sscanf(get(hEd8, 'String'),'%d');
					addr4 = sscanf(get(hEd9, 'String'),'%d');
					printl('Input port %s has been opened\n', cpname);
					cp_open = true;
				else
					printl('Input port %s open error\n', cpname);
					cp_open = false;
					% Find FILE in combo box list
					for val = 1:numel(s1)
						if strcmp(s1(val), 'FILE')
							set(hPm1,'Value', val);
							disp(val);
						end
					end
					cbInputPannel(hPm1);
					% Swith to stop state
					set(hBtn1, 'Value', get(hBtn1, 'Min'));
					cbStart(hBtn1);
				end
			catch ME
				printl('Input port %s open error\n', cpname);
				cp_open = false;
				% Find FILE in combo box list
				for val = 1:numel(s1)
					if strcmp(s1(val), 'FILE')
						set(hPm1,'Value', val);
					end
				end
				cbInputPannel(hPm1);
				set(hBtn1, 'Value', get(hBtn1, 'Min'));
				cbStart(hBtn1);
			end
		else
			% Open input file
			in_file = [in_file_path, in_file_name];
			in_fid = fopen(in_file);
			if in_fid > 2
				set(hEd1, 'String', in_file_name);
				printl('Input file %s has been opened\n', in_file);
			else
				in_fid = -1;
				printl('Input file open error\n');
				set(hBtn1, 'Value', get(hBtn1, 'Min'));
				cbStart(hBtn1);
			end
		end
	end
	
	% If output was changed
	if flag_out
		flag_out = false;
		% If writing to output ia enabled
		if get(hCb2,'Value') == get(hCb2,'Max')
			% Close output file
			if out_fid >= 0
				status = fclose(out_fid);
				out_fid = -1;
				printl('Output file has been closed\n');
			end
			
			% Open new output file
			out_file_name = LogFileName();
			out_file = [out_file_path, out_file_name];
			out_fid = fopen(out_file, 'at+', 'n', 'windows-1251');
			if out_fid < 0
				printl('Output file %s open error\n', out_file_name);
				% Disable output writing
				set(hCb2,'Value', get(hCb2,'Min'));
			else
				set(hTxt2,  'String', out_file_name);
				printl('Output file %s has been opened\n', out_file);
			end
		end
	end
	
	% If Start was pressed
	if get(hBtn1, 'Value') == get(hBtn1, 'Max')
		c = clock;

		% Change output file every hour
		if flag_hour && c(4) ~= c0(4)
			c0 = c;
			flag_out = true;
		end
			
		% Faded profiles: refresh every fpdt seconds
		if abs(c(6) - c1(6)) < fpdt
			fpi = fpi - 1;
		else
			fpi(1:end-1) = fpi(2:end);
			fpi(end) = nx;
			c1 = c;
		end
		
		% Read data from ADAMs
		cr = clock;
		[t3, ai3] = ADAM4118_read(cp_obj, addr1);
		[t4, ai4] = ADAM4118_read(cp_obj, addr2);
		[t2, ai2] = ADAM4118_read(cp_obj, addr3);
		
		% Log text red from ADAMs
		scroll_log(hTxt1, ai3);
		scroll_log(hTxt1, ai4);
		scroll_log(hTxt1, ai2);
			
		% Save line with data to output file If log writing is enabled
		if get(hCb2, 'Value') == get(hCb2, 'Max') && out_fid > 0
			% Separator is "; "
			sep = '; ';
			% Write time HH:MM:SS.SS
			fprintf(out_fid, ['%02.0f:%02.0f:%05.2f' sep], cr(4), cr(5), cr(6));
			% Data output format
			fmt = '%+09.4f';
			% Write first ADAM data array t3(1:8)
			fprintf(out_fid, [fmt sep], t3(1:8));
			% Write ADAM data array t4(1:8)
			fprintf(out_fid, [fmt sep], t4(1:8));
			% Write first 7 values of last ADAM data array t2(1:7)
			fprintf(out_fid, [fmt sep], t2(1:7));
			% Write last value t2(8) with NL instead of sepearator
			fprintf(out_fid, [fmt '\n'], t2(8));
		end
		
		% Shift data array and markers
		data(1:nx-1, :) = data(2:nx, :);
		mi = mi - 1;
		if mi < 1
			%mi = findPeak(data);
			[~, mi] = max(current);
		end
		mi1 = mi-mw;
		if mi1 < 1
			mi1 = 1;
		end
		mi2 = mi+mw;
		if mi2 > nx
			mi2 = nx;
		end
		
		% Fill last data point
		temp = data(nx, :);
		%temp(1) = now;
		temp(1) = datenum(cr);
		temp(2:9) = t3(1:8);
		temp(10:17) = t4(1:8);
		temp(18:25) = t2(1:8);
		
		% If temperature readings == 0 then use previous value
		ind = find(temp(1:17) == 0);
		temp(ind) = data(nx, ind);
		% Copy last reading
		data(nx, :) = temp;

		% Calculate minimum values
		if max(dmin) <= 0
			% First reading, fill arrays with defaults
			dmin = data(nx, :);
			for ii = 1:nx-1
				data(ii, :) = data(nx, :);
			end
		else
			% Calculate minimum
			for ii =2:numel(dmin)
				if temp(ii) < dmin(ii)
					dmin(ii) = temp(ii);
				end
			end
			dmin = min(data);
		end
		
		% Plot data traces for trn(:) channels
		for ii = 1:numel(trn)
			set(trh(ii), 'Ydata', data(:, trn(ii)));
		end
		
		% Determine index for targeting traces
		[v1, v2] = max(data(mi1:mi2, tpn));
		[~, v3] = max(v1);
		tpnm = v2(v3)+mi1;
		tpn2 = tpnm + tpw;
		if tpn2 > nx
			tpn2 = nx;
			tpn1 = nx-2*tpw-1;
		else
			tpn1 = tpnm - tpw;
			if tpn1 < 1
				tpn1 = 1;
				tpn2 = 2*tpw+1;
			end
		end
		
		% Determine beam durationi from targeting traces
		if (tpn1 > 1) && (tpn2 < nx)
			[v1, v2] = max(data(tpn1:tpn2, tpn));
			[d1, v3] = max(v1);
			d2 = min(data(tpn1:tpn2, tpn(v3)));
			d3 = d2+(d1-d2)*0.10;
			d4 = find(data(tpn1:tpn2, tpn(v3)) > d3) + tpn1;
			if numel(d4) > 1
				cdt = etime(datevec(data(d4(end), 1)), datevec(data(d4(1), 1)));
				if ~isMax(hCbDuration)
					% Replase with calculated valuevalue 
					set(hEdDuration, 'String', sprintf('%4.2f', cdt));
					duration = cdt;
				end
			end
		end
		
		% Plot targeting traces
		for ii = 1:numel(tpn)
			set(tph(ii), 'Ydata', data(:, tpn(ii))-dmin(tpn(ii)));
			set(hAxes4, 'XLimMode', 'manual', 'XLim', [tpn1, tpn2]);
			set(tph1(ii), 'Xdata', tpn1:tpn2);
			set(tph1(ii), 'Ydata', data(tpn1:tpn2, tpn(ii))-dmin(tpn(ii)));
		end

		% Plot acceleration grid traces
		for ii = 1:numel(agn)
			set(agh(ii), 'Ydata', smooth(data(:, agn(ii)), 20)-dmin(agn(ii)));
		end
		
		% Calculate and plot equivalent current
		% Calculate Delta T
		deltat = data(:, bctout)-data(:, bctin)-dmin(bctout)+dmin(bctin);  % C
		deltat = smooth(deltat,30);
		% Calculate measured flow
		cflow = data(:, bcflowchan);
		cflow(cflow <= 0.001) = 0.001;
		cflow = smooth(cflow,30);
		cflow = cflow*bcv2flow;
		if isMax(hCbFlow)
			cflow(:) = flow;
		else
			set(hEdFlow, 'String', sprintf('%5.2f', cflow(end)));
		end
		current = deltat.*cflow*Q/voltage;  %mA
		
		% Calculate current by intergal 
		[bcmax, ind] = max(current);
		bcw = 100;   % Intergation window is 2*bcw+1 points
		ind = mi;
		i2 = ind + bcw;
		if i2 > nx
			 i2 = nx;
			 i1 = nx -2*bcw-1;
		else
			i1 = ind - bcw;
			if i1 < 1
				i1 = 1;
				i2 = 2*bcw+1;
			end
		end
		if (i1 > 1) && (i2 < nx)
			ctotal = sum(current(i1:i2));
			cdt = etime(datevec(data(i2, 1)), datevec(data(i1, 1)));
			%ctotal = ctotal - (current(i1)+current(i2))/2*cdt;
			ctotal = ctotal - (current(i1)+current(i2))/2*(2*bcw);
			cdt1 = cdt/(2*bcw);
			%cbd = 2;   % sec Beam duration
			cbd = duration;   % sec Beam duration
			cti = ctotal*cdt1/cbd;
			set(hTxtCurrent, 'String', sprintf('Current %5.1f mA', cti));
		end

		set(bcmaxh, 'String', sprintf('%5.1f mA', bcmax));
		set(bcch, 'String', sprintf('%5.1f mA', current(end)));
		set(bch, 'Ydata', current - min(current));
		set(mh, 'Xdata', i1:i2);
		set(mh, 'Ydata', current(i1:i2) - min(current));
		
		% Calculate profiles prof1 - vertical and prof2 - horizontal
		prof1 = data(nx, p1range) - dmin(p1range);
		prof2 = data(nx, p2range) - dmin(p2range);
		% Calculate maximal profile
		[dmax, imax] = max(data(:, p1range));
		[~, immax] = max(dmax);
		prof1max  = data(imax(immax), p1range) - dmin(p1range);
		prof2max  = data(imax(immax), p2range) - dmin(p2range);
		if max(prof1max) < 1
			prof1max(:)  = 1;
		end
		if max(prof1max) > max(prof1max1)
			prof1max1  = prof1max;
		end
		
		% Plot profiles
		% Plot current vertical profile
		set(prof1h, 'Ydata',  prof1);
		
		% Plot current horizontal profile
		set(prof2h, 'Ydata',  prof2);
		
		% Plot faded profiles
		for ii = 1:numel(fpi)
			prof1  = data(fpi(ii), p1range) - dmin(p1range);
			set(fph(ii), 'Ydata',  prof1);
		end
			
		% Plot max1 profile
		if get(hCbMax1Prof, 'Value') == get(hCbMax1Prof, 'Max')
			set(prof1max1h, 'Ydata',  prof1max1);
		end
			
		% Plot max profile
		set(prof1maxh, 'Ydata',  prof1max);
		set(prof2maxh, 'Ydata',  prof2max);
		
		% Refresh Figure
		drawnow
	else
		% Refresh Figure
		drawnow
	end
end
 
%% Quit procedures

saveSettings;

delete(hFig);

status = fclose('all');
%status = fclose(out_fid);
%status = fclose(in_fid);
%status = fclose('all');
if cp_open
	try
		fclose(cp_obj);
	catch
	end
	try
		delete(cp_obj);
		%clear cp_obj
	catch
	end
end

printl('%s Version %s Stopped.\n', ProgName, ProgVersion);

%% Callback functions

	function bdAxes3(h, ~)
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
	
	function cbOutSelect(~, ~)
		[file_name, file_path] = uiputfile([out_file_path LogFileName()], 'Save Log to File');
		if ~isequal(file_name, 0)
			out_file_path = file_path;
            out_file_name = file_name;
			out_file = [out_file_path, out_file_name];
			set(hTxt2,  'String', out_file_name);
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

	function cbCb2(~, ~)
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
	function loadSettings
		load(iniFileName, '-mat');
	end
	
	function saveSettings
		save(iniFileName, 'out_file_name', 'out_file_path', 'out_fid', ...
		         'in_file_name', 'in_file_path', 'in_fid', ...
				 'voltage', 'duration', 'flow');
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


