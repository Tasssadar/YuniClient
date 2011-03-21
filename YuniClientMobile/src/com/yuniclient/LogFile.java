package com.yuniclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

class LogFile
{
    public LogFile()
    {
        initiated = false;
    }
    public void init(boolean withTime)
    {
        if(initiated)
            return;
        time = withTime;
        Calendar cal = Calendar.getInstance();
        File path = new File("/sdcard/YuniClient/logs/");
        if(!path.exists())
            path.mkdirs();
        file = new File("/sdcard/YuniClient/logs/log_" + cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.DATE) +
                "_" + cal.get(Calendar.MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY) + "_" + cal.get(Calendar.MINUTE) +
                "_" + cal.get(Calendar.SECOND) + ".txt");    
        try {
            file.createNewFile();
            stream = new FileOutputStream(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(stream != null)
            initiated = true;
        writeString("YuniClient play log started \r\n");
    }
    
    public void writeString(String text)
    {
        if(!initiated)
            return;
        try {
            String writeText = "";
            if(time)
            {
                Calendar cal = Calendar.getInstance();
                writeText = "[" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "] ";
            }
            writeText += text + "\r\n";
            stream.write(writeText.getBytes());
            stream.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void close()
    {
        if(!initiated || file == null)
            return;
        try {
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        file = null;
        initiated = false;
        stream = null;
    }
    
    private File file;
    private FileOutputStream stream;
    private boolean initiated;
    private boolean time;
}