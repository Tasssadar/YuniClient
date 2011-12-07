
package com.yuniclient;

import com.yuni.client.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager.WakeLock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Shared extends Activity
{
    protected void OpenSpeedDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Set speed");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.speed_dialog,
                                       (ViewGroup) findViewById(R.id.layout_speed_dial));
        ((TextView)layout.findViewById(R.id.speed_text)).setText(String.valueOf(controlAPI.GetInst().GetDefaultMaxSpeed()));
        ((TextView)layout.findViewById(R.id.div_text)).setText(String.valueOf(controlAPI.GetInst().GetTurnDiv()));
        builder.setView(layout);
        
        SeekBar bar = (SeekBar)layout.findViewById(R.id.speedSeekBar);
        bar.setProgress(controlAPI.GetInst().GetDefaultMaxSpeed());
        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            public void onProgressChanged(SeekBar bar, int val, boolean fromUser)
            {
                EditText text = (EditText)alertDialog.findViewById(R.id.speed_text);
                text.setText(Integer.valueOf(val).toString());
            }
            public void onStartTrackingTouch(SeekBar arg0) {}
            public void onStopTrackingTouch(SeekBar arg0) {}
        });
        
        bar = (SeekBar)layout.findViewById(R.id.divSeekBar);
        bar.setProgress(controlAPI.GetInst().GetTurnDiv());
        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            public void onProgressChanged(SeekBar bar, int val, boolean fromUser)
            {
                EditText text = (EditText)alertDialog.findViewById(R.id.div_text);
                text.setText(Integer.valueOf(val).toString());
            }
            public void onStartTrackingTouch(SeekBar arg0) {}
            public void onStopTrackingTouch(SeekBar arg0) {}
        });
        
        builder.setNeutralButton("Set", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface arg0, int arg1)
            {
               EditText text = (EditText)alertDialog.findViewById(R.id.speed_text);
               int tmp = controlAPI.GetInst().GetDefaultMaxSpeed();
               try
               {
                   tmp = Integer.valueOf(text.getText().toString());
               }
               catch(NumberFormatException e)
               {
                   Toast.makeText(context, "Wrong format!", Toast.LENGTH_SHORT).show();
               }
               controlAPI.GetInst().SetMaxSpeed(tmp);
               
               text = (EditText)alertDialog.findViewById(R.id.div_text);
               tmp = controlAPI.GetInst().GetTurnDiv();
               try
               {
                   tmp = Integer.valueOf(text.getText().toString());
               }
               catch(NumberFormatException e)
               {
                   Toast.makeText(context, "Wrong format!", Toast.LENGTH_SHORT).show();
               }
               controlAPI.GetInst().SetTurnDiv((byte)tmp);
           }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }
    
    protected AlertDialog alertDialog;
    protected Context context;
    protected WakeLock lock;
}