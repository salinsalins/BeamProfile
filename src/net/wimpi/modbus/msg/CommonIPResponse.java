/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wimpi.modbus.msg;

import net.wimpi.modbus.Modbus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Class implementing a <tt>CommonIPResponse</tt>.
 *
 * @author
 * @version @version@ (@date@)
 */
public final class CommonIPResponse
        extends ModbusResponse {

    private byte[] data;

    /**
     * Constructs a new <tt>CommonIPResponse</tt> instance.
     */
    public CommonIPResponse() {
        this(Modbus.READ_INPUT_REGISTERS, new byte[] {0,1,0,1});
    }//constructor

    /**
     * Constructs a new <tt>CommonIPResponse</tt> instance.
     *
     * @param newData the byte[] response data.
     * @param functionCode function code for the request.
     */
    public CommonIPResponse(int functionCode, byte[] newData) {
        super();
        setFunctionCode(functionCode);
        setDataLength(newData.length+1);
        data = newData;
    }//constructor

    @Override
    public void writeData(DataOutput dout)
            throws IOException {
        dout.write(data);
    }//writeData

    @Override
    public void readData(DataInput din)
            throws IOException {
        data = new byte[din.readUnsignedByte()];
        din.readFully(data);
    }//readData

    public byte[] getData() {
        return data;
    }//getData

}//class CommonIPResponse
