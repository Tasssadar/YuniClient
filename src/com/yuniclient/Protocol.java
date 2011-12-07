package com.yuniclient;

public class Protocol
{   
    public Protocol()
    {
        m_id = -1;
    }
    
    public void setId(byte id) { m_id = id; }
    public byte getId() { return m_id; }
    public String getName() { return null; }
    
    public byte[] BuildPawPacket(float percent) { return null; }
    public byte[] BuildMovementPacket(byte flags, boolean down, byte speed) { return null; }
    public byte[] BuildReelPacket(boolean up) { return null; }
    
    public void setMaxSpeed(short speed) { m_maxSpeed = speed; }
    public short getMaxSpeed() { return m_maxSpeed; }
    public byte getTurnDiv() { return m_div; }
    public void setTurnDiv(byte div) { m_div = div; }
    
    protected short m_maxSpeed;
    protected byte m_div;
    private byte m_id; 
};


class Header
{
    public byte length;
    public byte opcode;
    public boolean lengthWithOpcode;
    public byte[] startBytes;
    public byte opcodePos;
    public byte lengthPos;
    
    public void addStartByte(byte value, byte pos)
    {
        if(length <= 0 || pos >= length)
            return;
        
        if(startBytes == null)
            startBytes = new byte[length];
        startBytes[pos] = value;
    }
};

class Data
{
    public byte type;
    public byte[] speedLevels; // from min to max
    public byte[] moveFlags; // forward, backward, left, right
    public byte[] speedChars; // from min to max
    public byte moveFlagsPos;
    public byte speedPos;
    public byte keyPos;
    public byte pressPos; // d/u pos
    public byte pressKey;
    public byte releaseKey;
};
