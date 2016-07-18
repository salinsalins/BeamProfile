/*
 * Copyright (c) 2016, Andrey Sanin. All rights reserved.
 *
 */

package binp.nbi.beamprofile;

import binp.nbi.PET7000.PET7015;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import binp.nbi.tango.util.datafile.DataFile;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import jssc.SerialPort;
import jssc.SerialPortList;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import javax.swing.JToggleButton;
import net.wimpi.modbus.*;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.io.*;
import net.wimpi.modbus.net.*;
import net.wimpi.modbus.util.*;
 

public class BeamProfile extends javax.swing.JFrame implements WindowListener {
    static final Logger logger = Logger.getLogger(BeamProfile.class.getName());
    
    String ProgNameShort = "BeamProfile";
    
    ChartPanel chart1;
    ChartPanel chart2;
    JPanel chartPanel;
    Task task;

    private SerialPort serialPort;
    
    String progName = "Calorimeter Beam Profile";
    String progNameShort = "BeamProfile_";
    String progVersion = "10";
    String iniFileName = "BeamProfile" + progVersion + ".ini";

    // COM Port
    SerialPort	cp_obj;
    boolean cp_open = false;
    // Adresses of ADAMs
    int	addr1 = 3;
    int	addr2 = 4;
    int	addr3 = 2;
    int	addr4 = 5;

    // Input file
    String in_file_name = "ADAMTempLog_2014-12-30-13-00-00.txt";
    String in_file_path = ".\\2014-12-30\\";
    File in_file = new File(in_file_path, in_file_name);
    BufferedReader in_fid = null;

    // Output file
    String outFileName = LogFileName(progNameShort, "txt");
    String outFilePath = "D:\\";
    File outFile = new File(outFilePath, outFileName);
    BufferedWriter out_fid = null;

    // Logical flags
    boolean flag_stop = false;
    boolean flag_hour = true;
    boolean flag_out = true;
    boolean flag_in = false;
	
    // Data arrays for traces
    int nx = 2000;    // number of trace points
    int ny = 4*8+1;   // number of registered temperatures + time
    // Preallocate arrays
    double[][] data = new double[nx][ny];   // traces
    double[] dmin = new double[ny];         // minimal temperatures
    double[] dmax = new double[ny];         // maximal temperatures
	
    // Profile arrays and their plot handles
    int[] p1range = {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13}; // Channels for vertical profile
    int[] p1x = {0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12};       // X values for prof1
    int[] p2range = {15, 6, 14};     // Channels for horizontal profile
    int[] p2x = {2, 6, 10};          // X values for prof2
    double[] prof1  = new double[p1range.length];  // Vertical profile
    int prof1h = 0;     //Handle
    double[] prof2  = new double[p2range.length];  // Vertical profile
    int	prof2h = 0;         // handle
    double[] prof1max  = new double[prof1.length];      // Maximal vertical profile (over the plot)
    int	prof1maxh = 0;          // Maximal vertical profile handle
    double[] prof1max1  = new double[prof1max.length];      // Maximal vertical profile from the program start
    int prof1max1h = 0;         // Handle
    double[] prof2max  = new double[prof2.length];      // Maximal vertical profile (over the plot)
    int prof2maxh = 0;

    // Faded profiles
    int fpn = 10;               // Number of faded pofiles
    int[] fpi = new int[fpn];   // Faded pofiles indexes
    int[] fph = new int[fpn];   // Faded pofile plot handles
    double fpdt = 0.5;          // Faded pofile time inteval [s]

    // Traces to plot
    int[] trn = {6, 2, 10};     // Channel numbers of traces
    Color[] trc = {Color.RED, Color.GREEN, Color.BLUE};  // Colors of traces
    int[] trh = new int[trn.length];          // Handles of traces
	
    // Beam current calculations and plot
    double voltage = 80.0;   // keV Particles energy
    double duration = 2.0;     // s Beam duration
    double flow = 12.5;      // gpm Cooling water flow (gallons per minute) 
    int bctin = 9;        // Input water temperature channel number
    int bctout = 8;       // Output water temperature channel number
    // Current[mA] =	folw[gpm]*(OutputTemperature-InputTemperature)*Q/voltage
    double Q = 4.3*0.0639*1000; // Coeff to convert 
    int bch = 0;      // Handle for beam current plot
    double bcmax = 0.0;    // Max beam current on the screen
    double bcmax1 = 0.0;   // MaxMax beam current
    int bcmaxh = 0;   // Handle of max current text
    int bcflowchan = 22;  // Channel number for flowmeter output
    double bcv2flow = 12.0;    // V/gpm Conversion coefficienf for flowmeter 

    // Acceleration electrode voltage and current
    int agvn = 23;
    int agcn = 24;
    int[] agn = {agvn, agcn};
    Color[] agc = {Color.RED, Color.GREEN};  // Colors of traces
    int[] agh = new int[agc.length];        // Handles of traces

    // Targeting plots
    int tpt = 18;
    int tpb = 19;
    int tpl = 20;
    int tpr = 21;
    int[] tpn = {tpt, tpb, tpl, tpr};   // Channel numbers of traces
    Color[] tpc = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA};  // Colors of traces
    int[] tph = new int[tpn.length];    // Handles of traces
    int[] tph1 = new int[tpn.length];    // Handles of traces zoom
    double tpw = 30.0;                     // +- Zoom window halfwidth
	
    // Error logging file
    String logFileName = LogFileName("D:\\" + progNameShort + progVersion, "log");
    int	log_fid = 0;
	
    // Colors
    Color cWHITE = new Color(1.0f, 1.0f, 1.0f);
    Color cBLACK = new Color(0.0f, 0.0f, 0.0f);
    Color cGREY  = new Color(0.3f, 0.3f, 0.3f);
    Color cGREEN = new Color(0.0f, 0.8f, 0.0f);

    Date c0 = new Date();
    Date c1 = new Date();

    /**
     * Creates new form BeamProfile
     */
    public BeamProfile() {
        addWindowListener(this);
        initComponents();
        
        logger.addHandler(new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                jTextArea3.append(record.getMessage());
                jTextArea3.append("\n");
            }
        });
        
        String[] ports = SerialPortList.getPortNames();
        for(String port : ports){
            jComboBox1.addItem(port);
        }

        chart1 = new ChartPanel(ChartFactory.createXYLineChart(
                "Line Chart 1", // chart title
                "Time, ms", // x axis label
                "Signal, V", // y axis label
                new XYSeriesCollection(), // data
                PlotOrientation.VERTICAL,
                false, // include legend
                false, // tooltips
                false // urls
            ), true);
        chart1.setPreferredSize(new Dimension(100, 100));

        chart2 = new ChartPanel(ChartFactory.createXYLineChart(
                "Line Chart 2", // chart title
                "Time, ms", // x axis label
                "Signal, V", // y axis label
                new XYSeriesCollection(), // data
                PlotOrientation.VERTICAL,
                false, // include legend
                false, // tooltips
                false // urls
            ), true);
        chart2.setPreferredSize(new Dimension(100, 100));

        chartPanel = new JPanel();
        chartPanel.setLayout(new GridLayout(0, 1, 5, 5));
        chartPanel.add(chart1);
        chartPanel.add(chart2);

        jScrollPane2.setViewportView(chartPanel);
        
//-----------------------------------------------
        for (int i = 0; i < p1range.length; i++) {
            prof1[i] = data[0][p1range[i]];
            prof1max[i] = 1.0;
            prof1max1[i] = 1.0;
        }
        for (int i = 0; i < p2range.length; i++) {
            prof2[i] = data[0][p2range[i]];
            prof2max[i] = 1.0;
        }
        for (int i = 0; i < fpi.length; i++) {
            fpi[i] = nx;
        }
//    log_fid = fopen(logFileName, "at+", "n", "windows-1251");
//	if log_fid < 0
//		log_fid = 1;
//	}

        // Create ADAMs
//        for(int i = 0; i < adams.length; i++) {
//            adams[i] = new ADAM();
//        }

        c0 = new Date();
        c1 = new Date();

//-----------------------------------------------
        task = new Task(this);
        //task.addPropertyChangeListener(this);
        task.execute();
    }    
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jSpinner2 = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jSpinner1 = new javax.swing.JSpinner();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jSpinner3 = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jSpinner5 = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel7 = new javax.swing.JLabel();
        jSpinner6 = new javax.swing.JSpinner();
        jSpinner4 = new javax.swing.JSpinner();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jToggleButton1 = new javax.swing.JToggleButton();
        jButton5 = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        jSpinner7 = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        jSpinner8 = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        jSpinner9 = new javax.swing.JSpinner();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        jSpinner10 = new javax.swing.JSpinner();
        jTextField6 = new javax.swing.JTextField();
        jCheckBox1 = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();
        jCheckBox2 = new javax.swing.JCheckBox();
        jTextField7 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jComboBox5 = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Calorimeter Beam Profile Plotter");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Config"));

        jLabel3.setText("Address:");

        jSpinner2.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));

        jLabel6.setText("URL:");

        jTextField5.setText("http://magicbox:9091/");

        jLabel1.setText("Port:");

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(502, 0, null, 1));

        jButton1.setText("Read");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel4.setText("Unit ID:");

        jSpinner3.setModel(new javax.swing.SpinnerNumberModel(1, 0, 255, 1));

        jLabel5.setText("Count:");

        jSpinner5.setModel(new javax.swing.SpinnerListModel(new String[] {"192.168.1.202", "http://magicbox:9091/", "Item 2", "Item 3"}));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Read URL"));

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Responce"));

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jTextArea1.setRows(5);
        jScrollPane3.setViewportView(jTextArea1);

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane3)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Errors"));

        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        jTextArea2.setRows(5);
        jScrollPane4.setViewportView(jTextArea2);

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane4)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(49, 49, 49)
                .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel7.setText("Ref:");

        jSpinner6.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));

        jSpinner4.setModel(new javax.swing.SpinnerNumberModel(1, 0, 255, 1));

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(212, 212, 212)
                                .add(jLabel3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jSpinner2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel1Layout.createSequentialGroup()
                                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                .add(jSpinner1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 251, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                            .add(jPanel1Layout.createSequentialGroup()
                                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                    .add(jPanel1Layout.createSequentialGroup()
                                                        .add(jLabel7)
                                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                        .add(jSpinner6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 81, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                        .add(jLabel5)
                                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                        .add(jSpinner4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                    .add(jPanel1Layout.createSequentialGroup()
                                                        .add(jSpinner5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 157, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                        .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                        .add(64, 64, 64)
                                                        .add(jLabel4)))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jSpinner3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                        .add(0, 62, Short.MAX_VALUE))
                                    .add(jPanel1Layout.createSequentialGroup()
                                        .add(0, 0, Short.MAX_VALUE)
                                        .add(jButton1)))))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jSpinner2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jSpinner5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6)
                    .add(jLabel1)
                    .add(jSpinner1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4)
                    .add(jSpinner3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(jLabel7)
                    .add(jSpinner6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton1))
                .add(16, 16, 16)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot"));

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 481, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
        );

        jToggleButton1.setText("START");

        jButton5.setText("Cancel");

        jTabbedPane1.setVerifyInputWhenFocusTarget(false);

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 432, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 655, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Plot", jPanel6);

        jLabel2.setText("COM Port:");

        jComboBox1.setToolTipText("");

        jLabel8.setText("Address:");

        jSpinner7.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        jLabel9.setText("ADAM 1");

        jLabel10.setText("ADAM 2");

        jLabel11.setText("COM Port:");

        jComboBox2.setToolTipText("");

        jLabel12.setText("Address:");

        jSpinner8.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel13.setText("ADAM 3");

        jLabel14.setText("COM Port:");

        jComboBox3.setToolTipText("");

        jLabel15.setText("Address:");

        jSpinner9.setModel(new javax.swing.SpinnerNumberModel(2, 0, 127, 1));

        jLabel16.setText("ADAM 4");

        jLabel17.setText("COM Port:");

        jComboBox4.setToolTipText("");

        jLabel18.setText("Address:");

        jSpinner10.setModel(new javax.swing.SpinnerNumberModel(3, 0, 127, 1));

        jTextField6.setText("D:\\");

            jCheckBox1.setText("Read From File: ");

            jButton2.setText("...");

            jCheckBox2.setSelected(true);
            jCheckBox2.setText("Write to File: ");

            jTextField7.setText("D:\\");

                jButton3.setText("...");

                org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
                jPanel7.setLayout(jPanel7Layout);
                jPanel7Layout.setHorizontalGroup(
                    jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jLabel9)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jLabel2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jSpinner7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jLabel10)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jLabel11)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel12)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jSpinner8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jLabel13)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jLabel14)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel15)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jSpinner9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                .add(jLabel16)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jLabel17)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel18)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jSpinner10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                .add(jCheckBox1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 252, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton2))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jCheckBox2)
                                .add(18, 18, 18)
                                .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 252, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton3)))
                        .addContainerGap())
                );
                jPanel7Layout.setVerticalGroup(
                    jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(24, 24, 24)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel8)
                            .add(jSpinner7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel9))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel11)
                            .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel12)
                            .add(jSpinner8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel10))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel14)
                            .add(jComboBox3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel15)
                            .add(jSpinner9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel13))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel17)
                            .add(jComboBox4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel18)
                            .add(jSpinner10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel16))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jCheckBox1)
                            .add(jButton2))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jCheckBox2)
                            .add(jButton3))
                        .addContainerGap(465, Short.MAX_VALUE))
                );

                jTabbedPane1.addTab("Config", jPanel7);

                jLabel19.setText("Log Level:");

                jButton4.setText("Clear Log");
                jButton4.setFocusTraversalPolicyProvider(true);
                jButton4.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        jButton4ActionPerformed(evt);
                    }
                });

                jTextArea3.setColumns(20);
                jTextArea3.setRows(5);
                jScrollPane1.setViewportView(jTextArea3);

                jComboBox5.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "OFF", "ALL" }));
                jComboBox5.setToolTipText("");

                org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
                jPanel8.setLayout(jPanel8Layout);
                jPanel8Layout.setHorizontalGroup(
                    jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel8Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jScrollPane1)
                        .addContainerGap())
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(20, 20, 20)
                        .add(jLabel19)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jComboBox5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 118, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 122, Short.MAX_VALUE)
                        .add(jButton4)
                        .add(42, 42, 42))
                );
                jPanel8Layout.setVerticalGroup(
                    jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel8Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel19)
                            .add(jButton4)
                            .add(jComboBox5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 75, Short.MAX_VALUE)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 535, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                );

                jTabbedPane1.addTab("Log", jPanel8);

                org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                    layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(jToggleButton1)
                                .add(18, 18, 18)
                                .add(jButton5))
                            .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jTabbedPane1)
                        .addContainerGap())
                );
                layout.setVerticalGroup(
                    layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(4, 4, 4)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jToggleButton1)
                            .add(jButton5))
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addContainerGap())
                );

                jToggleButton1.getAccessibleContext().setAccessibleDescription("");

                pack();
            }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            //String url = jTextField5.getText();
            //String answer = readURL(url);
            //jTextArea1.setText(answer);
            //jTextArea2.setText("-OK-");
            
            /* The important instances of the classes mentioned before */
            TCPMasterConnection con = null;         //the connection
            ModbusTCPTransaction trans = null;      //the transaction
            //ReadInputDiscretesRequest req = null; //the request
            //ReadInputDiscretesResponse res = null; //the response
            /* Variables for storing the parameters */
            InetAddress addr = null; //the slave"s address
            int port = Modbus.DEFAULT_PORT;
            int unitid = 1; //the unit identifier we will be talking to
            int ref = 0;    //the reference; offset where to start reading from
            int count = 1;  //the number of DI"s or AI"s to read

            //1. Setup the parameters
            port = (int) jSpinner1.getValue();
            //addr = InetAddress.getByName("192.168.1.202");
            addr = InetAddress.getByName((String) jSpinner5.getValue());
            unitid =  (int) jSpinner3.getValue();
            ref = (int) jSpinner6.getValue();
            count =  (int) jSpinner4.getValue();;

            //2. Open the connection
            con = new TCPMasterConnection(addr);
            con.setPort(port);
            con.connect();

            //3. Prepare the request
            //req = new ReadInputDiscretesRequest(ref, count);
            ReadInputRegistersRequest req = new ReadInputRegistersRequest(ref, count);
            req.setUnitID(unitid);
            //req.setHeadless();
            System.out.println(req);
            System.out.println(req.getDataLength());
            System.out.println(req.getFunctionCode());
            System.out.println(req.getHexMessage());

            //byte[] requestData = {32};
            //requestData[1]=(byte)0x80;
            //CommonIPRequest reqInfo = new CommonIPRequest(0x46, requestData);
            //reqInfo.setUnitID(unitid);
            //System.out.System.out.printfn(reqInfo);
            //System.out.System.out.printfn(reqInfo.getDataLength());
            //System.out.System.out.printfn(reqInfo.getFunctionCode());
            //System.out.System.out.printfn(reqInfo.getHexMessage());
            
            //4. Prepare the transaction
            trans = new ModbusTCPTransaction(con);
            trans.setRequest(req);    

            //5. Execute the transaction
            trans.execute();
            ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();
            System.out.println(res);
            System.out.println(res.getDataLength());
            System.out.println(res.getFunctionCode());
            System.out.println(res.getHexMessage());
            jTextArea1.setText("");
            for (int n = 0; n < res.getWordCount(); n++) {
                System.out.printf("//2d - //2$d //2$H\n", n, res.getRegisterValue(n));
                jTextArea1.append(Integer.toHexString(res.getRegisterValue(n)) + "\n");

            }

            //6. Close the connection
            con.close();

            PET7015 pet = new PET7015("192.168.1.202"); 

        } catch (IOException ex) {
            jTextArea2.setText("IOException " + ex);
            logger.severe("IOException " + ex);
        } catch (Exception ex) {
            jTextArea2.setText("Exception " + ex);
            logger.log(Level.SEVERE, "Exception", ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jTextArea3.setText("");
    }//GEN-LAST:event_jButton4ActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels=javax.swing.UIManager.getInstalledLookAndFeels();
            for (int idx=0; idx<installedLookAndFeels.length; idx++)
                if ("Nimbus".equals(installedLookAndFeels[idx].getName())) {
                    javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
                    break;
                }
        } catch (ClassNotFoundException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BeamProfile().setVisible(true);
            }
        });
    }

    //<editor-fold defaultstate="collapsed" desc=" Variables declaration - do not modify ">
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner10;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JSpinner jSpinner3;
    private javax.swing.JSpinner jSpinner4;
    private javax.swing.JSpinner jSpinner5;
    private javax.swing.JSpinner jSpinner6;
    private javax.swing.JSpinner jSpinner7;
    private javax.swing.JSpinner jSpinner8;
    private javax.swing.JSpinner jSpinner9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
    
    //</editor-fold>

    @Override
    public void windowClosed(WindowEvent e) {
        saveConfig();
        //System.exit(0);
    }
    @Override
    public void windowOpened(WindowEvent e) {
        restoreConfig();
    }
    @Override
    public void windowClosing(WindowEvent e) {
    }
    @Override
    public void windowIconified(WindowEvent e) {
    }
    @Override
    public void windowDeiconified(WindowEvent e) {
    }
    @Override
    public void windowActivated(WindowEvent e) {
    }
    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    private void restoreConfig() {
//        String logFileName = null;
//        List<String> columnNames = new LinkedList<>();
//        try {
//            ObjectInputStream objIStrm = new ObjectInputStream(new FileInputStream("config.dat"));
//
//            Rectangle bounds = (Rectangle) objIStrm.readObject();
//            frame.setBounds(bounds);
//
//            logFileName = (String) objIStrm.readObject();
//            txtFileName.setText(logFileName);
//            fileLog = new File(logFileName);
//
//            String str = (String) objIStrm.readObject();
//            folder = str;
//
//            str = (String) objIStrm.readObject();
//            txtarExcludedColumns.setText(str);
//
//            str = (String) objIStrm.readObject();
//            txtarIncludedColumns.setText(str);
//
//            boolean sm = (boolean) objIStrm.readObject();
//            chckbxShowMarkers.setSelected(sm);
//
//            boolean sp = (boolean) objIStrm.readObject();
//            chckbxShowPreviousShot.setSelected(sp);
//            
//            columnNames = (List<String>) objIStrm.readObject();
//
//            objIStrm.close();
//
//            logger.info("Config restored.");
//        } catch (IOException | ClassNotFoundException e) {
//            logger.log(Level.WARNING, "Config read error {0}", e);
//        }
//        timer.cancel();
//        timer = new Timer();
//        timerTask = new DirWatcher(window);
//        timer.schedule(timerTask, 2000, 1000);
//
//        logViewTable.readFile(logFileName);
//        logViewTable.setColumnNames(columnNames);
//        columnNames = logViewTable.getColumnNames();
//        // Add event listener for logview table
//        ListSelectionModel rowSM = logViewTable.getSelectionModel();
//        rowSM.addListSelectionListener(new ListSelectionListener() {
//            @Override
//            public void valueChanged(ListSelectionEvent event) {
//                //Ignore extra messages.
//                if (event.getValueIsAdjusting()) {
//                    return;
//                }
//
//                ListSelectionModel lsm = (ListSelectionModel) event.getSource();
//                if (lsm.isSelectionEmpty()) {
//                    //System.out.System.out.printfn("No rows selected.");
//                } else {
//                    int selectedRow = lsm.getMaxSelectionIndex();
//                    //System.out.System.out.printfn("Row " + selectedRow + " is now selected.");
//                    //String fileName = folder + "\\" + logViewTable.files.get(selectedRow);
//                    try {
//                        File zipFile = logViewTable.files.get(selectedRow);
//                        readZipFile(zipFile);
//                        if (timerTask != null && timerTask.timerCount > 0) {
//                            dimLineColor();
//                        }
//                    } catch (Exception e) {
//                        logger.log(Level.WARNING, "Selection change exception ", e);
//                        //panel.removeAll();
//                    }
//                }
//            }
//        });
//        logViewTable.clearSelection();
//        logViewTable.changeSelection(logViewTable.getRowCount()-1, 0, false, false);
   }

    private void saveConfig() {
//        timer.cancel();
//
//        Rectangle bounds = frame.getBounds();
//        String txt = txtFileName.getText();
//        txt = fileLog.getAbsolutePath();
//        String txt1 = txtarExcludedColumns.getText();
//        String txt2 = txtarIncludedColumns.getText();
//        boolean sm = chckbxShowMarkers.isSelected();
//        boolean sp = chckbxShowPreviousShot.isSelected();
//        List<String> columnNames = logViewTable.getColumnNames();
//        try {
//            ObjectOutputStream objOStrm = new ObjectOutputStream(new FileOutputStream("config.dat"));
//            objOStrm.writeObject(bounds);
//            objOStrm.writeObject(txt);
//            objOStrm.writeObject(folder);
//            objOStrm.writeObject(txt1);
//            objOStrm.writeObject(txt2);
//            objOStrm.writeObject(sm);
//            objOStrm.writeObject(sp);
//            objOStrm.writeObject(columnNames);
//            objOStrm.close();
//            logger.info("Config saved.");
//        } catch (IOException e) {
//            logger.log(Level.WARNING, "Config write error ", e);
//        }
    }

//=================================================
    static String prefix, ext;
    public static String LogFileName(String arg, String... strs) {
	if (prefix==null || "".equals(prefix)) {
            prefix = "LogFile_";
        }
	if (ext==null || "".equals(ext)) {
		ext = "txt";
        }
	if (strs.length >= 1) {
            prefix = strs[0];
        }
	if (strs.length >= 2) {
            ext = strs[1];
        }
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	String timeStr = fmt.format(now);
	return prefix + arg + "_" + timeStr + "." + ext;
    }

//=================================================

    Adam4118[] adams;
    int[] addr = {6, 7, 8, 9};
    String[] ports = new String[4];

    public void CreateADAMs() {
        // Create ADAM objects
        addr = new int[4];
        addr[0] = (int) jSpinner7.getValue();
        addr[1] = (int) jSpinner8.getValue();
        addr[2] = (int) jSpinner9.getValue();
        addr[3] = (int) jSpinner10.getValue();
        ports = new String[4];
        ports[0] = (String) jComboBox1.getSelectedItem();
        ports[1] = (String) jComboBox2.getSelectedItem();
        ports[2] = (String) jComboBox3.getSelectedItem();
        ports[3] = (String) jComboBox4.getSelectedItem();
        
        for (int i = 0; i < addr.length; i++) {
            adams[i] = new Adam4118(ports[i], addr[i]);
            if (isReadFromFile()) {
                // Open input file
                adams[i].openFile(in_file_path, in_file_name);
            }
        }
    }

    public void DeleteADAMs() {
        for (int i = 0; i < adams.length; i++) 
            adams[i].closeFile();
    }

    public void closeFile(Closeable file) {
        try {
            // Close file if it was opened
            file.close();
            System.out.printf("File has been closed.\n");
        } catch (IOException ex) {
            Logger.getLogger(BeamProfile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void openOutputFile() {
        outFileName = LogFileName(ProgNameShort, "txt");
        outFile = new File(outFilePath, outFileName);
        try {
            out_fid = new BufferedWriter(new FileWriter(outFile));
            jTextField7.setText(outFileName);
            System.out.printf("Output file %s has been opened\n", outFileName);
        } catch (IOException ex) {
            // Disable output writing
            jCheckBox2.setSelected(false);
            Logger.getLogger(BeamProfile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void scroll_log(int h, String instr)
    {
    /*    
        s = get(h, "String");
        for i=2:numel(s)
            s{i-1} = s{i};
        end
        s{numel(s)} = instr;
        set(h, "String", s);
    */
    }

    public boolean isWriteEnabled() {
        return jCheckBox2.isSelected();
    }

    public boolean isReadFromFile() {
        return jCheckBox1.isSelected();
    }
    
    public double max(double[] array) {
        double result = array[0];
        for (double d: array)
            result = Math.max(d, result);
        return result;
    }

    public double min(double[] array) {
        double result = array[0];
        for (double d: array)
            result = Math.min(d, result);
        return result;
    }

    public double[] min(double[][] array) {
        int nx = array.length;
        int ny = array[0].length;
        double[] result = new double[ny];
        System.arraycopy(array[0], 0, result, 0, ny);
        for (int j = 0; j < ny; j++)
            for (int i = 0; i < nx; i++)
                result[j] = Math.min(array[i][j], result[j]);
        return result;
    }

//<editor-fold defaultstate="collapsed" desc=" Copied from BeamProfile.m ">
/*
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
	

    
*/    
//</editor-fold>

//================================================    
    class Task extends SwingWorker<Void, Void> {

        BeamProfile bp;
        
        Task(BeamProfile bp) {
            this.bp = bp;
        }
    
        /**
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            while(!flag_stop) {
                // If input was changed
                if(flag_in) {
                    // Reset flag
                    flag_in = false;
                    // Close input file
                    DeleteADAMs();
                    // Create ADAMs
                    CreateADAMs();
                }
                
                // If output was changed
                if(flag_out) {
                    // Reset flag
                    flag_out = false;
   
                    // If write to output is enabled
                    if (isWriteEnabled()) {
                        // Close output file
                        closeFile(out_fid);
                        // Open new output file
                        openOutputFile();
                    }
                }
                
                // If Start was pressed
                if (jToggleButton1.isSelected()) {
                    Date c = new Date();
                
                    // Change output file every hour
                    if (flag_hour && (c.getHours() != c0.getHours())) {
                        c0 = c;
			flag_out = true;
                    }
                
                    // Faded profiles - refresh every fpdt seconds
                    if (Math.abs(c.getSeconds() - c1.getSeconds()) < fpdt) {
                        for(int i = 0; i < fpi.length; i++) {
                            fpi[i] = fpi[i] - 1;
                        }
                    }
                    else {
                        for(int i = 0; i < fpi.length-1; i++) {
                            fpi[i] = fpi[i+1];
                        }
                        fpi[fpi.length - 1] = nx;
                        c1 = c;
                    }
		
                    // Read data from ADAMs
                    Date cr = new Date();
                    
                    double[] t3 = adams[0].readData();
                    double[] t4 = adams[1].readData();
                    double[] t2 = adams[3].readData();

                    double[] temp = new double[data[0].length];
                    System.arraycopy(data[nx-1], 0, temp, 0, temp.length);
                    
                    temp[0] = cr.getTime();
                    System.arraycopy(t3, 0, temp, 1, 8);
                    System.arraycopy(t4, 0, temp, 9, 8);
                    System.arraycopy(t2, 0, temp, 17, 8);
		
                    // If temperature readings == 0 then use previous value
                    for (int i = 0; i < temp.length; i++) {
                        if (temp[i] == 0.0)
                            temp[i] = data[nx-1][i];
                    } 

                    // Save line with data to output file If log writing is enabled
                    if (isWriteEnabled() && (out_fid != null)) {
                        try {
                            // Write Time in milliseconds - not time HH:MM:SS.SS
                            String str = String.format("%d; ", (long) temp[0]);
                            out_fid.write(str, 0, str.length());
                            // Data output format
                            String fmt = "%+07.2f";
                            // Separator is "; "
                            String sep = "; ";
                            // Write data array
                            for (int i = 1; i < temp.length-1; i++) {
                                str = String.format(fmt+sep, temp[i]);
                                out_fid.write(str, 0, str.length());
                            }
                            // Write last value with NL instead of sepearator
                            str = String.format(fmt+"\n", temp[temp.length-1]);
                            out_fid.write(str, 0, str.length());
                            out_fid.flush();
                        } catch (IOException ex) {
                            Logger.getLogger(BeamProfile.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    // Shift data array
                    for (int i = 0; i < data.length-1; i++) {
                        data[i] = data[i+1];
                    }
                    // Fill last data point
                    data[nx-1] = temp;

                    // Calculate minimum values
                    if (max(dmin) <= 0 ) {
			// First reading, fill arrays with defaults
                        System.arraycopy(data[nx-1], 0, dmin, 0, dmin.length);
			for (int i = 0; i < data[0].length-1; i++) {
                            System.arraycopy(data[data.length-1], 0, data[i], 0, dmin.length);
                            //data(ii, :) = data(nx, :);
			}
                    }
                    else {
			// Calculate minimum
			dmin = min(data);
                    }
		
//<editor-fold defaultstate="collapsed" desc=" Copied from BeamProfile.m ">
/*
                    // Shift marker
                    mi = mi - 1;
                    if mi < 1
			[~, mi] = max(current);
                    }
                    mi1 = max(mi - mw, 1);
                    mi2 = min(mi + mw, nx);


// Update data traces for trn(:) channels
                    for ii = 1:numel(trn)
			set(trh(ii), "Ydata", data(:, trn(ii)));
                    }
		
                    // Determine index for targeting traces
                    [v1, v2] = max(data(mi1:mi2, tpn));
                    [~, v3] = max(v1);
                    tpnm = v2(v3) + mi1;
                    tpn2 = tpnm + tpw;
                    if tpn2 > nx {
			tpn2 = nx;
			tpn1 = nx - 2*tpw - 1;
                    }
                    else {
			tpn1 = tpnm - tpw;
			if tpn1 < 1 {
                            tpn1 = 1;
                            tpn2 = 2*tpw + 2;
			}
                    }
		
                    // Determine beam durationi from targeting traces
                    if (tpn1 > 1) && (tpn2 < nx) {
			[v1, v2] = max(data(tpn1:tpn2, tpn));
			[d1, v3] = max(v1);
			d2 = min(data(tpn1:tpn2, tpn(v3)));
			d3 = d2+(d1-d2)*0.5;
			d4 = find(data(tpn1:tpn2, tpn(v3)) > d3) + tpn1;
			if numel(d4) > 1 {
                            cdt = etime(datevec(data(d4(}), 1)), datevec(data(d4(1), 1)));
                            if ~isMax(hCbDuration) {
                                // Replase with calculated value 
                                set(hEdDuration, "String", sprintf("//4.2f", cdt));
                                duration = cdt;
                            }
			}
                    }
		
                    // Update targeting traces
                    for ii = 1:numel(tpn) {
			set(tph(ii), "Ydata", data(:, tpn(ii))-dmin(tpn(ii)));
			set(hAxes4, "XLimMode", "manual", "XLim", [tpn1, tpn2]);
			set(tph1(ii), "Xdata", tpn1:tpn2);
			set(tph1(ii), "Ydata", data(tpn1:tpn2, tpn(ii))-dmin(tpn(ii)));
                    }

                    // Update acceleration grid traces
                    for ii = 1:numel(agn) {
			set(agh(ii), "Ydata", smooth(data(:, agn(ii)), 20)-dmin(agn(ii)));
                    }
		
                    // Calculate and plot equivalent current
                    // Calculate Delta T
                    deltat = data(:, bctout)-data(:, bctin)-dmin(bctout)+dmin(bctin);  // C
                    deltat = smooth(deltat,30);
                    // Calculate measured flow
                    cflow = data(:, bcflowchan);
                    cflow(cflow <= 0.001) = 0.001;
                    cflow = smooth(cflow,30);
                    cflow = cflow*bcv2flow;
                    if isMax(hCbFlow) {
			cflow(:) = flow;
                    }
                    else {
			set(hEdFlow, "String", sprintf("//5.2f", cflow(})));
                    }
                    current = deltat.*cflow*Q/voltage;  //mA
		
                    // Calculate current by intergal 
                    [bcmax, ind] = max(current);
                    bcw = mw;   // Intergation window is 2*bcw+1 points
                    ind = mi;
                    i2 = ind + bcw;
                    if (i2 > nx) {
                        i2 = nx;
                        i1 = nx -2*bcw-1;
                    }
                    else {
                        i1 = ind - bcw;
			if (i1 < 1) {
                            i1 = 1;
                            i2 = 2*bcw+1;
			}
                    }
                    if ((i1 > 1) && (i2 < nx)) {
			ctotal = sum(current(i1:i2));
			cdt = etime(datevec(data(i2, 1)), datevec(data(i1, 1)));
			ctotal = ctotal - (current(i1)+current(i2))/2*(2*bcw);
			cdt1 = cdt/(2*bcw);
			cbd = duration;   // sec Beam duration
			cti = ctotal*cdt1/cbd;
			set(hTxtCurrent, "String", sprintf("Current //5.1f mA", cti));
                    }

                    set(bcmaxh, "String", sprintf("//5.1f mA", bcmax));
                    set(bcch, "String", sprintf("//5.1f mA", current(})));
                    set(bch, "Ydata", current - min(current));
                    set(mh, "Xdata", i1:i2);
                    set(mh, "Ydata", current(i1:i2) - min(current));
		
                    // Calculate profiles prof1 - vertical and prof2 - horizontal
                    prof1 = data(nx, p1range) - dmin(p1range);
                    prof2 = data(nx, p2range) - dmin(p2range);
                    // Calculate maximal profile
                    [dmax, imax] = max(data(:, p1range));
                    [~, immax] = max(dmax);
                    prof1max  = data(imax(immax), p1range) - dmin(p1range);
                    prof2max  = data(imax(immax), p2range) - dmin(p2range);
                    if (max(prof1max) < 1) {
			prof1max(:) = 1;
                    }
                    
                    if (max(prof1max) > max(prof1max1)) {
			prof1max1  = prof1max;
                    }
		
                    // Plot profiles
                    // Plot current vertical profile
                    set(prof1h, "Ydata",  prof1);
		
                    // Plot current horizontal profile
                    set(prof2h, "Ydata",  prof2);
		
                    // Plot faded profiles
                    for (ii = 1:numel(fpi)) {
			prof1  = data(fpi(ii), p1range) - dmin(p1range);
			set(fph(ii), "Ydata",  prof1);
                    }
			
                    // Plot max1 profile
                    if (get(hCbMax1Prof, "Value") == get(hCbMax1Prof, "Max")) {
			set(prof1max1h, "Ydata",  prof1max1);
                    }
			
                    // Plot max profile
                    set(prof1maxh, "Ydata",  prof1max);
                    set(prof2maxh, "Ydata",  prof2max);
                    
                    // Refresh Figure
                    drawnow
                }
                else {
                    // Refresh Figure
                    drawnow
                }
*/
// </editor-fold> 
                }
            }
            return null;
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            //taskOutput.app}("Done!\n");
        }
    }
}
