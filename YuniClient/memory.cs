using System;
using System.Collections.Generic;
//using System.\;
using System.Text;
using System.IO;

namespace YuniClient
{
    class memory
    {
        public void SetDeviceInfo(chip_definition info) { deviceInfo = info; }
        public byte Get(int index) { return m_buffer[index]; }
        public int size() { return m_buffer.Count; }
        public int data() { return m_buffer.Count == 0 ? 0 : m_buffer[0]; }

        public bool Load(BinaryReader file)
        {
            if (deviceInfo.ToString() == "")
                return false;
            m_buffer = new List<byte>();
            m_buffer.Clear();
            int base_i = 0;
            List<byte> rec_nums = new List<byte>();
            file.BaseStream.Position = 0;
            for (int lineno = 1; ; ++lineno)
            {
                if (file.BaseStream.Position == file.BaseStream.Length)
                    break;
                char c = file.ReadChar();
                string line = "";
                if(c == System.Convert.ToChar(":"))
                {
                    line += c.ToString();
                    while (file.BaseStream.Position+1 != file.BaseStream.Length)
                    {
                        c = file.ReadChar();
                        if (c == System.Convert.ToChar("\r") && file.ReadChar() == System.Convert.ToChar("\n"))
                        {
                            c = '\n';
                            line += c.ToString();
                            break;
                        }
                        line += c.ToString();
                    }
                }
                if (line[0] != ':' || line.Length % 2 != 0)
                    return false;
                rec_nums.Clear();
                for (int i = 1; i+1 < line.Length;++i)
                {
                    string digit = line[i].ToString();
                    ++i;
                    digit += line[i];
                    byte res = byte.Parse(digit,System.Globalization.NumberStyles.HexNumber, null);
                    rec_nums.Add(res);    
                   // Form1.ActiveForm.Controls.Find("textBox1", true)[0].Text += "d"+digit +" " + res + "\r\n";
                }
                int length = rec_nums[0];
                int address = rec_nums[1] * 0x100 + rec_nums[2];
                int rectype = rec_nums[3];
                if (length != rec_nums.Count - 5)
                    return false;

                if (rectype == 2)
			    {
				    if (length != 2)
					    return false;
				    base_i = (rec_nums[4] * 0x100 + rec_nums[5]) * 16;
				    continue;
			    }

			    if (rectype == 1)
				    break;

			    if (rectype != 0)
				    return false;

			    for (int i = 0; i < length; ++i)
			    {
				    while (base_i + address + i >= m_buffer.Count)
                        m_buffer.Add(0xff);

                    if (m_buffer[base_i + address + i] != 0xff)
                        return false;

                    m_buffer[base_i + address + i] = rec_nums[i + 4];
			    }
            }
            return true;
        }

        public List<byte> m_buffer { get;set; }
        private int m_memsize;
        private int m_pagesize;
        private int m_patch_pos;
        private chip_definition deviceInfo;
    }
}

class Page
{
    public int address { get; set; }
    public List<byte> data { get; set; }
}