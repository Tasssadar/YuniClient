namespace YuniClient
{
    partial class Form1
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
        	this.components = new System.ComponentModel.Container();
        	System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(Form1));
        	this.connect = new System.Windows.Forms.Button();
        	this.serialPort1 = new System.IO.Ports.SerialPort(this.components);
        	this.portName = new System.Windows.Forms.ComboBox();
        	this.rate = new System.Windows.Forms.ComboBox();
        	this.textBox1 = new System.Windows.Forms.TextBox();
        	this.state_b = new System.Windows.Forms.Button();
        	this.botOut = new System.Windows.Forms.TextBox();
        	this.hackStop = new System.Windows.Forms.Button();
        	this.load_b = new System.Windows.Forms.Button();
        	this.openFileDialog1 = new System.Windows.Forms.OpenFileDialog();
        	this.filename = new System.Windows.Forms.Label();
        	this.flash = new System.Windows.Forms.Button();
        	this.device_label = new System.Windows.Forms.Label();
        	this.progressBar1 = new System.Windows.Forms.ProgressBar();
        	this.percentL = new System.Windows.Forms.Label();
        	this.Clear_b = new System.Windows.Forms.Button();
        	this.version = new System.Windows.Forms.Label();
        	this.SuspendLayout();
        	// 
        	// connect
        	// 
        	this.connect.Location = new System.Drawing.Point(209, 5);
        	this.connect.Name = "connect";
        	this.connect.Size = new System.Drawing.Size(75, 22);
        	this.connect.TabIndex = 0;
        	this.connect.Text = "Connect";
        	this.connect.UseVisualStyleBackColor = true;
        	this.connect.Click += new System.EventHandler(this.connect_Click_1);
        	// 
        	// serialPort1
        	// 
        	this.serialPort1.DtrEnable = true;
        	this.serialPort1.Parity = System.IO.Ports.Parity.Mark;
        	this.serialPort1.DataReceived += new System.IO.Ports.SerialDataReceivedEventHandler(this.serialPort1_DataReceived);
        	// 
        	// portName
        	// 
        	this.portName.FormattingEnabled = true;
        	this.portName.Items.AddRange(new object[] {
        	        	        	"COM1",
        	        	        	"COM2",
        	        	        	"COM3",
        	        	        	"COM4",
        	        	        	"COM5"});
        	this.portName.Location = new System.Drawing.Point(12, 7);
        	this.portName.Name = "portName";
        	this.portName.Size = new System.Drawing.Size(121, 21);
        	this.portName.TabIndex = 1;
        	this.portName.Text = "COM4";
        	// 
        	// rate
        	// 
        	this.rate.FormattingEnabled = true;
        	this.rate.Items.AddRange(new object[] {
        	        	        	"19200",
        	        	        	"38400",
        	        	        	"57600",
        	        	        	"115200",
        	        	        	"500000",
        	        	        	"1000000",
        	        	        	"1500000",
        	        	        	"2000000"});
        	this.rate.Location = new System.Drawing.Point(139, 7);
        	this.rate.Name = "rate";
        	this.rate.Size = new System.Drawing.Size(64, 21);
        	this.rate.TabIndex = 2;
        	this.rate.Text = "38400";
        	// 
        	// textBox1
        	// 
        	this.textBox1.BackColor = System.Drawing.Color.Black;
        	this.textBox1.ForeColor = System.Drawing.Color.White;
        	this.textBox1.Location = new System.Drawing.Point(12, 51);
        	this.textBox1.Multiline = true;
        	this.textBox1.Name = "textBox1";
        	this.textBox1.ReadOnly = true;
        	this.textBox1.ScrollBars = System.Windows.Forms.ScrollBars.Both;
        	this.textBox1.Size = new System.Drawing.Size(191, 368);
        	this.textBox1.TabIndex = 3;
        	this.textBox1.TextChanged += new System.EventHandler(this.textBox1_TextChanged);
        	// 
        	// state_b
        	// 
        	this.state_b.Enabled = false;
        	this.state_b.Location = new System.Drawing.Point(286, 5);
        	this.state_b.Name = "state_b";
        	this.state_b.Size = new System.Drawing.Size(75, 23);
        	this.state_b.TabIndex = 4;
        	this.state_b.Text = "Stop";
        	this.state_b.UseVisualStyleBackColor = true;
        	this.state_b.Click += new System.EventHandler(this.state_b_Click);
        	// 
        	// botOut
        	// 
        	this.botOut.BackColor = System.Drawing.Color.Black;
        	this.botOut.ForeColor = System.Drawing.Color.White;
        	this.botOut.ImeMode = System.Windows.Forms.ImeMode.On;
        	this.botOut.Location = new System.Drawing.Point(209, 51);
        	this.botOut.Multiline = true;
        	this.botOut.Name = "botOut";
        	this.botOut.ReadOnly = true;
        	this.botOut.ScrollBars = System.Windows.Forms.ScrollBars.Both;
        	this.botOut.Size = new System.Drawing.Size(708, 368);
        	this.botOut.TabIndex = 5;
        	this.botOut.WordWrap = false;
        	this.botOut.TextChanged += new System.EventHandler(this.botOut_TextChanged);
        	this.botOut.KeyDown += new System.Windows.Forms.KeyEventHandler(this.botOut_KeyDown);
        	this.botOut.KeyUp += new System.Windows.Forms.KeyEventHandler(this.botOut_KeyUp);
        	// 
        	// hackStop
        	// 
        	this.hackStop.Location = new System.Drawing.Point(367, 5);
        	this.hackStop.Name = "hackStop";
        	this.hackStop.Size = new System.Drawing.Size(75, 23);
        	this.hackStop.TabIndex = 6;
        	this.hackStop.Text = "Fix Stop";
        	this.hackStop.UseVisualStyleBackColor = true;
        	this.hackStop.Visible = false;
        	this.hackStop.Click += new System.EventHandler(this.hackStop_Click);
        	// 
        	// load_b
        	// 
        	this.load_b.Enabled = false;
        	this.load_b.Location = new System.Drawing.Point(447, 5);
        	this.load_b.Name = "load_b";
        	this.load_b.Size = new System.Drawing.Size(75, 23);
        	this.load_b.TabIndex = 8;
        	this.load_b.Text = "Load hex";
        	this.load_b.UseVisualStyleBackColor = true;
        	this.load_b.Click += new System.EventHandler(this.load_b_Click);
        	// 
        	// openFileDialog1
        	// 
        	this.openFileDialog1.Filter = "Hex Files|*.hex";
        	this.openFileDialog1.FileOk += new System.ComponentModel.CancelEventHandler(this.openFileDialog1_FileOk);
        	// 
        	// filename
        	// 
        	this.filename.AutoSize = true;
        	this.filename.Location = new System.Drawing.Point(215, 30);
        	this.filename.Name = "filename";
        	this.filename.Size = new System.Drawing.Size(49, 13);
        	this.filename.TabIndex = 9;
        	this.filename.Text = "<No file>";
        	// 
        	// flash
        	// 
        	this.flash.Enabled = false;
        	this.flash.Location = new System.Drawing.Point(597, 5);
        	this.flash.Name = "flash";
        	this.flash.Size = new System.Drawing.Size(75, 23);
        	this.flash.TabIndex = 10;
        	this.flash.Text = "Flash";
        	this.flash.UseVisualStyleBackColor = true;
        	this.flash.Click += new System.EventHandler(this.flash_Click);
        	// 
        	// device_label
        	// 
        	this.device_label.AutoSize = true;
        	this.device_label.Location = new System.Drawing.Point(528, 10);
        	this.device_label.Name = "device_label";
        	this.device_label.Size = new System.Drawing.Size(63, 13);
        	this.device_label.TabIndex = 11;
        	this.device_label.Text = "<unknown>";
        	this.device_label.Click += new System.EventHandler(this.device_label_Click);
        	// 
        	// progressBar1
        	// 
        	this.progressBar1.Location = new System.Drawing.Point(209, 34);
        	this.progressBar1.Name = "progressBar1";
        	this.progressBar1.Size = new System.Drawing.Size(709, 11);
        	this.progressBar1.TabIndex = 12;
        	this.progressBar1.Visible = false;
        	// 
        	// percentL
        	// 
        	this.percentL.AutoSize = true;
        	this.percentL.Location = new System.Drawing.Point(179, 30);
        	this.percentL.Name = "percentL";
        	this.percentL.Size = new System.Drawing.Size(24, 13);
        	this.percentL.TabIndex = 13;
        	this.percentL.Text = "0 %";
        	this.percentL.Visible = false;
        	// 
        	// Clear_b
        	// 
        	this.Clear_b.Location = new System.Drawing.Point(843, 5);
        	this.Clear_b.Name = "Clear_b";
        	this.Clear_b.Size = new System.Drawing.Size(75, 23);
        	this.Clear_b.TabIndex = 14;
        	this.Clear_b.Text = "Clear";
        	this.Clear_b.UseVisualStyleBackColor = true;
        	this.Clear_b.Click += new System.EventHandler(this.Clear_b_Click);
        	// 
        	// version
        	// 
        	this.version.AutoSize = true;
        	this.version.Enabled = false;
        	this.version.Location = new System.Drawing.Point(9, 34);
        	this.version.Name = "version";
        	this.version.Size = new System.Drawing.Size(0, 13);
        	this.version.TabIndex = 15;
        	// 
        	// Form1
        	// 
        	this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
        	this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
        	this.ClientSize = new System.Drawing.Size(929, 429);
        	this.Controls.Add(this.version);
        	this.Controls.Add(this.Clear_b);
        	this.Controls.Add(this.percentL);
        	this.Controls.Add(this.progressBar1);
        	this.Controls.Add(this.device_label);
        	this.Controls.Add(this.flash);
        	this.Controls.Add(this.filename);
        	this.Controls.Add(this.load_b);
        	this.Controls.Add(this.hackStop);
        	this.Controls.Add(this.botOut);
        	this.Controls.Add(this.state_b);
        	this.Controls.Add(this.textBox1);
        	this.Controls.Add(this.rate);
        	this.Controls.Add(this.portName);
        	this.Controls.Add(this.connect);
        	this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
        	this.MinimumSize = new System.Drawing.Size(937, 461);
        	this.Name = "Form1";
        	this.Text = "YuniClient";
        	this.Resize += new System.EventHandler(this.Form1Resize);
        	this.ResumeLayout(false);
        	this.PerformLayout();
        }

        #endregion

        private System.Windows.Forms.Button connect;
        private System.IO.Ports.SerialPort serialPort1;
        private System.Windows.Forms.ComboBox portName;
        private System.Windows.Forms.ComboBox rate;
        private System.Windows.Forms.TextBox textBox1;
        private System.Windows.Forms.Button state_b;
        private System.Windows.Forms.TextBox botOut;
        private System.Windows.Forms.Button hackStop;
        private System.Windows.Forms.Button load_b;
        private System.Windows.Forms.OpenFileDialog openFileDialog1;
        private System.Windows.Forms.Label filename;
        private System.Windows.Forms.Button flash;
        private System.Windows.Forms.Label device_label;
        private System.Windows.Forms.ProgressBar progressBar1;
        private System.Windows.Forms.Label percentL;
        private System.Windows.Forms.Button Clear_b;
        private System.Windows.Forms.Label version;
    }
}

