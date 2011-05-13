package com.yuniclient;

public class DeviceInfo
{
    DeviceInfo(String id)
    {
        mem_size = 0; page_size = 0; patch_pos = 0; name = null;
        if     (id.contentEquals("m16")) { name = "ATmega16";  mem_size = 16128; page_size = 128; }
        else if(id.contentEquals("m32")) { name = "ATmega32";  mem_size = 32256; page_size = 128; }
        else if(id.contentEquals("m48")) { name = "ATmega48";  mem_size = 3840;  page_size = 64; patch_pos = 3838; }
        else if(id.contentEquals("m88")) { name = "ATmega88";  mem_size = 7936;  page_size = 128; }
        else if(id.contentEquals("m168")){ name = "ATmega168"; mem_size = 16128; page_size = 128; }
        else if(id.contentEquals("m162")){ name = "ATmega162"; mem_size = 16128; page_size = 128; }
        /* FIXME: only 16-bit addresses are available */
        else if(id.contentEquals("m128")){ name = "ATmega128"; mem_size = 65536; page_size = 256; }
    }
    
    public boolean isSet() { return (mem_size != 0); }
    
    public String name;
    public int mem_size;
    public int page_size;
    public int patch_pos;
};
