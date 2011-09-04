package com.yuniclient;

import com.yuni.client.R;

import android.content.Context;
import android.widget.ArrayAdapter;

public class EEPROM
{
    public static final short EEPROM_READ_BLOCK = 128; 
    public EEPROM(DeviceInfo info)
    {
        m_info = info;
        m_data = new byte[m_info.eeprom_size];
    }
    
    public int getReadAddress() { return readAddress; }
    public void setReadAddress(int addr) { readAddress = addr; readItr = (short) addr; };
    public int getNextReadAddress() { readAddress += EEPROM_READ_BLOCK; return readAddress; };
    public int getSize() { return m_info.eeprom_size; }
    
    public short addData(byte[] data, short len)
    {
        for(short i = 0; i < len; ++i,++readItr)
            m_data[readItr] = data[i]; 
        return readItr;
    }
    
    public ArrayAdapter<String> CreateOrGetAdapter(Context context)
    {
        if(m_listAdapter == null)
            m_listAdapter = new ArrayAdapter<String>(context, R.layout.eeprom_entry);
        return m_listAdapter;
    }
    
    public void FillAdapter()
    {
        int size = m_data.length;
        for(int i = 0; i < size; ++i)
            m_listAdapter.add(YuniClient.numToHex(i, (byte) 4) + ": " + YuniClient.numToHex((m_data[i] & 0xFF), (byte) 2));
    }
    
    ArrayAdapter<String> m_listAdapter;
    private DeviceInfo m_info;
    private int readAddress;
    private byte[] m_data;
    private short readItr;
};