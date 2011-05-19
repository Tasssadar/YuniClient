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
    
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3; 
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5; 
    public static final int CONNECTION_FAILED = 1;
    public static final int CONNECTION_LOST = 2;
    
    public Connection(Handler handler, BluetoothDevice device)
    {
        mHandler = handler;
        mChatService = new BluetoothChatService(mBThandler);
        mChatService.start();
        mChatService.connect(device);
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
    
    private final Handler mBThandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            if((YuniClient.state & YuniClient.STATE_CONNECTED) == 0 && msg.what != MESSAGE_STATE_CHANGE  && msg.what != MESSAGE_TOAST)
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
                        if(((YuniClient.state & YuniClient.STATE_CONTROLS) != 0 || (YuniClient.state & YuniClient.STATE_TERMINAL) != 0) && seq != "")
                        {
                            Message resp = new Message();
                            resp.what = CONNECTION_DATA;
                            resp.arg1 = DATA_TEXT;
                            resp.obj = seq;
                            mHandler.sendMessage(resp);
                        }
                        else if((YuniClient.state & YuniClient.STATE_STOPPING) != 0)
                        {
                            Message resp = new Message();
                            resp.what = CONNECTION_DATA;
                            resp.arg1 = DATA_STOPPED;
                            mHandler.sendMessage(resp);
                        }
                        else if((YuniClient.state & YuniClient.STATE_WAITING_ID) != 0)
                        {
                            DeviceInfo deviceInfo = new DeviceInfo(seq);
                            if(deviceInfo.isSet())
                            {
                                Message resp = new Message();
                                  resp.what = CONNECTION_DATA;
                                   resp.arg1 = DATA_ID_RESPONSE;
                                   resp.obj = deviceInfo;
                                   mHandler.sendMessage(resp);
                                   mem = new memory(deviceInfo);
                                   Thread load = new Thread (new Runnable()
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
                                                {
                                                    Message resp = new Message();
                                                      resp.what = CONNECTION_DATA;
                                                       resp.arg1 = DATA_FLASH;
                                                       resp.arg2 = 0; // 0 = fail
                                                       Bundle bundle = new Bundle();
                                                    bundle.putString("text", "Failed to create pages (" + result + ")");
                                                    resp.setData(bundle);
                                                       mHandler.sendMessage(resp);
                                                    mem = null;
                                                }
                                            }
                                            else
                                            {                                               
                                                Message resp = new Message();
                                                  resp.what = CONNECTION_DATA;
                                                   resp.arg1 = DATA_FLASH;
                                                   resp.arg2 = 0; // 0 = fail
                                                   Bundle bundle = new Bundle();
                                                   bundle.putString("text", "Failed to load hex file (" + result + ")");
                                                resp.setData(bundle);
                                                   mHandler.sendMessage(resp);
                                                mem = null;
                                            }
                                            
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                            mem = null;
                                        }
                                    }
                                });
                                load.start();
                            }
                        }
                        else if((YuniClient.state & YuniClient.STATE_FLASHING) != 0)
                        {
                            Page page = mem.getNextPage();
                            if(page != null)
                            {
                                SendPage(page);
                                mHandler.obtainMessage(CONNECTION_DATA, DATA_FLASH_PAGE, 1).sendToTarget();
                            }
                            else
                            {
                                mHandler.obtainMessage(CONNECTION_DATA, DATA_FLASH_PAGE, 0).sendToTarget();
                                mem = null;
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
    
    private Handler mHandler;
    private BluetoothChatService mChatService;
    private memory mem;
    private File hexFile;
}