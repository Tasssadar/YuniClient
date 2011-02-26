package com.yuniclient;

import android.os.Handler;
import android.os.Message;

class controlAPI
{
    public static final byte API_KEYBOARD = 0;       // just keyboard characters
    public static final byte API_YUNIRC   = 1;       // keyboard with d or u for press and release
    public static final byte API_PACKETS  = 2;       // YuniControl packets1
    
    
    // For API_PACKETS
    public static final byte MOVE_NONE     = 0x00;
    public static final byte MOVE_FORWARD  = 0x01; 
    public static final byte MOVE_BACKWARD = 0x02; 
    public static final byte MOVE_LEFT     = 0x04; 
    public static final byte MOVE_RIGHT    = 0x08; 



    public controlAPI(Handler handler)
    {
        apiType = API_YUNIRC;
        EEPROM = null;
        playThread = null;
        mHandler = handler;
    }
    
    public void SetAPIType(byte type) { apiType = type; }
    public byte GetAPIType() { return apiType; }
    public byte[] BuildMovementPacket(byte flags, boolean down)
    {
        if(apiType == API_KEYBOARD && !down)
            return null;
        byte[] packet = null;
        
        switch(apiType)
        {
            case API_KEYBOARD:
            case API_YUNIRC:
            {
                packet = new byte[apiType == API_YUNIRC ? 2 : 1];
                switch(flags)
                {
                    default:
                    case MOVE_FORWARD:
                       packet[0] = (byte)'W';
                       break;
                    case MOVE_BACKWARD:
                       packet[0] = (byte)'S';
                       break;
                    case MOVE_LEFT:
                       packet[0] = (byte)'A';
                       break;
                    case MOVE_RIGHT:
                       packet[0] = (byte)'D';
                       break;
                }
                if(apiType == API_YUNIRC)
                {
                    if(down)
                        packet[1] = (byte)'d';
                    else
                        packet[1] = (byte)'u';
                }
                break;
            }
            case API_PACKETS:
            {
                packet = new byte[5];
                packet[0] = 1;
                packet[1] = 0x03; // SMSG_SET_MOVEMENT
                packet[2] = 2;    // lenght
                packet[3] = 127;  // speed
                packet[4] = down ? flags : 0;// moveflags, 0 to stop on release
                break;
            }
        }
        return packet;
    }
    public void Play(eeprom mem)
    {
    	EEPROM = mem;
    	playThread = new PlayThread(mem, YuniClient.eeprom_part);
    	playThread.start();
    }
    
    public void received(Packet pkt)
    {
    	playThread.PacketReceived(pkt);
    }
    public void StopPlay()
    {
    	if(playThread != null)
    	{
    	    playThread.cancel();
    	    playThread = null;
    	}
    }
    
    private class PlayThread extends Thread {
        eeprom EEPROM;
        byte eepromPart;
        boolean stop;
        boolean canContinue;
        byte speed;
        byte moveFlags;
        byte endEvent;
        int endData;

        public PlayThread(eeprom mem, byte part) {
        	EEPROM = mem;
        	eepromPart = part;
        	speed = 127;
        	canContinue = false;
        	stop = false;
        	endEvent = 0;
        	endData = 0;
        }

        public void run() {
        	byte[] rec = null;
        	boolean down;
        	Message msg = null;
        	byte[] packet = null;
        	int y = EEPROM.getPartRecCount(eepromPart == 1)*5;
            for(int i = 0; i < y && !stop; i +=5)
            {
            	rec = EEPROM.getRec(i);
            	down = rec[1] == (byte)'d';
            	packet = null;
            	// keys
            	switch(rec[0])
            	{
            		case (byte)'W':
            		case (byte)'S':
            		case (byte)'A':
            		case (byte)'D':
            			moveFlags = LetterToFlags(rec[0]);
            		    packet = Movement(down);
            	    	break;
            		case (byte)'a':
            			speed = 50;
            		    packet = Movement(down);
            		    break;
            		case (byte)'b':
            			speed = 100;
            		    packet = Movement(down);
            		    break;
            		case (byte)'c':
            			speed = 127;
            		    packet = Movement(down);
            		    break;
            		 
            	}
            	msg = new Message();
            	msg.what = 1;
            	msg.obj = packet;
            	msg.getData().putString("log", "Key " + (char)rec[0] + (char)rec[1] + " action sent\r\n"+
           			"Waiting for event " + rec[2] + " ...");
            	mHandler.sendMessage(msg);
            	
            	// wait events
            	switch(rec[2])
            	{
            	    default:
            	    case 0:   // EVENT_NONE
            	    	continue;
            		case 1:   // EVENT_TIME
            			int time = ((rec[3] << 8) | (rec[4] & 0xFF))*10;
						try {
							Thread.sleep(time);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
            			break;
            		case 2:  // EVENT_SENSOR_LEVEL_HIGHER
            			if(rec[3] == 8)
            			{
            				byte[] emergency = new byte[4];
            				emergency [0] = 1;
            				emergency [1] = Protocol.SMSG_SET_EMERGENCY_INFO;
            				emergency [2] = 1;    // lenght
            				emergency [3] = 1;
            				msg = new Message();
                        	msg.what = 1;
                        	msg.obj = emergency.clone();
                        	msg.getData().putString("log", "Enable emergency");
            				canContinue = false;
                			endEvent = rec[2];
                			endData = rec[3];
                			while(!canContinue)
                			{
	                			try {
	    							Thread.sleep(500);
	    						} catch (InterruptedException e) {
	    							// TODO Auto-generated catch block
	    							e.printStackTrace();
	    						}
                			}
            				emergency [3] = 0;
            				msg = new Message();
                        	msg.what = 1;
                        	msg.obj = emergency.clone();
                        	msg.getData().putString("log", "Disable emergency");
                        	endEvent = 0;
            			}
            			break;
            		case 3:  // EVENT_SENSOR_LEVEL_LOWER
            		case 4:  // EVENT_RANGE_HIGHER
            		case 5:  // EVENT_RANGE_LOWER
            			//TODO NYI
            			break;
            		case 6:  // EVENT_DISTANCE
            		case 7:  // EVENT_DISTANCE_LEFT
            		case 8:  // EVENT_DISTANCE_RIGHT
            			canContinue = false;
            			endEvent = rec[2];
            			endData = ((rec[3] << 8) | (rec[4] & 0xFF))*5;
            			byte[] encoders = new byte[3];
            			encoders [0] = 1;
            			encoders [1] = Protocol.SMSG_ENCODER_START;
            			encoders [2] = 0;    // lenght
            			msg = new Message();
                    	msg.what = 1;
                    	msg.obj = encoders.clone();
                    	msg.getData().putString("log", "Starting encoders");
                    	mHandler.sendMessage(msg);
                    	
                    	encoders [1] = Protocol.SMSG_ENCODER_GET;
                    	
            			while(!canContinue)
            			{
            				msg = new Message();
                        	msg.what = 1;
                        	msg.obj = encoders.clone();
                        	msg.getData().putString("log", "Get encoders val");
                        	mHandler.sendMessage(msg);
                        	
            				try {
    							Thread.sleep(500);
    						} catch (InterruptedException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
            			}
            			endEvent = 0;
            			
            			encoders = new byte[4];
            			encoders [0] = 1;
            			encoders [1] = Protocol.SMSG_ENCODER_STOP;
            			encoders [2] = 1;
            			encoders [3] = 1; // also clear encoders
            			msg = new Message();
                    	msg.what = 1;
                    	msg.obj = encoders.clone();
                    	msg.getData().putString("log", "Stopping encoders");
                    	mHandler.sendMessage(msg);
            			break;
            	}
            	
            }
        }
        public byte[] Movement(boolean down)
        {
        	byte[] packet = new byte[5];
            packet[0] = 1;
            packet[1] = Protocol.SMSG_SET_MOVEMENT; // SMSG_SET_MOVEMENT
            packet[2] = 2;    // lenght
            packet[3] = speed;  // speed
            packet[4] = down ? moveFlags : 0;// moveflags, 0 to stop on release
            return packet;
        }
        public byte LetterToFlags(byte character)
        {
        	switch(character)
        	{
	        	case (byte)'W':
	        		return controlAPI.MOVE_FORWARD;
	    		case (byte)'S':
	    			return controlAPI.MOVE_BACKWARD;
	    		case (byte)'A':
	    			return controlAPI.MOVE_LEFT;
	    		case (byte)'D':
	    			return controlAPI.MOVE_RIGHT;
        	}
        	return 0;
        }
        
        public void cancel()
        {
        	stop = true;
        	canContinue = true;
        }
        
        public void PacketReceived(Packet pkt)
        {
        	switch(endEvent)
        	{
        		case 0:
        			return;
        		case 2:
        			if(endData != 8 || pkt.getOpcode() != Protocol.CMSG_EMERGENCY_START) // TODO NYI
        		        return;
        			canContinue = true;
        			break;
        		case 6:
        		case 7:
        		case 8:
        			if(pkt.getOpcode() != Protocol.CMSG_ENCODER_SEND)
        				break;
        			pkt.setPos((byte)0);
        			int left = fabs(pkt.readUInt16());
        			int right = fabs(pkt.readUInt16());
        			if((endEvent == 6 && (left + right)/2 > endData) ||
        		        (endEvent == 7 && left > endData) ||
        		        (endEvent == 8 && right > endData))
        				canContinue = true;
        			break;
        	}
        }
        
        private int fabs(int i) {
            if(i >= 0) return i;
            return -i;
        }
        
    }
    private eeprom EEPROM;
    private PlayThread playThread;
    private byte apiType;
    private Handler mHandler;
        
};
