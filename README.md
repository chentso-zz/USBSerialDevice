# USBSerialDevice

Android module that wraps around https://github.com/mik3y/usb-serial-for-android. The purpose is to facilitate USB enumeration and file I/O of USB serial devices. 



Currently **only reads** in serial devices and saves buffer to specified (or timestamped) file. Serial writing will be implemented later. 

An example subclass ArduinoSensor is included. 

##Usage

1. Create your own subclass extending the USBSerialDevice class

2. Override the protected processData(...) method. The declaration looks like: `protected void processData(byte buffer[], int numBytesRead)`

3. Add storage file I/O permissions: `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />`

4. Using the ArduinoSensor (with external accelerometers) example and performing purely file writing of the incoming serial bytes, the usage is: 

    	arduino = new ArduinoSensor(this, 1027, 19200, 10);
    	arduino.setDirectory("/sdcard/mydirectory/");
    	arduino.setFileNameUsingTimestamp("ARDUINO");
    	arduino.setRecord(true);
    	arduino.start();