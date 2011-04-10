package com.yuniclient;

public class Protocol
{   
    public Protocol()
    {
        m_header = null;
        m_data = null;
    }
    
    public void setHeader(Header header) { m_header = header; }
    public Header getHeader() { return m_header; }
    
    public void setData(Data data) { m_data = data; }
    public Data getData() { return m_data; }
    
    private Header m_header;
    private Data m_data;
    
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
