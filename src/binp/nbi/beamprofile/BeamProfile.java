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
import javax.swing.JToggleButton;
import net.wimpi.modbus.*;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.io.*;
import net.wimpi.modbus.net.*;
import net.wimpi.modbus.util.*;
 

public class BeamProfile extends javax.swing.JFrame implements WindowListener {
    static final Logger logger = Logger.getLogger(BeamProfile.class.getName());

    ChartPanel chart1;
    ChartPanel chart2;
    JPanel chartPanel;
    Task task;

    private SerialPort serialPort;
    
    String progName = "Calorimeter Beam Profile";
    String progNameShort = "BeamProfile_";
    String progVersion = "10";
    String iniFileName = "BeamProfile" + progVersion + ".ini";

    // Output file
    String outFileName = LogFileName(progNameShort, "txt");
    String outFilePath = "D:\\";
    String outFile = outFilePath + outFileName;
    int out_fid = -1;

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
    String in_file = in_file_path + in_file_name;
    int	in_fid = -1;

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
    double[][] dmin = new double[1][ny];    // minimal temperatures
    double[][] dmax = new double[1][ny];    // maximal temperatures
	
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
//    log_fid = fopen(logFileName, 'at+', 'n', 'windows-1251');
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

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(jLabel9)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 57, Short.MAX_VALUE)
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
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(jLabel16)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jLabel17)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jComboBox4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jLabel18)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jSpinner10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
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
                .addContainerGap(533, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Config", jPanel7);

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
                .add(jTabbedPane1)
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
            InetAddress addr = null; //the slave's address
            int port = Modbus.DEFAULT_PORT;
            int unitid = 1; //the unit identifier we will be talking to
            int ref = 0;    //the reference; offset where to start reading from
            int count = 1;  //the number of DI's or AI's to read

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
    
    private static int count = 0;
    private static Date lastDate = new Date();
    public void mark() {
        Date date = new Date();
        System.out.printf("//d //tT.//2$tL //d\n", count++, date, date.getTime()-lastDate.getTime());
        lastDate= date;
    }
    public void mark(int c) {
        count = c;
        mark();
    }
    public void mark(String s) {
        System.out.printf("//s ", s);
        mark();
    }
    
    public static String readURL(String urlName) throws MalformedURLException, IOException {
        String result = "";
        // Create a URL 
        URL urlToRead = new URL(urlName);
        // Read and the URL characterwise
        // Open the streams
        InputStream inputStream = urlToRead.openStream();
        int c = inputStream.read();
        while (c != -1) {
            //System.out.print((char) c);
            result += (char) c;
            c = inputStream.read();
        }
        inputStream.close();
        return result;
    }

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
    private javax.swing.JButton jButton5;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
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
    private javax.swing.JTextField jTextField5;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
    
    //</editor-fold>

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

    ADAM[] adams;
    int[] addr = {6, 7, 8, 9};
    String[] ports = new String[4];

    ADAM[] CreateADAMs() {
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
        
        ADAM result[] = new ADAM[addr.length];
        for (int i = 0; i < addr.length; i++) {
            adams[i] = new ADAM(ports[i], addr[i]);
        }

/*        
        //  Attach to com ports
        for (int ica = 0; ica < addr.length; ica++) {
            if ("COM".equals(portname.substring(0,2))) {
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
            }
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
        }
*/
        }


//<editor-fold defaultstate="collapsed" desc=" Copied from BeamProfile.m ">
/*

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
            while(!bp.flag_stop) {
                // If input was changed
                if(bp.flag_in) {
                    // Reset flag
                    bp.flag_in = false;
                    
                    // Close input file
                    bp.closeInputFile();
                    //in_fid = CloseFile(in_fid);
                    // Delete ADAMs
                    bp.deleteADAMs();
                    // Create ADAMs
                    bp.adams = bp.createADAMs();
                }
                
                // If output was changed
                if(bp.flag_out) {
                    // Reset flag
                    bp.flag_out = false;
    /*
                    // If writing to output is enabled
                    if (get(hCbOut,'Value') == get(hCbOut,'Max')) {
                        // Close fout
                        out_fid = CloseFile(out_fid);
			// Open new output file
			outFileName = LogFileName(ProgNameShort, 'txt');
			outFile = [outFilePath, outFileName];
			out_fid = fopen(outFile, 'at+', 'n', 'windows-1251');
			if (out_fid < 0) {
                            System.out.printf('Output file //s open error\n', outFileName);
                            // Disable output writing
                            set(hCbOut,'Value', get(hCbOut,'Min'));
                        }
                        else {
                            set(hTxtOut,  'String', outFileName);
                            System.out.printf('Output file //s has been opened\n', outFile);
			}
                    }
    */
                }
                

                // If Start was pressed
                if (bp.jToggleButton1.isSelected()) {
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
/*
                    [t3, ai3] = ADAM4118_read(adams(1).port, adams(1).addr);
                    [t4, ai4] = ADAM4118_read(adams(2).port, adams(2).addr);
                    [t2, ai2] = ADAM4118_read(adams(3).port, adams(3).addr);
		
                    temp = data(nx, :);
                    temp(1) = datenum(cr);
                    temp(2:9) = t3(1:8);
                    temp(10:17) = t4(1:8);
                    temp(18:25) = t2(1:8);
		
                    // If temperature readings == 0 then use previous value
                    ind = find(temp(1:17) == 0);
                    temp(ind) = data(nx, ind);

                    // Save line with data to output file If log writing is enabled
                    if get(hCbOut, 'Value') == get(hCbOut, 'Max') && out_fid > 0
                        // Separator is "; "
                        sep = '; ';
                        // Write time HH:MM:SS.SS
                        fprintf(out_fid, ['//02.0f://02.0f://05.2f' sep], c2(4), c2(5), c2(6));
                        // Data output format
                        fmt = '//+09.4f';
                        // Write data array
                        fprintf(out_fid, [fmt sep], temp(2:}-1));
                        // Write last value with NL instead of sepearator
                        fprintf(out_fid, [fmt '\n'], temp(}));
                    }
		
                    // Shift data array
                    data(1:nx-1, :) = data(2:nx, :);
                    // Fill last data point
                    data(nx, :) = temp;
                    // Shift marker
                    mi = mi - 1;
                    if mi < 1
			[~, mi] = max(current);
                    }
                    mi1 = max(mi - mw, 1);
                    mi2 = min(mi + mw, nx);

                    // Calculate minimum values
                    if max(dmin) <= 0
			// First reading, fill arrays with defaults
			dmin = data(nx, :);
			for ii = 1:nx-1
                            data(ii, :) = data(nx, :);
			}
                    }
                    else {
			// Calculate minimum
			dmin = min(data);
                    }
		
                    // Update data traces for trn(:) channels
                    for ii = 1:numel(trn)
			set(trh(ii), 'Ydata', data(:, trn(ii)));
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
                                set(hEdDuration, 'String', sprintf('//4.2f', cdt));
                                duration = cdt;
                            }
			}
                    }
		
                    // Update targeting traces
                    for ii = 1:numel(tpn) {
			set(tph(ii), 'Ydata', data(:, tpn(ii))-dmin(tpn(ii)));
			set(hAxes4, 'XLimMode', 'manual', 'XLim', [tpn1, tpn2]);
			set(tph1(ii), 'Xdata', tpn1:tpn2);
			set(tph1(ii), 'Ydata', data(tpn1:tpn2, tpn(ii))-dmin(tpn(ii)));
                    }

                    // Update acceleration grid traces
                    for ii = 1:numel(agn) {
			set(agh(ii), 'Ydata', smooth(data(:, agn(ii)), 20)-dmin(agn(ii)));
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
			set(hEdFlow, 'String', sprintf('//5.2f', cflow(})));
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
			set(hTxtCurrent, 'String', sprintf('Current //5.1f mA', cti));
                    }

                    set(bcmaxh, 'String', sprintf('//5.1f mA', bcmax));
                    set(bcch, 'String', sprintf('//5.1f mA', current(})));
                    set(bch, 'Ydata', current - min(current));
                    set(mh, 'Xdata', i1:i2);
                    set(mh, 'Ydata', current(i1:i2) - min(current));
		
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
                    set(prof1h, 'Ydata',  prof1);
		
                    // Plot current horizontal profile
                    set(prof2h, 'Ydata',  prof2);
		
                    // Plot faded profiles
                    for (ii = 1:numel(fpi)) {
			prof1  = data(fpi(ii), p1range) - dmin(p1range);
			set(fph(ii), 'Ydata',  prof1);
                    }
			
                    // Plot max1 profile
                    if (get(hCbMax1Prof, 'Value') == get(hCbMax1Prof, 'Max')) {
			set(prof1max1h, 'Ydata',  prof1max1);
                    }
			
                    // Plot max profile
                    set(prof1maxh, 'Ydata',  prof1max);
                    set(prof2maxh, 'Ydata',  prof2max);
                    
                    // Refresh Figure
                    drawnow
                }
                else {
                    // Refresh Figure
                    drawnow
                }
*/
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
