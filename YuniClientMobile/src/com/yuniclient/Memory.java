package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;
import android.os.Message;

class memory
{
    public int Get(int index) { return m_buffer[index]; }
    public int size() { return size; }
    public int data() { if(size == 0) return 0; else return m_buffer[0]; }
    
    public boolean Load(File filePath, Handler handler, DeviceInfo deviceInfo) throws IOException
    {
        FileInputStream file = new FileInputStream(filePath);//openFileOutput(filePath.getAbsoluteFile().toString(), Context.MODE_PRIVATE);
        byte[] rec_nums = new byte[50];
        byte rec_nums_itr = 0;
        m_buffer = new byte[deviceInfo.mem_size];
        size = 0;
        long pos = 0;
        long lastSendPos = 0;
        Message msg = null;
        char c = (char)0;
        String digit = null;
        short length = 0;
        short address = 0;
        short rectype = 0;
        short base_i = 0;
        String line;
        while(true)
        {
            if(pos - lastSendPos >= 1024)
            {
                msg = new Message();
                msg.arg1 = (int)(pos/1024);
                handler.sendMessage(msg);
                lastSendPos = pos;
            }
            if (pos >= filePath.length())
                break;
            c = (char)file.read();
            ++pos;
            line = "";
            if (c == ':')
            {
                line += c;
                while (pos + 1 != filePath.length())
                {
                    c = (char)file.read();
                    ++pos;
                    handler.sendMessage(handler.obtainMessage());
                    if (c == '\r' && (char)file.read() == '\n')
                    {
                        c = '\n';
                        ++pos;
                        break;
                    }
                    line += c;
                }
            }
            if (line.charAt(0) != ':' || line.length() % 2 != 1)
                return false;
            rec_nums_itr = 0;
            for (short i = 1; i + 1 < line.length(); ++i)
            {
                digit = "0x";
                digit += line.charAt(i);
                ++i;
                digit += line.charAt(i);
                rec_nums[rec_nums_itr] = Integer.decode(digit).byteValue();
                ++rec_nums_itr;
            }
            length = (short) (0xFF & (int)rec_nums[0]);
            address = (short) ((0xFF & (int)rec_nums[1]) * 0x100 + (0xFF & (int)rec_nums[2]));
            rectype = (short)(0xFF & (int)rec_nums[3]);
            if (length != rec_nums_itr - 5)
                return false;

            if (rectype == 2)
            {
                if (length != 2)
                    return false;
                base_i = (short)((((0xFF & (int)rec_nums[4]) * 0x100 + (0xFF & (int)rec_nums[5])) * 16));
                continue;
            }

            if (rectype == 1)
                break;

            if (rectype != 0)
                return false;

            for (int i = 0; i < length; ++i)
            {
                for (;base_i + address + i >= size; ++size)
                {
                    if(size >= deviceInfo.mem_size)
                        return false;
                    m_buffer[size] = (byte) 0xFF;
                }

                if (m_buffer[base_i + address + i] != (byte)0xff)
                    return false;

                m_buffer[base_i + address + i] = rec_nums[i + 4];
            }
        }
        file.close();
        file = null;
        rec_nums = null;
        msg = null;
        digit = null;
        return true;
    }

    private byte[] m_buffer;
    private int size;
    
};

class Page
{
    public int address;
    public List<Integer> data;
};

class HexFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        File file = new File(dir, name);
        return (name.endsWith(".hex") || file.isDirectory());
    }
}