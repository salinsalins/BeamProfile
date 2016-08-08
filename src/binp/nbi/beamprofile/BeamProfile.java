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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import jssc.SerialPort;
import jssc.SerialPortList;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.wimpi.modbus.*;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.io.*;
import net.wimpi.modbus.net.*;
import net.wimpi.modbus.util.*;
import org.jfree.chart.HashUtilities;
import org.jfree.chart.util.ParamChecks;
import org.jfree.data.DomainInfo;
import org.jfree.data.DomainOrder;
import org.jfree.data.Range;
import org.jfree.data.RangeInfo;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.general.Series;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.IntervalXYDelegate;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.util.ObjectUtilities;
import org.jfree.util.PublicCloneable;
 

public class BeamProfile extends javax.swing.JFrame implements WindowListener {
    static final Logger logger = Logger.getLogger(BeamProfile.class.getPackage().getName());
    
    public ChartPanel chart1;
    public ChartPanel chart2;
    JPanel chartPanel;
    Task task;

    String progName = "Calorimeter Beam Profile";
    String progNameShort = "BeamProfile_";
    String progVersion = "20";
    String iniFileName = "BeamProfile" + progVersion + ".ini";

    // Input file
    volatile boolean inputChanged = false;
    String in_file_name = "ADAMTempLog_2014-12-30-13-00-00.txt";
    //File in_file_path = new File(".\\2014-12-30\\");
    File inputFile = new File(in_file_name);
    BufferedReader in_fid = null;

    // Output file
    volatile boolean outputChanged = true;
    String outFileName = LogFileName(progNameShort, "txt");
    File outFilePath = new File("D:\\");
    File outFile = new File(outFilePath, outFileName);
    BufferedWriter out_fid = null;

    // Logical flags
    volatile boolean runMeasurements = true;
    volatile boolean flag_hour = true;
	
    // Data arrays for traces
    int nx = 2000;    // number of trace points
    int ny = 4*8+1;   // number of registered temperatures + time
    // Preallocate arrays
    double[][] data = new double[nx][ny];   // traces
    double[] dmin = new double[ny];         // minimal temperatures
    double[] dmax = new double[ny];         // maximal temperatures
	
    // Profile arrays and their plot handles
    int[] p1range = {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13}; // Channels for vertical profile
    int[] p1x =     {0, 2, 3, 4, 5, 6, 7,  8,  9, 10, 12}; // X values for vertical profile
    int[] p2range = {15, 6, 14};     // Channels for horizontal profile
    int[] p2x =     { 2, 6, 10};     // X values for horizontal profile
    double[] prof1  = new double[p1range.length];  // Vertical profile
    int prof1h = 0;         // handle
    double[] prof2  = new double[p2range.length];  // Horizontal profile
    int	prof2h = 0;         // handle
    double[] prof1max  = new double[prof1.length];     // Maximal vertical profile (over the plot)
    int	prof1maxh = 0;          // Maximal vertical profile handle
    double[] prof1max1  = new double[prof1max.length]; // Maximal vertical profile from the program start
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
    
    // Traces
    int[] tpn = {tpt, tpb, tpl, tpr};   // Channel numbers for Targeting plots
    Color[] tpc = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA};  // Colors of traces
    int[] tph1 = new int[tpn.length];   // Handles of traces zoom
    double tpw = 30.0;                  // +- Zoom window halfwidth
	
    // Error logging file
    String logFileName = LogFileName("D:\\" + progNameShort + progVersion, "log");
    int	log_fid = 0;
	
    // Colors
    Color cWHITE = new Color(1.0f, 1.0f, 1.0f);
    Color cBLACK = new Color(0.0f, 0.0f, 0.0f);
    Color cGREY  = new Color(0.3f, 0.3f, 0.3f);
    Color cGREEN = new Color(0.0f, 0.8f, 0.0f);

    // Clocks
    Date c0 = new Date();
    Date c1 = new Date();
    
    volatile public boolean jToggleButton1Selected = false;
    volatile public boolean jCheckbox2Selected = false;

    /**
     * Creates new form BeamProfile
     */
    public BeamProfile() {
        addWindowListener(this);
        initComponents();
        
        
        logger.addHandler(new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                jTextArea3.append(getFormatter().format(record));
                //jTextArea3.append("\n");
            }
        });
        
        String[] pts = SerialPortList.getPortNames();
        for(String port : pts){
            jComboBox1.addItem(port);
            jComboBox2.addItem(port);
            jComboBox3.addItem(port);
            jComboBox4.addItem(port);
        }

        chart1 = new ChartPanel(
            ChartFactory.createXYLineChart(
                "Temperature Traces", // chart title
                "Time, s", // x axis label
                "Temperature, degC", // y axis label
                new XYSeriesCollection(), // data
                PlotOrientation.VERTICAL,
                false, // include legend
                false, // tooltips
                false // urls
            ), true
        );
        chart1.setPreferredSize(new Dimension(100, 100));
        chart1.getChart().getTitle().setFont(new Font("SansSerif", Font.PLAIN, 12));
        XYPlot plot = chart1.getChart().getXYPlot();
        //Color backgroundColor = new Color(28, 100, 140);
        //plot.setBackgroundPaint(backgroundColor);
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        //plot.setDomainGridlinePaint(Color.white); // x grid lines color
        //plot.setRangeGridlinePaint(Color.white);  // y grid lines color
        // Set trace colors
        //setLineColor(Color.red, Color.blue, Color.green, Color.gray);
        // Disable tooltips
        //getChart().getXYPlot().getRenderer().setBaseToolTipGenerator(null);

//        SyncronizedXYSeriesCollection dataset = new SyncronizedXYSeriesCollection();
//        plot.setDataset(dataset);

        boolean savedNotify = plot.isNotify();
        // Stop refreshing the plot
        plot.setNotify(false);
        //SyncronizedXYSeriesCollection dataset = new SyncronizedXYSeriesCollection();
        XYSeriesCollection dataset = new XYSeriesCollection();
        plot.setDataset(dataset);
        //XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
        dataset.removeAllSeries();
        for (int i = 0; i < trn.length; i++) { 
            XYSeries series = new XYSeries("Signal " + i);
            for (int j = 0; j < data.length; j++) {
                double x = j;
                //double x = (data[j][0] - data[0][0])/1000.0;
                double y = Math.sin(Math.PI*j/500.0);
                //double y = data[j][trn[i]];
                series.add(x, y);
            }
            dataset.addSeries(series);
        }
        // Restore refreshing state
        plot.setNotify(savedNotify);

        chart2 = new ChartPanel(ChartFactory.createXYLineChart(
                "Profiles", // chart title
                "Time, s", // x axis label
                "Profile", // y axis label
                new XYSeriesCollection(), // data
                PlotOrientation.VERTICAL,
                false, // include legend
                false, // tooltips
                false // urls
            ), true);
        chart2.setPreferredSize(new Dimension(100, 100));
        chart2.getChart().getTitle().setFont(new Font("SansSerif", Font.PLAIN, 12));
        plot = chart2.getChart().getXYPlot();
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        savedNotify = plot.isNotify();
        // Stop refreshing the plot
        plot.setNotify(false);
        plot.setDataset(dataset);
        dataset.removeAllSeries();
        for (int i = 0; i < trn.length; i++) { 
            XYSeries series = new XYSeries("Signal " + i);
            for (int j = 0; j < data.length; j++) {
                double x = j;
                double y = Math.sin(Math.PI*j/500.0);
                series.add(x, y);
            }
            dataset.addSeries(series);
        }
        // Restore refreshing state
        plot.setNotify(savedNotify);

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
    }    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jToggleButton1 = new javax.swing.JToggleButton();
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
        jCheckBox3 = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jComboBox5 = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Calorimeter Beam Profile Plotter");

        jTabbedPane1.setVerifyInputWhenFocusTarget(false);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot"));

        jToggleButton1.setText("RUN");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 481, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jToggleButton1)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 417, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 29, Short.MAX_VALUE)
                .add(jToggleButton1))
        );

        jToggleButton1.getAccessibleContext().setAccessibleDescription("");

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
            jTextField6.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jTextField6ActionPerformed(evt);
                }
            });

            jCheckBox1.setSelected(true);
            jCheckBox1.setText("Read From File: ");
            jCheckBox1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent evt) {
                    jCheckBox1PropertyChange(evt);
                }
            });

            jButton2.setText("...");
            jButton2.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jButton2ActionPerformed(evt);
                }
            });

            jCheckBox2.setText("Write to Folder: ");
            jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jCheckBox2ActionPerformed(evt);
                }
            });

            jTextField7.setText("D:\\");

                jButton3.setText("...");
                jButton3.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        jButton3ActionPerformed(evt);
                    }
                });

                jCheckBox3.setSelected(true);
                jCheckBox3.setText("Split");

                org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
                jPanel7.setLayout(jPanel7Layout);
                jPanel7Layout.setHorizontalGroup(
                    jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jCheckBox1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 252, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton2))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jCheckBox2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 252, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jCheckBox3))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel16)
                                    .add(jLabel13)
                                    .add(jLabel10)
                                    .add(jLabel9))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                        .add(jLabel17)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jComboBox4, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .add(jPanel7Layout.createSequentialGroup()
                                        .add(jLabel14)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jComboBox3, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .add(jPanel7Layout.createSequentialGroup()
                                        .add(jLabel11)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jComboBox2, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .add(jPanel7Layout.createSequentialGroup()
                                        .add(jLabel2)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .add(80, 80, 80)
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jPanel7Layout.createSequentialGroup()
                                                .add(jLabel8)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jSpinner7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                                .add(jLabel12)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jSpinner8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                            .add(jLabel15)
                                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                            .add(jSpinner9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                                        .add(jLabel18)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jSpinner10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                        .addContainerGap(22, Short.MAX_VALUE))
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
                            .add(jButton3)
                            .add(jCheckBox3))
                        .addContainerGap(307, Short.MAX_VALUE))
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
                jTextArea3.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
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
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 183, Short.MAX_VALUE)
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
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
                        .addContainerGap())
                );

                jTabbedPane1.addTab("Log", jPanel8);

                org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                    layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jTabbedPane1)
                );
                layout.setVerticalGroup(
                    layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 520, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 3, Short.MAX_VALUE))
                );

                pack();
            }// </editor-fold>//GEN-END:initComponents

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jTextArea3.setText("");
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        // TODO add your handling code here:
        jToggleButton1Selected = jToggleButton1.isSelected();
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        JFileChooser fc = new JFileChooser();
        //FileNameExtensionFilter filter = new FileNameExtensionFilter(
        //    "Text File", "txt");
        //fc.setFileFilter(filter);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(outFile.getParentFile());
        int result = fc.showDialog(null, "Select save folder");
        if (result == JFileChooser.APPROVE_OPTION) {
            outFilePath = fc.getSelectedFile();
            logger.log(Level.FINE, "Save folder {0}", outFilePath.getName());
            jTextField7.setText(outFilePath.getAbsolutePath());
            outputChanged = true;
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Text File", "txt");
        fileChooser.setFileFilter(filter);
        fileChooser.setCurrentDirectory(inputFile.getParentFile());
        System.out.println(inputFile.getName());
        System.out.println(inputFile.getParentFile().getName());
        System.out.println(fileChooser.getCurrentDirectory().getName());
        int result = fileChooser.showDialog(this, "Open Input File");
        if (result == JFileChooser.APPROVE_OPTION) {
            File newInputFile = fileChooser.getSelectedFile();
            logger.log(Level.FINE, "Input file {0} selected", newInputFile.getName());
            jTextField6.setText(newInputFile.getAbsolutePath());
            //Adam4118.file = inputFile;
            //jCheckBox1.setSelected(true);
            //inputChanged = true;
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jCheckBox1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jCheckBox1PropertyChange
        inputChanged = true;
    }//GEN-LAST:event_jCheckBox1PropertyChange

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        File newInputFile = new File(jTextField6.getText());
        if (newInputFile.canRead()) {
            inputFile = newInputFile;
            logger.log(Level.FINE, "Input file changed to {0}", newInputFile.getName());
            //in_file_path = inputFile.getParentFile();
            in_file_name = inputFile.getName();
            Adam4118.file = inputFile;
            inputChanged = true;
        } else {
            jTextField6.setText(inputFile.getAbsolutePath());
        }
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        jCheckbox2Selected = jCheckBox2.isSelected();
    }//GEN-LAST:event_jCheckBox2ActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        configLogger();
        //testLogger("-1");

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
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
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
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner jSpinner10;
    private javax.swing.JSpinner jSpinner7;
    private javax.swing.JSpinner jSpinner8;
    private javax.swing.JSpinner jSpinner9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
    
    //</editor-fold>

    @Override
    public void windowClosed(WindowEvent e) {
        saveConfig();
        System.exit(0);
    }
    @Override
    public void windowOpened(WindowEvent e) {
        restoreConfig();

        task = new Task(this);
        //task.addPropertyChangeListener(this);
        task.execute();
    }
    @Override
    public void windowClosing(WindowEvent e) {
        runMeasurements = false;
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
        try {
            ObjectInputStream objIStrm = new ObjectInputStream(new FileInputStream("config.dat"));
//
//            Rectangle bounds = (Rectangle) objIStrm.readObject();
//            frame.setBounds(bounds);
//
            String str = (String) objIStrm.readObject();
            jTextField6.setText(str);
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
            logger.fine("Config restored.");
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Config read error {0}", e);
        }
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
//        timerkimer.cancel();
//
//        Rectangle bounds = frame.getBounds();
//        txt = fileLog.getAbsolutePath();
//        String txt1 = txtarExcludedColumns.getText();
//        String txt2 = txtarIncludedColumns.getText();
//        boolean sm = chckbxShowMarkers.isSelected();
//        boolean sp = chckbxShowPreviousShot.isSelected();
//        List<String> columnNames = logViewTable.getColumnNames();
        try {
            ObjectOutputStream objOStrm = new ObjectOutputStream(new FileOutputStream("config.dat"));
            String txt = jTextField6.getText();
            objOStrm.writeObject(txt);
//            objOStrm.writeObject(folder);
//            objOStrm.writeObject(txt1);
//            objOStrm.writeObject(txt2);
//            objOStrm.writeObject(sm);
//            objOStrm.writeObject(sp);
//            objOStrm.writeObject(columnNames);
//            objOStrm.close();
            logger.fine("Config saved.");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Config write error ", e);
        }
    }

    static String prefix, ext;
    public static String LogFileName(String... strs) {
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
	return prefix + timeStr + "." + ext;
    }

//=================================================
    Adam4118[] adams;
    int[] addr = {6, 7, 8, 9};
    String[] ports = new String[4];

    public void createADAMs() {
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
        
        adams = new Adam4118[4];
        
        if (isReadFromFile()) {
            // Open input file
            Adam4118.openFile(jTextField6.getText());
        }
        for (int i = 0; i < addr.length; i++) {
            adams[i] = new Adam4118(ports[i], addr[i]);
        }
    }

    public void deleteADAMs() {
        if (adams == null) return;
        for (Adam4118 adam : adams) {
            adam.closeFile();
        }
        logger.fine("ADAMs deleted.");
    }

    public void closeFile(Closeable file) {
        try {
            // Close file if it was opened
            file.close();
            logger.fine("Output file has been closed.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Output file close error.");
        }
    }

    public void openOutputFile() {
        outFileName = LogFileName(progNameShort, "txt");
        outFile = new File(outFilePath, outFileName);
        try {
            out_fid = new BufferedWriter(new FileWriter(outFile, true));
            //jTextField7.setText(outFileName);
            logger.log(Level.FINE, "Output file {0} has been opened.", outFileName);
        } catch (IOException ex) {
            // Disable output writing
            jCheckBox2.setSelected(false);
            logger.log(Level.SEVERE, "Output file {0} open error.", outFileName);
        }
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

    public static void configLogger() {
        try {
            //Logger gl = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            
            // Load a properties file from class path that way can't be achieved with java.util.logging.config.file
            /*
            final LogManager logManager = LogManager.getLogManager();
            try (final InputStream is = getClass().getResourceAsStream("/logging.properties")) {
                logManager.readConfiguration(is);
            }
            */
            // Programmatic configuration
            //System.setProperty("java.util.logging.SimpleFormatter.format",
            //        "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");

            System.setProperty("java.util.logging.SimpleFormatter.format",
                    "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s %2$s %5$s %6$s%n");
            final ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.FINEST);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.setLevel(Level.FINEST);
            logger.addHandler(consoleHandler);
            logger.setUseParentHandlers(false);

            //final Logger app = Logger.getLogger("app");
            //app.setLevel(Level.FINEST);
            //app.addHandler(consoleHandler);
            //System.out.println("logger = " + logger.getName());
            //System.out.print("parent = ");
            //System.out.println(logger.getParent());
            //Logger parentLogger = logger.getParent();
            //Handler[] hs = logger.getParent().getHandlers();
            //System.out.println("Handlers " + hs.length);
            //for (Handler h: hs) {
            //    System.out.println(h);
                //logger.getParent().removeHandler(h);
            //}
        } catch (Exception e) {
            // The runtime won't show stack traces if the exception is thrown
            e.printStackTrace();
        }
    }    
    static void testLogger(String s) {
        logger.entering("Class", "testLogger " + s);
        logger.severe("Test Severe " + s);
        logger.warning("Test Warning" + s);
        logger.info("Test Info" + s);
        logger.fine("Test Fine" + s);
        logger.finer("Test Finer" + s);
        logger.finest("Test Finest" + s);
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
            outputChanged = true;
        end
    end

    function cbInSelect(~, ~)
        [file_name, file_path] = uigetfile([in_file_path in_file_name],'Read from File');
        if ~isequal(file_name, 0)
            in_file_path = file_path;
            in_file_name = file_name;
            inputFile = [in_file_path, in_file_name];
            set(hEd1, 'String', in_file_name);
            inputChanged = true;
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
        inputChanged = true;
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
        outputChanged = true;
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
        runMeasurements = true;
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
        //XYSeriesCollection dataset;
        DefaultXYDataset dataset;
        DefaultXYDataset PorfileDataset;
        DefaultXYDataset vertPorfDataset;
        
        Task(BeamProfile bp) {
            this.bp = bp;
        }
    
        /**
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            logger.fine("Background task started");
            try {
                while(runMeasurements) {
                    //logger.finest("LOOP");
                    // If input was changed
                    if(inputChanged) {
                        logger.fine("Input changed");
                        // Reset flag
                        inputChanged = false;
                        // Close input file
                        deleteADAMs();
                        // Create ADAMs
                        createADAMs();
                    }

                    // If output was changed
                    if(outputChanged) {
                        logger.fine("Output changed");
                        // Reset flag
                        outputChanged = false;

                        // If write to output is enabled
                        if (jCheckbox2Selected) {
                            // Close output file
                            closeFile(out_fid);
                            // Open new output file
                            openOutputFile();
                        }
                    }

                    // If Start was pressed
                    //System.out.println(jToggleButton1.isSelected());
                    if (jToggleButton1Selected) {
                        Date c = new Date();

                        // Change output file every hour
                        if (flag_hour && (c.getHours() != c0.getHours())) {
                            c0 = c;
                            outputChanged = true;
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
                        System.arraycopy(t3, 0, temp, 1, t3.length);
                        System.arraycopy(t4, 0, temp, 9, t4.length);
                        System.arraycopy(t2, 0, temp, 17, t2.length);

                        // If temperature readings == 0 then use previous value
                        for (int i = 0; i < temp.length; i++) {
                            if (temp[i] == 0.0)
                                temp[i] = data[nx-1][i];
                        } 

                        // Save line with data to output file if log writing is enabled
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
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }

                        // Shift data array
                        for (int i = 0; i < data.length-1; i++) {
                            data[i] = data[i+1];
                        }
                        // Fill last data point
                        data[nx-1] = temp;

                        // Calculate minimum values
                        if (data[0][0] <= 0 ) {
                            // First reading, fill arrays with defaults
                            System.arraycopy(temp, 0, dmin, 0, dmin.length);
                            for (int i = 0; i < data.length-1; i++) {
                                //System.arraycopy(data[data.length-1], 0, data[i], 0, dmin.length);
                                data[i] = data[nx-1];
                                //data(ii, :) = data(nx, :);
                            }
                        }
                        else {
                            // Calculate minimum
                            dmin = min(data);
                        }

                        // Plot traces
                        dataset = new DefaultXYDataset();
                        double[][] plottedData;
                        for (int i: trn) { 
                            plottedData = new double[2][nx];
                            for (int j = 0; j < data.length; j++) {
                                plottedData[0][j] = (data[j][0] - data[0][0])/1000.0;
                                plottedData[1][j] = data[j][i];
                            }
                            dataset.addSeries("Signal " + i, plottedData);
                        }
                        process(new ArrayList<Void>());

                        // Calculate and plot profiles prof1 - vertical and prof2 - horizontal
                        for (int i =0; i < p1range.length; i++) {
                            prof1[i] = data[nx-1][p1range[i]] - dmin[p1range[i]];
                        }
                        for (int i =0; i < p2range.length; i++) {
                            prof2[i] = data[nx-1][p2range[i]] - dmin[p2range[i]];
                        }
                        // Culate datasets for profiles
                        // Current horizontal profile
                        PorfileDataset = new DefaultXYDataset();
                        plottedData = new double[2][p1range.length];
                        for (int j = 0; j < p1range.length; j++) {
                            plottedData[0][j] = p1x[j];
                            plottedData[1][j] = prof1[j];
                        }
                        PorfileDataset.addSeries("horizProf", plottedData);
                        // Current vertical profile
                        plottedData = new double[2][p2range.length];
                        for (int j = 0; j < p2range.length; j++) {
                            plottedData[0][j] = p2x[j];
                            plottedData[1][j] = prof2[j];
                        }
                        //PorfileDataset.addSeries("vertProf", plottedData);
                        
                        // Calculate maximal profile
                        int imax = 0;
                        double dmax = -273.0;
                        for (int i = 0; i < nx; i++) {
                            for (int  j = 0; j < p1range.length; j++) {
                                if (data[i][p1range[j]] >= dmax) {
                                    dmax = data[i][p1range[j]];
                                    imax = i;
                                }
                            }
                        }
                        for (int i =0; i < p1range.length; i++) {
                            prof1max[i] = data[imax][p1range[i]] - dmin[p1range[i]];
                        }
                        if (max(prof1max) < 1) {
                            for (int i =0; i < p1range.length; i++) {
                                prof1max[i] = 1.0;
                            }
                        }
                        plottedData = new double[2][p1range.length];
                        for (int j = 0; j < p1range.length; j++) {
                            plottedData[0][j] = p1x[j];
                            plottedData[1][j] = prof1max[j];
                        }
                        PorfileDataset.addSeries("maxProf", plottedData);
                        for (int i =0; i < p2range.length; i++) {
                            prof2max[i] = data[imax][p2range[i]] - dmin[p2range[i]];
                        }

                        for (int i: tpn) { 
                            plottedData = new double[2][nx];
                            for (int j = 0; j < data.length; j++) {
                                plottedData[0][j] = (data[j][0] - data[0][0])/1000.0;
                                plottedData[1][j] = data[j][i];
                            }
                            dataset.addSeries("Targeting " + i, plottedData);
                        }
                        
//<editor-fold defaultstate="collapsed" desc=" Copied from BeamProfile.m ">
    /*

                        if (max(prof1max) > max(prof1max1)) {
                            prof1max1  = prof1max;
                        }

                        // Shift marker
                        mi = mi - 1;
                        if mi < 1
                            [~, mi] = max(current);
                        }
                        mi1 = max(mi - mw, 1);
                        mi2 = min(mi + mw, nx);

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
            }
            catch (Exception ex) {
                logger.log(Level.SEVERE, "Exception during doInBackground" , ex);
           }
            return null;
        }
        
        @Override
        protected void process(List<Void> chunks) {
            XYPlot plot = chart1.getChart().getXYPlot();
            plot.setDataset(dataset);
            chart2.getChart().getXYPlot().setDataset(PorfileDataset);
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
