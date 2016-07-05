/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;

/**
 *
 * @author sanin
 */
public class PET7015 {
    static final String[] typeName = {
"0x20: Platinum 100, α=0.00385, ‐100°C ~ 100°C",
"0x21: Platinum 100, α=0.00385, 0°C ~ 100°C",
"0x22: Platinum 100, α=0.00385, 0°C ~ 200°C",
"0x23: Platinum 100, α=0.00385, 0°C ~ 600°C",
"0x24: Platinum 100, α=0.003916, ‐100°C ~ 100°C",
"0x25: Platinum 100, α=0. 003916, 0°C ~ 100°C",
"0x26: Platinum 100, α=0. 003916, 0°C ~ 200°C",
"0x27: Platinum 100, α=0. 003916, 0°C ~ 600°C",
"0x28: Nickel 120, ‐80°C ~ 100°C",
"0x29: Nickel 120, 0°C ~ 100°C",
"0x2A: Platinum 1000, α=0. 00385, ‐200°C ~ 600°C",
"0x2B: Cu 100 @ 0°C, α=0. 00421, ‐20°C ~ 150°C",
"0x2C: Cu 100 @ 25°C, α=0. 00427, 0°C ~ 200°C",
"0x2D: Cu 1000 @ 0°C, α=0. 00421, ‐20°C ~ 150°C",
"0x2E: Platinum 100, α=0. 00385, ‐200°C ~ 200°C",
"0x2F: Platinum 100, α=0. 003916, ‐200°C ~ 200°C",
"0x80: Platinum 100, α=0. 00385, ‐200°C ~ 600°C",
"0x81: Platinum 100, α=0. 003916, ‐200°C ~ 600°C",
"0x82: Cu 50 @ 0°C, ‐50°C ~ 150°C",
"0x83: Nickel 100, ‐60°C ~ 180°C"};
    static final int[] type = { 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 
        0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x80, 0x81,
        0x82, 0x83};
    static final double[] p0 = { 0.20, 0.21, 0.22, 0.23, 0.24, 0.25, 0.26, 
        0.27, 0.28, 0.29, 0.20, 0.20, 0.20, 0.20, 0.20, 0.20, 0.80, 0.81,
        0.82, 0.83};
    static final double[] p1 = { 0.20, 0.21, 0.22, 0.23, 0.24, 0.25, 0.26, 
        0.27, 0.28, 0.29, 0.20, 0.20, 0.20, 0.20, 0.20, 0.20, 0.80, 0.81,
        0.82, 0.83};
    
    TCPMasterConnection con = null;         //the connection
    ModbusTCPTransaction trans = null;      //the transaction
    //ReadInputDiscretesRequest req = null; //the request
    //ReadInputDiscretesResponse res = null; //the response
    InetAddress addr = null; //the slave's address
    int port = Modbus.DEFAULT_PORT;
    int unitID = 1; //the unit identifier we will be talking to
    int ref = 0;    //the reference; offset where to start reading from
    int count = 1;  //the number of DI's or AI's to read
    
    void setInetAddress(String strAddr) throws UnknownHostException {
        addr  = InetAddress.getByName(strAddr);
    }

    void openConnection() {
        con.setPort(port);
        con.connect();
    }


    //3. Prepare the request
    //req = new ReadInputDiscretesRequest(ref, count);
    ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, count);
    req.setUnitID (unitid);
    //req.setHeadless();

            
    //4. Prepare the transaction
    trans  = new ModbusTCPTransaction(con);
    trans.setRequest (req);
    //5. Execute the transaction
    trans.execute();

    //res = (ReadInputDiscretesResponse) trans.getResponse();
    //System.out.println("Digital Inputs Status=" + res.getDiscretes().toString());
    //res = (CommonIPResponse) trans.getResponse();
    ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
    //ModbusResponse resInfo = trans.getResponse();

    //6. Close the connection

    con.close();

}
