package com.yuniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class memory
{
    private static final byte ERROR_WRONG_LINE_FORMAT     = 1;
    private static final byte ERROR_INVALID_RECORD_LENGHT = 2;
    private static final byte ERROR_INVALID_2_RECORD      = 3;
    private static final byte ERROR_INVALID_RECORD_TYPE   = 4;
    private static final byte ERROR_MEMORY_LOCATION     = 5;
    
    public memory(DeviceInfo info)
    {
        deviceInfo = info;
        m_buffer = null;
        size = 0;
        pages = null;
    }
    
    public String Load(File filePath) throws IOException
    {
        if(filePath.length() >= Integer.MAX_VALUE)
             return "You really should not try to flash files bigger than 2 gigabytes."; 

        final FileInputStream file = new FileInputStream(filePath);

        final byte[] fileBuff = new byte[(int) filePath.length()];
        file.read(fileBuff);
        file.close();
        
        m_buffer = parseHexFile(fileBuff, deviceInfo.mem_size, (int) filePath.length());
        if(m_buffer == null)
            return "Corrupted hex file!";
        else if(m_buffer[1] == 0)
        {
            switch(m_buffer[0])
            {
                case ERROR_WRONG_LINE_FORMAT:
                    return "Wrong line format";
                case ERROR_INVALID_RECORD_LENGHT:
                    return "Invalid record length specified";
                case ERROR_INVALID_2_RECORD:
                    return "Invalid type 2 record";
                case ERROR_INVALID_RECORD_TYPE:
                    return "Invalid record type";
                case ERROR_MEMORY_LOCATION:
                    return "A memory location was defined twice";
                default:
                    return "Corrupted hex file!";
            }
        }
        for(size = deviceInfo.mem_size; m_buffer[size-1] == 0; --size) {};
        sendItr = 0;
        return null;
    }
    
    public String CreatePages()
    {
        pages = Collections.checkedList(new ArrayList<Page>(), Page.class);
        if (size > deviceInfo.mem_size)
            for (int a = deviceInfo.mem_size; a < size; ++a)
                if (m_buffer[a] != 0xff)
                    return "Program is too big!";
        int alt_entry_page = deviceInfo.patch_pos / deviceInfo.page_size;
        boolean add_alt_page = deviceInfo.patch_pos != 0;

        int i = 0;
        short pageItr = 0;
        Page cur_page = new Page();
        int page_size = deviceInfo.page_size;
        int stopGenerate = deviceInfo.mem_size / deviceInfo.page_size;
            
        for (boolean generate = true; generate && i < stopGenerate; ++i)
        {
            cur_page = new Page();
            cur_page.data = new byte[page_size];
            cur_page.address = i * page_size;
            pageItr = 0;
            if (size <= (i + 1) * page_size)
            {
                for (int y = 0; y < page_size; ++y)
                {
                    if (i * page_size + y < size)
                        cur_page.data[pageItr] = m_buffer[i * page_size + y];
                    else
                        cur_page.data[pageItr] = (byte) 0xff;
                    ++pageItr;
                }
                generate = false;
            }
            else
            {
                for (int y = i * page_size; y < (i + 1) * page_size; ++y)
                {
                    cur_page.data[pageItr] = m_buffer[y];
                    ++pageItr;
                }
            }

            if (!patch_page(cur_page, deviceInfo.patch_pos, deviceInfo.mem_size, pageItr))
                return "Failed patching page"; 
            pages.add(cur_page);

            if (i == alt_entry_page)
                add_alt_page = false;
        }
        if (add_alt_page)
        {
            for (int y = 0; y < page_size; ++y)
                cur_page.data[y] = (byte)0xff;
            cur_page.address = alt_entry_page * page_size;
            patch_page(cur_page, deviceInfo.patch_pos, deviceInfo.mem_size, pageItr);
            pages.add(cur_page);
        }
        return null;
    }
    
    private boolean patch_page(Page page, int patch_pos, int boot_reset, short page_pos)
    {
        if (patch_pos == 0)
            return true;

        if (page.address == 0)
        {
            int entrypt_jmp = (boot_reset / 2 - 1) | 0xc000;
            if((entrypt_jmp & 0xf000) != 0xc000)
                return false;
            page.data[0] = (byte) entrypt_jmp;
            page.data[1] = (byte) (entrypt_jmp >> 8);
            return true;
        }

        if (page.address > patch_pos || page.address + page_pos <= patch_pos)
            return true;

        int new_patch_pos = patch_pos - page.address;

        if (page.data[new_patch_pos] != (byte)0xff || page.data[new_patch_pos + 1] != (byte)0xff)
            return false;

        int entrypt_jmp2 = m_buffer[0] | (m_buffer[1] << 8);
        if ((entrypt_jmp2 & 0xf000) != 0xc000)
            return false;

        int entry_addr = (entrypt_jmp2 & 0x0fff) + 1;
        entrypt_jmp2 = ((entry_addr - patch_pos / 2 - 1) & 0xfff) | 0xc000;
        page.data[new_patch_pos] = (byte) entrypt_jmp2;
        page.data[new_patch_pos + 1] =  (byte) (entrypt_jmp2 >> 8);
        return true;
    }
    
    public short pagesCount() { return (short)pages.size(); }
    public Page getNextPage()
    {
        if(sendItr >= pages.size())
            return null;
        return pages.get(sendItr++);
    }
    
    private native byte[] parseHexFile(byte[] file, int memsize, int fileLenght);

    private byte[] m_buffer;
    private int size;
    private static DeviceInfo deviceInfo;
    private List<Page> pages;
    private short sendItr;
};

class Page
{
    public int address;
    public byte[] data;
};

class HexFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        File file = new File(dir, name);
        return (name.endsWith(".hex") || file.isDirectory());
    }
}
