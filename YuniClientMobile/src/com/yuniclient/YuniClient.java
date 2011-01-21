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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    public static final int FILE_LOADED = 3;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    
    public DeviceInfo deviceInfo = null;

    BluetoothAdapter mBluetoothAdapter = null;
    private ArrayAdapter<String> mArrayAdapter;
    private ArrayAdapter<String> mPairedDevices;
    private ArrayAdapter<String> mEEPROMEntries;
    
    private BluetoothChatService mChatService = null;
    private View.OnTouchListener keyTouch;	
    public DialogInterface.OnClickListener fileSelect;
    
    public static final byte STATE_CONNECTED = 0x01;
    public static final byte STATE_CONTROLS  = 0x02;
    public static final byte STATE_STOPPING  = 0x04;
    public static final byte STATE_STOPPED   = 0x08;
    public static final byte STATE_WAITING_ID= 0x10;
    public static final byte STATE_FLASHING  = 0x20;
    public static final byte STATE_SCROLL    = 0x40;
    public static final short STATE_EEPROM   = 0x80;
    public static final short STATE_EEPROM_READING = 0x100;
    public static final short STATE_EEPROM_EDIT= 0x200;
    public static final short STATE_EEPROM_WRITE= 0x400;
    public static final short STATE_EEPROM_NEW_ADD=0x800;
    public static final byte REC_SIZE = 5;
    
    public static final short EEPROM_PART2=255;
    public static byte eeprom_part = 1;
    public static byte eeprom_write_part = 1;
    
    public int state;
    public List<Page> pages;
    public int pagesItr = 0;
    
    private int itr_buff;
    private eeprom EEPROM = null;
    private int curEditId = 0;
    private AlertDialog alertDialog;
    
    public ProgressDialog dialog;
    public File curFolder = null; 
    public Context context = null;
    
    public Thread autoScrollThread = null;

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
        autoScrollThread = null;
        mEEPROMEntries = null;
        EEPROM = null;
        alertDialog = null;
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
                    if(mChatService != null)
                        mChatService.stop();
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
    private final OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
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
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {	 
        	  if((state & STATE_EEPROM) != 0)
        	  {
        		  AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                  builder2.setMessage("Do you really want to leave EEPROM interface?")
                         .setCancelable(false)
                         .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog, int id) {
                                 if((state & STATE_CONNECTED) != 0)
                                 	InitMain();
                                 else
                                	 Disconnect(true); 
                             }
                         })
                         .setNegativeButton("No", new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog, int id) {
                                  dialog.dismiss();
                             }
                         });
                  AlertDialog alert = builder2.create();
                  alert.show();
        	  }
        	  else if((state & STATE_CONTROLS) != 0)
                  InitMain();
              else if((state & STATE_EEPROM_EDIT) != 0 || (state & STATE_EEPROM_NEW_ADD) != 0)
                  InitEEPROMList();
              else if((state & STATE_CONNECTED) != 0)
                  Disconnect(true);
              else
                  finish();
              return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_MENU)
        {
            if(((state & STATE_EEPROM) != 0  || (state & STATE_CONNECTED) == 0) &&
               (state & STATE_EEPROM_EDIT) == 0 && (state & STATE_EEPROM_NEW_ADD) == 0)
                return super.onKeyDown(keyCode, event);
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        prepareMenu(menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        prepareMenu(menu);
        return true;
    }
    void prepareMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        if((state & STATE_EEPROM) != 0 && menu.findItem(R.id.write) == null)
        {
            menu.clear();
            inflater.inflate(R.menu.menu, menu);
        }
        else if((state & STATE_EEPROM) == 0 && menu.findItem(R.id.offline) == null)
        {
            menu.clear();
            inflater.inflate(R.menu.menu2, menu);
        }
    }
    void ShowAlert(CharSequence text)
    {
    	if(dialog != null)
    		dialog.dismiss();
	    AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
	    builder2.setMessage(text)
	           .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	            	   dialog.dismiss();
	               }
	           });
	    AlertDialog alert = builder2.create();
	    alert.show();
    }
    // INITS
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
    
    void InitMain()
    {
        mEEPROMEntries = null;
        EEPROM = null;
        autoScrollThread = null;
        state &= ~(STATE_CONTROLS);
        state &= ~(STATE_EEPROM);
        context = this;
        curFolder = new File("/mnt/sdcard/hex/");
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
        button = (Button) findViewById(R.id.eeprom_b);
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                InitEEPROMList();
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
                StartStop((Button)v, ((state & STATE_STOPPED) != 0));
             }
        });
        button = (Button) findViewById(R.id.SendSpec_b);
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 AlertDialog.Builder builder;
                 alertDialog = null;
                 LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                 View layout = inflater.inflate(R.layout.save_data,
                                                (ViewGroup) findViewById(R.id.layout_root));
                 builder = new AlertDialog.Builder(context);
                 builder.setView(layout);
                 builder.setNeutralButton("Send", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface arg0, int arg1) {
                        EditText text = (EditText)alertDialog.findViewById(R.id.data_file_save);
                        byte[] out = new byte[text.getText().length()];
                        for(short i = 0; i < text.getText().length(); ++i)
                            out[i] = (byte) text.getText().charAt(i);
                        mChatService.write(out.clone());
                        Toast.makeText(context, "Text \"" + text.getText().toString() + "\" sent",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                 builder.setTitle("Send text");
                 alertDialog = builder.create();
                 alertDialog.setCancelable(true);
                 alertDialog.show();
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
                    final byte[] out = { 0x12 };
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
                    final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    curFolder = new File("/mnt/sdcard/hex/");
                    FilenameFilter filter = new HexFilter();
                    final CharSequence[] items = curFolder.list(filter);
                                        
                    builder.setTitle("Chose file");
                    builder.setItems(items, fileSelect);
                    final AlertDialog alert = builder.create();
                    alert.show();
             }
        });
        
    }
    void StartStop(Button v, boolean start)
    {
        byte[] out = null;
        if(start)
        {
            state &= ~(STATE_STOPPED);
            out = new byte[1];
            out[0] = 0x11;
            state &= ~(STATE_STOPPED);
            v.setText("Stop");
        }
        else
        {
            out = new byte[4];
            out[0] = 0x74; out[1] = 0x7E; out[2] = 0x7A; out[3] = 0x33;
            mChatService.write(out.clone());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            state |= STATE_STOPPING;
            TextView error = (TextView)findViewById(R.id.error);
            error.setText("Stopping...");
            v.setText("Start");
        }
        v = (Button) findViewById(R.id.Controls_b);
        v.setEnabled(start);
        v.setClickable(start);
        v = (Button) findViewById(R.id.SendSpec_b);
        v.setEnabled(start);
        v.setClickable(start);
        v = (Button) findViewById(R.id.Flash_b);
        v.setEnabled(!start);
        v.setClickable(!start);
        v = (Button) findViewById(R.id.eeprom_b);
        v.setEnabled(start);
        v.setClickable(start);
        mChatService.write(out.clone());
    }
    private final Handler fileClick = new Handler() {
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
        
        button = (Button) findViewById(R.id.Sensors_b);
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                InitMain();
             }
          });

        button = (Button) findViewById(R.id.Clear_b);
        button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                TextView out = (TextView) findViewById(R.id.output);
                out.setText("");
             }
           });
        
        autoScrollThread = new Thread (new Runnable()
        {
            public void run()
            {
                TextView out = (TextView) findViewById(R.id.output);
                ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
                while(true)
                {
                    if((state & STATE_CONTROLS) == 0)
                        break;

                    if((state & STATE_SCROLL) != 0 && scroll.getScrollY() != out.getHeight())
                    {
                        scrollHandler.sendEmptyMessage(0);
                        state &= ~(STATE_SCROLL);
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        autoScrollThread.setPriority(1);
        autoScrollThread.start();
    }
    public final Handler scrollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final TextView out = (TextView) findViewById(R.id.output);
            final ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
            if(scroll == null || out == null)
                return;
            scroll.scrollTo(0, out.getHeight());
        }
    };
    
    private void InitEEPROMList()
    {
        state |= STATE_EEPROM;
        state &= ~(STATE_EEPROM_EDIT);
        state &= ~(STATE_EEPROM_NEW_ADD);
        setContentView(R.layout.eeprom_list);
        if(EEPROM != null && mEEPROMEntries != null)
            readHandler.sendEmptyMessage(0);
        else
        {
        	eeprom_part = 1;
            EEPROM = new eeprom();
            mEEPROMEntries = new ArrayAdapter<String>(this, R.layout.device_name);
            if((state & STATE_CONNECTED) != 0)
                LoadEEPROM();
            else
            	OpenLoadDialog();
        }
        mEEPROMEntries.clear();
        ListView eepromListView = (ListView) findViewById(R.id.eeprom_entries);
        eepromListView.setAdapter(mEEPROMEntries);
        eepromListView.setOnItemClickListener(mEditEntryListener);
        eepromListView.setLongClickable(true);
        eepromListView.setOnItemLongClickListener(mEditEntryLongListener);
    }
    private void LoadEEPROM()
    {
        mEEPROMEntries.clear();
        dialog= new ProgressDialog(this);
        dialog.setMessage("Reading EEPROM...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);	
        dialog.setMax(0);
        dialog.setProgress(0);
        dialog.setCancelable(true);
        dialog.show();
        dialog.setOnCancelListener(new Dialog.OnCancelListener()
        {
            public void onCancel(DialogInterface dia)
            {
                state &= ~(STATE_EEPROM_READING);
            }
        });
        
        state |= STATE_EEPROM_READING;
        itr_buff = 0;
        byte[] out = {0x16};
        mChatService.write(out);
    }
    private final OnItemClickListener mEditEntryListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
        {
            state |= STATE_EEPROM_EDIT;
            state &= ~(STATE_EEPROM);
            setContentView(R.layout.eeprom_edit);
            String info = ((TextView) v).getText().toString();
            curEditId = Integer.valueOf( info.substring(0, info.indexOf(" ")) ).intValue();
            
            EditText edit = (EditText)findViewById(R.id.key_input);
            edit.setText(Character.toString((char)EEPROM.get(curEditId)));
            edit = (EditText)findViewById(R.id.downUp_input);
            edit.setText(Character.toString((char)EEPROM.get(curEditId+1)));
            
            edit = (EditText)findViewById(R.id.event_input);
            edit.setText(Integer.valueOf(EEPROM.get(curEditId+2)).toString());
            
            edit = (EditText)findViewById(R.id.byte1_input);
            edit.setText(Integer.valueOf(0xFF & EEPROM.get(curEditId+3)).toString());
            edit = (EditText)findViewById(R.id.byte2_input);
            edit.setText(Integer.valueOf(0xFF & EEPROM.get(curEditId+4)).toString());
            
            edit = (EditText)findViewById(R.id.big_input);
            int bigNum = ((EEPROM.get(curEditId+3) << 8) | (EEPROM.get(curEditId+4)) & 0xFF);
            edit.setText(Integer.valueOf(bigNum).toString());
            
            Button button = (Button) findViewById(R.id.button_save_eeprom_item);
            button.setOnClickListener(new View.OnClickListener() {
                 public void onClick(View v) {
                    SaveEntry();
                    state &= ~(STATE_EEPROM_EDIT);
                 }
              });
        }
    };
    
    private final OnItemLongClickListener mEditEntryLongListener = new OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                int arg2, long arg3)
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            CharSequence[] items = {"Add new behind this one", "Erase", "Erase all"};
            String info = ((TextView) arg1).getText().toString();
            curEditId = Integer.valueOf( info.substring(0, info.indexOf(" ")) ).intValue();
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item)
                    {
                        case 0:
                            curEditId+=REC_SIZE;
                            state |= STATE_EEPROM_NEW_ADD;
                            state &= ~(STATE_EEPROM);
                            setContentView(R.layout.eeprom_edit);
                            Button button = (Button) findViewById(R.id.button_save_eeprom_item);
                            button.setOnClickListener(new View.OnClickListener() {
                                 public void onClick(View v) {
                                    SaveEntry();
                                    state &= ~(STATE_EEPROM_NEW_ADD);
                                 }
                              });
                            break;
                        case 1:
                            EEPROM.erase(curEditId);
                            mEEPROMEntries.clear();
                            readHandler.sendEmptyMessage(0);
                            break;
                        case 2:
                        	AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                            builder2.setMessage("Do you really want to erase all entries?")
                                   .setCancelable(false)
                                   .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialog, int id) {
                                           mEEPROMEntries.clear();
                                           EEPROM.clear();
                                           dialog.dismiss();
                                       }
                                   })
                                   .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                       }
                                   });
                            AlertDialog alert = builder2.create();
                            alert.show();
                        	break;
                    }
                }
            });
            builder.create().show();
            return true;
        }
    }; 
    private void SaveEntry()
    {
        if((state & STATE_EEPROM_NEW_ADD) != 0)
            EEPROM.insert(curEditId);
        EditText edit = (EditText)findViewById(R.id.key_input);
        EEPROM.set(curEditId, (byte)edit.getText().charAt(0));	
        edit = (EditText)findViewById(R.id.downUp_input);
        EEPROM.set(curEditId+1, (byte)edit.getText().charAt(0));
        
        edit = (EditText)findViewById(R.id.event_input);
        EEPROM.set(curEditId+2, Integer.valueOf(edit.getText().toString()).byteValue());
        switch(EEPROM.get(curEditId+2))
        {
            case 2: // EVENT_SENSOR_LEVEL_HIGHER
            case 3: // EVENT_SENSOR_LEVEL_LOWER
            case 4: // EVENT_RANGE_HIGHER
            case 5: // EVENT_RANGE_LOWER
                edit = (EditText)findViewById(R.id.byte1_input);
                EEPROM.set(curEditId+3, Integer.valueOf(edit.getText().toString()).byteValue());
                edit = (EditText)findViewById(R.id.byte2_input);
                EEPROM.set(curEditId+4, Integer.valueOf(edit.getText().toString()).byteValue());
                break;
            case 1: // EVENT_TIME
            case 6: // EVENT_DISTANCE 
            case 7: // EVENT_DISTANCE_LEFT
            case 8: // EVENT_DISTANCE_RIGHT
            default:
                edit = (EditText)findViewById(R.id.big_input);
                EEPROM.set(curEditId+3, (byte)(Integer.valueOf(edit.getText().toString()).intValue() >> 8));
                EEPROM.set(curEditId+4, (byte)(Integer.valueOf(edit.getText().toString()).intValue() & 0xFF));
                break;
        }
        InitEEPROMList();
    }
    void OpenLoadDialog()
    {
        final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
        curFolder = new File("/mnt/sdcard/YuniData/");
        final CharSequence[] items = curFolder.list();
        builder2.setTitle("Chose file");
        builder2.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                final CharSequence[] items = curFolder.list();
                File file = new File(curFolder, items[item].toString());
                if(!file.isFile())
                    return;
                dialog.dismiss();
                try {
                    EEPROM.fromFile(file, mHandler);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        final AlertDialog alert = builder2.create();
        alert.show();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.New_entry:
                curEditId = mEEPROMEntries.getCount()*REC_SIZE;
                state |= STATE_EEPROM_NEW_ADD;
                state &= ~(STATE_EEPROM);
                setContentView(R.layout.eeprom_edit);
                Button button = (Button) findViewById(R.id.button_save_eeprom_item);
                button.setOnClickListener(new View.OnClickListener() {
                     public void onClick(View v) {
                        SaveEntry();
                        state &= ~(STATE_EEPROM_NEW_ADD);
                     }
                  });
                return true;
            case R.id.switchPart:
            	if(eeprom_part == 1) eeprom_part = 2;
            	else eeprom_part = 1;
            	mEEPROMEntries.clear();
            	readHandler.sendEmptyMessage(0);
            	Toast.makeText(context, "Switched to part " + eeprom_part,
                        Toast.LENGTH_SHORT).show();
            	return true;
            case R.id.reload:
                if((state & STATE_CONNECTED) != 0)
                    LoadEEPROM();
                else
                	ShowAlert("You are in offline mode");
                return true;
            case R.id.write:
                if((state & STATE_CONNECTED) == 0)
                {
                	ShowAlert("You are in offline mode");
                    return true;
                }
                state |= STATE_EEPROM_WRITE;
                eeprom_write_part = eeprom_part;
                eeprom_part = 1;
                dialog= new ProgressDialog(this);
                dialog.setMessage("Erasing EEPROM...");
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);	
                dialog.setMax(0);
                dialog.setProgress(0);
                dialog.setCancelable(false);
                dialog.show();
                itr_buff = 0;
                byte[] out = {0x1C};
                mChatService.write(out);
                curEditId = EEPROM.getTotalRecCount();
                return true;
            case R.id.save:
                AlertDialog.Builder builder;
                alertDialog = null;
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.save_data,
                                               (ViewGroup) findViewById(R.id.layout_root));
                builder = new AlertDialog.Builder(context);
                builder.setView(layout);
                builder.setNeutralButton("Save", saveDataFile);
                builder.setTitle("Chose filename");
                alertDialog = builder.create();
                alertDialog.show();
                return true;
            case R.id.load:
                OpenLoadDialog();
                return true;
            case R.id.offline:
                InitEEPROMList();
                return true;
            case R.id.exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private final OnClickListener saveDataFile = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            EditText text = (EditText)alertDialog.findViewById(R.id.data_file_save);
            String filename = text.getText().toString();
            dialog.dismiss();
            File folder = new File("/mnt/sdcard/YuniData/");
            if(!folder.exists())
                folder.mkdirs();
            try {
                EEPROM.toFile(filename, mHandler);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
       }
    };

    // INITS END
    // HANDLERS
    private void ButtonTouched(CharSequence button, boolean down)
    {
        byte[] out = new byte[2];
        out[0] = (byte)button.charAt(0);

        if(down)
            out[1] = (byte)'d';
        else
            out[1] = (byte)'u';
        mChatService.write(out);
    }
    private final Handler progressHandler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1 != 0)
                dialog.setProgress(msg.arg1);
        }
    };
    
    private final Handler flashHandler = new Handler() {
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
    private final Handler failedHandler = new Handler() {
        public void handleMessage(Message msg) {
        	final String text = msg.getData().getString("text");
        	ShowAlert(text);
            dialog.dismiss();
        }
    };
    
    private final Handler readHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(dialog != null)
                dialog.dismiss();
            state &= ~(STATE_EEPROM_READING);
            
            String item = null;
            for(int itr = 0; itr < 255;)
            {
                if(EEPROM.get(itr) == 0 && EEPROM.get(itr+1) == 0)
                    break;
                item = "";
                item = itr + " Key " + (char)EEPROM.get(itr) + " " + (char)EEPROM.get(itr+1) + "\n";
                switch(EEPROM.get(itr+2))
                {
                    case 0:
                    	item += "NONE";
                    	break;
                    case 1:
                        item += "TIME, " + (((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF)*10) + "ms";
                        break;
                    case 2:
                        item += "SENSOR_LEVEL_HIGHER, sensor " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF & EEPROM.get(itr+4));
                        break;
                    case 3:
                        item += "SENSOR_LEVEL_LOWER, sensor " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF & EEPROM.get(itr+4));
                        break;
                    case 4: 
                        item += "RANGE_HIGHER adr " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF & EEPROM.get(itr+4)) + "cm";
                        break;
                    case 5: 
                        item += "RANGE_LOWER adr " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF &  EEPROM.get(itr+4)) + "cm";
                        break;
                    case 6:
                        item += "EVENT_DISTANCE " + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + " mm";
                        break;
                    case 7:
                        item += "EVENT_DISTANCE_LEFT " + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + " mm";
                        break;
                    case 8:
                        item += "EVENT_DISTANCE_RIGHT " + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + " mm";
                        break;
                    default:
                       item += "event " + EEPROM.get(itr+2) + " values " + (0xFF & EEPROM.get(itr+3)) + " " + (0xFF & EEPROM.get(itr+4)) + 
                           " (" + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + ")";
                }
                itr += 5;
                mEEPROMEntries.add(item);
            }
        }
    };
    private final Handler writeCompleteHandler = new Handler() {
        public void handleMessage(Message msg) {
            dialog.dismiss();
            state &= ~(STATE_EEPROM_WRITE);
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
                    final String text = msg.getData().getString(TOAST);
                    if(text == null)
                        break;
                    Toast.makeText(context, text,
                            Toast.LENGTH_SHORT).show();
                    if(msg.arg1 == CONNECTION_LOST)
                        Disconnect(true);
                    else if(msg.arg1 == CONNECTION_FAILED)
                        EnableConnect(true);
                    else if(msg.arg1 == FILE_LOADED)
                    {
                        mEEPROMEntries.clear();
                        readHandler.sendEmptyMessage(0);
                    }
                    break;
                case MESSAGE_READ:
                    if(msg.obj != null)
                    {
                        final byte[] buffer = (byte[])msg.obj;
                        String seq = "";
                        for(int y = 0; y < msg.arg1; ++y)
                            seq += (char)buffer[y];
                        if((state & STATE_CONTROLS) != 0)
                        {
                            final TextView out = (TextView) findViewById(R.id.output);
                            if(seq != "")
                            {
                                out.setText(out.getText() + seq);
                                state |= STATE_SCROLL;
                            }
                        }
                        else if((state & STATE_STOPPING) != 0)
                        {
                            state &= ~(STATE_STOPPING);
                            final TextView error = (TextView)findViewById(R.id.error);
                            error.setText("");
                            state |= STATE_STOPPED;
                        }
                        else if((state & STATE_WAITING_ID) != 0)
                        {
                            state &= ~(STATE_WAITING_ID);
                            deviceInfo = new DeviceInfo(seq);
                            if(deviceInfo.isSet())
                            {
                                final TextView file = (TextView)findViewById(R.id.hex_file);
                                File hex = new File(file.getText().toString());
                                dialog.dismiss();
                                if(Debug.isDebuggerConnected())
                                {
                                    dialog= new ProgressDialog(context);
                                    dialog.setProgress(0);
                                    dialog.setMax((int)(hex.length()/1024));
                                    dialog.setProgress(0);
                                    dialog.setCancelable(false);
                                    dialog.setMessage("Loading file...");
                                    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    dialog.show();
                                }
                                Thread load = new Thread (new Runnable()
                                {
                                    public void run()
                                    {
                                        final TextView file = (TextView)findViewById(R.id.hex_file);
                                        final File hex = new File(file.getText().toString());
                                        memory mem = new memory();
                                        try
                                        {
                                        	String result = mem.Load(hex, progressHandler2, deviceInfo);
                                            if(result == "")
                                            {
                                            	result = CreatePages(mem);
                                                if(result == "")
                                                    flashHandler.sendMessage(flashHandler.obtainMessage());
                                                else
                                                {
                                                	Message msg = new Message();
                                                	Bundle bundle = new Bundle();
                                                    bundle.putString("text", "Failed to create pages (" + result + ")");
                                                    msg.setData(bundle);
                                                    failedHandler.sendMessage(msg);
                                                }
                                            }
                                            else
                                            {
                                            	Message msg = new Message();
                                            	Bundle bundle = new Bundle();
                                                bundle.putString("text", "Failed to load hex file (" + result + ")");
                                                msg.setData(bundle);
                                                failedHandler.sendMessage(msg);
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
                        else if((state & STATE_EEPROM_READING) != 0)
                        {
                            boolean skip = (buffer[0] == 0x17);
                            if(skip)
                            {
                                itr_buff = 0;
                                EEPROM.clear();
                            }
                            for(int itr = 0; itr_buff < 512 && itr < msg.arg1; ++itr)
                            {
                                if(skip && itr == 0)
                                    continue;
                                EEPROM.set_nopart(itr_buff, buffer[itr]);
                                ++itr_buff;
                            }
                            if(itr_buff >= 512)
                                readHandler.sendEmptyMessage(0);
                        }
                        else if((state & STATE_EEPROM_WRITE) != 0)
                        {
                            if(buffer[0] == 0x1D)
                            {
                                dialog.dismiss();
                                dialog= new ProgressDialog(context);
                                dialog.setMessage("Writing into EEPROM...");
                                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                dialog.setMax(EEPROM.getTotalRecCount());
                                dialog.setProgress(0);
                                dialog.setCancelable(false);
                                dialog.show();
                            }
                            else if(buffer[0] != 0x1F)
                                return;
                            
                            if(eeprom_part == 1 && itr_buff >= EEPROM.getPartRecCount(true)*REC_SIZE && curEditId != 0)
                            {
                            	eeprom_part = 2;
                            	byte[] out = {0x16};
                                mChatService.write(out);
                                curEditId -= itr_buff/REC_SIZE;
                                itr_buff = 0;
                                break;
                            }
                            else if(itr_buff >= 510 || (itr_buff >= curEditId*REC_SIZE && itr_buff%5 == 0) || curEditId == 0)
                            {
                                writeCompleteHandler.sendEmptyMessage(0);
                                dialog.incrementProgressBy(1);
                                byte[] out = {0x1E};
                                mChatService.write(out);
                                eeprom_part = eeprom_write_part;
                                return;
                            }
                            
                            byte[] out = {EEPROM.get(itr_buff)};
                            mChatService.write(out);
                            if(itr_buff%5 == 0)
                                dialog.incrementProgressBy(1);
                            ++itr_buff;
                        }
                    }
                    break;
            }
        }
    };
    // HANDLERS END
    // FLASH FUNCTIONS
    private void SendPage(Page page)
    {	
        final byte[] out = { 0x10 };
        mChatService.write(out);

        final byte[] adress = { (byte)(page.address >> 8), (byte)(page.address) };
        mChatService.write(adress);
        mChatService.write(page.data);
        if(dialog.isShowing())
            dialog.setProgress(pagesItr+1);
    }
    
    private String CreatePages(memory mem)
    {
        pages.clear();
        final TextView error = (TextView)findViewById(R.id.error);
        if (mem.size() > deviceInfo.mem_size)
            for (int a = deviceInfo.mem_size; a < mem.size(); ++a)
                if (mem.Get(a) != 0xff)
                    return "Program is too big!";
        int alt_entry_page = deviceInfo.patch_pos / deviceInfo.page_size;
        boolean add_alt_page = deviceInfo.patch_pos != 0;

        int i = 0;
        short pageItr = 0;
        Page cur_page = new Page();
        for (boolean generate = true; generate && i < deviceInfo.mem_size / deviceInfo.page_size; ++i)
        {
            cur_page = new Page();
            cur_page.data = new byte[deviceInfo.page_size];
            cur_page.address = i * deviceInfo.page_size;
            pageItr = 0;
            if (mem.size() <= (i + 1) * deviceInfo.page_size)
            {
                for (int y = 0; y < deviceInfo.page_size; ++y)
                {
                    if (i * deviceInfo.page_size + y < mem.size())
                        cur_page.data[pageItr] = mem.Get(i * deviceInfo.page_size + y);
                    else
                        cur_page.data[pageItr] = (byte) 0xff;
                    ++pageItr;
                }
                generate = false;
            }
            else
            {
                for (int y = i * deviceInfo.page_size; y < (i + 1) * deviceInfo.page_size; ++y)
                {
                    cur_page.data[pageItr] = mem.Get(y);
                    ++pageItr;
                }
            }

            if (!patch_page(mem, cur_page, deviceInfo.patch_pos, deviceInfo.mem_size, pageItr))
                return "Failed patching page"; 
            pages.add(cur_page);

            if (i == alt_entry_page)
                add_alt_page = false;
        }
        if (add_alt_page)
        {
            for (int y = 0; y < deviceInfo.page_size; ++y)
                cur_page.data[y] = (byte)0xff;
            cur_page.address = alt_entry_page * deviceInfo.page_size;
            patch_page(mem, cur_page, deviceInfo.patch_pos, deviceInfo.mem_size, pageItr);
            pages.add(cur_page);
        }
        return "";
    }
    private boolean patch_page(memory mem, Page page, int patch_pos, int boot_reset, short page_pos)
    {
        if (patch_pos == 0)
            return true;
        
        if (page.address == 0)
        {
            int entrypt_jmp = (boot_reset / 2 - 1) | 0xc000;
            if((entrypt_jmp & 0xf000) != 0xc000)
                return false;
            page.data[0] = (byte) entrypt_jmp;
            page.data[1] = (byte) (entrypt_jmp >> 8);
            return true;
        }

        if (page.address > patch_pos || page.address + page_pos <= patch_pos)
            return true;

        int new_patch_pos = patch_pos - page.address;

        if (page.data[new_patch_pos] != (byte)0xff || page.data[new_patch_pos + 1] != (byte)0xff)
            return false;

        int entrypt_jmp2 = mem.Get(0) | (mem.Get(1) << 8);
        if ((entrypt_jmp2 & 0xf000) != 0xc000)
            return false;

        int entry_addr = (entrypt_jmp2 & 0x0fff) + 1;
        entrypt_jmp2 = ((entry_addr - patch_pos / 2 - 1) & 0xfff) | 0xc000;
        page.data[new_patch_pos] = (byte) entrypt_jmp2;
        page.data[new_patch_pos + 1] =  (byte) (entrypt_jmp2 >> 8);
        return true;
    }
}
