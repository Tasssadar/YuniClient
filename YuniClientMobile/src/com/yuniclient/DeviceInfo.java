package com.yuniclient;

class DeviceInfo
{
    DeviceInfo(String id)
    {
        mem_size = 0; page_size = 0; patch_pos = 0;
        if     (id.contains("m48")) { mem_size = 3840;  page_size = 64; patch_pos = 3838; }
        else if(id.contains("m168")){ mem_size = 16128; page_size = 128; }
        else if(id.contains("m88")) { mem_size = 7936;  page_size = 128; }
        else if(id.contains("m16")) { mem_size = 16128; page_size = 128; }
        else if(id.contains("m32")) { mem_size = 32256; page_size = 128; }
        /* FIXME: only 16-bit addresses are available */
        else if(id.contains("m16")) { mem_size = 65536; page_size = 256; }
    }
    public boolean isSet() { return (mem_size != 0); }
    
    public int mem_size;
    public int page_size;
    public int patch_pos;
};