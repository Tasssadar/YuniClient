package com.yuniclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

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
    public final static byte SMSG_TEST                = 0x27;
    public final static byte CMSG_TEST_RESULT         = 0x28;
    public final static byte SMSG_ENCODER_RM_EVENT    = 0x29;
    public final static byte CMSG_RANGE_VALUE         = 0x30;
    public final static byte SMSG_SHUTDOWN_RANGE      = 0x31;
    public final static byte CMSG_DEADEND_DETECTED    = 0x32;
    public final static byte CMSG_SET_POWER_REQ       = 0x33;
    public final static byte CMSG_RANGE_ADDR_EMPTY    = 0x34;
    public final static byte SMSG_SET_RANGE_ADDR      = 0x35;
    public final static byte SMSG_TEST_RANGE          = 0x36;
    
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
            parsedText = Terminal.Parse(terminalText);
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
                char[] array = text.toCharArray();
                for(char i : array)
                    res += "0x" + Integer.toHexString(((byte)i) & 0xFF).toUpperCase() + " ";
                break;
            }
            case PARSER_BYTE:
            {
                res = "";
                char[] array = text.toCharArray();
                for(char i : array)
                    res += String.valueOf(((byte)i) & 0xFF) + " ";
                break;
            }
            case PARSER_PACKETS:
            {
                res = "";
                int lenght = text.length();
                byte packetLen = 0;
                for(int i = 0; i < lenght; ++i)
                {
                    if(text.length() <= i+3)
                        break;
                    if(((byte)text.charAt(i) & 0xFF) == 0xFF && ((byte)text.charAt(i+1) & 0xFF) == 0xEF && 
                        text.length() > i+3+(byte)text.charAt(i+2))
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
    public void SetText(String text, String parsed)
    {
        terminalText = text;
        parsedText = parsed;
    }
    public void Append(String text, String parsed)
    {
        if(text == null)
            return;
        if(terminalText == null)
        {
            terminalText = "";
            parsedText = "";
        }
        terminalText += text;
        parsedText += parsed;
    }
    
    public void toFile(String name, Handler handler) throws IOException
    {
        if(!name.contains("."))
            name += ".txt";
        File file = new File("/mnt/sdcard/YuniData/"+name);
        if(!file.exists())
            file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write(parsedText.getBytes());
        out.close();
        Message msg = new Message();
        msg.what = Connection.MESSAGE_TOAST;
        Bundle bundle = new Bundle();
        bundle.putString(YuniClient.TOAST, "File " + file.getName() + " saved");
        msg.arg1 = 0;
        msg.setData(bundle);
        handler.sendMessage(msg); 
    }
    
    public static String opcodeToString(byte opcode)
    {
        switch(opcode)
        {
            default: return "0x"+Integer.toHexString(opcode).toUpperCase();
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
            case SMSG_TEST:                  return "SMSG_TEST";
            case CMSG_TEST_RESULT:           return "CMSG_TEST_RESULT";
            case SMSG_ENCODER_RM_EVENT:      return "SMSG_ENCODER_RM_EVENT";
            case CMSG_RANGE_VALUE:           return "CMSG_RANGE_VALUE";
            case SMSG_SHUTDOWN_RANGE:        return "SMSG_SHUTDOWN_RANGE";
            case CMSG_DEADEND_DETECTED:      return "CMSG_DEADEND_DETECTED";
            case CMSG_SET_POWER_REQ:         return "CMSG_SET_POWER_REQ";
            case CMSG_RANGE_ADDR_EMPTY:      return "CMSG_RANGE_ADDR_EMPTY";
            case SMSG_SET_RANGE_ADDR:        return "SMSG_SET_RANGE_ADDR";
            case SMSG_TEST_RANGE:            return "SMSG_TEST_RANGE";
        }
    }
    
    
    private static byte currentParser;
    private String terminalText;
    private String parsedText;
}