/*
 * Copyright (c) 2016, Andrey Sanin. All rights reserved.
 *
 */

package binp.nbi.beamprofile;

import binp.nbi.tango.util.TodayFolder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import jssc.SerialPortList;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.ini4j.Wini;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;

public class BeamProfile extends javax.swing.JFrame implements WindowListener {
    static final Logger LOGGER = Logger.getLogger(BeamProfile.class.getPackage().getName());
    
    public ChartPanel chart1;
    public ChartPanel chart2;
    JPanel chartPanel;
    Task task;

    static String progName = "Calorimeter Beam Profile";
    static String progNameShort = "Beam_Profile";
    static String progVersion = "32";
    String iniFileName = progNameShort + "_" + progVersion + ".ini";
    String configFileName = progNameShort + ".ini";

    // Input file
    volatile boolean inputChanged = true;
    volatile boolean readFromFile = true;
    File inputFile = new File("Beam_Profile.txt");

    // Output file
    volatile boolean outputChanged = true;
    volatile boolean writeToFile = false;
    volatile boolean splitOutputFile = true;
    volatile boolean createSubfolders = true;
    volatile boolean compressOutput = false;
    volatile String outputFileName = outputFileName(progNameShort, "txt");
    volatile File outputFilePath = new File("D:\\");
    File outputFile = new File(outputFilePath, outputFileName);
    volatile Writer outputWriter = null;

    // Run measurements button
    volatile boolean loopDoInBackground = true;
    volatile boolean runMeasurements = false;
	
    // Data arrays for traces
    int nx = 2000;    // number of trace points
    int ny = 4*8+1;   // number of registered temperatures + time
    // Preallocate arrays
    double[][] data = new double[nx][ny];   // traces
    double[] dmin = new double[ny];         // minimal temperatures
    double[] dmax = new double[ny];         // maximal temperatures
	
/*
    // Traces to plot
    int[] trn = {6, 2, 10};     // Channel numbers of traces
    Color[] trc = {Color.RED, Color.GREEN, Color.BLUE};  // Colors of traces

    // Acceleration electrode voltage and current
    int agvn = 23;
    int agcn = 24;
    int[] agn = {agvn, agcn};
    Color[] agc = {Color.RED, Color.GREEN};  // Colors of traces
    
// Targeting plots
    int tpt = 18;
    int tpb = 19;
    int tpl = 20;
    int tpr = 21;
    // Targeting traces
    int[] tpn = {tpt, tpb, tpl, tpr};   // Channel numbers for Targeting plots
    Color[] tpc = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA};  // Colors of traces
    int[] tph1 = new int[tpn.length];   // Handles of traces zoom
*/

    int tpw = 30;                  // +- Integration window halfwidth
    int tpnm;
    int tpn1;
    int tpn2;
    
    
    // Profile arrays
    // Vertical profile
    int[] p1range = {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13};  // Channels for vertical profile
    int[] p1x =     {0, 2, 3, 4, 5, 6, 7,  8,  9, 10, 12};  // X values for vertical profile
    double[] prof1  = new double[p1range.length];           // Vertical profile
    double[] prof1max  = new double[p1range.length];        // Maximal vertical profile
    // Horizontal profile
    int[] p2range = {15, 6, 14};     // Channels for horizontal profile
    int[] p2x =     { 2, 6, 10};     // cm X values for horizontal profile
    double[] prof2  = new double[p2range.length];  // Horizontal profile
    double[] prof2max  = new double[prof2.length]; // Maximal horizontal profile
    double maxTime;
    double lastProfTime = 3.0*60.*1000.; // 3 min
    
/*
    // Faded profiles
    int fpn = 10;               // Number of faded pofiles
    int[] fpi = new int[fpn];   // Faded pofiles indexes
    int[] fph = new int[fpn];   // Faded pofile plot handles
    double fpdt = 0.5;          // Faded pofile time inteval [s]
*/
    
    // Beam current calculations and plot
    int bctin = 8;          //  Input water temperature channel number
    int bctout = 7;         // Output water temperature channel number
    double beamVoltage = 80.0;  // keV Particles energy
    double beamDuration = 2.0;  // s Beam duration
    double integrationTime = 60000.0;
    int bcflowchan = 22;    // Channel number for flowmeter output
    double flow = 1.0;      // [V] Default cooling water flow signal  
    // Current[mA] =	folwSignal[V]*(OutputTemperature-InputTemperature)[degrees C]*Q/voltage[V]
    double voltsToGPM = 2.32;
    double Q = voltsToGPM*63.09*4.2; // Coeff to convert Volts to Watts/degreeC 
    double bcmax = 0.0;    // Max beam current on the screen
    double bcmax1 = 0.0;   // MaxMax beam current

    String statusLine = "";
    
    // Marker window
    int mi;     // Center of marker window
    int mi1;    // Left index
    int mi2;    // Right ingex
    int mw = 50; // Marker window half-width in points
	
    // Clocks
    Date c0 = new Date();
    Date c1 = new Date();

    AdamReader adamReader;
    Adam4118[] adams;
    int[] addrs = {8, 2, 4, 5};
    String[] ports = new String[addrs.length];
    List<SerialPort> portList = null;

    /**
     * Creates new form BeamProfile
     */
    public BeamProfile() {
        addWindowListener(this);
        initComponents();

        LOGGER.addHandler(new ConsoleHandler() {
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
                "Traces", // chart title
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
        JFreeChart chart = chart1.getChart();
        chart.getTitle().setFont(new Font("SansSerif", Font.PLAIN, 12));
        XYPlot plot = chart.getXYPlot();
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
        //plot.getRenderer().setBaseToolTipGenerator(null);
        DateAxis axis = new DateAxis("Time", TimeZone.getTimeZone("GMT+7"));
        plot.setDomainAxis(axis);
        
    /*
        // Add simple sinusoidal data
        boolean savedNotify = plot.isNotify();
        // Stop refreshing the plot
        plot.setNotify(false);
        //SyncronizedXYSeriesCollection tracesDataset = new SyncronizedXYSeriesCollection();
        XYSeriesCollection dataset = new XYSeriesCollection();
        plot.setDataset(dataset);
        //XYSeriesCollection tracesDataset = (XYSeriesCollection) plot.getDataset();
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
    */
        chart2 = new ChartPanel(ChartFactory.createXYLineChart(
                "Profiles", // chart title
                "Distance, cm", // x axis label
                "Profile", // y axis label
                new XYSeriesCollection(), // data
                PlotOrientation.VERTICAL,
                false, // include legend
                false, // tooltips
                false // urls
            ), true);
        chart2.setPreferredSize(new Dimension(100, 100));
        chart = chart2.getChart();
        chart.getTitle().setFont(new Font("SansSerif", Font.PLAIN, 12));
        plot = chart.getXYPlot();
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
    /*
        // Add simple sinusoidal data 
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
    */
        chartPanel = new JPanel();
        chartPanel.setLayout(new GridLayout(0, 1, 5, 5));
        chartPanel.add(chart1);
        chartPanel.add(chart2);

        jScrollPane2.setViewportView(chartPanel);
        
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Channel", "Name", "Color", "Plot", "Ymin", "Ymax"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, Color.class, java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class
            };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.setValueAt(new Color(153, 0, 153), 0, 2);
        
        // Initialize profiles
        for (int i = 0; i < p1range.length; i++) {
            prof1[i] = data[0][p1range[i]];
            prof1max[i] = 1.0;
        }
        for (int i = 0; i < p2range.length; i++) {
            prof2[i] = data[0][p2range[i]];
            prof2max[i] = 1.0;
        }
//        for (int i = 0; i < fpi.length; i++) {
//            fpi[i] = nx - 1;
//        }

        c0 = new Date();
        c1 = new Date();
    }    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jPopupMenu2 = new javax.swing.JPopupMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jDialog1 = new javax.swing.JDialog();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jToggleButton1 = new javax.swing.JToggleButton();
        jLabel3 = new javax.swing.JLabel();
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
        jPanel1 = new javax.swing.JPanel();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jTextField7 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox5 = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jTextField6 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jSpinner11 = new javax.swing.JSpinner();
        jLabel20 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jComboBox5 = new javax.swing.JComboBox();

        jMenuItem1.setText("Add Line");
        jMenuItem1.setToolTipText("");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem1);

        jMenuItem2.setText("Delete selected line");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem2);

        jMenuItem3.setText("Add Line");
        jMenuItem3.setToolTipText("");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem3);

        jMenuItem4.setText("Delete selected line");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jPopupMenu2.add(jMenuItem4);

        jDialog1.setTitle("Measurements stopped");
        jDialog1.setModal(true);

        org.jdesktop.layout.GroupLayout jDialog1Layout = new org.jdesktop.layout.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );

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

        jLabel3.setText("Flow:");

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 414, Short.MAX_VALUE)
                .add(jToggleButton1)
                .addContainerGap())
            .add(jScrollPane2)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jToggleButton1)
                    .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Output"));

        jCheckBox4.setText("Create Year/Month/Day subfolders");
        jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox4ActionPerformed(evt);
            }
        });

        jCheckBox2.setText("Write to Folder: ");
        jCheckBox2.setActionCommand("Write to Folder:");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jTextField7.setText("D:\\");
            jTextField7.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jTextField7ActionPerformed(evt);
                }
            });

            jButton3.setText("...");
            jButton3.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jButton3ActionPerformed(evt);
                }
            });

            jCheckBox3.setSelected(true);
            jCheckBox3.setText("Split output file every hour");
            jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jCheckBox3ActionPerformed(evt);
                }
            });

            jCheckBox5.setText("Compress output");
            jCheckBox5.setEnabled(false);
            jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jCheckBox5ActionPerformed(evt);
                }
            });

            org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
            jPanel1.setLayout(jPanel1Layout);
            jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                            .add(jCheckBox3)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jCheckBox4)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                            .add(jCheckBox5))
                        .add(jPanel1Layout.createSequentialGroup()
                            .add(jCheckBox2)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 322, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                            .add(jButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jCheckBox2)
                        .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jCheckBox3)
                        .add(jCheckBox4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jCheckBox5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(3, 3, 3))
            );

            jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Input"));

            jCheckBox1.setSelected(true);
            jCheckBox1.setText("Read From File: ");
            jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jCheckBox1ActionPerformed(evt);
                }
            });

            jTextField6.setText("D:\\");
                jTextField6.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        jTextField6ActionPerformed(evt);
                    }
                });

                jButton2.setText("...");
                jButton2.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        jButton2ActionPerformed(evt);
                    }
                });

                org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                    jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jCheckBox1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 319, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                    jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jCheckBox1)
                            .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jButton2))
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                jSpinner11.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.0d, 127.0d, 0.10000000000000009d));
                jSpinner11.setEnabled(false);

                jLabel20.setText("Flow meter calibration liters/sec/Volt : ");
                jLabel20.setEnabled(false);

                jTable1.setModel(new javax.swing.table.DefaultTableModel(
                    new Object [][] {
                        { new Integer(1), null, "", null, null, null},
                        { new Integer(2), null, null, null, null, null},
                        { new Integer(3), null, null, null, null, null},
                        { new Integer(4), null, null, null, null, null}
                    },
                    new String [] {
                        "Channel", "Name", "Color", "Plot", "Ymin", "Ymax"
                    }
                ) {
                    Class[] types = new Class [] {
                        java.lang.Integer.class, java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class
                    };

                    public Class getColumnClass(int columnIndex) {
                        return types [columnIndex];
                    }
                });
                jTable1.setComponentPopupMenu(jPopupMenu2);
                //Set up renderer and editor for the Color column.
                jTable1.setDefaultRenderer(Color.class,
                    new ColorRenderer(true));
                jTable1.setDefaultEditor(Color.class,
                    new ColorEditor());
                jScrollPane3.setViewportView(jTable1);

                jTable2.setModel(new javax.swing.table.DefaultTableModel(
                    new Object [][] {
                        {null,  new Integer(8),  new Boolean(false)},
                        {null,  new Integer(2),  new Boolean(false)},
                        {null,  new Integer(4),  new Boolean(false)},
                        {null,  new Integer(5),  new Boolean(false)}
                    },
                    new String [] {
                        "COM", "Address", "Enabed"
                    }
                ) {
                    Class[] types = new Class [] {
                        java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class
                    };

                    public Class getColumnClass(int columnIndex) {
                        return types [columnIndex];
                    }
                });
                jTable2.setComponentPopupMenu(jPopupMenu1);
                jTable2.setName("ADAM Config Table"); // NOI18N
                jTable2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                jTable2.getTableHeader().setResizingAllowed(false);
                jTable2.getTableHeader().setReorderingAllowed(false);
                setUpColumnComboBoxEditor(jTable2, 0);
                jScrollPane4.setViewportView(jTable2);

                jTable3.setModel(new javax.swing.table.DefaultTableModel(
                    new Object [][] {
                        {null, null},
                        {null, null},
                        {null, null},
                        {null, null}
                    },
                    new String [] {
                        "Name", "Value"
                    }
                ) {
                    Class[] types = new Class [] {
                        java.lang.String.class, java.lang.Double.class
                    };

                    public Class getColumnClass(int columnIndex) {
                        return types [columnIndex];
                    }
                });
                jTable3.setComponentPopupMenu(jPopupMenu2);
                //Set up renderer and editor for the Color column.
                jTable1.setDefaultRenderer(Color.class,
                    new ColorRenderer(true));
                jTable1.setDefaultEditor(Color.class,
                    new ColorEditor());
                jScrollPane5.setViewportView(jTable3);

                org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
                jPanel7.setLayout(jPanel7Layout);
                jPanel7Layout.setHorizontalGroup(
                    jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel7Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel7Layout.createSequentialGroup()
                                        .add(jLabel20)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jSpinner11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
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
                                        .add(18, 18, 18)
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
                                                .add(jSpinner10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 188, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                );
                jPanel7Layout.setVerticalGroup(
                    jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(24, 24, 24)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel7Layout.createSequentialGroup()
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
                                    .add(jLabel16)))
                            .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(28, 28, 28)
                        .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel20)
                                    .add(jSpinner11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(29, Short.MAX_VALUE))
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
                jComboBox5.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        jComboBox5ActionPerformed(evt);
                    }
                });

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
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 215, Short.MAX_VALUE)
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
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)
                        .addContainerGap())
                );

                jTabbedPane1.addTab("Log", jPanel8);

                org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                    layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jTabbedPane1)
                        .addContainerGap())
                );
                layout.setVerticalGroup(
                    layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jTabbedPane1)
                        .addContainerGap())
                );

                pack();
            }// </editor-fold>//GEN-END:initComponents

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jTextArea3.setText("");
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        runMeasurements = jToggleButton1.isSelected();
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(outputFile.getParentFile());
        int result = fc.showDialog(null, "Select save to folder");
        if (result == JFileChooser.APPROVE_OPTION) {
            outputFilePath = fc.getSelectedFile();
            LOGGER.log(Level.FINE, "Save to folder {0} selected", outputFilePath.getName());
            jTextField7.setText(outputFilePath.getAbsolutePath());
            outputChanged = true;
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Text File", "txt");
        fileChooser.setFileFilter(filter);
        if (inputFile != null) fileChooser.setCurrentDirectory(inputFile.getParentFile());
        int result = fileChooser.showDialog(this, "Open Input File");
        if (result == JFileChooser.APPROVE_OPTION) {
            File newInputFile = fileChooser.getSelectedFile();
            LOGGER.log(Level.FINEST, "Input file {0} selected", newInputFile.getName());
            setInputFile(newInputFile);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        File newInputFile = new File(jTextField6.getText());
        setInputFile(newInputFile);
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        writeToFile = jCheckBox2.isSelected();
        outputChanged = true;
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        readFromFile = jCheckBox1.isSelected();
        inputChanged = true;
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
        splitOutputFile = jCheckBox3.isSelected();
    }//GEN-LAST:event_jCheckBox3ActionPerformed

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField7ActionPerformed
        outputFilePath = new File(jTextField7.getText());
        outputChanged = true;
    }//GEN-LAST:event_jTextField7ActionPerformed

    private void jComboBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox5ActionPerformed
        switch (jComboBox5.getSelectedIndex()) {
            case 0:
                LOGGER.setLevel(Level.SEVERE);
                break;
            case 1:
                LOGGER.setLevel(Level.WARNING);
                break;
            case 2:
                LOGGER.setLevel(Level.INFO);
                break;
            case 3:
                LOGGER.setLevel(Level.CONFIG);
                break;
            case 4:
                LOGGER.setLevel(Level.FINE);
                break;
            case 5:
                LOGGER.setLevel(Level.FINER);
                break;
            case 6:
                LOGGER.setLevel(Level.FINEST);
                break;
            case 7:
                LOGGER.setLevel(Level.OFF);
                break;
            case 8:
                LOGGER.setLevel(Level.ALL);
                break;
        }
    }//GEN-LAST:event_jComboBox5ActionPerformed

    private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox4ActionPerformed
        createSubfolders = jCheckBox4.isSelected();
    }//GEN-LAST:event_jCheckBox4ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        DefaultTableModel tableModel = (DefaultTableModel)jTable2.getModel();
        tableModel.addRow((Vector) tableModel.getDataVector().elementAt(0));
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        DefaultTableModel tableModel = (DefaultTableModel)jTable2.getModel();
        int row = jTable2.getSelectedRow();
        if (row >=0 ) tableModel.removeRow(row);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox5ActionPerformed
        compressOutput = jCheckBox5.isSelected();
    }//GEN-LAST:event_jCheckBox5ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        DefaultTableModel tableModel = (DefaultTableModel)jTable1.getModel();
        tableModel.setRowCount(tableModel.getRowCount()+1);
        //Vector newRow = (Vector) tableModel.getDataVector().elementAt(0);
        //tableModel.addRow((Vector) tableModel.getDataVector().elementAt(0));
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        DefaultTableModel tableModel = (DefaultTableModel)jTable1.getModel();
        int row = jTable1.getSelectedRow();
        if (row >=0 ) tableModel.removeRow(row);
    }//GEN-LAST:event_jMenuItem4ActionPerformed
    
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
    /*
        try {
            javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels=javax.swing.UIManager.getInstalledLookAndFeels();
            for (int idx=0; idx<installedLookAndFeels.length; idx++)
                if ("Nimbus".equals(installedLookAndFeels[idx].getName())) {
                    javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
                    break;
                }
        } catch (ClassNotFoundException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, null, ex);
        }
    */
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
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
    private javax.swing.JDialog jDialog1;
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
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JPopupMenu jPopupMenu2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSpinner jSpinner10;
    private javax.swing.JSpinner jSpinner11;
    private javax.swing.JSpinner jSpinner7;
    private javax.swing.JSpinner jSpinner8;
    private javax.swing.JSpinner jSpinner9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
    
    //</editor-fold>

    @Override
    public void windowClosed(WindowEvent e) {
        saveConfigToIni();
        System.exit(0);
    }
    @Override
    public void windowOpened(WindowEvent e) {
        restoreConfigFromIni();
        setValuesFromComponens();
        task = new Task(this);
        task.execute();
    }
    @Override
    public void windowClosing(WindowEvent e) {
        runMeasurements = false;
        loopDoInBackground = false;
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

    private void setInputFile(File newInputFile) {                                            
        if (newInputFile == null) return;
        if (inputFile != null && newInputFile.getAbsolutePath().equals(inputFile.getAbsolutePath())) return;
        if (newInputFile.canRead()) {
            inputChanged = true;
            inputFile = newInputFile;
            LOGGER.log(Level.FINE, "Input file changed to {0}", inputFile.getName());
            jTextField6.setText(inputFile.getAbsolutePath());
        } else {
            LOGGER.log(Level.WARNING, "Input file {0} can't be opened", newInputFile.getName());
            stopMeasuremets();
            inputChanged = true;
        }
    }                                           

    private void restoreConfigFromIni() {
        // Restore config from *.ini file
        try {
            int i;
            boolean b;
            String s;
            Wini ini = new Wini(new File(configFileName));
            // Restore input file parameters
            // File name
            s = ini.get("Input", "file");
            jTextField6.setText(s);
            // readFromFile
            b =  ini.get("Input", "readFromFile", boolean.class);
            jCheckBox1.setSelected(b);
            // Restore output file parameters
            // File name
            s = ini.get("Output", "dir");
            jTextField7.setText(s);
            // writeToFolder
            b =  ini.get("Output", "writeToFolder", boolean.class);
            jCheckBox2.setSelected(b);
            ini.put("Output", "writeToFolder", b);
            // splitOutput
            b =  ini.get("Output", "splitOtput", boolean.class);
            jCheckBox3.setSelected(b);
            // createSubfolders
            b =  ini.get("Output", "createSubfolders", boolean.class);
            jCheckBox4.setSelected(b);

            // Restore addresses and ports of ADAMs
            i =  ini.get("ADAM_1", "address", int.class);
            jSpinner7.setValue(i);
            s = ini.get("ADAM_1", "port");
            jComboBox1.setSelectedItem(s);
            i =  ini.get("ADAM_2", "address", int.class);
            jSpinner8.setValue(i);
            s = ini.get("ADAM_2", "port");
            jComboBox2.setSelectedItem(s);
            i =  ini.get("ADAM_3", "address", int.class);
            jSpinner9.setValue(i);
            s = ini.get("ADAM_3", "port");
            jComboBox3.setSelectedItem(s);
            i =  ini.get("ADAM_4", "address", int.class);
            jSpinner10.setValue(i);
            s = ini.get("ADAM_4", "port");
            jComboBox4.setSelectedItem(s);

            // Restore log level
            s = ini.get("Log", "level");
            jComboBox5.setSelectedItem(s);
            
            // Restore Table1
            restoreTable(jTable1, ini, "Channel");
            // Restore Table2
            restoreTable(jTable2, ini, "ADAM");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Config write error");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
        jComboBox5ActionPerformed(null);
        LOGGER.fine("Config restored");
    }

    void setValuesFromComponens() {
        // Read and set state of volatile variables
        jComboBox5ActionPerformed(null);
        jCheckBox3ActionPerformed(null);
        jCheckBox2ActionPerformed(null);
        jTextField7ActionPerformed(null);
        jCheckBox1ActionPerformed(null);
        jTextField6ActionPerformed(null);
        jToggleButton1ActionPerformed(null);
    }

    private void saveConfigToIni() {
        // Save config to *.ini file
        try {
            int i;
            boolean b;
            String s;
            Wini ini = new Wini(new File(configFileName));
            // Save input file parameters
            // File name
            s = jTextField6.getText();
            ini.put("Input", "file", s);
            // readFromFile
            b = jCheckBox1.isSelected();
            ini.put("Input", "readFromFile", b);
            // Save output file parameters
            // File name
            s = jTextField7.getText();
            ini.put("Output", "dir", s);
            // writeToFolder
            b = jCheckBox2.isSelected();
            ini.put("Output", "writeToFolder", b);
            // splitOutput
            b = jCheckBox3.isSelected();
            ini.put("Output", "splitOtput", b);
            // createSubfolders
            b = jCheckBox4.isSelected();
            ini.put("Output", "createSubfolders", b);

            // Save addresses and ports of ADAMs
            i = (int) jSpinner7.getValue();
            ini.put("ADAM_1", "address", i);
            s = (String) jComboBox1.getSelectedItem();
            ini.put("ADAM_1", "port", s);
            i = (int) jSpinner8.getValue();
            ini.put("ADAM_2", "address", i);
            s = (String) jComboBox2.getSelectedItem();
            ini.put("ADAM_2", "port", s);
            i = (int) jSpinner9.getValue();
            ini.put("ADAM_3", "address", i);
            s = (String) jComboBox3.getSelectedItem();
            ini.put("ADAM_3", "port", s);
            i = (int) jSpinner10.getValue();
            ini.put("ADAM_4", "address", i);
            s = (String) jComboBox4.getSelectedItem();
            ini.put("ADAM_4", "port", s);

            // Save log level
            s = (String) jComboBox5.getSelectedItem();
            ini.put("Log", "level", s);

            // Save addresses and ports of ADAMs from jTable2
            saveTable(jTable2, ini, "ADAM");
            // Save jTable1
            saveTable(jTable1, ini, "Channel");

            ini.store();
           
            LOGGER.fine("Config saved");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Config file write error");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
    }

    void restoreTable(JTable table, Wini ini, String prefix) {       // Save jTable1
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        try {
            try {
                rowCount = ini.get(prefix, "rowCount", int.class);
            } catch (Exception e) {
            }
            if (table.getRowCount() < rowCount) {
                DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
                tableModel.setRowCount(rowCount);
            }
            for (int j = 0; j < rowCount; j++) {
                for (int k = 0; k < columnCount; k++) {
                    try {
                        if (table.getColumnClass(k) == Color.class) {
                            int red = ini.get(prefix + j, table.getColumnName(k) + ".red", int.class);
                            int green = ini.get(prefix + j, table.getColumnName(k) + ".green", int.class);
                            int blue = ini.get(prefix + j, table.getColumnName(k) + ".blue", int.class);
                            table.setValueAt(new Color(red, green, blue), j, k);
                        } else if (ini.get(prefix + j, table.getColumnName(k)).equals("")) {
                            table.setValueAt(null, j, k);
                        } else {
                            Object o = ini.get(prefix + j, table.getColumnName(k), table.getColumnClass(k));
                            table.setValueAt(o, j, k);
                        }
                    } catch (Exception ex) {
                        table.setValueAt(null, j, k);
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    void saveTable(JTable table, Wini ini, String tableName) {       // Save jTable1
        ini.put(tableName, "rowCount", table.getRowCount());
        ini.put(tableName, "columnCount", table.getColumnCount());
        for (int j=0; j<table.getRowCount(); j++){
            for (int k=0; k<table.getColumnCount(); k++){
                if (table.getColumnClass(k) == Color.class) {
                    Color c = (Color) table.getValueAt(j, k);
                    ini.put(tableName+j, table.getColumnName(k)+".red", c.getRed()) ;
                    ini.put(tableName+j, table.getColumnName(k)+".green", c.getGreen()) ;
                    ini.put(tableName+j, table.getColumnName(k)+".blue", c.getBlue()) ;
                }
                else
                    ini.put(tableName+j, table.getColumnName(k), table.getValueAt(j, k));
            }
        }
    }

    public static String outputFileName(String prefix, String ext) {
	if (prefix==null || "".equals(prefix)) {
            prefix = "Data";
        }
	if (ext==null || "".equals(ext)) {
		ext = "txt";
        }
	String formatString = "_yyyy-MM-dd-HH-mm-ss";
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat(formatString);
	String timeString = fmt.format(now);
	return prefix + timeString + "." + ext;
    }

    void stopMeasuremets() {
        // Stop measurements
        jToggleButton1.setSelected(false);
        runMeasurements = false;
        LOGGER.log(Level.WARNING, "Measurements stopped");
    }
    
    void disableOutputWrite() {
        outputWriter = null;
        jCheckBox2.setSelected(false);
        writeToFile = false;
        LOGGER.log(Level.INFO, "Output disabled");
    }

    public void createADAMs() {
        try {
            // Create ADAM objects
            adams = new Adam4118[3];

            if (readFromFile) {
                // Open input file
                if (adamReader == null) {
                    adamReader = new AdamReader(jTextField6.getText());
                }
                else {
                    adamReader.closeFile();
                    adamReader.openFile(jTextField6.getText());
                }
                for (int i = 0; i < adams.length; i++) {
                        adams[i] = new Adam4118(adamReader);
                }
                LOGGER.finest("ADAMs created");
                return;
            }

            // Read ports and addresses
            addrs = new int[4];
            addrs[0] = (int) jSpinner7.getValue();
            addrs[1] = (int) jSpinner8.getValue();
            addrs[2] = (int) jSpinner9.getValue();
            addrs[3] = (int) jSpinner10.getValue();
            ports = new String[4];
            ports[0] = (String) jComboBox1.getSelectedItem();
            ports[1] = (String) jComboBox2.getSelectedItem();
            ports[2] = (String) jComboBox3.getSelectedItem();
            ports[3] = (String) jComboBox4.getSelectedItem();

            if (portList == null) portList = new LinkedList<>();
            for (int i = 0; i < adams.length; i++) {
                adams[i] = null;
                // Check if port is in used port list
                for (SerialPort p:portList) {
                    if (ports[i].equalsIgnoreCase(p.getPortName())) {
                        // Create Adam for existent port
                        adams[i] = new Adam4118(p, addrs[i]);
                        break;
                    }
                }
                // If Adam was created skip the rest of for 
                if (adams[i] != null) continue;
                // Othervise create port and add it to used port list
                SerialPort serialPort = new SerialPort(ports[i]);
                if (!serialPort.isOpened()) serialPort.openPort();
                serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                portList.add(serialPort);
                // Create Adam for new port
                adams[i] = new Adam4118(serialPort, addrs[i]);
            }
            LOGGER.finest("ADAMs created");
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Input file not found ");
            LOGGER.log(Level.INFO, "Exception info", ex);
            stopMeasuremets();
        } catch (SerialPortException | ADAM.ADAMException ex) { 
            LOGGER.log(Level.SEVERE, "ADAM creation error ");
            LOGGER.log(Level.INFO, "Exception info", ex);
            stopMeasuremets();
        }
    }

    public void deleteADAMs() {
        if (adams != null) {
            for (Adam4118 adam : adams) {
                if (adam != null) adam.delete();
            }
        }
        if (readFromFile) {
            // Open input file
            if (adamReader != null) 
                adamReader.closeFile();
        }
        LOGGER.finest("ADAMs deleted");
    }

    public void closeOutputFile() {
        try {
            // Close file if it was opened
            if (outputWriter != null) outputWriter.close();
            LOGGER.fine("Output file has been closed");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Output file close error ");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
        outputWriter = null;
    }

    public void openOutputFile() {
        outputFileName = outputFileName(progNameShort, "txt");
        File outputPath;
        if (createSubfolders) {
            outputPath = new TodayFolder(outputFilePath.getAbsolutePath());
        }
        else {
            outputPath = outputFilePath;
        }
        outputFile = new File(outputPath, outputFileName);
        try {
            outputWriter = new BufferedWriter(new FileWriter(outputFile, true));
            LOGGER.log(Level.FINE, "Output file {0} has been opened.", outputFileName);
        } catch (IOException ex) {
            // Disable output writing
            LOGGER.log(Level.SEVERE, "Output file {0} open error.", outputFileName);
            disableOutputWrite();
        }
    }

    public double max(double[] array) {
        double result = array[0];
        for (double d: array)
            result = Math.max(d, result);
        return result;
    }
    public double max(double[] array, int i1, int i2) {
        if (i1 < 0) i1 = 0;
        if (i2 >= array.length) i2 = array.length - 1;
        double result = array[i1];
        for (int i = i1; i<i2; i++)
            if (result < array[i]) {
                result = array[i];
            }
        return result;
    }
    public double min(double[] array) {
        double result = array[0];
        for (double d: array)
            result = Math.min(d, result);
        return result;
    }
    public double min(double[] array, int i1, int i2) {
        if (i1 < 0) i1 = 0;
        if (i2 >= array.length) i2 = array.length - 1;
        double result = array[i1];
        for (int i = i1; i<i2; i++)
            if (result > array[i]) {
                result = array[i];
            }
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
    public int maxIndex(double[] array) {
        int index = 0;
        double max = array[0];
        for (int i=0; i<array.length; i++) {
            if (array[i] > max) {
                index = i;
                max = array[i];
            }
        }
        return index;
    }
    public double[] maxAndIndex(double[] array) {
        double index = 0.0;
        double max = array[0];
        for (int i=0; i<array.length; i++) {
            if (array[i] > max) {
                index = i;
                max = array[i];
            }
        }
        double[] result = new double[2];
        result[0] = max;
        result[1] = index;
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
            LOGGER.setLevel(Level.FINEST);
            LOGGER.addHandler(consoleHandler);
            LOGGER.setUseParentHandlers(false);

            //final Logger app = Logger.getLogger("app");
            //app.setLevel(Level.FINEST);
            //app.addHandler(consoleHandler);
            //System.out.println("LOGGER = " + LOGGER.getName());
            //System.out.print("parent = ");
            //System.out.println(LOGGER.getParent());
            //Logger parentLogger = LOGGER.getParent();
            //Handler[] hs = LOGGER.getParent().getHandlers();
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
        LOGGER.entering("Class", "testLogger " + s);
        LOGGER.severe("Test Severe " + s);
        LOGGER.warning("Test Warning" + s);
        LOGGER.info("Test Info" + s);
        LOGGER.fine("Test Fine" + s);
        LOGGER.finer("Test Finer" + s);
        LOGGER.finest("Test Finest" + s);
    }

//<editor-fold defaultstate="collapsed" desc=" Copied from BeamProfile.m ">
/*
%% Callback functions
    // Click in plot
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

    
    function cbMax1Prof(hObj, ~)
        if get(hObj, 'Value') == get(hObj, 'Min')
            set(prof1max1h, 'Visible', 'off');
        else
            set(prof1max1h, 'Visible', 'on');
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

    public void setUpColumnComboBoxEditor(JTable table, int columnIndex) {
        //Set up the editor for the column cells.
        String[] pts = SerialPortList.getPortNames();
        JComboBox comboBox = new JComboBox(pts);
        comboBox.setEditable(true);
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setCellEditor(new DefaultCellEditor(comboBox));

        //Set up tool tips for the cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for COM port list");
        column.setCellRenderer(renderer);
    }

//****************************************************************************
    class Task extends SwingWorker<Void, Void> {

        BeamProfile bp;
        BeamProfileDataset tracesDataset;
        DefaultXYDataset profileDataset;
        
        Task(BeamProfile bp) {
            this.bp = bp;
        }
    
        /**
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            LOGGER.fine("Background task started");
            while(loopDoInBackground) {
                try {
                    //logger.finest("LOOP");
                    // If Start was pressed
                    if (runMeasurements) {
                        Date c = new Date();

                        // If input was changed
                        if(inputChanged) {
                            LOGGER.fine("Input changed");
                            // Reset flag
                            inputChanged = false;
                            deleteADAMs();
                            createADAMs();
                        }

                        // If output was changed
                        if(outputChanged) {
                            LOGGER.fine("Output changed");
                            // Reset flag
                            outputChanged = false;
                            closeOutputFile();
                            // If write to output file was enabled
                            if (writeToFile) {
                                openOutputFile();
                            }
                        }

                        // Split output file every hour
                        if (splitOutputFile && (c.getHours() != c0.getHours())) {
                            c0 = c;
                            outputChanged = true;
                        }

                        // Read data from ADAMs
                        Date cr = new Date();

                        double[] t0 = adams[0].readData();
                        double[] t1 = adams[1].readData();
                        double[] t2 = adams[2].readData();

                        double[] temp = new double[data[0].length];
                        temp[0] = cr.getTime();
                        System.arraycopy(t0, 0, temp,  1, t0.length);
                        System.arraycopy(t1, 0, temp,  9, t1.length);
                        System.arraycopy(t2, 0, temp, 17, t2.length);

                        // Save line with data to output file if output writing is enabled
                        if (writeToFile && (outputWriter != null)) {
                            try {
                                // Separator is "; "
                                String sep = "; ";
                                // Write Time in milliseconds - not time HH:MM:SS.SS
                                String str = String.format("%d", (long) temp[0]);
                                //String str = String.format("%d; ", (long) temp[0]);
                                //outputWriter.write(str, 0, str.length());
                                outputWriter.write(str + sep);
                                // Write data array
                                for (int i = 1; i < temp.length; i++) {
                                    str = String.format("%+07.2f", temp[i]);
                                    if (str.length() > 7) 
                                        str = str.substring(0, 7);
                                    if (i < temp.length-1)
                                        outputWriter.write(str + sep);
                                    else
                                        outputWriter.write(str + "\n"); // last value with NL
                                }
                                outputWriter.flush();
                            } catch (IOException ex) {
                                LOGGER.log(Level.SEVERE, "Output write error");
                                LOGGER.log(Level.INFO, "Exception info", ex);
                                closeOutputFile();
                                disableOutputWrite();
                            }
                        }

                        // If temperature readings <= 0 then use previous value
                        for (int i = 1; i < temp.length; i++) {
                            if (temp[i] <= 0.0)
                                temp[i] = data[nx-1][i];
                            if (temp[i] < 0.0)
                                temp[i] = 0.0;
                            // If temperature readings > 8000 then use previous value
                            if (temp[i] > 8000.0)
                                temp[i] = data[nx-1][i];
                        } 

                        // Shift data array
                        for (int i = 0; i < nx-1; i++) {
                            data[i] = data[i+1];
                        }
                        // Fill last data point
                        data[nx-1] = temp;

                        // If first reading, fill arrays with defaults
                        if (data[0][0] <= 0.0 ) {
                            System.arraycopy(temp, 0, dmin, 0, dmin.length);
                            System.arraycopy(temp, 0, dmax, 0, dmax.length);
                            for (int i = 0; i < data.length-1; i++) {
                                data[i] = data[nx-1];
                            }
                        }
                        
                        // Calculate data minimum and maximum
                        for (int i = 1; i < temp.length; i++) {
                            if (temp[i] > 0.0 && dmin[i] > temp[i]) {
                                // !! Only for temperature channels 1 - 16
                                if (i < 17 ) dmin[0] = temp[0];
                                dmin[i] = temp[i];
                            }
                            if (dmax[i] < temp[i]) {
                                // !! Only for temperature channels 1 - 16
                                if (i < 17 ) dmax[0] = temp[0];
                                dmax[i] = temp[i];
                            }
                        }

                        // Prepare traces to plot
                        tracesDataset = new BeamProfileDataset(data);
                        JFreeChart chart = chart1.getChart();
                        XYPlot plot = chart.getXYPlot();
                        XYItemRenderer renderer = plot.getRenderer();
                        boolean savedNotify = plot.isNotify();
                        // Stop refreshing the plot
                        plot.setNotify(false);
                        for (int i=0, j=0; i<jTable1.getRowCount(); i++) { 
                            try {
                                if ((boolean) jTable1.getValueAt(i, 3)) {
                                    tracesDataset.addSeries((int) jTable1.getValueAt(i, 0));
                                    renderer.setSeriesPaint(j++, (Color) jTable1.getValueAt(i, 2));
                                }
                            } catch (Exception e) {
                            }
                        }
                        plot.setNotify(savedNotify);
                        
                        // Calculate profiles prof1 - vertical and prof2 - horizontal
                        // Dataset for profiles
                        profileDataset = new DefaultXYDataset();
                        // 1. Current profile
                        for (int i =0; i < p1range.length; i++) {
                            prof1[i] = data[nx-1][p1range[i]] - dmin[p1range[i]];
                        }
                        for (int i =0; i < p2range.length; i++) {
                            prof2[i] = data[nx-1][p2range[i]] - dmin[p2range[i]];
                        }
                        // Add Vertical profile
                        double[][] plottedData = new double[2][p1range.length];
                        for (int j = 0; j < p1range.length; j++) {
                            plottedData[0][j] = p1x[j];
                            plottedData[1][j] = prof1[j];
                        }
                        profileDataset.addSeries("vertProf", plottedData);
                        // Add Horizontal profile
                        plottedData = new double[2][p2range.length];
                        for (int j = 0; j < p2range.length; j++) {
                            plottedData[0][j] = p2x[j];
                            plottedData[1][j] = prof2[j];
                        }
                        profileDataset.addSeries("horizProf", plottedData);
                        
                        // Calculate maximal horizontal and vertical profiles
                        // Refresh maximal profiles
                        if (max(prof1max) < max(prof1)) {
                            maxTime = temp[0];
                            System.arraycopy(prof1, 0, prof1max, 0, prof1max.length);
                            System.arraycopy(prof2, 0, prof2max, 0, prof2max.length);
                        }
                        // Vertical profile
                        plottedData = new double[2][prof1max.length];
                        for (int j = 0; j < prof1max.length; j++) {
                            plottedData[0][j] = p1x[j];
                            plottedData[1][j] = prof1max[j];
                        }
                        profileDataset.addSeries("maxVertProf", plottedData);
                        // Horizontal profile
                        plottedData = new double[2][p2range.length];
                        for (int j = 0; j < p2range.length; j++) {
                            plottedData[0][j] = p2x[j];
                            plottedData[1][j] = prof2max[j];
                        }
                        profileDataset.addSeries("maxHorizProf", plottedData);
                        
                        // Find max vertical profile for last 3 min
                        double maxValue = data[nx-1][p1range[0]];
                        int maxXIndex = nx-1;
                        int maxYIndex = p1range[0];
                        for (int i = nx-1; i >=0; i--) {
                            if (data[nx-1][0]-data[i][0] >= lastProfTime)
                                break;
                            for (int j = 0; j < p1range.length; j++) {
                                if (data[i][p1range[j]] > maxValue) {
                                    maxValue = data[i][p1range[j]];
                                    maxXIndex = i;
                                    maxYIndex = p1range[j];
                                }
                            }
                        }
                        // Vertical profile
                        plottedData = new double[2][p1range.length];
                        for (int j = 0; j < p1range.length; j++) {
                            plottedData[0][j] = p1x[j];
                            plottedData[1][j] = data[maxXIndex][p1range[j]]- dmin[p1range[j]];
                        }
                        profileDataset.addSeries("lastMaxVertProf", plottedData);
                        // Horizontal profile
                        plottedData = new double[2][p2range.length];
                        for (int j = 0; j < p2range.length; j++) {
                            plottedData[0][j] = p2x[j];
                            plottedData[1][j] = data[maxXIndex][p2range[j]]- dmin[p2range[j]];
                        }
                        profileDataset.addSeries("lastMaxHorizProf", plottedData);

/*                        
                        // Faded profiles - refresh every fpdt seconds
                        if (Math.abs(c.getSeconds() - c1.getSeconds()) < fpdt) {
                            for(int i = 0; i < fpi.length; i++) {
                                if (fpi[i] > 0) fpi[i] = fpi[i] - 1;
                            }
                        }
                        else {
                            for(int i = 0; i < fpi.length-1; i++) {
                                fpi[i] = fpi[i+1];
                            }
                            fpi[fpi.length - 1] = nx - 1;
                            c1 = c;
                        }
                        for (int i: fpi) {
                            //System.out.println(i);
                            plottedData = new double[2][p1range.length];
                            for (int j = 0; j < p1range.length; j++) {
                                plottedData[0][j] = p1x[j];
                                plottedData[1][j] = data[i][p1range[j]] - dmin[p1range[j]];
                            }
                            profileDataset.addSeries("Faded " + i, plottedData);
                        }
*/
                        // Shift marker
                        mi = mi - 1;
                        if (mi < 0) {
                            int nx = data.length;
                            int ny = data[0].length;
                            double maxdata = data[0][1];
                            int index = 0;
                            for (int j = 1; j < ny; j++) {
                                for (int i = 0; i < nx; i++) {
                                    if (data[i][j] > maxdata) {
                                        index = i;
                                        maxdata = data[i][j];
                                    }
                                }
                            }
                            mi = index;
                        }
                        mi1 = Math.max(mi - mw, 0);
                        mi2 = Math.min(mi + mw, nx-1);

                        // Calculate and plot equivalent current
                        // Intgeration window limits [tpn1, tpn2]
                        tpn2 = nx-1;
                        tpn1 = tpn2 - 2*tpw;
                        double dt;
                        double integralin = 0.0;
                        double integralout = 0.0;
                        double integraldt = 0.0;
                        double integralflow = 0.0;
                        double minout = data[tpn1][bctout];
                        double minin = data[tpn1][bctin];
                        for (int i = nx-2; i > 0; i--) {
                            if ((data[nx-1][0] - data[i][0]) > integrationTime)
                                break;
                            dt = data[i+1][0] - data[i][0];
                            if (minout > data[i][bctout]) {
                                minout = data[i][bctout];
                            }
                            if (minin > data[i][bctin]) {
                                minin = data[i][bctin];
                            }
                            integraldt += dt; 
                            integralout += data[i][bctout]*dt; 
                            integralin += data[i][bctin]*dt; 
                            integralflow += data[i][bcflowchan]*dt; 
                        }
                        double flow = integralflow/integraldt;
                        double deltaTout = integralout - minout*integraldt;
                        double deltaTin = integralin - minin*integraldt;
                        //System.out.println("Integral In " + integralin + " min " + minin);
                        //System.out.println("Flow " + flow + " dt " + integraldt);
                        double beamCurrent = (deltaTout - deltaTin)*flow*Q/beamVoltage/1000.0/beamDuration;  //mA
                        statusLine = String.format("Flow: %7.3f V; Out: %7.3f C; In: %7.3f C; %7.3f s; %7.3f mA;", 
                                flow, deltaTout, deltaTin, integraldt/1000.0, beamCurrent);
                        //System.out.println("In " + (integralin - minin*integraldt));
                        //System.out.println("Out " + (integralout - minout*integraldt));
                        //System.out.println("Beam current " + beamCurrent);

//<editor-fold defaultstate="collapsed" desc=" Copied from BeamProfile.m and other staff">
    /*

// Determine integration window from targeting traces
                        double maxdata = data[mi1][tpn[0]];
                        double mindata = data[mi1][tpn[0]];
                        int indexx = mi1;
                        int indexy = tpn[0];
                        for (int j = 0; j < tpn.length; j++) {
                            for (int i = mi1; i < mi2; i++) {
                                if (data[i][tpn[j]] > maxdata) {
                                    indexx = i;
                                    indexy = tpn[j];
                                    maxdata = data[i][tpn[j]];
                                }
                                if (data[i][tpn[j]] < mindata) {
                                    mindata = data[i][tpn[j]];
                                }
                            }
                        }
                        tpnm = indexx;
                        tpn2 = tpnm + tpw;
                        if (tpn2 > (nx-1)) {
                            tpn2 = nx-1;
                        }
                        tpn1 = tpn2 - 2*tpw;
                        if (tpn1 < 0) {
                            tpn1 = 0;
                            tpn2 = tpn1 + 2*tpw;
                        }

// Calculate and plot equivalent current
                        double dt;
                        double integralin = 0.0;
                        double integralout = 0.0;
                        double integraldt = 0.0;
                        double integralflow = 0.0;
                        double minout = data[tpn1 + 1][bctout];
                        double minin = data[tpn1 + 1][bctin];
                        for (int i = tpn1 + 1; i < tpn2; i++) {
                            dt = data[i-1][0] - data[i][0];
                            if (minout > data[i][bctout]) {
                                minout = data[i][bctout];
                            }
                            if (minin > data[i][bctin]) {
                                minin = data[i][bctin];
                            }
                            integraldt += dt; 
                            integralout += data[i][bctout]*dt; 
                            integralin += data[i][bctin]*dt; 
                            integralflow += data[i][bcflowchan]*dt; 
                        }
                        double beamCurrent = ((integralout - integralin) - (minout - minin)*integraldt)*
                                                integralflow/integraldt*Q/voltage;  //mA

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
*/
    // </editor-fold> 

                        // Refresh plots
                        process(new ArrayList<Void>());
                    }
                    else {
                        // Refresh Figure
                        //process(new ArrayList<Void>());
                    }
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Exception during doInBackground");
                    LOGGER.log(Level.INFO, "Exception info", ex);
                    stopMeasuremets();
                }
            }
            return null;
        }
        
        @Override
        protected void process(List<Void> chunks) {
            chart1.getChart().getXYPlot().setDataset(tracesDataset);
            chart2.getChart().getXYPlot().setDataset(profileDataset);
            jLabel3.setText(statusLine);
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            //taskOutput.append("Done!\n");
        }
    }

//****************************************************************************
    class AdamReader {
        File file;
        BufferedReader reader = null;
        String line;
        String[] columns;
        int index;

        AdamReader(File _file) throws FileNotFoundException {
            openFile(_file);
            LOGGER.log(Level.FINEST, "AdamReader: {0} created", file.getName()); 
        }
    
        AdamReader(String fileName) throws FileNotFoundException {
            openFile(fileName);
            LOGGER.log(Level.FINEST, "AdamReader: {0} created", file.getName()); 
        }

        public void openFile(File _file) throws FileNotFoundException {
            if (reader == null) {
                if (!_file.canRead()) {
                    LOGGER.log(Level.WARNING, "AdamReader: File is unreadable");
                    throw new FileNotFoundException("File is unreadable");
                }
                file = _file;
                reader = new BufferedReader(new FileReader(file));
                // Reset input buffer
                index = -1;
                LOGGER.log(Level.FINEST, "AdamReader: File {0} has been opened", file.getName());
                return;
            }
            LOGGER.log(Level.WARNING, "AdamReader: Input file should be closed first");
            throw new FileNotFoundException("Input file should be closed first");
        }

        public void openFile(String fileName) throws FileNotFoundException {
            openFile(new File(fileName));
        }

        public void openFile(String filePath, String fileName) throws FileNotFoundException {
            openFile(new File(filePath, fileName));
        }

        public void closeFile() {
            if (reader != null) {
                try {
                    reader.close();
                    LOGGER.log(Level.FINEST, "AdamReader: Input file has been closed");
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "AdamReader: Input file closing error ");
                    LOGGER.log(Level.INFO, "Exception info", ex);
                }
            } 
            reader = null;
        }
        
        public String readString() throws FileNotFoundException, IOException {
            if (reader != null) {
                // Reading next line from reader
                if (index <= 0) {
                    // Read next line
                    line = reader.readLine();
                    // Reopen file if EOF
                    if (line == null) {
                        closeFile();
                        openFile(file);
                        line = reader.readLine();
                    }
                    columns = line.split(";");
                    // Skip fist value = time
                    index = 1;
                }
                StringBuilder result = new StringBuilder("<");
                String str;
                for (int i = 0; i < 8; i++)
                {
                    if (index >= columns.length) 
                        str = "+000.00";
                    else
                        str = columns[index].trim();
                    index++;
                    double d;
                    try {
                        d = Double.parseDouble(str);
                    }
                    catch (NumberFormatException | NullPointerException ex) {
                        d = -8888.8;
                    }
                    str = String.format("%+07f", d);
                    str = str.replaceAll(",", ".");
                    result.append(str);
                }
                if (index >= 24)
                    index = 0;
                return result.toString();
            } 
            LOGGER.log(Level.WARNING, "AdamReader: Reading from closed file");
            throw new FileNotFoundException("Reading from closed file");
        }
    } 

//****************************************************************************
}
