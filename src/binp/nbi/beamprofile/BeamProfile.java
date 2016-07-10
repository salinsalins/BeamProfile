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
import java.util.Date;
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
    
    /**
     * Creates new form BeamProfile
     */
    public BeamProfile() {
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
        addWindowListener(this);
        
        task = new Task();
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
        jComboBox1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Calorimeter Beam Profile Plotter");
        setPreferredSize(new java.awt.Dimension(488, 800));
        setSize(new java.awt.Dimension(0, 0));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Config"));

        jComboBox1.setToolTipText("");

        jLabel2.setText("COM Port:");

        jLabel3.setText("Address:");

        jSpinner2.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));

        jLabel6.setText("URL:");

        jTextField5.setText("http://magicbox:9091/");

        jLabel1.setText("Port:");

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(502, 0, null, 1));

        jButton1.setText("Read");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });
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
                                .add(jLabel2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 140, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jSpinner2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
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
                                        .add(0, 0, Short.MAX_VALUE))
                                    .add(jPanel1Layout.createSequentialGroup()
                                        .add(0, 0, Short.MAX_VALUE)
                                        .add(jButton1)))))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2)
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
            .add(jScrollPane2)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
        );

        jToggleButton1.setText("START");

        jButton5.setText("Cancel");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap(266, Short.MAX_VALUE)
                .add(jToggleButton1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton5)
                .add(64, 64, 64))
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
            //System.out.println(reqInfo);
            //System.out.println(reqInfo.getDataLength());
            //System.out.println(reqInfo.getFunctionCode());
            //System.out.println(reqInfo.getHexMessage());
            
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
                System.out.printf("%2d - %2$d %2$H\n", n, res.getRegisterValue(n));
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

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1MouseClicked
    
    private static int count = 0;
    private static Date lastDate = new Date();
    public void mark() {
        Date date = new Date();
        System.out.printf("%d %tT.%2$tL %d\n", count++, date, date.getTime()-lastDate.getTime());
        lastDate= date;
    }
    public void mark(int c) {
        count = c;
        mark();
    }
    public void mark(String s) {
        System.out.printf("%s ", s);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton5;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JSpinner jSpinner3;
    private javax.swing.JSpinner jSpinner4;
    private javax.swing.JSpinner jSpinner5;
    private javax.swing.JSpinner jSpinner6;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
    
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
//                    //System.out.println("No rows selected.");
//                } else {
//                    int selectedRow = lsm.getMaxSelectionIndex();
//                    //System.out.println("Row " + selectedRow + " is now selected.");
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


    class Task extends SwingWorker<Void, Void> {

        DataFile outputFile;
        boolean newOutput = false;
	
// Data arrays for traces
        int nx = 2000;
	int ny = 4*8+1;
	double[][] data = new double[nx][ny];
	double[] dmin = new double[ny];
	double[] dmax = new double[ny];

// Profile arrays and their plot handles
	int[] p1range = {2,3,4,5,6,7,10,11,12,13,14};    // Channels for vertical profile
	double[] p1x = {1,3,4,5,6,7,8,9,10,11,13};       // X values for prof1
	int[] p2range = {16, 7, 15};                     // Channels for horizontal profile
	int[] p2x = {3, 7, 11};                          // X values for prof2
	double[] prof1  = new double[p1range.length];    // Vertical profile and handle
	double prof1h = 0;
	double[] prof2  = new double[p2range.length];    // Horizontal profile
	double prof2h = 0;
	double[] prof1max  = prof1.clone();      // Maximal vertical profile (over the plot)
        //Arrays.fill(prof1max, 1.0);
        double prof1maxh = 0;          // Maximal vertical profile handle
	double[] prof1max1  = prof1max.clone();  // Maximal vertical profile (from the program start)
	double prof1max1h = 0;         // Handle
	double[] prof2max  = prof2.clone();      // Maximal horizontal profile (ofer the plot)
	//Arrays.fill(prof2max, 1.0);
	double prof2maxh = 0;
        
        
        Task() {
            //outputFile = new DataFile("log.txt");
            newOutput = true;
        }
    
        /**
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            // Initialize progress property.
            while(jToggleButton1.isSelected()) {
                //readData();
                //plotData();
            }
            return null;
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            //taskOutput.append("Done!\n");
        }
    }

}
