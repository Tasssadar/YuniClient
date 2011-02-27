package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import com.yuni.client.R;

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
        
     /*   final byte[] rec_nums = new byte[50];
        byte rec_nums_itr = 0;
        m_buffer = new byte[deviceInfo.mem_size];
        size = 0;
        int pos = 0;
        int lastSendPos = 0;
        Message msg = null;
        short length = 0;
        short address = 0;
        short rectype = 0;
        short base_i = 0;
        char[] line = new char[50];
        byte lineLenght;

        while(true)
        {
            if(Debug.isDebuggerConnected() && pos - lastSendPos >= 1024)
            {
                msg = new Message();
                msg.arg1 = (int)(pos/1024);
                handler.sendMessage(msg);
                lastSendPos = pos;
            }
            if (pos >= filePath.length())
                break;
            
            line[0] = (char)fileBuff[pos];
            lineLenght = 1;
            ++pos;
            if (line[0] == ':')
            {
                while (pos + 1 != filePath.length())
                {
                    if ((char)fileBuff[pos] == '\r' && (char)fileBuff[pos+1] == '\n')
                    {
                        pos+=2;
                        break;
                    }
                    line[lineLenght] = (char)fileBuff[pos];
                    ++pos;
                    ++lineLenght;
                    if(lineLenght >= 50)
                    {
                        char[] tmp = line;
                        line = new char[100];
                        for(byte z = 0; z < 50; ++z)
                            line[z] = tmp[z];
                    }
                    else if(lineLenght >= 100)
                        return "Wrong line length";
                }
            }
            if (line[0] != ':' || lineLenght % 2 != 1)
                return "Wrong line format";
            rec_nums_itr = 0;
            for (short i = 1; i + 1 < lineLenght; i+=2)
            {
                rec_nums[rec_nums_itr] = (byte) Integer.parseInt(String.copyValueOf(line, i, 2), 16);
                ++rec_nums_itr;
            }
            length = (short) (0xFF & (int)rec_nums[0]);
            address = (short) ((0xFF & (int)rec_nums[1]) * 0x100 + (0xFF & (int)rec_nums[2]));
            rectype = (short)(0xFF & (int)rec_nums[3]);
            if (length != rec_nums_itr - 5)
                return "Wrong line length";

            if (rectype == 2)
            {
                if (length != 2)
                    return "Wrong rectype 2 length";
                base_i = (short)((((0xFF & (int)rec_nums[4]) * 0x100 + (0xFF & (int)rec_nums[5])) * 16));
                continue;
            }

            if (rectype == 1)
                break;

            if (rectype != 0)
                return "Unexpected rectype";

            for (int i = 0; i < length; ++i)
            {
                for (;base_i + address + i >= size; ++size)
                {
                    if(size >= deviceInfo.mem_size)
                        return "File is too big for this device";
                    m_buffer[size] = (byte) 0xFF;
                }

                if (m_buffer[base_i + address + i] != (byte)0xff)
                    return "wtf?";

                m_buffer[base_i + address + i] = rec_nums[i + 4];
            }
        }
        msg = null;*/
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
