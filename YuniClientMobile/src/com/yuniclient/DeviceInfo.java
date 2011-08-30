package com.yuniclient;

public class DeviceInfo
{
    DeviceInfo(String id)
    {
        mem_size = 0; page_size = 0; patch_pos = 0; name = null;
        
        if     (id.contentEquals("m16")) { name = "ATmega16";    mem_size = 16128; page_size = 128; eeprom_size = 512; }
        else if(id.contentEquals("m32")) { name = "ATmega32";    mem_size = 32256; page_size = 128; eeprom_size = 1024;}
        else if(id.contentEquals("m48")) { name = "ATmega48";    mem_size = 3840;  page_size = 64; patch_pos = 3838; eeprom_size = 256; }
        else if(id.contentEquals("m88")) { name = "ATmega88";    mem_size = 7936;  page_size = 64;  eeprom_size = 512; }
        else if(id.contentEquals("m168")){ name = "ATmega168";   mem_size = 16128; page_size = 128; eeprom_size = 512; }
        else if(id.contentEquals("m162")){ name = "ATmega162";   mem_size = 16128; page_size = 128; eeprom_size = 512; }
        else if(id.contentEquals("m328")){ name = "ATmega328P";  mem_size = 32256; page_size = 128; eeprom_size = 1024;}
        else if(id.contentEquals("m8u2")){ name = "ATmega8u2";   mem_size = 7680;  page_size = 128; eeprom_size = 512; }
        /* FIXME: only 16-bit addresses are available */
        else if(id.contentEquals("m128")){ name = "ATmega128";   mem_size = 65536; page_size = 256; eeprom_size = 4096;}
        /* FIXME: only 16-bit addresses are available and too long chip name */
        else if(id.contentEquals("p128")){ name = "ATmega1284P"; mem_size = 65536; page_size = 256; eeprom_size = 4096;}
    }
    
    public boolean isSet() { return (mem_size != 0); }
    
    public String name;
    public int mem_size;
    public int page_size;
    public int patch_pos;
    public int eeprom_size;
};
