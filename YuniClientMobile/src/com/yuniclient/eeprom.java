
package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

class eeprom
{
    public eeprom() { buffer = new byte[512]; }
    public void set(int index, byte val)
    {
        if(YuniClient.eeprom_part == 2)
            index += YuniClient.EEPROM_PART2;
        buffer[index] = val ;
    }
    public void set_nopart(int index, byte val){buffer[index] = val ;}
    
    public byte get(int index)
    {
        if(YuniClient.eeprom_part == 2)
            index += YuniClient.EEPROM_PART2;
        return buffer[index];
    }
    
    public byte[] getRec(int index)
    {
        if(YuniClient.eeprom_part == 2)
            index += YuniClient.EEPROM_PART2;
        byte[] rec = new byte[5];
        for(byte i = 0; i < 5; ++i)
            rec[i] = buffer[index+i];
        return rec;
    }
    public void clear()
    {
        for(short i = 0; i < 255; ++i)
            buffer[(YuniClient.eeprom_part == 2) ? (i + YuniClient.EEPROM_PART2) : i] = 0;
    }
    public void clear_all()
    {
        for(short i = 0; i < 512; ++i)
            buffer[i] = 0;
    }
    public short getTotalRecCount()
    {
        short count = 0;
        for(short i = 0; i < 510; i+=5)
        {
            if(buffer[i] != 0 && buffer[i+1] != 0)
                ++count; 
        }
        return count;
    }
    public short getPartRecCount(boolean firstPart)
    {
        short count = 0;
        final short limit = (short) ((firstPart) ? 255 : 510);
        for(short i = (short) (firstPart ? 0 : 255); i < limit; i+=5)
        {
            if(buffer[i] != 0 && buffer[i+1] != 0)
                ++count; 
        }
        return count;
    }
    
    public void toFile(String name, Handler handler) throws IOException
    {
        if(!name.contains(".dta"))
            name += ".dta";
        File file = new File("/mnt/sdcard/YuniData/"+name);
        if(!file.exists())
            file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write(buffer);
        out.close();
        Message msg = new Message();
        msg.what = YuniClient.MESSAGE_TOAST;
        Bundle bundle = new Bundle();
        bundle.putString(YuniClient.TOAST, "File " + file.getName() + " saved");
        msg.arg1 = 0;
        msg.setData(bundle);
        handler.sendMessage(msg); 
    }
    
    public void fromFile(File file, Handler handler) throws IOException
    {
        FileInputStream in = new FileInputStream(file);
        in.read(buffer);
        in.close();
        Message msg = new Message();
        msg.what = YuniClient.MESSAGE_TOAST;
        Bundle bundle = new Bundle();
        bundle.putString(YuniClient.TOAST, "File " + file.getName() + " loaded");
        msg.arg1 = YuniClient.FILE_LOADED;
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
    
    public void erase(int index)
    {
        if(YuniClient.eeprom_part == 2)
            index += YuniClient.EEPROM_PART2;
        byte[] tmp =  new byte[512];
        short y = 0;
        for(short i = 0; i < 512;++i)
        {
            if(i >= index && i < index+5)
                continue;
            tmp[y] = buffer[i];
            ++y;
        }
        buffer = tmp.clone();
    }
    
    public void insert(int index)
    {
        if(YuniClient.eeprom_part == 2)
            index += YuniClient.EEPROM_PART2;
        byte[] tmp =  new byte[512];
        short y = 0;
        for(short i = 0; i < 512;++i)
        {
            if(i >= index && i < index+5)
            {
                tmp[i] = 0;
                continue;
            }
            tmp[i] = buffer[y];
            ++y;
        }
        buffer = tmp.clone();
    }
    
    private byte[] buffer;
}
