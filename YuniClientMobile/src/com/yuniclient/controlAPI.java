package com.yuniclient;

public class controlAPI
{
    public static final byte API_KEYBOARD = 0;       // just keyboard characters
    public static final byte API_YUNIRC   = 1;       // keyboard with d or u for press and release
    public static final byte API_PACKETS  = 2;       // YuniControl packets
    public static final byte API_QUORRA   = 3;       // Packets for robot Quorra  
    public static final byte API_QUORRA_FINAL= 4;       // Packets for robot Quorra  
    
    
    // For API_PACKETS
    public static final byte MOVE_NONE     = 0x00;
    public static final byte MOVE_FORWARD  = 0x01; 
    public static final byte MOVE_BACKWARD = 0x02; 
    public static final byte MOVE_LEFT     = 0x04; 
    public static final byte MOVE_RIGHT    = 0x08; 
    
    private static final double PI = 3.141592;
    
    public controlAPI()
    {
        apiType = API_YUNIRC;
        quorraSpeed = 300; // base calculation speed of quorra
    }
    
    public void SetAPIType(byte type) {    apiType = type; }
    public byte GetAPIType() { return apiType; }
    public byte[] BuildMovementPacket(byte flags, boolean down, byte speed)
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
                byte[] data = {(speed == 0 ? 127 : speed),  (down ? flags : 0)};
                Packet pkt = new Packet(ProtocolMgr.YUNICONTROL_SET_MOVEMENT, data, (byte) 2);
                packet = pkt.getSendData();
                break;
            }
            case API_QUORRA:
            {
                byte[] tmp = new byte[4];
                Packet pkt = new Packet(ProtocolMgr.QUORRA_SET_POWER, tmp, (byte) 4);
                int[] data = null;
                if(down)
                {
                    int targetSpeed = quorraSpeed;
                    switch(speed)
                    {
                        case 50: targetSpeed = quorraSpeed/3; break;
                        case 100: targetSpeed = quorraSpeed/2;break;
                        case 127:
                        default:
                            break;
                    }
                    data = MoveFlagsToQuorra(targetSpeed, flags);
                    if(data != null)
                    {
                        pkt.writeUInt16(data[0]);
                        pkt.writeUInt16(data[1]);
                    }
                }
                
                if(data == null)
                {
                    pkt.writeUInt16(0);
                    pkt.writeUInt16(0);
                }
                pkt.CountOpcode(true);
                packet = pkt.getSendData();
                break;
            }
            case API_QUORRA_FINAL:
            {
                byte quorraPkt[] = new byte[8];
                quorraPkt[0] = (byte)0xFF;
                quorraPkt[1] = (byte)0x00;
                quorraPkt[2] = (byte)5;
                quorraPkt[3] = (byte)5;
                int[] data = null;
                if(down)
                {
                    int targetSpeed = quorraSpeed;
                    switch(speed)
                    {
                        case 50: targetSpeed = quorraSpeed/3; break;
                        case 100: targetSpeed = quorraSpeed/2;break;
                        case 127:
                        default:
                            break;
                    }
                    data = MoveFlagsToQuorra(targetSpeed, flags);
                    if(data != null)
                    {
                        quorraPkt[4] = (byte) (data[0] >> 8);
                        quorraPkt[5] = (byte) (data[0]);
                        quorraPkt[6] = (byte) (data[1] >> 8);
                        quorraPkt[7] = (byte) (data[1]);
                    }
                }
                
                if(data == null)
                {
                    quorraPkt[4] = (byte) 0;
                    quorraPkt[5] = (byte) 0;
                    quorraPkt[6] = (byte) 0;
                    quorraPkt[7] = (byte) 0;
                }
                packet = quorraPkt;
                break;
            }
        }
        return packet;
    }
    
    public byte[] XYToMoveFlags(float x, float y)
    {
        byte[] flags = {0, 0};
        if(fabs(y - mDefY) >= 20)
        {
            if(mDefY - y < -20)
                flags[0] |= MOVE_BACKWARD;
            else if(mDefY - y > 20)
                flags[0] |= MOVE_FORWARD;
            
            //speed
            if(fabs(y - mDefY) < 30)
                flags[1] = 50;
            else if(fabs(y - mDefY) < 35)
                flags[1] = 100;
            else
                flags[1] = 127;
        }
        if(fabs(x - mDefX) >= 20)
        {
            if(mDefX - x < -20)
                flags[0] |= MOVE_LEFT;
            else if(mDefX - x > 20)
                flags[0] |= MOVE_RIGHT;
            if(flags[1] == 0)
            {
                if(fabs(x - mDefX) < 30)
                    flags[1] = 50;
                else if(fabs(x - mDefX) < 35)
                    flags[1] = 100;
                else
                    flags[1] = 127;
            }
        }
        return flags;
    }
    
    public int[] MoveFlagsToQuorra(int speed, byte flags)
    {
        int[] result = {speed, speed};
        if(speed == 0 || flags == MOVE_NONE)
            return null;
        
        if((flags & MOVE_FORWARD) != 0)
        {
            if((flags & MOVE_LEFT) != 0)
                result[1] = speed/3;
            else if((flags & MOVE_RIGHT) != 0)
                result[0] = speed/3;
        }
        else if((flags & MOVE_BACKWARD) != 0)
        {
            result[0] = -speed;
            result[1] = -speed;
            if((flags & MOVE_LEFT) != 0)
                result[1] = -(speed/3);
            else if((flags & MOVE_RIGHT) != 0)
                result[0] = -(speed/3);
        }
        else if((flags & MOVE_LEFT) != 0)
            result[1] = -speed;
        else if((flags & MOVE_RIGHT) != 0)
            result[0] = -speed;
        
        return result;
    }
    
    public static boolean IsTargetSpeedDefined(byte apiType) { return !(apiType == API_QUORRA || apiType == API_QUORRA_FINAL); }
    public static boolean HasSeparatedSpeed(byte apiType) { return (apiType == API_YUNIRC || apiType == API_KEYBOARD); }
    public static boolean HasPacketStructure(byte apiType) { return (apiType == API_PACKETS || apiType == API_QUORRA || apiType == API_QUORRA_FINAL); }
    
    private float fabs(float val)
    {
        if(val < 0)
            return -val;
        return val;
    }
    public void SetDefXY(float x, float y) { mDefX = x; mDefY = y; }
    public float GetDefX() { return mDefX; }
    public float GetDefY() { return mDefY; }

    
    
    public byte[] BallXYToFlags(float x, float y, int width, int height, boolean spinAtPlace)
    {
        float dx = x - width/2;
        float dy = y - height/2;
        float dist = (float) Math.sqrt((dx*dx) + (dy*dy));
        float distPct = (float) (dist/(((float)(width/2))/100.0));
        
        byte speed = 127;
        if(distPct < 33)
            speed = 50;
        else if(distPct < 66)
            speed = 100;
        
        byte[] flags = {0, speed };
        
                    
        float ang = (float) Math.atan2(dy, dx);                                                                                                 
        ang = (float) ((ang >= 0) ? ang : 2 * 3.141592 + ang);  
        
        if(ang > 5.18362 || ang <  1.09956 || (!spinAtPlace && (ang >= PI*1.5 || ang <= PI*0.5))) // 0.7 PI range
            flags[0] |= MOVE_FORWARD;
        else if(ang > 2.04203 && ang < 4.24115 || (!spinAtPlace && (ang > PI*0.5 && ang < PI*1.5))) 
            flags[0] |= MOVE_BACKWARD;
        
        if(ang > 0.471236 && ang < 2.67036)
            flags[0] |= MOVE_RIGHT;
        else if(ang > 3.61283 && ang <  5.81195)
            flags[0] |= MOVE_LEFT;
        return flags;
        
    }

    public void SetQuarraSpeed(int speed) { quorraSpeed = speed; }
    
    private float mDefX = 0;
    private float mDefY = 0;

    private byte apiType;
    private int quorraSpeed;
        
};
