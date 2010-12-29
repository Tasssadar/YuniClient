package com.yuniclient;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class YuniClient extends Activity {
	private static final int REQUEST_ENABLE_BT = 2;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3; 
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5; 
    public static final int CONNECTION_FAILED = 1;
    public static final int CONNECTION_LOST = 2;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    
    public DeviceInfo deviceInfo = null;

	BluetoothAdapter mBluetoothAdapter = null;
	private ArrayAdapter<String> mArrayAdapter;
	private ArrayAdapter<String> mPairedDevices;
	
	private BluetoothChatService mChatService = null;
	private View.OnTouchListener keyTouch;	
	public DialogInterface.OnClickListener fileSelect;
	
	public static final byte STATE_CONNECTED = 0x01;
	public static final byte STATE_CONTROLS  = 0x02;
	public static final byte STATE_STOPPING  = 0x04;
	public static final byte STATE_STOPPED   = 0x08;
	public static final byte STATE_WAITING_ID= 0x10;
	public static final byte STATE_FLASHING  = 0x20;
    
	public short state;
	public List<Page> pages;
	public int pagesItr = 0;
	
	public ProgressDialog dialog;
	public File curFolder = null; 
	public Context context = null;
	
	final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        state = 0;
        pages = Collections.checkedList(new ArrayList<Page>(), Page.class);
        setContentView(R.layout.device_list);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
        	if (!mBluetoothAdapter.isEnabled()) {
        	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        	}
        }
        init();
    }
    public void onDestroy() 
    {
    	super.onDestroy();
    	Disconnect(false);
    	unregisterReceiver(mReceiver);
    }
    
    public void Disconnect(boolean resetUI)
    {
    	state = 0;
    	curFolder = null;
    	if(mChatService != null)
    		mChatService.stop();
    	mChatService = null;
    	mArrayAdapter = null;
    	mPairedDevices = null;
    	curFolder = null;
    	keyTouch = null;
    	fileSelect = null;
    	dialog = null;
    	deviceInfo = null;
    	if(resetUI)
    	{
    		setContentView(R.layout.device_list);
    		init();
    	}
    	else
    	{
    		pages = null;
    		context = null;
    		mBluetoothAdapter = null;		
    	}
    }
    public void init()
    {
    	mPairedDevices = new ArrayAdapter<String>(this, R.layout.device_name);
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevices);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); 
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
            	mPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }
        
        final Button button = (Button) findViewById(R.id.button_scan);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter != null) {
                	FindDevices();
                } 
            }
        });
        keyTouch = new View.OnTouchListener() {
       	 public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP)
                {
               	 boolean down = event.getAction() == MotionEvent.ACTION_DOWN;
               	 if(((Button)v).getId() == R.id.LeftForw_b)
               	 {
               		ButtonTouched("W", down);
               		ButtonTouched("A", down);
               	 }
               	 else if(((Button)v).getId() == R.id.RightForw_b)
               	 {
                		ButtonTouched("W", down);
                		ButtonTouched("D", down);
                 }
               	 else if(((Button)v).getId() == R.id.Space_b)
               			ButtonTouched(" ", down);
               	 else
               		 ButtonTouched(((Button)v).getText(), down);
                }
				return false;
       	 }
        };
    }
    
    public void EnableConnect(boolean enable)
    {
    	if(!enable)
    	{
    		dialog= new ProgressDialog(this);
			dialog.setCancelable(true);
		    dialog.setMessage("Connecting...");
		    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);	
		    dialog.setMax(0);
			dialog.setProgress(0);
			dialog.setOnCancelListener(new Dialog.OnCancelListener()
			{
				public void onCancel(DialogInterface dia)
				{
					EnableConnect(true);
				}
			});
			dialog.show();
    	}
    	else
	    	dialog.dismiss();
    	Button button = (Button) findViewById(R.id.button_scan);
        button.setEnabled(enable);
        ListView listView = (ListView) findViewById(R.id.new_devices);
        listView.setEnabled(enable);
        listView = (ListView) findViewById(R.id.paired_devices);
        listView.setEnabled(enable);
    }

    public void FindDevices()
    {
    	if (mBluetoothAdapter.isDiscovering())
    		mBluetoothAdapter.cancelDiscovery();
    	
    	mArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
    	mPairedDevices = new ArrayAdapter<String>(this, R.layout.device_name);
    	
    	ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
    	newDevicesListView.setAdapter(mArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevices);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); 
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
            	mPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }
        
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
    	mBluetoothAdapter.startDiscovery();
    }
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
        	EnableConnect(false);
        	
            // Cancel discovery because it's costly and we're about to connect
        	mBluetoothAdapter.cancelDiscovery();
        	if(mChatService != null)
        		mChatService.stop();
        	mChatService = new BluetoothChatService(this, mHandler);
            if(mChatService.getState() == BluetoothChatService.STATE_CONNECTING)
            	return;
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mChatService.start();
            if(device != null)
            	mChatService.connect(device);
        }
    }; 
    
    public void InitControls()
    {
    	state |= STATE_CONTROLS;
    	setContentView(R.layout.controls);
   	 	Button button = (Button) findViewById(R.id.Forward_b);
        button.setOnTouchListener(keyTouch); 
       
        button = (Button) findViewById(R.id.Backward_b);
        button.setOnTouchListener(keyTouch); 
        button = (Button) findViewById(R.id.Left_b);
        button.setOnTouchListener(keyTouch); 
        button = (Button) findViewById(R.id.Right_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.LeftForw_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.RightForw_b);
        button.setOnTouchListener(keyTouch);
        
        button = (Button) findViewById(R.id.Speed1_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.Speed2_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.Speed3_b);
        button.setOnTouchListener(keyTouch);
        
        button = (Button) findViewById(R.id.Space_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.Record_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.Play_b);
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.Regulator_b);
        button.setOnTouchListener(keyTouch);
        
        button = (Button) findViewById(R.id.Return_b);
        button.setOnClickListener(new View.OnClickListener() {
         	 public void onClick(View v) {
         		InitMain();
         	 }
          });
        
        button.setOnTouchListener(keyTouch);
        button = (Button) findViewById(R.id.Clear_b);
        button.setOnClickListener(new View.OnClickListener() {
          	 public void onClick(View v) {
          		TextView out = (TextView) findViewById(R.id.output);
          		out.setText("");
          	 }
           });
        
    }
    public final Handler fileClick = new Handler() {
    	@Override
        public void handleMessage(Message msg) {
    		File file = (File)msg.obj;
    		if(!file.isDirectory())
	        {
	        	TextView error = (TextView)findViewById(R.id.hex_file);
	        	error.setText(file.getAbsolutePath());
	        }
	        else
	        {
	        	curFolder = file;
	        	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    		 	FilenameFilter filter = new HexFilter();
    		 	final CharSequence[] items = curFolder.list(filter); 		 	
    	        builder.setTitle("Chose file");
    	        builder.setItems(items, fileSelect);
    	        AlertDialog alert = builder.create();
    	        alert.show();
	        }
        }
    };
    void InitMain()
    {
    	state &= ~(STATE_CONTROLS);
    	context = this;
    	curFolder = new File("/mnt/sdcard/");
    	fileSelect = new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	dialog.dismiss();
    	    	FilenameFilter filter = new HexFilter();
    	    	final CharSequence[] items = curFolder.list(filter);
    	        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
    	        File file = new File(curFolder, items[item].toString());
    	        Message msg = new Message();
    	        msg.obj = file;
    	        fileClick.sendMessage(msg);
    	    }
    	};
    	setContentView(R.layout.main);
    	Button button = (Button) findViewById(R.id.Disconnect_b);
        button.setOnClickListener(new View.OnClickListener() {
          	 public void onClick(View v) {
          		Disconnect(true);
          	 }
        });
        
        button = (Button) findViewById(R.id.Controls_b);
        button.setOnClickListener(new View.OnClickListener() {
          	 public void onClick(View v) {
          		if((state & STATE_STOPPED) == 0)
          			InitControls();
          	 }
        });
        button = (Button) findViewById(R.id.Start_b);
        button.setOnClickListener(new View.OnClickListener() {
          	 public void onClick(View v) {
          		byte[] out = { 0x11 };
          		mChatService.write(out.clone());
          		state &= ~(STATE_STOPPED);
          		Button controls = (Button) findViewById(R.id.Controls_b);
          		controls.setEnabled(true);
          		controls.setClickable(true);
          		((Button)v).setEnabled(false);
          		((Button)v).setClickable(false);
          		controls = (Button) findViewById(R.id.Stop_b);
          		controls.setEnabled(true);
          		controls.setClickable(true);
          		controls = (Button) findViewById(R.id.Flash_b);
          		controls.setEnabled(false);
          		controls.setClickable(false);
          	 }
        });
        button = (Button) findViewById(R.id.Stop_b);
        button.setOnClickListener(new View.OnClickListener() {
          	 public void onClick(View v) {
          		byte[] out = { 0x74, 0x7E, 0x7A, 0x33 };
          		mChatService.write(out.clone());
          		try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				state |= STATE_STOPPING;
          		mChatService.write(out.clone());
          		TextView error = (TextView)findViewById(R.id.error);
          		error.setText("Stopping...");
          		Button button2 = (Button) findViewById(R.id.Controls_b);
          		button2.setEnabled(false);
          		button2.setClickable(false);
          		((Button)v).setEnabled(false);
          		((Button)v).setClickable(false);
          		button2 = (Button) findViewById(R.id.Start_b);
          		button2.setEnabled(true);
          		button2.setClickable(true);
          		button2 = (Button) findViewById(R.id.Flash_b);
          		button2.setEnabled(true);
          		button2.setClickable(true);
          	 }
        });
        button = (Button) findViewById(R.id.Flash_b);
        button.setOnClickListener(new View.OnClickListener() {
          	 public void onClick(View v) {
          		TextView error = (TextView)findViewById(R.id.hex_file);
          		File hex = new File(error.getText().toString());
          		error = (TextView)findViewById(R.id.error);
          		if(hex.exists() && hex.canRead())
          		{
          			error.setText("Hex file exists\n");
          			byte[] out = { 0x12 };
               		mChatService.write(out.clone());
               		state |= STATE_WAITING_ID;
               		error.append("Waiting for ID and preparing hex file...");
          		}
          		else
          			error.setText("Hex file does not exists or can not be read\n");
          	 }
        });

        button = (Button) findViewById(R.id.List_b);
        button.setOnClickListener(new View.OnClickListener() {
        	 public void onClick(View v) {
        		 AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        		    curFolder = new File("/mnt/sdcard/");
        		 	FilenameFilter filter = new HexFilter();
        		 	final CharSequence[] items = curFolder.list(filter);
        		 	        		 	
        	        builder.setTitle("Chose file");
        	        builder.setItems(items, fileSelect);
        	        AlertDialog alert = builder.create();
        	        alert.show();
        	 }
        });
        
    }
	
    public final Handler progressHandler2 = new Handler() {
    	@Override
        public void handleMessage(Message msg) {
    		if(msg.arg1 != 0)
    			dialog.setProgress(msg.arg1);
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
        	if((state & STATE_CONNECTED) == 0 && msg.what != MESSAGE_STATE_CHANGE  && msg.what != MESSAGE_TOAST)
        		return;
        	
            switch (msg.what)
            {
	            case MESSAGE_STATE_CHANGE:
	                switch (msg.arg1) {
	                case BluetoothChatService.STATE_CONNECTED:
	                    dialog.dismiss();
	                	InitMain();
	                    state |= STATE_CONNECTED;
	                    break;
	                }
	            case MESSAGE_TOAST:
	            	String text = msg.getData().getString(TOAST);
	            	if(text == null)
	            		break;
	            	Toast.makeText(context, text,
	                        Toast.LENGTH_SHORT).show();
	            	if(msg.arg1 == CONNECTION_LOST)
	            		Disconnect(true);
	            	else if(msg.arg1 == CONNECTION_FAILED)
	            		EnableConnect(true);
	            	break;
	            case MESSAGE_READ:
	            	if(msg.obj != null)
	            	{
	            		byte[] buffer = (byte[])msg.obj;
	            		String seq = "";
	            		for(int y = 0; y < msg.arg1; ++y)
	            			seq += (char)buffer[y];
	            		if((state & STATE_CONTROLS) != 0)
	            		{
	            			
		            		TextView out = (TextView) findViewById(R.id.output);
		            		if(seq != "")
		            		{
			            		
			            		out.setText(out.getText() + seq);
			            		ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
			            		scroll.scrollTo(0, out.getHeight());
		            		}
	            		}
	            		else if((state & STATE_STOPPING) != 0)
	            		{
	            			state &= ~(STATE_STOPPING);
	            			TextView error = (TextView)findViewById(R.id.error);
	                  		error.setText("");
	                  		state |= STATE_STOPPED;
	            		}
	            		else if((state & STATE_WAITING_ID) != 0)
	            		{
	            			state &= ~(STATE_WAITING_ID);
	            			deviceInfo = new DeviceInfo(seq);
	            			if(deviceInfo.isSet())
	            			{
	            				TextView file = (TextView)findViewById(R.id.hex_file);
	            				File hex = new File(file.getText().toString());
	            				dialog.dismiss();
	            				dialog= new ProgressDialog(context);
	            				dialog.setProgress(0);
	            				dialog.setMax((int)(hex.length()/1024));
								dialog.setProgress(0);
								dialog.setCancelable(false);
							    dialog.setMessage("Loading file...");
							    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
								dialog.show();
	            				Thread load = new Thread (new Runnable()
	            				{
	            					public void run()
	         			            {
	            						TextView file = (TextView)findViewById(R.id.hex_file);
									    File hex = new File(file.getText().toString());
									    memory mem = new memory();
									    try
									    {
									    	if(mem.Load(hex, progressHandler2))
									    	{
									    		Message msg = new Message();
									    		msg.obj = mem;
									    		pagesHandler.sendMessage(msg);
									    		if(!CreatePages(mem))
									    			return;
									    		flashHandler.sendMessage(flashHandler.obtainMessage());
									    	}
									    } catch (IOException e) {
									    	// TODO Auto-generated catch block
									    	e.printStackTrace();
									    }
	         			            }
	            				});
	            				load.start();
	            			}
	            		}
	            		else if((state & STATE_FLASHING) != 0)
	            		{
	            			SendPage(pages.get(pagesItr));
							++pagesItr;
							if(pagesItr >= pages.size())
							{
								state &= ~(STATE_FLASHING);
								TextView error = (TextView)findViewById(R.id.error);
								error.setText("Flashing done");
								dialog.dismiss();
							}
	            		}
	            	}
	            	break;
            }
        }
    };

    Handler progressHandler = new Handler() {
        public void handleMessage(Message msg) {
        	if(dialog.isShowing())
        		dialog.incrementProgressBy(1);
        }
    };
    
    Handler pagesHandler = new Handler() {
        public void handleMessage(Message msg) {
        	memory mem = (memory)msg.obj;
        	dialog.dismiss();
        	dialog= new ProgressDialog(context);
        	dialog.setCancelable(false);
	        dialog.setMessage("Creating pages...");
	        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        dialog.setMax((mem.size()/deviceInfo.page_size)+1);
	        dialog.setProgress(0);
	        dialog.show();
        }
    };
    Handler flashHandler = new Handler() {
        public void handleMessage(Message msg) {
        	dialog.dismiss();
        	dialog= new ProgressDialog(context);
        	dialog.setCancelable(false);
	        dialog.setMessage("Flashing into device...");
	        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        dialog.setMax(pages.size());
	        dialog.setProgress(0);
	        dialog.show();
			pagesItr = 0;
			SendPage(pages.get(pagesItr));
			++pagesItr;
			state |= STATE_FLASHING;
        }
    };
    public void SendPage(Page page)
    {	
    	byte[] out = { 0x10 };
  		mChatService.write(out.clone());
        // second value must be 128 or 0, wish c# would allow overflow...
        int adress_sec = page.address;
        while (adress_sec > 255)
            adress_sec -= 256;

        byte[] adress = { (byte)(page.address >> 8), (byte)(adress_sec) };
        mChatService.write(adress);
        byte[] data = new byte[page.data.size()];
        for (int i = 0; i < page.data.size(); ++i)
            data[i] = (byte)(page.data.get(i).shortValue());

        mChatService.write(data);
        TextView error = (TextView)findViewById(R.id.error);
        float percent = (float)(pagesItr +1) / ((float)pages.size() / 100);
        error.setText("Flashing " + (int)percent + "%...");
        if(dialog.isShowing())
        	dialog.setProgress(pagesItr+1);
    }
    public void ButtonTouched(CharSequence button, boolean down)
    {
    	byte[] out = new byte[2];
    	if(button.length() == 1)
    		out[0] = (byte)button.charAt(0);

        if(down)
        	out[1] = (byte)'d';
        else
        	out[1] = (byte)'u';
        mChatService.write(out);
    }
    
    public boolean CreatePages(memory mem)
    {
    	pages.clear();
    	TextView error = (TextView)findViewById(R.id.error);
    	if (mem.size() > deviceInfo.mem_size)
            for (int a = deviceInfo.mem_size; a < mem.size(); ++a)
                if (mem.Get(a) != 0xff)
                {
                	error.setText(error.getText() + "Failed, program is too big!\n");
                    return false;
                }
    	int alt_entry_page = deviceInfo.patch_pos / deviceInfo.page_size;
        boolean add_alt_page = deviceInfo.patch_pos != 0;

        int i = 0;
        Page cur_page = new Page();
        for (boolean generate = true; generate && i < deviceInfo.mem_size / deviceInfo.page_size; ++i)
        {
            cur_page = new Page();
            cur_page.data = Collections.checkedList(new ArrayList<Integer>(), Integer.class);
            cur_page.address = i * deviceInfo.page_size;
            if (mem.size() <= (i + 1) * deviceInfo.page_size)
            {
                for (int y = 0; y < deviceInfo.page_size; ++y)
                {
                    if (i * deviceInfo.page_size + y < mem.size())
                        cur_page.data.add(mem.Get(i * deviceInfo.page_size + y));
                    else
                        cur_page.data.add(0xff);
                }
                generate = false;
            }
            else
            {
                for (int y = i * deviceInfo.page_size; y < (i + 1) * deviceInfo.page_size; ++y)
                {
                    cur_page.data.add(mem.Get(y));
                }
            }

            if (!patch_page(mem, cur_page, deviceInfo.patch_pos, deviceInfo.mem_size))
                return false; 
            pages.add(cur_page);

            if (i == alt_entry_page)
                add_alt_page = false;
            progressHandler.sendMessage(progressHandler.obtainMessage());
        }
        if (add_alt_page)
        {
            for (int y = 0; y < deviceInfo.page_size; ++y)
                cur_page.data.set(y, 0xff);
            cur_page.address = alt_entry_page * deviceInfo.page_size;
            patch_page(mem, cur_page, deviceInfo.patch_pos, deviceInfo.mem_size);
            pages.add(cur_page);
        }
    	return true;
    }
    private boolean patch_page(memory mem, Page page, int patch_pos, int boot_reset)
    {
        if (patch_pos == 0)
            return true;
        
        if (page.address == 0)
        {
            int entrypt_jmp = (boot_reset / 2 - 1) | 0xc000;
            if((entrypt_jmp & 0xf000) != 0xc000)
                return false;
            page.data.set(0, entrypt_jmp);
            page.data.set(1, (entrypt_jmp >> 8));
            return true;
        }

        if (page.address > patch_pos || page.address + page.data.size() <= patch_pos)
            return true;

        int new_patch_pos = patch_pos - page.address;

        if (page.data.get(new_patch_pos) != 0xff || page.data.get(new_patch_pos + 1) != 0xff)
            return false;

        int entrypt_jmp2 = mem.Get(0) | (mem.Get(1) << 8);
        if ((entrypt_jmp2 & 0xf000) != 0xc000)
            return false;

        int entry_addr = (entrypt_jmp2 & 0x0fff) + 1;
        entrypt_jmp2 = ((entry_addr - patch_pos / 2 - 1) & 0xfff) | 0xc000;
        page.data.set(new_patch_pos, entrypt_jmp2);
        page.data.set(new_patch_pos + 1, (entrypt_jmp2 >> 8));
        return true;
    }

}