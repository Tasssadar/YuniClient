package com.yuniclient;

public class ChessBotProtocol extends Protocol
{
    public ChessBotProtocol()
    {
        m_maxSpeed = 255;
        m_div = 5;
    }
    
    public String getName() { return "ChessBot"; }
    
    public byte[] BuildPawPacket(float percent)
    {
        byte[] tmp = new byte[4];
        Packet pkt = new Packet(ProtocolMgr.QUORRA_PAWS, tmp, (byte) 4);
        int pos = (int)((2000*(percent/100))-1000);
        int pos2 = (int)((2000*((100 - percent)/100))-1000);
        pkt.writeUInt16(pos);
        pkt.writeUInt16(pos2);
        pkt.CountOpcode(true);
        return pkt.getSendData();
    }
    
    public byte[] BuildReelPacket(boolean up)
    {
        byte[] tmp = new byte[2];
        Packet pkt = new Packet(ProtocolMgr.QUORRA_REEL, tmp, (byte) 2);
        pkt.writeByte(up ? (byte)1 : (byte)0);
        pkt.writeByte((byte)100);
        pkt.CountOpcode(true);
        return pkt.getSendData();
    }
    
    public byte[] BuildMovementPacket(byte flags, boolean down, byte speed)
    {
        byte[] tmp = new byte[4];
        Packet pkt = new Packet(ProtocolMgr.QUORRA_SET_POWER, tmp, (byte) 4);
        int[] data = null;
        if(down)
        {
            int targetSpeed = m_maxSpeed;
            switch(speed)
            {
                case 50: targetSpeed = m_maxSpeed/3; break;
                case 100: targetSpeed = m_maxSpeed/2;break;
                case 127:
                default:
                    break;
            }
            data = controlAPI.MoveFlagsToQuorra(targetSpeed, flags, getTurnDiv());
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
        return pkt.getSendData();
    }
  
}