package com.ecem.usbserialdevice;

import android.content.Context;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by tso on 20/04/15.
 */
public class BufferWriter {
    private String filename;
    private File file;
    private FileOutputStream fout;
    private boolean record = false;




    /**
     * Set the filename to save the file to
     * @param filename
     * @throws IOException
     */
    public void setFile(String filename) {
        this.filename = filename;
        file = new File(this.filename);
    }




    /** Set the filename using the timestamp
     *
     */
    public void setFileUsingTimestamp(){
        this.filename = (DateFormat.format("yyyyMMdd-kkmmss", new java.util.Date()).toString()) + ".bin";
    }




    /** Set the filename using the timestamp plus a custom suffix
     *
     * @param suffix
     */
    public void setFileUsingTimestamp(String suffix){
        this.filename = (DateFormat.format("yyyyMMdd-kkmmss", new java.util.Date()).toString()) + "-" + suffix + ".bin";
    }




    /**
     * Enable file writing
     */
    public void startWrite() {
        // Create a new file if the file doesn't exist
        try{
            if (!file.exists()){
                file.createNewFile();
            }

            // Create new fileOutputStream
            fout = new FileOutputStream(file);
            record = true;
        } catch (IOException e) {}
    }





    /**
     * Disable file writing
     */
    public void stopWrite() {
        try {
            record = false;
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    /**
     * Write the buffer to files
     * @param buffer
     * @throws IOException
     */
    public void write(byte buffer[]) {
        // Ensure record flag is set to true and the fileoutputstream isn't null
        if (record && (fout != null)) {
            // Write out the file
            try {
                fout.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
