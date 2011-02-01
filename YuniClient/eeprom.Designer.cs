﻿/*
 * Created by SharpDevelop.
 * User: Tassadar
 * Date: 30.12.2010
 * Time: 16:03
 * 
 * To change this template use Tools | Options | Coding | Edit Standard Headers.
 */
namespace YuniClient
{
    partial class eeprom
    {
        /// <summary>
        /// Designer variable used to keep track of non-visual components.
        /// </summary>
        private System.ComponentModel.IContainer components = null;
        
        /// <summary>
        /// Disposes resources used by the form.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing) {
                if (components != null) {
                    components.Dispose();
                }
            }
            base.Dispose(disposing);
        }
        
        /// <summary>
        /// This method is required for Windows Forms designer support.
        /// Do not change the method contents inside the source code editor. The Forms designer might
        /// not be able to load this method if it was changed manually.
        /// </summary>
        private void InitializeComponent()
        {
        	this.components = new System.ComponentModel.Container();
        	System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(eeprom));
        	this.read_b = new System.Windows.Forms.Button();
        	this.write_b = new System.Windows.Forms.Button();
        	this.serialPort1 = new System.IO.Ports.SerialPort(this.components);
        	this.recordsList = new System.Windows.Forms.ListBox();
        	this.edit_b = new System.Windows.Forms.Button();
        	this.id_t = new System.Windows.Forms.TextBox();
        	this.key_t = new System.Windows.Forms.TextBox();
        	this.comboBox1 = new System.Windows.Forms.ComboBox();
        	this.label1 = new System.Windows.Forms.Label();
        	this.label2 = new System.Windows.Forms.Label();
        	this.label3 = new System.Windows.Forms.Label();
        	this.label4 = new System.Windows.Forms.Label();
        	this.time_t = new System.Windows.Forms.TextBox();
        	this.save_b = new System.Windows.Forms.Button();
        	this.Add_b = new System.Windows.Forms.Button();
        	this.erase_b = new System.Windows.Forms.Button();
        	this.behind_t = new System.Windows.Forms.TextBox();
        	this.label6 = new System.Windows.Forms.Label();
        	this.label7 = new System.Windows.Forms.Label();
        	this.label5 = new System.Windows.Forms.Label();
        	this.byte1 = new System.Windows.Forms.TextBox();
        	this.byte2 = new System.Windows.Forms.TextBox();
        	this.endEvent = new System.Windows.Forms.TextBox();
        	this.label8 = new System.Windows.Forms.Label();
        	this.eraseAll = new System.Windows.Forms.Button();
        	this.openFileDialog1 = new System.Windows.Forms.OpenFileDialog();
        	this.open_file = new System.Windows.Forms.Button();
        	this.to_file = new System.Windows.Forms.Button();
        	this.saveFileDialog1 = new System.Windows.Forms.SaveFileDialog();
        	this.switch_part = new System.Windows.Forms.Button();
        	this.part_l = new System.Windows.Forms.Label();
        	this.SuspendLayout();
        	// 
        	// read_b
        	// 
        	this.read_b.Location = new System.Drawing.Point(12, 12);
        	this.read_b.Name = "read_b";
        	this.read_b.Size = new System.Drawing.Size(105, 26);
        	this.read_b.TabIndex = 0;
        	this.read_b.Text = "Read";
        	this.read_b.UseVisualStyleBackColor = true;
        	this.read_b.Click += new System.EventHandler(this.Read_bClick);
        	// 
        	// write_b
        	// 
        	this.write_b.Location = new System.Drawing.Point(123, 12);
        	this.write_b.Name = "write_b";
        	this.write_b.Size = new System.Drawing.Size(105, 26);
        	this.write_b.TabIndex = 1;
        	this.write_b.Text = "Write";
        	this.write_b.UseVisualStyleBackColor = true;
        	this.write_b.Click += new System.EventHandler(this.Write_bClick);
        	// 
        	// serialPort1
        	// 
        	this.serialPort1.DataReceived += new System.IO.Ports.SerialDataReceivedEventHandler(this.SerialPort1DataReceived);
        	// 
        	// recordsList
        	// 
        	this.recordsList.Font = new System.Drawing.Font("Lucida Console", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(238)));
        	this.recordsList.FormattingEnabled = true;
        	this.recordsList.HorizontalScrollbar = true;
        	this.recordsList.ItemHeight = 11;
        	this.recordsList.Location = new System.Drawing.Point(12, 44);
        	this.recordsList.Name = "recordsList";
        	this.recordsList.Size = new System.Drawing.Size(425, 301);
        	this.recordsList.TabIndex = 4;
        	// 
        	// edit_b
        	// 
        	this.edit_b.Location = new System.Drawing.Point(315, 16);
        	this.edit_b.Name = "edit_b";
        	this.edit_b.Size = new System.Drawing.Size(75, 22);
        	this.edit_b.TabIndex = 5;
        	this.edit_b.Text = "Edit";
        	this.edit_b.UseVisualStyleBackColor = true;
        	this.edit_b.Click += new System.EventHandler(this.Edit_bClick);
        	// 
        	// id_t
        	// 
        	this.id_t.Enabled = false;
        	this.id_t.Location = new System.Drawing.Point(526, 41);
        	this.id_t.Name = "id_t";
        	this.id_t.ReadOnly = true;
        	this.id_t.Size = new System.Drawing.Size(75, 20);
        	this.id_t.TabIndex = 6;
        	// 
        	// key_t
        	// 
        	this.key_t.Location = new System.Drawing.Point(526, 67);
        	this.key_t.MaxLength = 1;
        	this.key_t.Name = "key_t";
        	this.key_t.Size = new System.Drawing.Size(75, 20);
        	this.key_t.TabIndex = 7;
        	// 
        	// comboBox1
        	// 
        	this.comboBox1.FormattingEnabled = true;
        	this.comboBox1.Items.AddRange(new object[] {
        	        	        	"Down",
        	        	        	"Up"});
        	this.comboBox1.Location = new System.Drawing.Point(526, 93);
        	this.comboBox1.Name = "comboBox1";
        	this.comboBox1.Size = new System.Drawing.Size(75, 21);
        	this.comboBox1.TabIndex = 8;
        	this.comboBox1.Text = "Down";
        	// 
        	// label1
        	// 
        	this.label1.Location = new System.Drawing.Point(458, 44);
        	this.label1.Name = "label1";
        	this.label1.Size = new System.Drawing.Size(62, 17);
        	this.label1.TabIndex = 9;
        	this.label1.Text = "ID";
        	// 
        	// label2
        	// 
        	this.label2.Location = new System.Drawing.Point(458, 70);
        	this.label2.Name = "label2";
        	this.label2.Size = new System.Drawing.Size(62, 17);
        	this.label2.TabIndex = 10;
        	this.label2.Text = "Key";
        	// 
        	// label3
        	// 
        	this.label3.Location = new System.Drawing.Point(458, 96);
        	this.label3.Name = "label3";
        	this.label3.Size = new System.Drawing.Size(62, 18);
        	this.label3.TabIndex = 11;
        	this.label3.Text = "Down/Up";
        	// 
        	// label4
        	// 
        	this.label4.Location = new System.Drawing.Point(458, 176);
        	this.label4.Name = "label4";
        	this.label4.Size = new System.Drawing.Size(62, 16);
        	this.label4.TabIndex = 12;
        	this.label4.Text = "BigNum";
        	// 
        	// time_t
        	// 
        	this.time_t.Location = new System.Drawing.Point(526, 173);
        	this.time_t.Name = "time_t";
        	this.time_t.Size = new System.Drawing.Size(75, 20);
        	this.time_t.TabIndex = 13;
        	// 
        	// save_b
        	// 
        	this.save_b.Location = new System.Drawing.Point(458, 225);
        	this.save_b.Name = "save_b";
        	this.save_b.Size = new System.Drawing.Size(152, 23);
        	this.save_b.TabIndex = 15;
        	this.save_b.Text = "Save";
        	this.save_b.UseVisualStyleBackColor = true;
        	this.save_b.Click += new System.EventHandler(this.Save_bClick);
        	// 
        	// Add_b
        	// 
        	this.Add_b.Location = new System.Drawing.Point(234, 16);
        	this.Add_b.Name = "Add_b";
        	this.Add_b.Size = new System.Drawing.Size(75, 22);
        	this.Add_b.TabIndex = 17;
        	this.Add_b.Text = "Add new";
        	this.Add_b.UseVisualStyleBackColor = true;
        	this.Add_b.Click += new System.EventHandler(this.Add_bClick);
        	// 
        	// erase_b
        	// 
        	this.erase_b.Location = new System.Drawing.Point(396, 16);
        	this.erase_b.Name = "erase_b";
        	this.erase_b.Size = new System.Drawing.Size(75, 22);
        	this.erase_b.TabIndex = 18;
        	this.erase_b.Text = "Erase";
        	this.erase_b.UseVisualStyleBackColor = true;
        	this.erase_b.Click += new System.EventHandler(this.Erase_bClick);
        	// 
        	// behind_t
        	// 
        	this.behind_t.Location = new System.Drawing.Point(526, 199);
        	this.behind_t.Name = "behind_t";
        	this.behind_t.Size = new System.Drawing.Size(75, 20);
        	this.behind_t.TabIndex = 19;
        	// 
        	// label6
        	// 
        	this.label6.Location = new System.Drawing.Point(458, 202);
        	this.label6.Name = "label6";
        	this.label6.Size = new System.Drawing.Size(62, 17);
        	this.label6.TabIndex = 20;
        	this.label6.Text = "Behind ID..";
        	// 
        	// label7
        	// 
        	this.label7.Location = new System.Drawing.Point(458, 123);
        	this.label7.Name = "label7";
        	this.label7.Size = new System.Drawing.Size(62, 17);
        	this.label7.TabIndex = 21;
        	this.label7.Text = "End event";
        	// 
        	// label5
        	// 
        	this.label5.Location = new System.Drawing.Point(458, 150);
        	this.label5.Name = "label5";
        	this.label5.Size = new System.Drawing.Size(70, 23);
        	this.label5.TabIndex = 23;
        	this.label5.Text = "Byte params";
        	// 
        	// byte1
        	// 
        	this.byte1.Location = new System.Drawing.Point(526, 147);
        	this.byte1.MaxLength = 3;
        	this.byte1.Name = "byte1";
        	this.byte1.Size = new System.Drawing.Size(39, 20);
        	this.byte1.TabIndex = 24;
        	// 
        	// byte2
        	// 
        	this.byte2.Location = new System.Drawing.Point(571, 147);
        	this.byte2.Name = "byte2";
        	this.byte2.Size = new System.Drawing.Size(39, 20);
        	this.byte2.TabIndex = 25;
        	// 
        	// endEvent
        	// 
        	this.endEvent.Location = new System.Drawing.Point(526, 120);
        	this.endEvent.Name = "endEvent";
        	this.endEvent.Size = new System.Drawing.Size(75, 20);
        	this.endEvent.TabIndex = 26;
        	// 
        	// label8
        	// 
        	this.label8.Font = new System.Drawing.Font("Lucida Console", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
        	this.label8.Location = new System.Drawing.Point(458, 251);
        	this.label8.Name = "label8";
        	this.label8.Size = new System.Drawing.Size(283, 94);
        	this.label8.TabIndex = 27;
        	this.label8.Text = resources.GetString("label8.Text");
        	// 
        	// eraseAll
        	// 
        	this.eraseAll.Location = new System.Drawing.Point(691, 16);
        	this.eraseAll.Name = "eraseAll";
        	this.eraseAll.Size = new System.Drawing.Size(75, 23);
        	this.eraseAll.TabIndex = 28;
        	this.eraseAll.Text = "Erase all";
        	this.eraseAll.UseVisualStyleBackColor = true;
        	this.eraseAll.Click += new System.EventHandler(this.EraseAllClick);
        	// 
        	// openFileDialog1
        	// 
        	this.openFileDialog1.FileName = "openFileDialog1";
        	this.openFileDialog1.FileOk += new System.ComponentModel.CancelEventHandler(this.OpenFileDialog1FileOk);
        	// 
        	// open_file
        	// 
        	this.open_file.BackgroundImageLayout = System.Windows.Forms.ImageLayout.None;
        	this.open_file.Location = new System.Drawing.Point(526, 15);
        	this.open_file.Name = "open_file";
        	this.open_file.Size = new System.Drawing.Size(75, 23);
        	this.open_file.TabIndex = 29;
        	this.open_file.Text = "From file";
        	this.open_file.UseVisualStyleBackColor = true;
        	this.open_file.Click += new System.EventHandler(this.Open_fileClick);
        	// 
        	// to_file
        	// 
        	this.to_file.Location = new System.Drawing.Point(607, 16);
        	this.to_file.Name = "to_file";
        	this.to_file.Size = new System.Drawing.Size(75, 23);
        	this.to_file.TabIndex = 30;
        	this.to_file.Text = "To file";
        	this.to_file.UseVisualStyleBackColor = true;
        	this.to_file.Click += new System.EventHandler(this.To_fileClick);
        	// 
        	// saveFileDialog1
        	// 
        	this.saveFileDialog1.FileOk += new System.ComponentModel.CancelEventHandler(this.SaveFileDialog1FileOk);
        	// 
        	// switch_part
        	// 
        	this.switch_part.Location = new System.Drawing.Point(607, 65);
        	this.switch_part.Name = "switch_part";
        	this.switch_part.Size = new System.Drawing.Size(75, 23);
        	this.switch_part.TabIndex = 31;
        	this.switch_part.Text = "Switch Part";
        	this.switch_part.UseVisualStyleBackColor = true;
        	this.switch_part.Click += new System.EventHandler(this.Switch_partClick);
        	// 
        	// part_l
        	// 
        	this.part_l.Location = new System.Drawing.Point(607, 44);
        	this.part_l.Name = "part_l";
        	this.part_l.Size = new System.Drawing.Size(75, 23);
        	this.part_l.TabIndex = 32;
        	this.part_l.Text = "Part 1";
        	// 
        	// eeprom
        	// 
        	this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
        	this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
        	this.ClientSize = new System.Drawing.Size(778, 358);
        	this.Controls.Add(this.switch_part);
        	this.Controls.Add(this.part_l);
        	this.Controls.Add(this.to_file);
        	this.Controls.Add(this.open_file);
        	this.Controls.Add(this.eraseAll);
        	this.Controls.Add(this.label8);
        	this.Controls.Add(this.endEvent);
        	this.Controls.Add(this.byte2);
        	this.Controls.Add(this.byte1);
        	this.Controls.Add(this.label5);
        	this.Controls.Add(this.label7);
        	this.Controls.Add(this.label6);
        	this.Controls.Add(this.behind_t);
        	this.Controls.Add(this.erase_b);
        	this.Controls.Add(this.Add_b);
        	this.Controls.Add(this.save_b);
        	this.Controls.Add(this.time_t);
        	this.Controls.Add(this.label4);
        	this.Controls.Add(this.label3);
        	this.Controls.Add(this.label2);
        	this.Controls.Add(this.label1);
        	this.Controls.Add(this.comboBox1);
        	this.Controls.Add(this.key_t);
        	this.Controls.Add(this.id_t);
        	this.Controls.Add(this.edit_b);
        	this.Controls.Add(this.recordsList);
        	this.Controls.Add(this.write_b);
        	this.Controls.Add(this.read_b);
        	this.Name = "eeprom";
        	this.Text = "EEPROM manager";
        	this.FormClosed += new System.Windows.Forms.FormClosedEventHandler(this.EepromFormClosed);
        	this.ResumeLayout(false);
        	this.PerformLayout();
        }
        private System.Windows.Forms.Label part_l;
        private System.Windows.Forms.Button switch_part;
        private System.Windows.Forms.SaveFileDialog saveFileDialog1;
        private System.Windows.Forms.Button to_file;
        private System.Windows.Forms.Button open_file;
        private System.Windows.Forms.OpenFileDialog openFileDialog1;
        private System.Windows.Forms.Button eraseAll;
        private System.Windows.Forms.Label label8;
        private System.Windows.Forms.TextBox byte2;
        private System.Windows.Forms.TextBox byte1;
        private System.Windows.Forms.TextBox endEvent;
        private System.Windows.Forms.Label label7;
        private System.Windows.Forms.Label label6;
        private System.Windows.Forms.TextBox behind_t;
        private System.Windows.Forms.Button erase_b;
        private System.Windows.Forms.Button Add_b;
        private System.Windows.Forms.Button save_b;
        private System.Windows.Forms.Label label5;
        private System.Windows.Forms.TextBox time_t;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.ComboBox comboBox1;
        private System.Windows.Forms.TextBox key_t;
        private System.Windows.Forms.TextBox id_t;
        private System.Windows.Forms.Button edit_b;
        private System.Windows.Forms.ListBox recordsList;
        private System.IO.Ports.SerialPort serialPort1;
        private System.Windows.Forms.Button write_b;
        private System.Windows.Forms.Button read_b;
    }
}
