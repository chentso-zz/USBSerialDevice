package com.ecem.usbserialdevice;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;

/**
 * Created by tso on 16/04/15.
 */
public class USBSerialDevice {
    private Context appContext;
    private IntentFilter filter;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbManager mUsbManager;
    private ArrayList<UsbDevice> myDeviceList = new ArrayList<UsbDevice>();
    private int usbEnum = 0;
    private PendingIntent mPermissionIntent;
    private int vid;
    private int baudrate;
    private int updateRate;

    // buffer to store input from serial
    private byte buffer[] = new byte[2048];

    // For writing buffers to files
    private BufferWriter bufferWriter = new BufferWriter();

    private UsbSerialPort port;
    private UsbSerialDriver driver;

    private static final String ACTION_USB_PERMISSION = "SERIALUSB.USB_PERMISSION";
    private static final int USB_ADD_MODE = 1;
    private static final int USB_REMOVE_MODE = 0;


    // Constructor
    //      vid = VendorId to read from
    //      baud = vaud rate to set to for the serial device
    public USBSerialDevice(Context c, int vid, int baud, int updateRate){
        appContext = c;
        set_baud(baud);
        set_vid(vid);
        set_updateRate(updateRate);
    }


    // Start listening and make USB device active as soon as user allows permission
    // USB BroadcastReceiver registration
    public void start(){
        filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        appContext.registerReceiver(usbDevicesReceiver, filter);
        scanUSB(USB_ADD_MODE, vid);
        return;
    }


    /** Set the vendor id of the usb serial device
     *
     * @param vid
     */
    public void set_vid(int vid){
        this.vid = vid;
        return;
    }


    /** Set the baud rate of the usb serial device
     *
     * @param brate
     */
    public void set_baud(int brate){
        this.baudrate = brate;
        return;
    }


    /** Set the updateRate of the usb serial device
     *
     * @param updateRate
     */
    public void set_updateRate(int updateRate){
        this.updateRate = updateRate;
        return;
    }


    /** Helper function to remove multiple spaces in a string leaving only one space behind
     *
     * @param str
     * @return
     */
    protected String removeMultipleSpaces(String str){
        StringBuffer sb = new StringBuffer();

        int i = 0;
        boolean firstSpace = false;
        while (i < str.length()) {
            if (str.charAt(i) != ' ') {
                sb.append(str.charAt(i));
                firstSpace = true;
            } else {
                if (firstSpace) {
                    sb.append(str.charAt(i));
                    firstSpace = false;
                }
            }
            i++;
        }
        return sb.toString();
    }


    /** The function that processes the buffer stream read back from USB Serial Devices
     *
     * NOTE: This method needs to be overridden or the subclass object won't do anything
     * @param buffer
     * @param numBytesRead
     */
    protected void processData(byte buffer[], int numBytesRead){
        return;
    }


    // Timer task event that will be called
    private TimerTask serialLoopTask = new TimerTask(){
        @Override
        public void run() {
            try {
                // Empty the buffer
                Arrays.fill(buffer, (byte) 0);

                // Read the buffer
                int numBytesRead = port.read(buffer, 200);

                // Process the data if buffer isn't empty
                if (numBytesRead != 0) processData(buffer, numBytesRead);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    // Functions to enumerate and scan for USB devices
    private void scanUSB(int mode, int vid){
        // Arduino vID = 1027
        // FTDI vID = 1659
        mUsbManager = (UsbManager) appContext.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(appContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ArrayList<UsbDevice> tmpDeviceList = new ArrayList<UsbDevice>();
        while(deviceIterator.hasNext()){
            device = deviceIterator.next();
            if (device.getVendorId() == vid) {
                tmpDeviceList.add(device);
            }
        }

        myDeviceList.retainAll(tmpDeviceList);
        tmpDeviceList.removeAll(myDeviceList);
        myDeviceList.addAll(tmpDeviceList);

        if ((myDeviceList.size() != 0) && (mode==1)) {
            usbEnum = 0;
            mUsbManager.requestPermission(myDeviceList.get(usbEnum), mPermissionIntent);
        } else if (myDeviceList.size() == 0) {
        }
    }


    /** Broadcast receiver to handle USB attach/detach + USB Permission events
     *
     */
    private final BroadcastReceiver usbDevicesReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                scanUSB(USB_ADD_MODE, vid);
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                scanUSB(USB_REMOVE_MODE, vid);
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (myDeviceList.get(usbEnum).getVendorId() == vid){

                            // Get a list of usb serial drivers
                            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                            for (UsbSerialDriver usd : availableDrivers){
                                // get the usb with the vid we want
                                if (usd.getDevice().getVendorId() == vid){
                                    driver = usd;
                                    break;
                                }
                            }

                            // Once we've verified that the user has given permission for the usb
                            // we should unregister the broadcast receiver here
                            appContext.unregisterReceiver(usbDevicesReceiver);

                            // Create a connection to the USB device
                            connection = mUsbManager.openDevice(myDeviceList.get(usbEnum));

                            // Read some data! Most have just one port (port 0).
                            final List<UsbSerialPort> ports = driver.getPorts();

                            // Only start if we actually at least a port with serial comm
                            if (!ports.isEmpty()){
                                try {
                                    // Get the port number
                                    port = ports.get(0);
                                    port.open(connection);

                                    // Set the baud rate of the port
                                    port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                                    (new Timer()).scheduleAtFixedRate(serialLoopTask, 0, updateRate);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                }
            }
        }
    };


    /** Sets the system to start or stop recording
     *
     * @param status
     */
    public void setRecord(boolean status){
        if (status){

            // If no filename has been set, create a filename based on timestamp
            if (!bufferWriter.hasFileName()){
                bufferWriter.setFileUsingTimestamp();
            }

            bufferWriter.startWrite();
        } else {
            bufferWriter.stopWrite();
        }
    }


    /** Write the buffer bytes into the output file
     *
     * @param buffer
     */
    public void write(byte buffer[], int byteCount){
        bufferWriter.write(buffer, byteCount);
    }

    public void setDirectory(String directory){
        bufferWriter.setDirectory(directory);
    }


    /** Set the filename to write the files out to
     *
     * @param filename
     */
    public void setFileName(String filename){
        bufferWriter.setFile(filename);
    }


    /** Set the filename using a timestamp
     *
     */
    public void setFileNameUsingTimeStamp(){
        bufferWriter.setFileUsingTimestamp();
    }


    /** Set the filename using a timestamp with an added suffix
     *
     * @param suffix
     */
    public void setFileNameUsingTimestamp(String suffix){
        bufferWriter.setFileUsingTimestamp(suffix);
    }
}
