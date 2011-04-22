package com.yuniclient;

class Terminal
{
    public final static byte SMSG_PING                = 0x01;
    public final static byte CMSG_PONG                = 0x02;
    public final static byte SMSG_SET_MOVEMENT        = 0x03;
    public final static byte SMSG_SET_CORRECTION_VAL  = 0x04;
    public final static byte CMSG_RANGE_BLOCK         = 0x05;
    public final static byte CMSG_RANGE_BLOCK_GONE    = 0x06;
    public final static byte CMSG_EMERGENCY_START     = 0x07;
    public final static byte CMSG_EMERGENCY_END       = 0x08;
    public final static byte SMSG_SET_EMERGENCY_INFO  = 0x09;
    public final static byte SMSG_SET_SERVO_VAL       = 0x10;
    public final static byte SMSG_ENCODER_START       = 0x11;
    public final static byte SMSG_ENCODER_GET         = 0x12;
    public final static byte CMSG_ENCODER_SEND        = 0x13;
    public final static byte SMSG_ENCODER_STOP        = 0x14;
    public final static byte SMSG_ENCODER_SET_EVENT   = 0x15;
    public final static byte CMSG_ENCODER_EVENT_DONE  = 0x16;
    public final static byte CMSG_LASER_GATE_STAT     = 0x17;
    public final static byte SMSG_LASER_GATE_SET      = 0x18;
    public final static byte CMSG_BUTTON_STATUS       = 0x19;
    public final static byte SMSG_ADD_STATE           = 0x20;
    public final static byte SMSG_REMOVE_STATE        = 0x21;
    public final static byte SMSG_STOP                = 0x22;
    public final static byte CMSG_LOCKED              = 0x23;
    public final static byte SMSG_UNLOCK              = 0x24;
    public final static byte SMSG_CONNECT_REQ         = 0x25;                                                                                                          
    public final static byte CMSG_CONNECT_RES         = 0x26; 
    
    public final static byte PARSER_TEXT = 0;
    public final static byte PARSER_HEX  = 1;
    public final static byte PARSER_BYTE = 2;
    public final static byte PARSER_PACKETS = 3;
    
    Terminal()
    {
        currentParser = PARSER_TEXT;
        terminalText = null;
        parsedText = null;
    }
    
    public byte GetCurrentParser() { return currentParser; }
    public void SetCurrentParser(byte parser)
    {
        currentParser = parser;
        if(terminalText != null)
            parsedText = Parse(terminalText);
    }
    
    public static String Parse(String text)
    {
        if(text == null)
            return null;
        String res = null;
        switch(currentParser)
        {
            case PARSER_TEXT:
                res = text;
                break;
            case PARSER_HEX:
            {
                res = "";
                int lenght = text.length();
                for(int i = 0; i < lenght; ++i)
                    res += "0x" + Integer.toHexString(((byte)text.charAt(i)) & 0xFF).toUpperCase() + " ";
                break;
            }
            case PARSER_BYTE:
            {
                res = "";
                int lenght = text.length();
                for(int i = 0; i < lenght; ++i)
                    res += String.valueOf(((byte)text.charAt(i)) & 0xFF) + " ";
                break;
            }
            case PARSER_PACKETS:
            {
                res = "";
                int lenght = text.length();
                byte packetLen = 0;
                for(int i = 0; i < lenght; ++i)
                {
                    if(((byte)text.charAt(i) & 0xFF) == 0xFF && (byte)text.charAt(i+1) == 0x01)
                    {
                        packetLen = (byte)text.charAt(i+2);
                        res += "Packet: " + opcodeToString((byte) text.charAt(i+3)) + "\r\n";
                        res += "Lenght: " + packetLen + "\r\n";
                        res += "Data:\r\n";
                        for(byte y = 0; y < packetLen; ++y)
                        {
                            res += y + ": " + (short)((byte)text.charAt(i+y+4) & 0xFF) + " (0x" +
                                Integer.toHexString(((byte)text.charAt(i+y+4)) & 0xFF).toUpperCase() + ")\r\n";
                        }
                        res += "\r\n";
                        i += 4 + packetLen;
                    }
                }
                break;
            }
        }
        return res;
    }
    
    
    public String GetText() { return parsedText; }
    public void SetText(String text)
    {
        terminalText = text;
        parsedText = Parse(terminalText);
    }
    public void Append(String text)
    {
        if(terminalText == null)
        {
            terminalText = "";
            parsedText = "";
        }
        terminalText += text;
        parsedText += Parse(text);
    }
    
    public static String opcodeToString(byte opcode)
    {
        switch(opcode)
        {
            default: return String.valueOf(opcode);
            case SMSG_PING:                  return "SMSG_PING";
            case CMSG_PONG:                  return "CMSG_PONG";
            case SMSG_SET_MOVEMENT:          return "SMSG_SET_MOVEMENT";
            case SMSG_SET_CORRECTION_VAL:    return "SMSG_SET_CORRECTION_VAL";
            case CMSG_RANGE_BLOCK:           return "CMSG_RANGE_BLOCK";
            case CMSG_RANGE_BLOCK_GONE:      return "CMSG_RANGE_BLOCK_GONE";
            case CMSG_EMERGENCY_START:       return "CMSG_EMERGENCY_START";
            case CMSG_EMERGENCY_END:         return "CMSG_EMERGENCY_END";
            case SMSG_SET_EMERGENCY_INFO:    return "SMSG_SET_EMERGENCY_INFO";
            case SMSG_SET_SERVO_VAL:         return "SMSG_SET_SERVO_VAL";
            case SMSG_ENCODER_START:         return "SMSG_ENCODER_START";
            case SMSG_ENCODER_GET:           return "SMSG_ENCODER_GET";
            case CMSG_ENCODER_SEND:          return "CMSG_ENCODER_SEND";
            case SMSG_ENCODER_STOP:          return "SMSG_ENCODER_STOP";
            case SMSG_ENCODER_SET_EVENT:     return "SMSG_ENCODER_SET_EVENT";
            case CMSG_ENCODER_EVENT_DONE:    return "CMSG_ENCODER_EVENT_DONE";
            case CMSG_LASER_GATE_STAT:       return "CMSG_LASER_GATE_STAT";
            case SMSG_LASER_GATE_SET:        return "SMSG_LASER_GATE_SET";
            case CMSG_BUTTON_STATUS:         return "CMSG_BUTTON_STATUS";
            case SMSG_ADD_STATE:             return "SMSG_ADD_STATE";
            case SMSG_REMOVE_STATE:          return "SMSG_REMOVE_STATE";
            case SMSG_STOP:                  return "SMSG_STOP";
            case CMSG_LOCKED:                return "CMSG_LOCKED";
            case SMSG_UNLOCK:                return "SMSG_UNLOCK";
            case SMSG_CONNECT_REQ:           return "SMSG_CONNECT_REQ";
            case CMSG_CONNECT_RES:           return "CMSG_CONNECT_RES";
        }
    }
    
    
    private static byte currentParser;
    private String terminalText;
    private String parsedText;
}