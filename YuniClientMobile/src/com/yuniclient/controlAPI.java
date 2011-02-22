package com.yuniclient;

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



    public controlAPI()
    {
        apiType = API_YUNIRC;
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
    
    
    private byte apiType;
        
};
