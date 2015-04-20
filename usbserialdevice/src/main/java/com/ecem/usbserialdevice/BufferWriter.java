package com.ecem.usbserialdevice;

import android.text.format.DateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Log;

/**
 * Created by tso on 20/04/15.
 */
public class BufferWriter {
    private String filename;
    private File file;
    private FileOutputStream fout;
    private boolean record = false;
    private String directory = "/sdcard/";

    /** Set the directory to save the file in
     *
     * @param directory
     */
    public void setDirectory(String directory){
        this.directory = directory;
        File dir = new File(directory);

        // If the directory doesn't exist, make a new directory
        if (!dir.exists()){
            dir.mkdir();
        }
    }


    /** Set the filename to save the file to
     *
     * @param filename
     * @throws IOException
     */
    public void setFile(String filename) {
        this.filename = directory + filename;
        file = new File(this.filename);

        Log.d("part", "setFile: " + this.filename);
    }


    /** Set the filename using the timestamp
     *
     */
    public void setFileUsingTimestamp(){
        setFile((DateFormat.format("yyyyMMdd-kkmmss", new java.util.Date()).toString()) + ".bin");

    }


    /** Set the filename using the timestamp plus a custom suffix
     *
     * @param suffix
     */
    public void setFileUsingTimestamp(String suffix){
        setFile((DateFormat.format("yyyyMMdd-kkmmss", new java.util.Date()).toString()) + "-" + suffix + ".bin");
    }


    /** Enable file writing
     *
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



    /** Disable file writing
     *
     */
    public void stopWrite() {
        try {
            record = false;
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** Write the buffer to files
     *
     * @param buffer
     * @throws IOException
     */
    public void write(byte buffer[], int byteCount) {
        // Ensure record flag is set to true and the fileoutputstream isn't null
        if (record && (fout != null)) {
            // Write out the file
            try {
                fout.write(buffer, 0, byteCount);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /** Boolean to determine if the client has set a filename
     *
     * @return
     */
    public boolean hasFileName(){
        return (this.filename != null);
    }
}
