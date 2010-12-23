using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.IO;
using System.IO.Ports;
using System.Threading;
using System.Diagnostics;

[Flags]
enum Status
{
    STATE_CONNECTED        = 0x01,
    STATE_FLASH_MODE       = 0x02,
    STATE_WAITING_STOP     = 0x04,
    STATE_WAITING_FLASH    = 0x08,
    STATE_WAITING_ID_FLASH = 0x10,
    STATE_WAITING_ID       = 0x20,
    STATE_WAITING_STOP2    = 0x40,
    STATE_HAS_FILE         = 0x80,
};

namespace YuniClient
{
    public partial class Form1 : Form
    {
        delegate void SetTextCallback(string text);

        char[] flash_mode_sequence = { '\x74', '\x7E', '\x7A', '\x33' };
        char[] device_id_seq = { '\x12' };
        char[] start_seq = { '\x11' };
        //char[] flash_seq = { '\x10' };

        public Form1()
        {
            InitializeComponent();
            version.Text = "6";
        }

        Status state = 0;
        BinaryReader hexFile;
        static BackgroundWorker _bw;
        string deviceId = "";
        string lastKey = "";
        chip_definition deviceInfo;
        memory mem;

        private void connect_Click_1(object sender, EventArgs e)
        {
            if (System.Convert.ToInt32(state & Status.STATE_CONNECTED) == 0)
            {
                textBox1.Text += "Opening port " + portName.Text + "...";
                _bw = new BackgroundWorker();
                _bw.WorkerReportsProgress = true;
                _bw.WorkerSupportsCancellation = true;
                ConnectParam con_info = new ConnectParam();
                con_info.port = portName.Text;
                con_info.rate_val = System.Convert.ToInt32(rate.Text);
                con_info.serialPort = serialPort1;
    
                _bw.DoWork += connect_v;
                _bw.RunWorkerCompleted += bw_connect_complete;
                _bw.RunWorkerAsync(con_info);
                connect.Enabled = false;
                load_b.Enabled = true;
                portName.Enabled = false;
                rate.Enabled = false;
            }
            else
            {
                textBox1.Text += "Closing port " + portName.Text + "...";
                serialPort1.Close();
                state = (state & Status.STATE_HAS_FILE) != 0 ? Status.STATE_HAS_FILE : 0;
                connect.Text = "Connect";
                textBox1.Text += "done\r\n";
                state_b.Enabled = false;
                hackStop.Visible = false;
                load_b.Enabled = false;
                flash.Enabled = false;
                portName.Enabled = true;
                rate.Enabled = true;
                state_b.Text = "Stop";
                deviceId = "";
            }
        }
        static void connect_v(object sender, DoWorkEventArgs e)
        {
            e.Result = "good";
            try
            {
                ConnectParam con_info = e.Argument as ConnectParam;
                con_info.serialPort.BaudRate = con_info.rate_val;
                con_info.serialPort.PortName = con_info.port;
                con_info.serialPort.Open();
                int i = 0;
                for (; !con_info.serialPort.IsOpen && i < 500; ++i)
                    Thread.Sleep(10);
                if (i >= 500)
                    e.Result = "Timeout";
            }
            catch (Exception ex)
            {
                e.Result = ex.Message.ToString();
            }
        }
        private void bw_connect_complete(object sender, RunWorkerCompletedEventArgs e)
        {
            if (e.Result != "good")
            {
                textBox1.Text += "failed(" + e.Result.ToString() + ")!\r\n";
                connect.Enabled = true;
                portName.Enabled = true;
                rate.Enabled = true;
            }
            else
            {
                state |= Status.STATE_CONNECTED;
                connect.Text = "Disconnect";
                textBox1.Text += "done \r\n";
                state_b.Enabled = true;
                connect.Enabled = true;
            }
        }

        private void state_b_Click(object sender, EventArgs e)
        {
            if (System.Convert.ToInt32(state & Status.STATE_CONNECTED) == 0)
            {
                state_b.Enabled = false;
                return;
            }

            if (System.Convert.ToInt32(state & Status.STATE_FLASH_MODE) == 0)
            {
                state |= Status.STATE_WAITING_STOP2;
                serialPort1.Write(flash_mode_sequence, 0, 4);
                state_b.Enabled = false;
                state_b.Text = "Switching..";
                hackStop.Visible = true;
                int retryCount = 0;
                while (retryCount < 100)
                {
                    string s = serialPort1.ReadExisting();
                    if (s.Length == 1 && System.Convert.ToChar(s) == System.Convert.ToChar(20))
                        break;
                    Thread.Sleep(10);
                    ++retryCount;
                }
                state &= ~(Status.STATE_WAITING_STOP2);
                state |= Status.STATE_WAITING_STOP;
                serialPort1.Write(flash_mode_sequence, 0, 4);
            }
            else
            {
                serialPort1.Write(start_seq, 0, 1);
                state &= ~(Status.STATE_FLASH_MODE);
                state_b.Text = "Stop";
                flash.Enabled = false;
            }
        }

        private void serialPort1_DataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            if (progressBar1.Visible || System.Convert.ToInt32(state & Status.STATE_WAITING_STOP2) != 0)
                return;
            string text = "";
            text = serialPort1.ReadExisting();
            BotOuput(text);
        }
        private void BotOuput(string text)
        {
            if (this.botOut.InvokeRequired)
            {
                try
                {
                    SetTextCallback d = new SetTextCallback(BotOuput);
                    this.Invoke(d, new object[] { text });
                }
                catch (Exception) { }
            }
            else
            {
                if (System.Convert.ToInt32(state & Status.STATE_WAITING_STOP) != 0 && text.Length == 1 &&
                    System.Convert.ToChar(text) == System.Convert.ToChar(20))
                {
                    state &= ~(Status.STATE_WAITING_STOP);
                    state |= Status.STATE_FLASH_MODE;
                    state_b.Text = "Start";
                    state_b.Enabled = true;
                    hackStop.Visible = false;
                    flash.Enabled = true;
                }
                else if (System.Convert.ToInt32(state & Status.STATE_WAITING_ID_FLASH) != 0)
                {
                    state &= ~(Status.STATE_WAITING_ID_FLASH);
                    deviceId = text;
                    if (ReadDeviceId())
                        flashChip();
                    else
                    {
                        textBox1.Text += "failed!\r\n";
                        load_b.Enabled = true;
                        state_b.Enabled = true;
                        flash.Enabled = true;
                        hexFile.Close();
                    }
                }
                else if (System.Convert.ToInt32(state & Status.STATE_WAITING_ID) != 0)
                {
                    state &= ~(Status.STATE_WAITING_ID);
                    deviceId = text;
                    if (!ReadDeviceId())
                        textBox1.Text += "failed!\r\n";
                    else
                        textBox1.Text += "done\r\n";
                }
                else
                    this.botOut.Text += text;
            }
        }

        private void hackStop_Click(object sender, EventArgs e)
        {
            if (System.Convert.ToInt32(state & Status.STATE_FLASH_MODE) == 0)
            {
                serialPort1.Write(flash_mode_sequence, 0, 4);
                state |= Status.STATE_FLASH_MODE;
                state |= Status.STATE_WAITING_STOP;
            }
            else
            {
                state &= ~(Status.STATE_WAITING_STOP);
                state_b.Text = "Start";
                state_b.Enabled = true;
                hackStop.Visible = false;
                flash.Enabled = true;
            }
        }

        private void load_b_Click(object sender, EventArgs e)
        {
            openFileDialog1.ShowDialog();
        }

        private void openFileDialog1_FileOk(object sender, CancelEventArgs e)
        {
            textBox1.Text += "Loading hex file...";
            bool succes = true;
            try
            {
                hexFile = new BinaryReader(File.Open(openFileDialog1.FileName, FileMode.Open));
                hexFile.Close();
            }
            catch (Exception)
            {
                succes = false;
            }
            if (succes)
            {
                state |= Status.STATE_HAS_FILE;
                filename.Text = openFileDialog1.FileName;
                textBox1.Text += "done\r\n";

            }
            else
                textBox1.Text += "failed\r\n";
        }

        private bool ReadDeviceId()
        {
            chip_definition[] defs = new chip_definition[6];
            for(byte i = 0; i < 6; ++i) defs[i] = new chip_definition();
            defs[0].chip_id = "m48";  defs[0].chip_name = "atmega48";  defs[0].memory_size = 3840;  defs[0].page_size = 64;  defs[0].patch_pos = 3838;
            defs[1].chip_id = "m168"; defs[1].chip_name = "atmega168"; defs[1].memory_size = 16128; defs[1].page_size = 128; defs[1].patch_pos = 0;
            defs[2].chip_id = "m88";  defs[2].chip_name = "atmega88";  defs[2].memory_size = 7936;  defs[2].page_size = 128; defs[2].patch_pos = 0;
            defs[3].chip_id = "m16";  defs[3].chip_name = "atmega16";  defs[3].memory_size = 16128; defs[3].page_size = 128; defs[3].patch_pos = 0;
            defs[4].chip_id = "m32";  defs[4].chip_name = "atmega32";  defs[4].memory_size = 32256; defs[4].page_size = 128; defs[4].patch_pos = 0;
            /* FIXME: only 16-bit addresses are available */
            defs[5].chip_id = "m128"; defs[5].chip_name = "atmega128"; defs[5].memory_size = 65536; defs[5].page_size = 256; defs[5].patch_pos = 0;

            foreach (chip_definition y in defs)
            {
                if (y.chip_id == deviceId)
                {
                    deviceInfo = y;
                    device_label.Text = y.chip_name;
                    return true;
                }
            }
            return false;
        }

        private void flash_Click(object sender, EventArgs e)
        {
            textBox1.Text += "Starting flash...";
            if (System.Convert.ToInt32(state & Status.STATE_HAS_FILE) != 0)
            {
                hexFile = new BinaryReader(File.Open(filename.Text, FileMode.Open));
                load_b.Enabled = false;
                state_b.Enabled = false;
                flash.Enabled = false;
                connect.Enabled = false;
                if (deviceId == "")
                {
                    textBox1.Text += "\r\nReading device ID...";
                    state |= Status.STATE_WAITING_ID_FLASH;
                    serialPort1.Write(device_id_seq, 0, 1);
                }
                else
                    flashChip();
            }
            else
                textBox1.Text += "no file!\r\n";
        }
        public void flashChip()
        {
            textBox1.Text += deviceInfo.chip_name + "\r\n";
            textBox1.Text += "Converting hex file...";
            mem = new memory();
            mem.SetDeviceInfo(deviceInfo);
            if (!mem.Load(hexFile))
            {
                textBox1.Text += "failed!\r\n";
                load_b.Enabled = true;
                state_b.Enabled = true;
                flash.Enabled = true;
                connect.Enabled = true;
                hexFile.Close();
                return;
            }
            else
                textBox1.Text += "done\r\n";
            hexFile.Close();
            textBox1.Text += "Creating pages...";
            List<Page> pages = new List<Page>();
            if (!CreatePages(mem, pages))
                return;
            textBox1.Text += "done!\r\n";
            textBox1.Text += "Flashing to device...";
            _bw = new BackgroundWorker();
            _bw.WorkerReportsProgress = true;
            _bw.WorkerSupportsCancellation = true;
            ConnectParam con_info = new ConnectParam();
            con_info.port = portName.Text;
            con_info.rate_val = System.Convert.ToInt32(rate.Text);
            con_info.serialPort = serialPort1;
            con_info.pages = pages;
            progressBar1.Value = 0;
            progressBar1.Visible = true;
            percentL.Visible = true;
            filename.Visible = false;
            _bw.DoWork += flash_v;
            _bw.RunWorkerCompleted += bw_flash_complete;
            _bw.ProgressChanged += bw_flash_progress;
            _bw.RunWorkerAsync(con_info);
        }
        static void flash_v(object sender, DoWorkEventArgs e)
        {
            e.Result = "good";
            try
            {
                ConnectParam con_info = e.Argument as ConnectParam;
                Debug.Assert(con_info.pages.Count > 0);
                for (int y = 0; y < con_info.pages.Count; ++y)
                {
                    char[] flash_seq = { '\x10' };
                    con_info.serialPort.Write(flash_seq, 0, 1);
                    // second value must be 128 or 0, wish c# would allow overflow...
                    int adress_sec = con_info.pages[y].address;
                    while (adress_sec > 255)
                        adress_sec -= 256;
                    byte[] adress = { System.Convert.ToByte(con_info.pages[y].address >> 8), System.Convert.ToByte(adress_sec) };
                    con_info.serialPort.Write(adress, 0, 2);
                    byte[] data = new byte[con_info.pages[y].data.Count];
                    for (int i = 0; i < con_info.pages[y].data.Count; ++i)
                        data[i] = con_info.pages[y].data[i];
                    con_info.serialPort.Write(data, 0, con_info.pages[y].data.Count);

                    int timeout = 0;
                    while (timeout < 100000)
                    {
                        string ins = "";
                        try { ins = con_info.serialPort.ReadExisting(); }
                        catch (Exception) { }
                        if (ins != "")
                        {
                            char c = System.Convert.ToChar(ins);
                            if (c == System.Convert.ToChar(20))
                                break;
                        }
                        ++timeout;
                    }
                    if (timeout >= 100000)
                    {
                        e.Result = "timeout";
                        return;
                    }
                    double a = System.Convert.ToDouble(con_info.pages.Count) / 100;
                    _bw.ReportProgress(System.Convert.ToInt32(System.Convert.ToDouble(y + 1) / (a != 0 ? a : 1)));
                }
            }
            catch (Exception ex)
            {
                e.Result = ex.Message.ToString();
            }
        }
        private void bw_flash_complete(object sender, RunWorkerCompletedEventArgs e)
        {
            load_b.Enabled = true;
            state_b.Enabled = true;
            flash.Enabled = true;
            progressBar1.Visible = false;
            filename.Visible = true;
            percentL.Visible = false;
            connect.Enabled = true;
            if (e.Result != "good")
                textBox1.Text += "failed(" + e.Result.ToString() + ")!\r\n";
            else
                textBox1.Text += "done \r\n";
        }
        private void bw_flash_progress(object sender, ProgressChangedEventArgs e)
        {
            progressBar1.Value = e.ProgressPercentage;
            percentL.Text = e.ProgressPercentage.ToString() + " %";
        }

        private bool CreatePages(memory mem, List<Page> output)
        {
            if (mem.size() > deviceInfo.memory_size)
                for (int a = deviceInfo.memory_size; a < mem.size(); ++a)
                    if (mem.Get(a) != 0xff)
                    {
                        textBox1.Text += "Failed, program is too big!\r\n";
                        return false;
                    }

            int alt_entry_page = deviceInfo.patch_pos / deviceInfo.page_size;
            bool add_alt_page = deviceInfo.patch_pos != 0;

            int i = 0;
            Page cur_page = new Page();
            for (bool generate = true; generate && i < deviceInfo.memory_size / deviceInfo.page_size; ++i)
            {
                cur_page = new Page();
                cur_page.data = new List<byte>();
                cur_page.data.Capacity = deviceInfo.page_size;
                cur_page.address = i * deviceInfo.page_size;
                if (mem.size() <= (i + 1) * deviceInfo.page_size)
                {
                    for (int y = 0; y < cur_page.data.Capacity; ++y)
                    {
                        if (i * deviceInfo.page_size + y < mem.size())
                            cur_page.data.Add(mem.Get(i * deviceInfo.page_size + y));
                        else
                            cur_page.data.Add(0xff);
                    }
                    generate = false;
                }
                else
                {
                    for (int y = i * deviceInfo.page_size; y < (i + 1) * deviceInfo.page_size; ++y)
                    {
                        cur_page.data.Add(mem.Get(y));
                    }
                }

                if (!patch_page(mem, cur_page, deviceInfo.patch_pos, deviceInfo.memory_size))
                {
                    textBox1.Text += "Patch failed!\r\n";
                    return false;
                }
                output.Add(cur_page);

                if (i == alt_entry_page)
                    add_alt_page = false;
            }
            if (add_alt_page)
            {
                for (int y = 0; y < cur_page.data.Capacity; ++y)
                    cur_page.data[y] = 0xff;
                cur_page.address = alt_entry_page * deviceInfo.page_size;
                patch_page(mem, cur_page, deviceInfo.patch_pos, deviceInfo.memory_size);
                output.Add(cur_page);
            }
            return true;
        }

        private bool patch_page(memory mem, Page page, int patch_pos, int boot_reset)
        {
            if (patch_pos == 0)
                return true;
            if (page.address == 0)
            {
                int entrypt_jmp = (boot_reset / 2 - 1) | 0xc000;
                Debug.Assert((entrypt_jmp & 0xf000) == 0xc000);
                page.data[0] = System.Convert.ToByte(entrypt_jmp);
                page.data[1] = System.Convert.ToByte(entrypt_jmp >> 8);
                return true;
            }

            if (page.address > patch_pos || page.address + page.data.Count <= patch_pos)
                return true;

            int new_patch_pos = patch_pos - page.address;

            if (page.data[new_patch_pos] != 0xff || page.data[new_patch_pos + 1] != 0xff)
                return false;

            int entrypt_jmp2 = mem.Get(0) | (mem.Get(1) << 8);
            if ((entrypt_jmp2 & 0xf000) != 0xc000)
                return false;

            int entry_addr = (entrypt_jmp2 & 0x0fff) + 1;
            entrypt_jmp2 = ((entry_addr - patch_pos / 2 - 1) & 0xfff) | 0xc000;
            page.data[new_patch_pos] = System.Convert.ToByte(entrypt_jmp2);
            page.data[new_patch_pos + 1] = System.Convert.ToByte(entrypt_jmp2 >> 8);
            return true;
        }


        private void device_label_Click(object sender, EventArgs e)
        {
            if (System.Convert.ToInt32(state & (Status.STATE_WAITING_ID | Status.STATE_WAITING_ID_FLASH | Status.STATE_WAITING_STOP | Status.STATE_WAITING_FLASH)) != 0 ||
                (System.Convert.ToInt32(state & Status.STATE_CONNECTED) == 0) || (System.Convert.ToInt32(state & Status.STATE_FLASH_MODE) == 0))
                return;
            textBox1.Text += "Reading device ID...";
            state |= Status.STATE_WAITING_ID;
            serialPort1.Write(device_id_seq, 0, 1);
        }

        private void Clear_b_Click(object sender, EventArgs e)
        {
            textBox1.Text = "";
            botOut.Text = "";
        }

        private void SendKey(string key, bool down)
        {
            char[] text = new char[key.Length + 3];
            for (int i = 0; i < key.Length; ++i)
                text[i] = key[i];
            text[key.Length] = ' ';
            text[key.Length + 1] = '-';
            if (down)
                text[key.Length + 2] = 'd';
            else
                text[key.Length + 2] = 'u';
            serialPort1.Write(text, 0, key.Length + 3);
        }
        private void botOut_KeyDown(object sender, KeyEventArgs e)
        {
            if (System.Convert.ToInt32(state & (Status.STATE_WAITING_ID | Status.STATE_WAITING_ID_FLASH | Status.STATE_WAITING_STOP | Status.STATE_WAITING_STOP2 | Status.STATE_WAITING_FLASH | Status.STATE_FLASH_MODE)) != 0 ||
                (System.Convert.ToInt32(state & Status.STATE_CONNECTED) == 0))
                return;
            if (e.KeyCode.ToString() == lastKey)
                return;
            lastKey = e.KeyCode.ToString();
            SendKey(e.KeyCode.ToString(), true);
        }

        private void botOut_KeyUp(object sender, KeyEventArgs e)
        {
            if (System.Convert.ToInt32(state & (Status.STATE_WAITING_ID | Status.STATE_WAITING_ID_FLASH | Status.STATE_WAITING_STOP | Status.STATE_WAITING_STOP2 | Status.STATE_WAITING_FLASH | Status.STATE_FLASH_MODE)) != 0 ||
                (System.Convert.ToInt32(state & Status.STATE_CONNECTED) == 0))
                return;
            lastKey = "";
            SendKey(e.KeyCode.ToString(), false);
        }

        //Autoscroll
        private void botOut_TextChanged(object sender, EventArgs e)
        {
            botOut.SelectionStart = botOut.Text.Length;
            botOut.ScrollToCaret();
            botOut.Refresh();
        }

        private void textBox1_TextChanged(object sender, EventArgs e)
        {
            textBox1.SelectionStart = textBox1.Text.Length;
            textBox1.ScrollToCaret();
            textBox1.Refresh();
        }
    }
}

class ConnectParam
{
    public string port;
    public int rate_val;
    public SerialPort serialPort;
    public List<Page> pages;
}

class chip_definition
{
    public string chip_id;
    public string chip_name;
    public int memory_size;
    public int page_size;
    public int patch_pos;
}
