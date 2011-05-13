package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import android.os.Handler;

class memory
{
    private static final byte ERROR_WRONG_LINE_FORMAT     = 1;
    private static final byte ERROR_INVALID_RECORD_LENGHT = 2;
    private static final byte ERROR_INVALID_2_RECORD      = 3;
    private static final byte ERROR_INVALID_RECORD_TYPE   = 4;
    private static final byte ERROR_MEMORY_LOCATION     = 5;
    
    public byte Get(int index) { return m_buffer[index]; }
    public int size() { return size; }
    
    
    public String Load(File filePath, Handler handler, DeviceInfo deviceInfo) throws IOException
    {
        if(filePath.length() >= Integer.MAX_VALUE)
             return "You really should not try to flash files bigger than 2 gigabytes."; 

        final FileInputStream file = new FileInputStream(filePath);

        final byte[] fileBuff = new byte[(int) filePath.length()];
        file.read(fileBuff);
        file.close();

        m_buffer = null;
        System.loadLibrary("jni_functions");
        m_buffer = parseHexFile(fileBuff, deviceInfo.mem_size, (int) filePath.length());
        if(m_buffer == null)
            return "Corrupted hex file!";
        else if(m_buffer[1] == 0)
        {
            switch(m_buffer[0])
            {
                case ERROR_WRONG_LINE_FORMAT:
                    return "Wrong line format";
                case ERROR_INVALID_RECORD_LENGHT:
                    return "Invalid record length specified";
                case ERROR_INVALID_2_RECORD:
                    return "Invalid type 2 record";
                case ERROR_INVALID_RECORD_TYPE:
                    return "Invalid record type";
                case ERROR_MEMORY_LOCATION:
                    return "A memory location was defined twice";
                default:
                    return "Corrupted hex file!";
            }
        }
        for(size = deviceInfo.mem_size; m_buffer[size-1] == 0; --size) {};
        return null;
    }
    private native byte[] parseHexFile(byte[] file, int memsize, int fileLenght);
    private byte[] m_buffer;
    private int size;
};

class Page
{
    public int address;
    public byte[] data;
};

class HexFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        File file = new File(dir, name);
        return (name.endsWith(".hex") || file.isDirectory());
    }
}
