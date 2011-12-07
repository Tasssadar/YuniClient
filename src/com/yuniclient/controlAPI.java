package com.yuniclient;

public class controlAPI
{
    public static final byte API_KEYBOARD    = 0;       // just keyboard characters
    public static final byte API_YUNIRC      = 1;       // keyboard with d or u for press and release
    public static final byte API_PACKETS     = 2;       // YuniControl packets
    public static final byte API_CHESSBOT    = 3;       // Packets for robot Chessbot
    public static final byte API_QUORRA      = 4;       // Packets for robot Quorra  

    // For API_PACKETS
    public static final byte MOVE_NONE     = 0x00;
    public static final byte MOVE_FORWARD  = 0x01; 
    public static final byte MOVE_BACKWARD = 0x02; 
    public static final byte MOVE_LEFT     = 0x04; 
    public static final byte MOVE_RIGHT    = 0x08; 
    
    private controlAPI()
    {
        apiType = API_YUNIRC;
        m_maxSpeed = 255; // base calculation speed of quorra
    }
    
    public static controlAPI InitInstance()
    {
        instance = new controlAPI();
        return instance;
    }
    
    public static controlAPI GetInst()
    {
        return instance;
    }
    
    public static void Destroy()
    {
        instance = null;
    }
    
    public void SetAPIType(byte type)
    {
        if(apiType == type)
            return;

        apiType = type;
        m_protocol = null;

        switch(apiType)
        {
            default:
                return;
            case API_CHESSBOT:
                m_protocol = new ChessBotProtocol();
                return;
            case API_QUORRA:
                m_protocol = new QuorraProtocol();
                return;
        }
    }
    public static byte GetAPITypeFromString(CharSequence type)
    {
        if(type == "Keyboard")      return API_KEYBOARD;
        else if(type == "YuniRC")   return API_YUNIRC;
        else if(type == "Packets")  return API_PACKETS;
        else if(type == "Chessbot") return API_CHESSBOT;
        else if(type == "Quorra")   return API_QUORRA;
        return API_YUNIRC;
    }
    public byte GetAPIType() { return apiType; }
    
    public byte[] BuildPawPacket(float percent)
    {
        switch(apiType)
        {
            case API_CHESSBOT:
            case API_QUORRA:
                return m_protocol.BuildPawPacket(percent);
            default:
                return null;
        }
    }
    
    public byte[] BuildReelPacket(boolean up)
    {
        switch(apiType)
        {
            case API_CHESSBOT:
            case API_QUORRA:
                return m_protocol.BuildReelPacket(up);
            default:
                return null;
        }
    }
    
    public byte[] BuildMovementPacket(byte flags, boolean down, byte speed, short left, short right)
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
            case API_CHESSBOT:
            case API_QUORRA:
            {
                packet = m_protocol.BuildMovementPacket(flags, down, speed);
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
    
    public static int[] MoveFlagsToQuorra(int speed, byte flags, byte div)
    {
        int[] result = {speed, speed};
        if(speed == 0 || flags == MOVE_NONE)
            return null;
        
        if((flags & MOVE_FORWARD) != 0)
        {
            if((flags & MOVE_LEFT) != 0)
                result[1] -= speed/div;
            else if((flags & MOVE_RIGHT) != 0)
                result[0] -= speed/div;
        }
        else if((flags & MOVE_BACKWARD) != 0)
        {
            result[0] = -speed;
            result[1] = -speed;
            if((flags & MOVE_LEFT) != 0)
                result[1] += speed/div;
            else if((flags & MOVE_RIGHT) != 0)
                result[0] += speed/div;
        }
        else if((flags & MOVE_LEFT) != 0)
            result[1] = -speed;
        else if((flags & MOVE_RIGHT) != 0)
            result[0] = -speed;
        
        return result;
    }
    
    public static boolean IsTargetSpeedDefined(byte apiType) { return !(apiType == API_CHESSBOT || apiType == API_QUORRA); }
    public static boolean HasSeparatedSpeed(byte apiType) { return (apiType == API_YUNIRC || apiType == API_KEYBOARD); }
    public static boolean HasPacketStructure(byte apiType) { return (apiType == API_PACKETS || apiType == API_CHESSBOT || apiType == API_QUORRA); }
    
    private float fabs(float val)
    {
        if(val < 0)
            return -val;
        return val;
    }
    public void SetDefXY(float x, float y) { mDefX = x; mDefY = y; }
    public float GetDefX() { return mDefX; }
    public float GetDefY() { return mDefY; }

    public void SetMaxSpeed(int speed)
    {
        switch(apiType)
        {
            case API_CHESSBOT:
            case API_QUORRA:
                m_protocol.setMaxSpeed((short) speed);
                break;
            default:
                m_maxSpeed = speed;
                break;    
        }
    }
    
    public short GetDefaultMaxSpeed()
    {
        switch(apiType)
        {
            case API_CHESSBOT:
            case API_QUORRA:
                return m_protocol.getMaxSpeed();
            default:
                return (short)m_maxSpeed;
        }
    }
    
    public byte GetTurnDiv()
    {
        switch(apiType)
        {
            case API_CHESSBOT:
            case API_QUORRA:
                return m_protocol.getTurnDiv();
            default:
                return 5;
        }
    }
    
    public void SetTurnDiv(byte div)
    {
        switch(apiType)
        {
            case API_CHESSBOT:
            case API_QUORRA:
                m_protocol.setTurnDiv(div);
                break;
        }
    }
    
    private float mDefX;
    private float mDefY;

    private byte apiType;
    private int m_maxSpeed;
    
    private static controlAPI instance;
    private Protocol m_protocol;
};
