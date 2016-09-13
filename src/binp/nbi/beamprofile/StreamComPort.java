/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binp.nbi.beamprofile;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import jssc.SerialPort;

/**
 *
 * @author Sanin
 */
public class StreamComPort {
    static List<StreamComPort> portList = new LinkedList<>();
    SerialPort  port;
    List<Command> commands;
    List<Response> responses;
    
    class Command {
        long time;
        byte[] data;

        Command() {
            time = System.currentTimeMillis();
            data = null;
        }

        Command(byte[] d) {
            time = System.currentTimeMillis();
            data = d;
        }

        Command(long t, byte[] d) {
            time = t;
            data = d;
        }

        Command(String s) {
            time = System.currentTimeMillis();
            data = s.getBytes();
        }

        Command(long t, String s) {
            time = t;
            data = s.getBytes();
        }

        byte[] getData() {
            return data;
        }

        String getDataString() {
            return new String(data);
        }

        long getTime() {
            return time;
        }

        void setTime() {
            time = System.currentTimeMillis();
        }

        void setTime(long t) {
            time = t;
        }

        void setTime(Date d) {
            time = d.getTime();
        }

        Date getDate() {
            return new Date(time);
        }

    }
    
    class Response extends Command {
 
        Response() {
            super();
        }

        Response(byte[] d) {
            super(d);
        }

        Response(String s) {
            super(s);
        }

    }
}
