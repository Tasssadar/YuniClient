package com.yuniclient;

import java.io.File;
import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

class Connection
{
    public static final byte CONNECTION_STATE = 1;
    public static final byte CONNECTION_DATA  = 2;
    
    public static final byte DATA_TEXT        = 1;
    public static final byte DATA_STOPPED     = 2;
    public static final byte DATA_ID_RESPONSE = 3;
    public static final byte DATA_FLASH       = 4;
    public static final byte DATA_FLASH_PAGE  = 5;
    public static final byte DATA_EEPROM_READ = 6;
    
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3; 
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5; 
    public static final int CONNECTION_FAILED = 1;
    public static final int CONNECTION_LOST = 2;
    
    private Connection(Handler handler, BluetoothDevice device)
    {
        mHandler = handler;
        mChatService = new BluetoothChatService(mBThandler);
        mChatService.start();
        mChatService.connect(device);
    }
    
    public static Connection InitInstance(Handler handler, BluetoothDevice device)
    {
        instance = new Connection(handler, device);
        return instance;
    }
    
    public static Connection GetInst()
    {
        return instance;
    }
    
    public static void Destroy()
    {
        instance = null;
    }
    
    public void cancel()
    {
        mChatService.stop();
    }
    
    public void write(byte[] data)
    {
        mChatService.write(data);
    }
    
    public void setHexFile(File file) { hexFile = file; }
    
    private void SendPage(Page page)
    {
        final byte[] out = { 0x10 };
        mChatService.write(out);

        final byte[] adress = { (byte)(page.address >> 8), (byte)(page.address) };
        mChatService.write(adress);
        mChatService.write(page.data);
    }
    
    public void StartEEPROMRead(EEPROM eeprom)
    {
        m_eeprom = eeprom;
        eeprom.setReadAddress(0);
        final byte[] out = { 0x13, 0, 0, (byte) EEPROM.EEPROM_READ_BLOCK };
        mChatService.write(out);
    }
    
    private boolean SendNextEEPROMRead()
    {
        int address = m_eeprom.getNextReadAddress();
        if(address >= m_eeprom.getSize())
            return false;
        
        final byte[] out = { 0x13, (byte)(address >> 8), (byte)(address), (byte)EEPROM.EEPROM_READ_BLOCK };
        mChatService.write(out);
        return true;
    }
    
    private final Handler mBThandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            int state = YuniClient.getState();
            if((state & YuniClient.STATE_CONNECTED) == 0 && msg.what != MESSAGE_STATE_CHANGE  && msg.what != MESSAGE_TOAST)
                return;
                
            switch (msg.what)
            {
                case MESSAGE_STATE_CHANGE:
                    if(msg.arg1 == BluetoothChatService.STATE_CONNECTED)
                    {
                        //TODO
                        //ProtocolMgr.getInstance().ScanForProtocols();
                        mHandler.obtainMessage(CONNECTION_STATE, BluetoothChatService.STATE_CONNECTED, -1).sendToTarget();
                    }
                    break;
                case MESSAGE_TOAST:
                    if(msg.arg1 == CONNECTION_LOST)
                        mHandler.obtainMessage(CONNECTION_STATE, CONNECTION_LOST, -1).sendToTarget();
                    else if(msg.arg1 == CONNECTION_FAILED)
                        mHandler.obtainMessage(CONNECTION_STATE, CONNECTION_FAILED, -1).sendToTarget();
                    break;
                case MESSAGE_READ:
                {
                    if(msg.obj != null)
                    {
                        final byte[] buffer = (byte[])msg.obj;
                        String seq = "";
                        for(int i = 0; i < msg.arg1; ++i)
                            seq += (char)buffer[i];
                        if(((state & YuniClient.STATE_CONTROLS) != 0 || (state & YuniClient.STATE_TERMINAL) != 0) && seq != "")
                        {
                            Message resp = new Message();
                            resp.what = CONNECTION_DATA;
                            resp.arg1 = DATA_TEXT;
                            resp.obj = seq;
                            mHandler.sendMessage(resp);
                        }
                        else if((state & YuniClient.STATE_STOPPING) != 0)
                        {
                            Message resp = new Message();
                            resp.what = CONNECTION_DATA;
                            resp.arg1 = DATA_STOPPED;
                            mHandler.sendMessage(resp);
                        }
                        else if((state & YuniClient.STATE_WAITING_ID) != 0)
                        {
                            DeviceInfo deviceInfo = new DeviceInfo(seq);
                            if(deviceInfo.isSet())
                            {
                                Message resp = new Message();
                                resp.what = CONNECTION_DATA;
                                resp.arg1 = DATA_ID_RESPONSE;
                                resp.obj = deviceInfo;
                                mHandler.sendMessage(resp);
                                if((state & YuniClient.STATE_EEPROM_READ) == 0)
                                {
                                    mem = new memory(deviceInfo);
                                    HexLoadThread load = new HexLoadThread();
                                    load.start();
                                }
                            }
                        }
                        else if((state & YuniClient.STATE_FLASHING) != 0)
                        {
                            Page page = mem.getNextPage();
                            if(page != null)  SendPage(page);
                            else              mem = null;
                            mHandler.obtainMessage(CONNECTION_DATA, DATA_FLASH_PAGE, page != null ? 1 : 0).sendToTarget();
                        }
                        else if((state & YuniClient.STATE_EEPROM_READ) != 0)
                        {
                            if(m_eeprom.addData(buffer, (short) msg.arg1)%EEPROM.EEPROM_READ_BLOCK == 0)
                            {
                                boolean doNext = SendNextEEPROMRead();
                                mHandler.obtainMessage(CONNECTION_DATA, DATA_EEPROM_READ, doNext ? 1 : 0).sendToTarget();
                            }
                        }
                        else if(seq != "")
                        {
                            Message resp = new Message();
                            resp.what = CONNECTION_DATA;
                            resp.arg1 = DATA_TEXT;
                            resp.obj = seq;
                            mHandler.sendMessage(resp);
                        }
                     }
                     break;
                }
            }
        }
    };
    
    private class HexLoadThread extends Thread
    {
        public void run()
        {
            try
            {
                String result = mem.Load(hexFile);
                if(result == null)
                {
                    result = mem.CreatePages();
                    if(result == null)
                    {
                        Message resp = new Message();
                        resp.what = CONNECTION_DATA;
                        resp.arg1 = DATA_FLASH;
                        resp.arg2 = 1; // 1 = ok
                        Bundle bundle = new Bundle();
                        bundle.putInt("pagesCount", mem.pagesCount());
                        resp.setData(bundle);
                        mHandler.sendMessage(resp);
                        SendPage(mem.getNextPage());
                    }
                    else
                        SendFailedMsg("Failed to create pages (" + result + ")");
                }
                else
                    SendFailedMsg("Failed to load hex file (" + result + ")");
            }
            catch (IOException e)
            {
                SendFailedMsg("Failed to load hex file (" + e.getMessage() + ")");
            }
            hexFile = null;
        }
    }

    private void SendFailedMsg(String text)
    {
        Message resp = new Message();
        resp.what = CONNECTION_DATA;
        resp.arg1 = DATA_FLASH;
        resp.arg2 = 0; // 0 = fail
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        resp.setData(bundle);
        mHandler.sendMessage(resp);
        mem = null;
    }

    public static Connection instance;
    
    private Handler mHandler;
    private BluetoothChatService mChatService;
    private memory mem;
    private File hexFile;
    private EEPROM m_eeprom;
}