package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import android.os.Handler;

class memory
{
    public byte Get(int index) { return m_buffer[index]; }
    public int size() { return size; }
    
    
    public String Load(File filePath, Handler handler, DeviceInfo deviceInfo) throws IOException
    {
        final FileInputStream file = new FileInputStream(filePath);
        if(filePath.length() >= Integer.MAX_VALUE)
        {
             file.close();
             return "You really should not try to flash files bigger than 2 gigabytes."; 
        }
        final byte[] fileBuff = new byte[(int) filePath.length()];
        file.read(fileBuff);
        file.close();
        
        m_buffer = null;
        System.loadLibrary("load_hex");
        m_buffer = parseHexFile(fileBuff, deviceInfo.mem_size, (int) filePath.length());
        if(m_buffer == null)
            return "Corrupted hex file!";
        for(size = deviceInfo.mem_size; m_buffer[size-1] == 0; --size) {};
        return "";
    }
    public native byte[] parseHexFile(byte[] file, int memsize, int fileLenght);
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
