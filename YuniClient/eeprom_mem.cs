/*
 * Created by SharpDevelop.
 * User: dv
 * Date: 1/31/2011
 * Time: 3:27 PM
 * 
 * To change this template use Tools | Options | Coding | Edit Standard Headers.
 */

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Text;
using System.IO;
using System.IO.Ports;

namespace YuniClient
{
    /// <summary>
    /// Description of eeprom_mem.
    /// </summary>
    public class eeprom_mem
    {
        public eeprom_mem() { buffer = new byte[512]; part = 1; }
        public void set(int index, byte val)
        {
            if(part == 2) index += 255;
            buffer[index] = val ;
        }
        public void set_nopart(int index, byte val){buffer[index] = val ;}
        
        public byte get(int index)
        {
            if(part == 2) index += 255;
            return buffer[index];
        }
        
        public byte[] getRec(int index)
        {
            if(part == 2) index += 255;
            byte[] rec = new byte[5];
            for(byte i = 0; i < 5; ++i)
                rec[i] = buffer[index+i];
            return rec;
        }
        public void clear()
        {
            for(short i = 0; i < 255; ++i)
                buffer[(part == 2) ? (i + 255) : i] = 0;
        }
         public void clear_all()
        {
            for(short i = 0; i < 512; ++i)
                buffer[i] = 0;
        }
        public short getTotalRecCount()
        {
            short count = 0;
            for(short i = 0; i < 510; i+=5)
            {
                if(buffer[i] != 0 && buffer[i+1] != 0)
                    ++count; 
            }
            return count;
        }
        public short getPartRecCount(bool firstPart)
        {
            short count = 0;
            short limit = (short) ((firstPart) ? 255 : 510);
            for(short i = (short) (firstPart ? 0 : 255); i < limit; i+=5)
            {
                if(buffer[i] != 0 && buffer[i+1] != 0)
                    ++count; 
            }
            return count;
        }
        public void toFile(String name)
        {
            if(!name.Contains(".dta"))
                name += ".dta";
           try
            {
                BinaryWriter dataFile = new BinaryWriter(File.Open(name, FileMode.OpenOrCreate));
                dataFile.Write(buffer);
                dataFile.Close();
                dataFile = null;
            }
            catch (Exception)
            {
            }
        }
        
        public void fromFile(String file)
        {
            try
            {
                BinaryReader dataFile = new BinaryReader(File.Open(file, FileMode.Open));
                buffer = dataFile.ReadBytes(512);
                dataFile.Close();
                dataFile = null;
            }
            catch (Exception)
            {
            }
        } 
        
        public void erase(int index)
        {
            if(part == 2) index += 255;
            byte[] tmp =  new byte[512];
            short y = 0;
            for(short i = 0; i < 512;++i)
            {
                if(i >= index && i < index+5)
                    continue;
                tmp[y] = buffer[i];
                ++y;
            }
            buffer = tmp;
        }
        
        public void insert(int index)
        {
            if(part == 2) index += 255;
            byte[] tmp =  new byte[512];
            short y = 0;
            for(short i = 0; i < 512;++i)
            {
                if(i >= index && i < index+5)
                {
                    tmp[i] = 0;
                    continue;
                }
                tmp[i] = buffer[y];
                ++y;
            }
            buffer = tmp;
        }
        public void SetPart(byte set_part) { part = set_part; }
        public byte GetPart() { return part; }
        private byte[] buffer;
        private byte part;
    }
}
