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
            EEPROM = new eeprom_mem();
            part = 1;
        }
        
        delegate void SetTextCallback(byte[] bytes, int lenght);
        
        char[] eeprom_read_seq = { '\x16' };
        char[] eeprom_write_seq  = { '\x1C' };
        char[] eeprom_write_stop_seq  = { '\x1E'};
        
        short state = 0;
        
        eeprom_mem EEPROM;
        int itr_buff;
        byte part;
        
        void EepromFormClosed(object sender, FormClosedEventArgs e)
        {
            this.Owner.Enabled = true;
            serialPort1.DataReceived -= SerialPort1DataReceived;
            Owner.Focus();
        }
        
        void Read_bClick(object sender, EventArgs e)
        {
            if(!serialPort1.IsOpen)
                return;
            serialPort1.Write(eeprom_read_seq, 0, 1);
            state |= (short)eeprom_states.STATE_READING;
        }
        
        void SerialPort1DataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            byte[] bytes = new byte[serialPort1.ReadBufferSize];
            serialPort1.Read(bytes, 0, serialPort1.ReadBufferSize);
            BotOuput(bytes, serialPort1.ReadBufferSize);
        }
        
        private void BotOuput(byte[] bytes, int lenght)
        {
            if (this.read_b.InvokeRequired)
            {
                try
                {
                    SetTextCallback d = new SetTextCallback(BotOuput);
                    this.Invoke(d, new object[] { bytes, lenght });
                }
                catch (Exception) { }
            }
            else
            {        
                if((state & (short)eeprom_states.STATE_READING) != 0)
                {
                    bool skip = (bytes[0] == '\x17');
                    if(skip)
                    {
                        itr_buff = 0;
                        EEPROM.clear();
                    }
                    for(int itr = 0; itr_buff < 512 && itr < lenght; ++itr)
                    {
                        if(skip && itr == 0)
                           continue;
                        EEPROM.set_nopart(itr_buff, bytes[itr]);
                        ++itr_buff;
                    }
                    if(itr_buff >= 512)
                    {
                         state &= System.Convert.ToInt16(~(eeprom_states.STATE_READING));
                         IntoList();
                         return;
                    }
                }
                else if((state & System.Convert.ToInt16(eeprom_states.STATE_WRITING)) != 0)
                {
                    if(bytes[0] != 0x1F)
                        return;
                            
                    if(EEPROM.GetPart() == 1 && itr_buff >= EEPROM.getPartRecCount(true)*5)
                    {
                        EEPROM.SetPart(2);
                        byte[] send = { 0x16 };
                        serialPort1.Write(send, 0, 1);
                        itr_buff = 0;
                        return;
                     }
                     else if(itr_buff >= 510 || (itr_buff >= EEPROM.getPartRecCount(false)*5 && itr_buff%5 == 0))
                     {
                         byte[] send = { 0x1E };
                         serialPort1.Write(send, 0, 1);
                         EEPROM.SetPart(part);
                         state &= (short)(~(eeprom_states.STATE_WRITING));
                         return;
                     }
                            
                     byte[] send_rec = EEPROM.getRec(itr_buff);
                     serialPort1.Write(send_rec, 0, 5);
                     itr_buff += 5;
                }
            }
        }

        public void IntoList()
        {
            List<string> _items = new List<string>();
            String item = null;
            for(int itr = 0; itr < 255;)
            {
                if(EEPROM.get(itr) == 0 && EEPROM.get(itr+1) == 0)
                    break;
                item = "";
                item = itr + " Key " + (char)EEPROM.get(itr) + " " + (char)EEPROM.get(itr+1) + " - ";
                switch(EEPROM.get(itr+2))
                {
                    case 0:
                        item += "NONE";
                        break;
                    case 1:
                        item += "TIME, " + (((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF)*10) + "ms";
                        break;
                    case 2:
                        item += "SENSOR_LEVEL_HIGHER, sensor " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF & EEPROM.get(itr+4));
                        break;
                    case 3:
                        item += "SENSOR_LEVEL_LOWER, sensor " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF & EEPROM.get(itr+4));
                        break;
                    case 4: 
                        item += "RANGE_HIGHER adr " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF & EEPROM.get(itr+4)) + "cm";
                        break;
                    case 5: 
                        item += "RANGE_LOWER adr " + (0xFF & EEPROM.get(itr+3)) + " val " + (0xFF &  EEPROM.get(itr+4)) + "cm";
                        break;
                    case 6:
                        item += "EVENT_DISTANCE " + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + " mm";
                        break;
                    case 7:
                        item += "EVENT_DISTANCE_LEFT " + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + " mm";
                        break;
                    case 8:
                        item += "EVENT_DISTANCE_RIGHT " + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + " mm";
                        break;
                    default:
                       item += "event " + EEPROM.get(itr+2) + " values " + (0xFF & EEPROM.get(itr+3)) + " " + (0xFF & EEPROM.get(itr+4)) + 
                           " (" + ((EEPROM.get(itr+3) << 8) | (EEPROM.get(itr+4)) & 0xFF) + ")";
                       break;
                }
                itr += 5;
                _items.Add(item);
            }
            recordsList.DataSource = null;
            recordsList.DataSource = _items;
        }
        void Edit_bClick(object sender, EventArgs e)
        {
            if(recordsList.SelectedItem == null)
                return;
            String indexS = recordsList.SelectedValue.ToString().Substring(0, recordsList.SelectedValue.ToString().IndexOf(" "));
            int index = System.Convert.ToInt32(indexS);
            id_t.Text = index.ToString();
            key_t.Text = ((char)EEPROM.get(index)).ToString();
            if(EEPROM.get(index+1) == 100)
                comboBox1.SelectedItem = "Down";
            else
                comboBox1.SelectedItem = "Up";
            endEvent.Text = EEPROM.get(index+2).ToString(); 
            byte1.Text = EEPROM.get(index+3).ToString(); 
            byte2.Text = EEPROM.get(index+4).ToString(); 
            time_t.Text = ((EEPROM.get(index+3) << 8) | (EEPROM.get(index+4)) & 0xFF).ToString();         
        }
        
        void Save_bClick(object sender, EventArgs e)
        {
            int index = 0;
            if(id_t.Text != "")
                index = System.Convert.ToInt32(id_t.Text);
            else if(behind_t.Text != "")
            {
                index = System.Convert.ToInt32(behind_t.Text)+5;
                EEPROM.insert(index);
            }
               EEPROM.set(index,   System.Convert.ToByte(key_t.Text.ToCharArray()[0]));
               if(comboBox1.SelectedItem == "Down")
                   EEPROM.set(index+1, 100);
               else
                   EEPROM.set(index+1, 117);
               EEPROM.set(index+2, System.Convert.ToByte(endEvent.Text));
               switch(EEPROM.get(index+2))
            {
                   case 0: // EVENT_NONE
                       EEPROM.set(index+3, 0);
                       EEPROM.set(index+4, 0);
                       break;
                case 2: // EVENT_SENSOR_LEVEL_HIGHER
                case 3: // EVENT_SENSOR_LEVEL_LOWER
                case 4: // EVENT_RANGE_HIGHER
                case 5: // EVENT_RANGE_LOWER
                    EEPROM.set(index+3,  System.Convert.ToByte(byte1.Text));
                    EEPROM.set(index+4,  System.Convert.ToByte(byte2.Text));
                    break;
                case 1: // EVENT_TIME
                case 6: // EVENT_DISTANCE 
                case 7: // EVENT_DISTANCE_LEFT
                case 8: // EVENT_DISTANCE_RIGHT
                default:
                    EEPROM.set(index+3,  (byte)(System.Convert.ToUInt32(time_t.Text) >> 8));
                    EEPROM.set(index+4,  (byte)(System.Convert.ToUInt32(time_t.Text) & 0xFF ));
                    break;
            }
               IntoList();
       }
       
        void Write_bClick(object sender, EventArgs e)
        {
            if(!serialPort1.IsOpen)
                return;
            read_b.Enabled = false;
            edit_b.Enabled = false;
            save_b.Enabled = false;
            write_b.Enabled = false;
            serialPort1.Write(eeprom_write_seq, 0, 1);
            state |= System.Convert.ToInt16(eeprom_states.STATE_WRITING);
            EEPROM.SetPart(1);
        }
        
        void Add_bClick(object sender, EventArgs e)
        {
            id_t.Text = "";
            key_t.Text = "";
            comboBox1.SelectedItem = "Down";
            time_t.Text = "";
            behind_t.Text = "";
        }
        
        void Erase_bClick(object sender, EventArgs e)
        {
            if(recordsList.SelectedItem == null)
                return;
            String indexS = recordsList.SelectedValue.ToString().Substring(0, recordsList.SelectedValue.ToString().IndexOf(" "));
            int index = System.Convert.ToInt32(indexS);
            EEPROM.erase(index);
            IntoList();
        }
        
        void EraseAllClick(object sender, EventArgs e)
        {
            EEPROM.clear();
            IntoList();
        }
        
        void Open_fileClick(object sender, EventArgs e)
        {
            openFileDialog1.ShowDialog();
        }
        
        void OpenFileDialog1FileOk(object sender, CancelEventArgs e)
        {
            EEPROM.clear_all();
            EEPROM.fromFile(openFileDialog1.FileName);
            IntoList();
        }
        
        void To_fileClick(object sender, EventArgs e)
        {
            saveFileDialog1.ShowDialog();
        }
        
        void SaveFileDialog1FileOk(object sender, CancelEventArgs e)
        {
            EEPROM.toFile(saveFileDialog1.FileName);
        }
        
        void Switch_partClick(object sender, EventArgs e)
        {
            if(part == 1) part = 2;
            else part = 1;
            EEPROM.SetPart(part);
            part_l.Text = "Part " + part.ToString();
            IntoList();
        }
    }
}
