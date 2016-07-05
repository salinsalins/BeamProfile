/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

/**
 *
 * @author Sanin
 */

import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.ProcessImageFactory;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.Modbus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.wimpi.modbus.msg.ModbusResponse;

/**
 * Class implementing a <tt>CommonModbusResponse</tt>.
 * The implementation directly correlates with the class 0
 * function <i>read multiple registers (FC 4)</i>. It
 * encapsulates the corresponding response message.
 *
 * @author 
 * @version @version@ (@date@)
 */
public final class CommonModbusResponse
    extends ModbusResponse {

  private int m_ByteCount;
  private InputRegister[] m_Registers;
  byte[] data;

  /**
   * Constructs a new <tt>CommonModbusResponse</tt>
   * instance.
   */
  public CommonModbusResponse() {
    super();
    setFunctionCode(Modbus.READ_INPUT_REGISTERS);
  }//constructor


  /**
   * Constructs a new <tt>CommonModbusResponse</tt>
   * instance.
   *
   * @param newData byte[] array of read data.
   */
  public CommonModbusResponse(byte[] newData) {
    super();
    //setFunctionCode(Modbus.READ_INPUT_REGISTERS);
    //m_ByteCount = registers.length * 2;
    //m_Registers = registers;
    //setDataLength(m_ByteCount + 1);
    
    data = newData;
  }//constructor


  /**
   * Returns the number of bytes that have been read.
   * <p/>
   *
   * @return the number of bytes that have been read
   *         as <tt>int</tt>.
   */
  public int getByteCount() {
    return data.length;
  }//getByteCount

  @Override
  public void writeData(DataOutput dout)
      throws IOException {
    dout.write(data);
  }//writeData

  @Override
  public void readData(DataInput din)
      throws IOException {
    int length = din.readUnsignedByte();
    data = new byte[length];
    din.readFully(data);
    /*
    ProcessImageFactory pimf = ModbusCoupler.getReference().getProcessImageFactory();
    for (int k = 0; k < getWordCount(); k++) {
      registers[k] = pimf.createInputRegister(din.readByte(), din.readByte());
    }
    m_Registers = registers;
    //update data length
    setDataLength(getByteCount() + 1);
*/
  }//readData

}//class CommonModbusResponse