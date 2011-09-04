package com.yuniclient;

public class QuorraProtocol extends Protocol
{
    public QuorraProtocol()
    {
        m_maxSpeed = 500;
    }
    
    public String getName() { return "Quorra"; }
    
    public byte[] BuildPawPacket(float percent)
    {
        return null;
    }
    
    public byte[] BuildMovementPacket(byte flags, boolean down, byte speed)
    {
          byte quorraPkt[] = new byte[8];
          quorraPkt[0] = (byte)0xFF;
          quorraPkt[1] = (byte)0x00;
          quorraPkt[2] = (byte)5;
          quorraPkt[3] = (byte)5;
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
              data = controlAPI.MoveFlagsToQuorra(targetSpeed, flags);
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
          return quorraPkt;
    }
    
    public void setMaxSpeed(short speed) { m_maxSpeed = speed; }
    public short getMaxSpeed() { return m_maxSpeed; }
    
    private short m_maxSpeed;
}