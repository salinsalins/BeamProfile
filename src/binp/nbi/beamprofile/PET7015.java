/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteMultipleCoilsRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.util.BitVector;

/**
 *
 * @author sanin
 */
public class PET7015 {
    static final Logger logger = Logger.getLogger(PET7015.class.getName());
    static final String[] typeNames = {
"0x20: Platinum 100, α=0.00385, -100°C ~ 100°C",
"0x21: Platinum 100, α=0.00385, 0°C ~ 100°C",
"0x22: Platinum 100, α=0.00385, 0°C ~ 200°C",
"0x23: Platinum 100, α=0.00385, 0°C ~ 600°C",
"0x24: Platinum 100, α=0.003916, -100°C ~ 100°C",
"0x25: Platinum 100, α=0. 003916, 0°C ~ 100°C",
"0x26: Platinum 100, α=0. 003916, 0°C ~ 200°C",
"0x27: Platinum 100, α=0. 003916, 0°C ~ 600°C",
"0x28: Nickel 120, -80°C ~ 100°C",
"0x29: Nickel 120, 0°C ~ 100°C",
"0x2A: Platinum 1000, α=0. 00385, -200°C ~ 600°C",
"0x2B: Cu 100 @ 0°C, α=0. 00421, -20°C ~ 150°C",
"0x2C: Cu 100 @ 25°C, α=0. 00427, 0°C ~ 200°C",
"0x2D: Cu 1000 @ 0°C, α=0. 00421, -20°C ~ 150°C",
"0x2E: Platinum 100, α=0. 00385, -200°C ~ 200°C",
"0x2F: Platinum 100, α=0. 003916, -200°C ~ 200°C",
"0x80: Platinum 100, α=0. 00385, -200°C ~ 600°C",
"0x81: Platinum 100, α=0. 003916, -200°C ~ 600°C",
"0x82: Cu 50 @ 0°C, -50°C ~ 150°C",
"0x83: Nickel 100, -60°C ~ 180°C"};
    static final int[] types =   { 0x20,   0x21,   0x22,   0x23,   0x24,   0x25,   0x26, 
        0x27,   0x28,   0x29,   0x2A,   0x2B,   0x2C,   0x2D,   0x2E,   0x2F,   0x80,   0x81,
        0x82,   0x83  };
    static final double[] pmin = { -100.0, -100.0, -200.0, -600.0, -100.0, -100.0, -200.0, 
        -600.0, -100.0, -100.0, -600.0, -150.0, -200.0, -150.0, -200.0, -200.0, -600.0, -600.0,
        -150.0, -180.0};
    static final double[] pmax = {  100.0,  100.0,  200.0,  600.0,  100.0,  100.0,  200.0, 
         600.0,  100.0,  100.0,  600.0,  150.0,  200.0,  150.0,  200.0,  200.0,  600.0,  600.0,
         150.0,  180.0};
    
    TCPMasterConnection con = null;     //the connection
    ModbusTransaction trans = null;     //the transaction
    InetAddress addr = null;            //the slave's address
    int port = Modbus.DEFAULT_PORT;
    int unitID = 1; //the unit identifier we will be talking to
    int ref = 0;    //the reference; offset where to start reading from
    int count = 1;  //the number of DI's or AI's to read
    
    int moduleName = 0;
    int[] channels = new int[7];
    int[] cti = new int[7];
    
    public PET7015(String strAddr, int port) throws UnknownHostException, Exception {
        setInetAddress(strAddr);
        setPort(port);
        openConnection();

        moduleName = readMultipleRegisters(559, 1)[0];
        if(moduleName != 0x7015) {
            System.out.printf("Incorrect module name: 0x%H\n", moduleName);
        } else {
            System.out.printf("Module name: 0x%H\n", moduleName);
            channels = readMultipleRegisters(427, 7);
            cti = new int[channels.length];
            for (int i=0; i < channels.length; i++) {
                int n = -1;
                for (int j=0; j < types.length; j++) {
                    if(types[j] == channels[i]) {
                        n = j;
                        break;
                    } 
                }
                cti[i] = n;
                System.out.printf("Channel %d", i);
                if(n >= 0) 
                    System.out.printf(" %s\n", typeNames[n]);
                else
                   System.out.printf(" Unknown type 0x%H\n", channels[i]);
            }
        }
    }

/*    
    public PET7015(String strAddr) throws UnknownHostException, Exception {
        this(strAddr, Modbus.DEFAULT_PORT);
    }
*/

    public void setInetAddress(String strAddr) throws UnknownHostException, Exception {
        addr  = InetAddress.getByName(strAddr);
        closeConnection();
        openConnection();
    }
    
    public void setPort(int newPort) throws Exception {
        port  = newPort;
        closeConnection();
        openConnection();
    }
    
    public void openConnection() throws Exception {
        con = new TCPMasterConnection(addr);
        con.setPort(port);
        con.connect();
    }

    public void closeConnection() {
        con.close();
    }
    
    public boolean isConnected() {
        return con.isConnected();
    }

    public ModbusResponse getResponse() {
        return trans.getResponse();
    }
    
    public final int[] readMultipleRegisters(int ref, int count) throws ModbusSlaveException, ModbusException {
        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, count);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();

        int[] registers = new int[res.getWordCount()];
        for (int i = 0; i < res.getWordCount(); i++) {
            registers[i] = res.getRegisterValue(i);
        }
        return registers;
    }

    public int[] readInputRegisters(int ref, int count) throws ModbusSlaveException, ModbusException {
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(ref, count);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
        ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();

        int[] registers = new int[res.getWordCount()];
        for (int i = 0; i < res.getWordCount(); i++) {
            registers[i] = res.getRegisterValue(i);
        }
        return registers;
    }

    public boolean[] readCoils(int ref, int count) throws ModbusSlaveException, ModbusException {
        ReadCoilsRequest req = new ReadCoilsRequest(ref, count);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
        ReadCoilsResponse res = (ReadCoilsResponse) trans.getResponse();

        boolean[] coils = new boolean[res.getBitCount()];
        for (int i = 0; i < res.getBitCount(); i++) {
            coils[i] = res.getCoilStatus(i);
        }
        return coils;
    }

    public boolean[] readInputDiscretes(int ref, int count) throws ModbusSlaveException, ModbusException {
        ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(ref, count);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
        ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) trans.getResponse();

        boolean[] bits = new boolean[res.getBitCount()];
        for (int i = 0; i < res.getBitCount(); i++) {
            bits[i] = res.getDiscreteStatus(i);
        }
        return bits;
    }

    public void write(int ref, int[] values) throws  ModbusIOException, ModbusSlaveException, ModbusException {
        Register[] regs = new SimpleRegister[values.length];
        for(int i=0; i < values.length; i++) {
            regs[i] = new SimpleRegister(values[i]);
        }
        WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(ref, regs);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
        //WriteMultipleRegistersResponse res = (WriteMultipleRegistersResponse) trans.getResponse();
    }

    public void write(int ref, boolean[] values) throws  ModbusIOException, ModbusSlaveException, ModbusException {
        BitVector bv = new BitVector(values.length);
        for(int i=0; i < values.length; i++) {
            bv.setBit(i, values[i]);
        }
        WriteMultipleCoilsRequest req = new WriteMultipleCoilsRequest(ref, bv);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
        //WriteMultipleCoilsResponse res = (WriteMultipleCoilsResponse) trans.getResponse();
    }

    public void write(int ref, boolean value) throws  ModbusIOException, ModbusSlaveException, ModbusException {
        WriteCoilRequest req = new WriteCoilRequest(ref, value);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
    }

    public void write(int ref, int value) throws  ModbusIOException, ModbusSlaveException, ModbusException {
        Register reg = new SimpleRegister(value);
        WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(ref, reg);
        req.setUnitID(unitID);
        trans  = new ModbusTCPTransaction(con);
        trans.setRequest(req);
        trans.execute();
    }

    public double[] read() {
        double[] result = new double[7];
        try {
            int[] registers = readInputRegisters(0, 7);
            for (int i = 0; i < registers.length; i++) {
                int index = cti[i];
                result[i] = pmax[index]/0x7fff*registers[i];
                if(registers[i] == 0x8000) result[i] = -9999.9;
            }
            return result;
        } catch (ModbusException ex) {
            logger.log(Level.WARNING, "ModbusException", ex);
            for (int i = 0; i < result.length; i++) {
                result[i] = -9999.9;
            }
            return result;
        }
    }

    public double read(int channel) {
        try {
            int[] register = readInputRegisters(channel, 1);
            int index = cti[channel];
            double result;
            result = pmax[index]/0x7fff*register[0];
            if(register[0] == 0x8000) result = -9999.9;
            return result;
        } catch (ModbusException ex) {
            logger.log(Level.WARNING, "ModbusException", ex);
            return -9999.9;
        }
    }
}
