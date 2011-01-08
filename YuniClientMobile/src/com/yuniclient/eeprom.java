
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
	public void set(int index, byte val) { buffer[index] = val ;}
	public byte get(int index) { return buffer[index]; }
	public void clear() { for(short i = 0; i < 512; ++i) buffer[i] = 0; }
	
	public void toFile(String name, Handler handler) throws IOException
	{
		File file = new File("/mnt/sdcard/YuniData/"+name);
		if(!file.exists())
			file.createNewFile();
		FileOutputStream out = new FileOutputStream(file);
		out.write(buffer);
		out.close();
		Message msg = new Message();
		msg.what = YuniClient.MESSAGE_TOAST;
        Bundle bundle = new Bundle();
        bundle.putString(YuniClient.TOAST, "File saved");
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
        bundle.putString(YuniClient.TOAST, "File loaded");
        msg.arg1 = YuniClient.FILE_LOADED;
        msg.setData(bundle);
        handler.sendMessage(msg);
	}
	
	public void erase(int index)
	{
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