/*
 * Created by SharpDevelop.
 * User: Tassadar
 * Date: 30.12.2010
 * Time: 16:03
 * 
 * To change this template use Tools | Options | Coding | Edit Standard Headers.
 */

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.IO;
using System.IO.Ports;

[Flags]
enum eeprom_states
{
    STATE_READING     = 0x01,
    STATE_WRITING     = 0x02,
    STATE_WRITING_WAIT= 0x04,
};

class Record
{
    public int id;
    public byte key;
    public byte downUp;
    public UInt16 time;
};


namespace YuniClient
{
    /// <summary>
    /// Description of eeprom.
    /// </summary>
    ///
    
    public partial class eeprom : Form
    {
        public eeprom(SerialPort port)
        {
            InitializeComponent();     
            serialPort1 = port;
            serialPort1.DataReceived += SerialPort1DataReceived;
            records = new List<Record>();
        }
        
        delegate void SetTextCallback(string text);
        
        char[] eeprom_read_seq = { '\x16' };
        char[] eeprom_write_seq  = { '\x1C' };
        char[] eeprom_write_stop_seq  = { '\x1E' };
        
        short state = 0;
        Record curRec;
        byte recItr = 0;
        List<Record> records;        
        
        void EepromFormClosed(object sender, FormClosedEventArgs e)
        {
            this.Owner.Enabled = true;
            serialPort1.DataReceived -= SerialPort1DataReceived;
            Owner.Focus();
        }
        
        void Read_bClick(object sender, EventArgs e)
        {
            serialPort1.Write(eeprom_read_seq, 0, 1);
        }
        
        void SerialPort1DataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            if((state & (short)eeprom_states.STATE_WRITING_WAIT) != 0)
                return;
        	string text = "";
            text = serialPort1.ReadExisting();
            BotOuput(text);
        }
        
        private void BotOuput(string text)
        {
            if (this.read_b.InvokeRequired)
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
                char[] bytes = text.ToCharArray();
                int itr = 0;
                if(bytes[itr] == '\x17')
                {
                    state |= System.Convert.ToInt16(eeprom_states.STATE_READING);
                    records.Clear();
                    recItr = 0;
                    curRec = new Record();
                    ++itr;
                }
                
                if((state & System.Convert.ToInt16(eeprom_states.STATE_READING)) != 0)
                {
                    for(; itr < text.Length; ++itr)
                    {
                        if(recItr == 0 && bytes[itr] == '\x18')
                        {
                            state &= System.Convert.ToInt16(~(eeprom_states.STATE_READING));
                            IntoList();
                            break;
                        }
                        switch(recItr)
                        {
                            case 0: curRec.key = System.Convert.ToByte(bytes[itr]);    break;
                            case 1: curRec.downUp = System.Convert.ToByte(bytes[itr]); break;
                            case 2: curRec.time = System.Convert.ToUInt16(System.Convert.ToByte(bytes[itr]) << 8); break;
                            case 3:
                                curRec.time |= System.Convert.ToUInt16(System.Convert.ToByte(bytes[itr]) & 0xFF);
                                curRec.id = records.Count;
                                records.Add(curRec);
                                curRec = new Record();
                                break;
                        }
                        if(recItr == 3)
                           recItr = 0;
                        else 
                            ++recItr;
                    }
                }
                else if((state & System.Convert.ToInt16(eeprom_states.STATE_WRITING)) != 0 && bytes[itr] == '\x1D')
                {
                    textBox1.Text += "Sending...";
                    byte[] buffer;
                    state |= (short)eeprom_states.STATE_WRITING_WAIT;
                    for(int i = 0; i < records.Count; ++i)
                    {
                        if(records[i].key == 0)
                            break;
                        buffer = new byte[1];
                        buffer[0] = records[i].key;
                        serialPort1.Write(buffer, 0, 1);
                        while(serialPort1.ReadByte() != 0x1F) { }
                        buffer[0] = records[i].downUp;
                        serialPort1.Write(buffer, 0, 1);
                        while(serialPort1.ReadByte() != 0x1F) { }
                        buffer[0] = (byte)(records[i].time >> 8);
                        serialPort1.Write(buffer, 0, 1);
                        while(serialPort1.ReadByte() != 0x1F) { }
                        buffer[0] = (byte)(records[i].time & 0xFF);
                        serialPort1.Write(buffer, 0, 1);
                        while(serialPort1.ReadByte() != 0x1F) { }
                    }
                    serialPort1.Write(eeprom_write_stop_seq, 0, 1);
                    textBox1.Text += "done\r\n";
                    read_b.Enabled = true;
                	edit_b.Enabled = true;
                	save_b.Enabled = true;
                	write_b.Enabled = true;
                	state &= (short)(~(eeprom_states.STATE_WRITING_WAIT));
                	state &= (short)(~(eeprom_states.STATE_WRITING));
                }
            }
        }
        
        void Parse_bClick(object sender, EventArgs e)
        {
            if(records.Count == 0 || (state & System.Convert.ToInt16(eeprom_states.STATE_READING)) != 0)
                return;
           IntoList();
        }
        public void IntoList()
        {
            List<string> _items = new List<string>();
            for(int i = 0; i < records.Count; ++i)
            {
                if(records[i].key == 0)
                    break;
                _items.Add(records[i].id + " Key \""+ System.Convert.ToChar(records[i].key).ToString() + "\" " + System.Convert.ToChar(records[i].downUp).ToString() + ", time " + (records[i].time*10).ToString()+ " ms");
            }
            recordsList.DataSource = null;
            recordsList.DataSource = _items;
        }
        Record GetRec(int id)
        {
            for(int i = 0; i < records.Count; ++i)
            {
                if(records[i].id == id)
                    return records[i];
            }
            return null;
        }
        void Edit_bClick(object sender, EventArgs e)
        {
            if(recordsList.SelectedItem == null)
                return;
            char[] text = recordsList.SelectedValue.ToString().ToCharArray();
            string id_st = "";
            for(int i = 0; text[i] != ' '; ++i)
                id_st += text[i];
            int id=System.Convert.ToInt32(id_st);
            Record rec = GetRec(id);
            if(rec == null)
                return;
            id_t.Text = id_st;
            key_t.Text = System.Convert.ToChar(rec.key).ToString();
            if(System.Convert.ToChar(rec.downUp) == 'd')
                comboBox1.SelectedItem = "Down";
            else
                comboBox1.SelectedItem = "Up";
            time_t.Text = (rec.time*10).ToString();
        }
        
        void Save_bClick(object sender, EventArgs e)
        {
            if(key_t.Text == "" || time_t.Text == "")
                return;
            
            if(id_t.Text != "")
            {
                Record rec = GetRec(System.Convert.ToInt32(id_t.Text));
                if(rec == null)
                    return;
                rec.key = (byte)(key_t.Text.ToCharArray()[0]);
                rec.time = (ushort)(System.Convert.ToUInt16(time_t.Text)/10);
                if(comboBox1.SelectedItem == null || comboBox1.SelectedItem == "Down")
                    rec.downUp = (byte)'d';
                else
                    rec.downUp = (byte)'u';
            }
            else
            {
                Record rec = new Record();  
                
                int behind = -1;
                if(behind_t.Text != "")
                {
                    behind = System.Convert.ToInt32(behind_t.Text);
                    rec = GetRec(behind+1);            
                }
                else
                {
                    for(int i = 0; i < records.Count; ++i)
                    {
                        if(records[i].key == 0 && records[i].time == 0)
                        {
                            rec = records[i];
                            break;
                        }
                    }
                }
                rec.key = (byte)(key_t.Text.ToCharArray()[0]);
                rec.time = (ushort)(System.Convert.ToUInt16(time_t.Text)/10);
                if(comboBox1.SelectedItem == null || comboBox1.SelectedItem == "Down")
                    rec.downUp = (byte)'d';
                else
                    rec.downUp = (byte)'u';
            }
            textBox1.Text += "Value saved\r\n";
             List<Record> records2 = records;
            IntoList();
        }
        
        void Write_bClick(object sender, EventArgs e)
        {
        	read_b.Enabled = false;
        	edit_b.Enabled = false;
        	save_b.Enabled = false;
        	write_b.Enabled = false;
        	serialPort1.Write(eeprom_write_seq, 0, 1);
        	textBox1.Text += "Erasing EEPROM...\r\n";
        	state |= System.Convert.ToInt16(eeprom_states.STATE_WRITING);
        }
        
        void Add_bClick(object sender, EventArgs e)
        {
        	id_t.Text = "";
            key_t.Text = "";
            comboBox1.SelectedItem = "Down";
            time_t.Text = "";
            behind_t.Text = "";
        }
    }
}
