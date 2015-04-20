package com.ecem.arduino;

import android.content.Context;

import com.ecem.usbserialdevice.USBSerialDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;

/** Example Arduino Sensor using the USBSerialDevice Class
 *
 *  This is an example of reading data from an Arduino in which a total of 14 bytes are read
 *  for every read op. My Arduino sketch outputs:
 *
 *      time in microsecs   (long)
 *      x accel             (short)
 *      y accel             (short)
 *      z accel             (short)
 *
 */
public class ArduinoSensor extends USBSerialDevice {
    private short accel_x;
    private short accel_y;
    private short accel_z;
    private char decimator = 0;
    private Queue<Byte> buf = new LinkedList();
    private static final int CHUNKSIZE = 14;

    /**
     * The constructor
     * @param c             Context of the calling object
     * @param vid           Vendor ID of the serial USB device
     * @param baud          Baud rate of the serial USB device
     * @param updateRate    Serial polling data rate
     */
    public ArduinoSensor(Context c, int vid, int baud, int updateRate) {
        super(c, vid, baud, updateRate);
    }


    /**
     * This is only for UI purposes and is not necessary for saving data
     * @param buffer
     */
    private void readBinary(byte buffer[]){
        ByteBuffer bf = ByteBuffer.allocate(2);
        bf.order(ByteOrder.LITTLE_ENDIAN);

        // Get x component
        bf.put(buffer, 8, 2).flip();
        accel_x = (short)(bf.getShort() & 0xFFFF);
        bf.clear();

        // Get y component
        bf.put(buffer, 10, 2).flip();
        accel_y = (short)(bf.getShort() & 0xFFFF);
        bf.clear();

        // Get z component
        bf.put(buffer, 12, 2).flip();
        accel_z = (short)(bf.getShort() & 0xFFFF);
        bf.clear();
    }



    /**
     * Wrapper to determine how much to update the object variables
     * @param buffer
     */
    private void updateValueDecimation(byte buffer[]){
        // For now let's read the buffer every loop
        // may be implemented later on to only update every few samples
        readBinary(buffer);
    }



    /**
     * Function to process data retrieved from serial device
     * @param buffer
     * @param numBytesRead
     */
    @Override
    protected void processData(byte buffer[], int numBytesRead) {
        int i;

        if (numBytesRead != 0) {
            // Write the output to a file (superclass checks for record status)
            write(buffer, numBytesRead);
        }

        // Serial communication isn't perfect and sometimes gets delayed
        // so that we don't receive the full message until the next read that
        // catches up with the missed messages. If the number of bytes isn't
        // correct it probably means we're missing or have gotten extra missed
        // bytes store this into a queue and process them when a full line of
        // data is for sure received.

        // If there are data in the buf queue or the number of bytes
        // doesn't match the chunk size we're looking for, store data into the buffer
        if ((buf.size() != 0) || (numBytesRead != CHUNKSIZE)) {
            for (i = 0; i < numBytesRead; i++) {
                // Push the bytes into the queue
                buf.offer(buffer[i]);
            }

            // If there are enough data in the stream
            while (buf.size() >= CHUNKSIZE) {
                int j;
                byte[] b = new byte[CHUNKSIZE];

                // Put values in a byte array
                for( j = 0 ; j < CHUNKSIZE ; j++){
                    b[j] = buf.poll();
                }
                updateValueDecimation(b);
            }
        } else {
            updateValueDecimation(buffer);
        }
    }
}
