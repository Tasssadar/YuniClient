package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.util.HashMap;

public class ProtocolMgr
{
    public static final byte YUNICONTROL_SET_MOVEMENT  = 0x03;
    public static final byte QUORRA_SET_POWER          = 4;
    public static final byte QUORRA_PAWS               = 6;
    
    private static final String protocolVersion        = "#YCProtocol01"; 
    private static final byte headerLenght             = 13;

    public ProtocolMgr()
    {
        protocols = new HashMap();
        lineEnd = 0;
    }
    
    public void ScanForProtocols()
    {
    	File folder = new File("/mnt/sdcard/YuniClient/protocols/");
    	if(!folder.exists() || !folder.isDirectory() || folder.listFiles() == null || folder.listFiles().length == 0)
    	    return;
    	
    	File[] files = folder.listFiles();
    	Protocol tmpProtocol = null;
    	int protocolItr = 0;
    	for(short i = 0; i < files.length; ++i)
    	{
    		 if(!files[i].canRead() || files[i].length() >= Integer.MAX_VALUE || files[i].length() < headerLenght)
                 continue;
    		 
    		 try {
				 tmpProtocol = parseFile(files[i]);
			 } catch (IOException e) {
				 tmpProtocol = null;
			 }
    		 
    		 if(tmpProtocol == null)
    			 continue;
    		 
    		 protocols.put(protocolItr++, tmpProtocol);
    	}
    }
    private Protocol parseFile(File file) throws IOException 
    {	
    	lineEnd = 0;
    	FileInputStream stream = new FileInputStream(file);
    	byte[] fileBuff = new byte[(int) file.length()];
        stream.read(fileBuff);
        stream.close();
        
        int fileLenght = (int) file.length();
    	
        String header = readLine(fileBuff, fileLenght);  	
    	if(!header.contentEquals(protocolVersion))
    		return null;
    	
    	Protocol protocol = new Protocol();
    	byte section = -1; // 0 = header, 1 = data lenght, 2 = opcode, 3 = data
        for(String line = readLine(fileBuff, fileLenght); line != null; line = readLine(fileBuff, fileLenght))
        {
            if(line == "")
            	continue;
            
            switch(line.charAt(0))
            {
            	case '*':
            		if(line.contentEquals("*Header"))
            			section = 0;
            		else if(line.contentEquals("*Data lenght"))
            			section = 1;
            		else if(line.contentEquals("*Opcode"))
            			section = 2;
            		else if(line.contentEquals("*Data"))
            			section = 3;
            		continue;
            	default: break;
            }
            
            switch(section)
            {
                case -1: 
                default:
                	continue;
                case 0:
                {
                	byte headerLength = Integer.valueOf(line).byteValue();
                	if(headerLength != 0)
                	{
                		Header tmpHeader = new Header();
                		tmpHeader.length =  headerLength;section = -1;
                		
                		for(byte y = 0; y < headerLength; ++y)
                		{
                			line = readLine(fileBuff, fileLenght);
                			if(line.contentEquals("!opcode"))
                				tmpHeader.opcodePos = y;
                			else if(line.contentEquals("!length"))
                				tmpHeader.lengthPos = y;
                			else 
                			    tmpHeader.addStartByte(Integer.valueOf(line).byteValue(), y);
                		}
                		protocol.setHeader(tmpHeader);
                	}
                	section = -1;
                	break;
                }
                case 1:
                {
                	if(line.subSequence(0, line.indexOf('=')) == "withOpcode")
                	{
                		protocol.getHeader().lengthWithOpcode = Boolean.parseBoolean(line.substring(line.indexOf('=')+1, line.length()));
                	}
                	section = -1;
                	break;
                }
                case 2:
                {
                	protocol.getHeader().opcode = Integer.valueOf(line).byteValue();
                	section = -1;
                	break;
                }
                case 3:
                {
                	byte type = Integer.valueOf(line).byteValue();
                	Data tmpData = new Data();
        			tmpData.type = type;
                	switch(type)
                	{
                		case 1:
                		{
                			String s = null;
                			byte posItr = 0;
                			while(true)
                			{
                				line = readLine(fileBuff, fileLenght);
                				if(line == null)
                					break;
                				s = line.substring(0, line.indexOf('='));
                				if(s == "speedChars")
                					tmpData.speedChars = GetArray(s, (byte) 3);
                				else if(s == "pressKey")
                					tmpData.pressKey = (byte)line.substring(line.indexOf('=')+1).charAt(0);
                				else if(s == "releaseKey")
                					tmpData.releaseKey = (byte)line.substring(line.indexOf('=')+1).charAt(0);
                				else
                				{
                					if(line.contentEquals("!key"))
                						tmpData.keyPos = posItr++;
                					else if(line.contentEquals("!pressKey"))
                						tmpData.pressPos = posItr++;
                				}
                			}
                			break;
                			//TODO
                		}
                			
                	}
                	break;
                }
                	
            }
        }
    	
    	return null;
    }
    
    private byte[] GetArray(String line, byte length)
    {
    	byte[] array = new byte[length];
        char[] chars = line.toCharArray();
        int charsCount = chars.length;
        byte arrayItr = 0;
        
        String curDigit = "";
        boolean hex = false;
        for(int i = line.indexOf('[')+1; i < charsCount;++i)
        {
        	if(chars[i] == ']')
        		break;
        	if(chars[i] == 'x' || chars[i] == 'X')
        		hex = true;
        	
            if(chars[i] == ';')
            {
            	try
            	{
            		array[arrayItr] = Integer.valueOf(curDigit, hex ? 16 : 10).byteValue();
            	}
            	catch(NumberFormatException ex)
            	{
            		array[arrayItr] = (byte) curDigit.charAt(0);
            	}
            	++arrayItr;	
            	curDigit = "";
            	hex = false;
            	continue;
            }
        	curDigit += chars[i];
        }
        
        return array;
    }
    
    private String readLine(byte[] data, int lenght)
    {
    	if(lineEnd >= lenght)
    		return null;
    	
    	String line = "";
    	for(int i = lineEnd; i < lenght; ++i)
    	{
    		if((char)data[i] == '\r' || (char)data[i] == '\n')
    		{
    			if((char)data[i+1] == '\n')
    				++i;
    			lineEnd = i+1;	
    			break;
    		}
    		else if((char)data[i] == '/' && (char)data[i+1] == '/')
    		{
    			++i;
    			continue;
    		}
    		else if((char)data[i] == ' ')
    			continue;

    		line += (char)data[i];
    	}
    	return line;
    }
    
    public static ProtocolMgr getInstance()
    {
    	if(instance == null)
    		instance = new ProtocolMgr();
        return instance;
    }
    
    private static ProtocolMgr instance;
    private int lineEnd;
    private HashMap protocols;
};