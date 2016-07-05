/**
 * *
 * Copyright 2002-2010 jamod development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **
 */
package net.wimpi.modbus.msg;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.wimpi.modbus.Modbus;

/**
 * Class implementing a <tt>CommonIPRequest</tt>. 
 * 
 * @author Dieter Wimberger
 * @version @version@ (@date@)
 */
public final class CommonIPRequest
        extends ModbusRequest {

    private byte[] data;

    /**
     * Constructs a new <tt>CommonIPRequest</tt> instance.
     */
    public CommonIPRequest() {
        this(Modbus.READ_INPUT_REGISTERS, new byte[] {0,1,0,1});
    }//constructor

    /**
     * Constructs a new <tt>CommonIPRequest</tt> instance
     * with a given function code and data array.
     * <p>
     * @param functionCode function code for the request.
     * @param requestData data array of the request.
     */
    public CommonIPRequest(int functionCode, byte[] requestData) {
        super();
        setFunctionCode(functionCode);
        setDataLength(requestData.length);
        data = requestData;
    }//constructor

    @Override
    public ModbusResponse createResponse() {
        CommonIPResponse response = new CommonIPResponse(this.getFunctionCode(), this.data);
        if (!isHeadless()) {
            response.setTransactionID(this.getTransactionID());
            response.setProtocolID(this.getProtocolID());
        } else {
            response.setHeadless();
        }
        response.setUnitID(this.getUnitID());
        return response;
    }//createResponse//createResponse
    
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

    public void setData(byte[] newData) {
        data = newData;
    }//setData

    public byte[] getData() {
        return data;
    }//getData

}//class ReadInputRegistersRequest
