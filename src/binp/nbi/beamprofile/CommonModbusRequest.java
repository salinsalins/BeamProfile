/**
 * *
 * Copyright 2016 Andery Sanin
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.
 **
 */
package binp.nbi.beamprofile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.procimg.IllegalAddressException;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.ProcessImage;

/**
 * Class implementing a <tt>CommonModbusRequest</tt>.
 *
 * @author
 * @version @version@ (@date@)
 */
public final class CommonModbusRequest
        extends ModbusRequest {

    //instance attributes
    private int m_Reference;
    private int m_WordCount;
    private byte[] data;

    /**
     * Constructs a new <tt>CommonModbusRequest</tt>
     * instance.
     */
    public CommonModbusRequest() {
        super();
        // Make read input registerd request by default
        setFunctionCode(Modbus.READ_INPUT_REGISTERS);
        //4 bytes (unit id and function code is excluded)
        setDataLength(4);
        data = new byte[4]; //{0, 1, 0, 1}
    }//constructor

    /**
     * Constructs a new <tt>CommonModbusRequest</tt>
     * instance with a given function code and data array.
     * <p>
     * @param functionCode function code for the request.
     * @param requestData data array of the request.
     */
    public CommonModbusRequest(int functionCode, byte[] requestData) {
        super();
        setFunctionCode(functionCode);
        setDataLength(requestData.length);
        data = requestData;
    }//constructor

    @Override
    public ModbusResponse createResponse() {
        return null;
    /*            
        ReadInputRegistersResponse response = null;
        InputRegister[] inpregs = null;

        //1. get process image
        ProcessImage procimg = ModbusCoupler.getReference().getProcessImage();
        //2. get input registers range
        try {
            inpregs = procimg.getInputRegisterRange(this.getReference(), this.getWordCount());
        } catch (IllegalAddressException iaex) {
            return createExceptionResponse(Modbus.ILLEGAL_ADDRESS_EXCEPTION);
        }
        response = new ReadInputRegistersResponse(inpregs);
        //transfer header data
        if (!isHeadless()) {
            response.setTransactionID(this.getTransactionID());
            response.setProtocolID(this.getProtocolID());
        } else {
            response.setHeadless();
        }
        response.setUnitID(this.getUnitID());
        response.setFunctionCode(this.getFunctionCode());
        return response;
    */
    }//createResponse

    /**
     * Sets the data for
     * <tt>CommonModbusRequest</tt>.
     * <p>
     * @param newData byte[] array.
     */
    public void setData(byte[] newData) {
        data = newData;
    }//setData

    @Override
    public void writeData(DataOutput dout)
            throws IOException {
        dout.write(data);
    }//writeData

    @Override
    public void readData(DataInput din)
            throws IOException {
        din.readFully(data);
    }//readData

}//class ReadInputRegistersRequest
