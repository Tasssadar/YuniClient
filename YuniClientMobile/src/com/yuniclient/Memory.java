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
	public int Get(int index) { return m_buffer.get(index); }
	public int size() { return m_buffer.size(); }
    public int data() { if(m_buffer.size() == 0) return 0; else return m_buffer.get(0); }
    public boolean Load(File filePath, Handler handler) throws IOException
    {
    	m_buffer = Collections.checkedList(new ArrayList<Integer>(), Integer.class);
        m_buffer.clear();
        int base_i = 0;
        List<Integer> rec_nums = Collections.checkedList(new ArrayList<Integer>(), Integer.class);
        
        FileInputStream file = new FileInputStream(filePath);//openFileOutput(filePath.getAbsoluteFile().toString(), Context.MODE_PRIVATE);
        long pos = 0;
        long lastSendPos = 0;
        Message msg;
        for (int lineno = 1; ; ++lineno)
        {
        	if(pos - lastSendPos > 1024)
        	{
        		msg = new Message();
        		msg.arg1 = (int)(pos/1024);
        		handler.sendMessage(msg);
        		lastSendPos = pos;
        	}
            if (pos >= filePath.length())
                break;
            char c = (char)file.read();
            ++pos;
            String line = "";
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
            rec_nums.clear();
            for (int i = 1; i + 1 < line.length(); ++i)
            {
            	String digit = "0x";
                digit += line.charAt(i);
                ++i;
                digit += line.charAt(i);
                int value = Integer.decode(digit);
                value = (value & 0xFF);
                rec_nums.add(Integer.decode(digit));
                // Form1.ActiveForm.Controls.Find("textBox1", true)[0].Text += "d"+digit +" " + res + "\r\n";
            }
            int length = rec_nums.get(0).intValue();
            int address = rec_nums.get(1).intValue() * 0x100 + rec_nums.get(2).intValue();
            int rectype = rec_nums.get(3).intValue();
            if (length != rec_nums.size() - 5)
                return false;

            if (rectype == 2)
            {
                if (length != 2)
                    return false;
                base_i = (rec_nums.get(4).intValue() * 0x100 + rec_nums.get(5).intValue()) * 16;
                continue;
            }

            if (rectype == 1)
                break;

            if (rectype != 0)
                return false;

            for (int i = 0; i < length; ++i)
            {
                while (base_i + address + i >= m_buffer.size())
                    m_buffer.add(0xff);

                if (m_buffer.get(base_i + address + i) != 0xff)
                    return false;

                m_buffer.set(base_i + address + i, rec_nums.get(i + 4));
            }
        }
        file.close();
        return true;
    }

	public List<Integer> m_buffer;
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