package com.yuniclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.yuni.client.R;

public class Accelerometer extends Activity
{
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);  
    //    startAccel();
    }

    @Override
    protected void onDestroy()
    {
  //      stopAccel();
        super.onDestroy();
    }
    
    @Override
    protected void onPause()
    {
        stopAccel();
        super.onPause();
    }
    
    @Override
    protected void onResume()
    {
        startAccel();
        super.onResume();
    }
    
    private void startAccel()
    {
        if(view == null)
            view = new AccelerometerView(this);
        setContentView(view);

        accelerometerListener = new AccelerometerListener();
        lock = ((PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE))
            .newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK), "YuniClient accelerometer lock");
        lock.acquire();
        context = this;
        started = false;
        controlAPI.GetInst().SetDefXY(0, 0);
        pawsClosed = ((YuniClient.getState() & YuniClient.STATE_PAWS_OPEN) != 0);
        reelUp = ((YuniClient.getState() & YuniClient.STATE_REEL_UP) != 0);
        ShowStartDialog();
    }
    
    private void stopAccel()
    {
        ((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(accelerometerListener);
        lock.release();
        lock = null;
        accelerometerListener = null;
        if(Connection.GetInst() != null && started)
        {
            if(!controlAPI.HasSeparatedSpeed(controlAPI.GetInst().GetAPIType()))
                mMovementFlags = controlAPI.MOVE_NONE;
            byte[] data = controlAPI.GetInst().BuildMovementPacket(mMovementFlags, false, (byte)0, (short)0, (short)0);
            
            if(data != null)
                Connection.GetInst().write(data);
        }
        controlAPI.GetInst().SetDefXY(0, 0);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_MENU:
                ShowAPIDialog();
                return true;
            case KeyEvent.KEYCODE_SEARCH:
            {
                float pct = pawsClosed ? 75 : 5;
                pawsClosed = !pawsClosed;
                
                byte[] data = controlAPI.GetInst().BuildPawPacket(pct);
    
                if(data != null)
                    Connection.GetInst().write(data.clone());
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                reelUp = !reelUp;
                byte[] data = controlAPI.GetInst().BuildReelPacket(reelUp);
                if(data != null)
                    Connection.GetInst().write(data);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void ShowStartDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accelerometer");
        builder.setMessage("Move phone into default position and tap to \"Start\".");
        builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                mSensorManager.registerListener(accelerometerListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
                started = true;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {    
            public void onCancel(DialogInterface dialog)
            {
                finish();
            }
        });
        builder.create().show();
    }
    
    private void ShowAPIDialog()
    {
        final CharSequence[] items = {"Keyboard", "YuniRC", "Packets", "Chessbot", "Quorra"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose control API");
        builder.setSingleChoiceItems(items, controlAPI.GetInst().GetAPIType(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item)
            {
                controlAPI.GetInst().SetAPIType((byte) item);
                Toast.makeText(context, items[item] + " has been chosen as control API.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                
                if(!controlAPI.IsTargetSpeedDefined((byte) item))
                    OpenSpeedDialog();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void OpenSpeedDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Set speed");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.speed_dialog,
                                       (ViewGroup) findViewById(R.id.layout_speed_dial));
        ((TextView)layout.findViewById(R.id.speed_text)).setText(String.valueOf(controlAPI.GetInst().GetDefaultMaxSpeed()));
        builder.setView(layout);
        SeekBar bar = (SeekBar)layout.findViewById(R.id.speedSeekBar);
        bar.setProgress(controlAPI.GetInst().GetDefaultMaxSpeed());
        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            public void onProgressChanged(SeekBar bar, int val, boolean fromUser) {
                // TODO Auto-generated method stub
                EditText text = (EditText)alertDialog.findViewById(R.id.speed_text);
                text.setText(Integer.valueOf(val).toString());
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
                
            }

            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
                
            }}
        );
        builder.setNeutralButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
               EditText text = (EditText)alertDialog.findViewById(R.id.speed_text);
               int speed = controlAPI.GetInst().GetDefaultMaxSpeed();
               try
               {
                   speed = Integer.valueOf(text.getText().toString());
               }
               catch(NumberFormatException e)
               {
                   Toast.makeText(context, "Wrong format!", Toast.LENGTH_SHORT).show();
               }
               controlAPI.GetInst().SetMaxSpeed(speed);
           }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }
    
    private class AccelerometerListener implements SensorEventListener
    {    
        public AccelerometerListener() { }

        public void onAccuracyChanged(Sensor arg0, int arg1) {}

        public void onSensorChanged(SensorEvent event)
        {
            if (event.sensor.getType() != Sensor.TYPE_ORIENTATION)
                return;
            
            if(controlAPI.GetInst().GetDefX() == 0 && controlAPI.GetInst().GetDefY() == 0)
            {
                controlAPI.GetInst().SetDefXY(event.values[1], event.values[2]);
                return;
            }
            
            byte[] moveFlags = controlAPI.GetInst().XYToMoveFlags(event.values[1], event.values[2]);

            if(moveFlags == null || (mMovementFlags == moveFlags[0] && mSpeed == moveFlags[1]))
                return;
            
            mSpeed = moveFlags[1];
            if(controlAPI.HasSeparatedSpeed(controlAPI.GetInst().GetAPIType()))
            {
                byte[] speedData = null;
                if(controlAPI.GetInst().GetAPIType() == controlAPI.API_KEYBOARD)
                    speedData = new byte[1];
                else
                {
                    speedData = new byte[2];
                    speedData[1] = (byte)'d';
                }
                if(mSpeed == 50)
                    speedData[0] = (byte)'a';
                else if(mSpeed == 100)
                    speedData[0] = (byte)'b';
                else 
                    speedData[0] = (byte)'c';
                Connection.GetInst().write(speedData.clone());
                if(controlAPI.GetInst().GetAPIType() == controlAPI.API_YUNIRC && mMovementFlags != 0)
                {
                    speedData = controlAPI.GetInst().BuildMovementPacket(mMovementFlags, false, mSpeed, (short)0, (short)0);
                    if(speedData != null)
                        Connection.GetInst().write(speedData.clone());
                }
            }
            
            mMovementFlags = moveFlags[0];
            if(moveFlags[0] != controlAPI.MOVE_NONE || controlAPI.HasPacketStructure(controlAPI.GetInst().GetAPIType()))
            {
                byte[] data = controlAPI.GetInst().BuildMovementPacket(mMovementFlags, moveFlags[0] != 0, mSpeed, (short)0, (short)0);
                if(data != null)
                {
                    Connection.GetInst().write(data.clone());
                    view.drawArrows();
                }
            }
            else if(moveFlags[0] == controlAPI.MOVE_NONE)
                view.drawArrows();
        }
    }
    
    public class AccelerometerView extends SurfaceView implements SurfaceHolder.Callback
    {
        private static final float FONT_SIZE_PER_PX = (float)0.053125;
        
        private Paint mLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint mSpeedFrame = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint mSpeedPnt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private SurfaceHolder holder = null;
        private Bitmap arrow_up = null;
        private Bitmap arrow_down = null;
        private Bitmap arrow_left = null;
        private Bitmap arrow_right = null;
        
        private float textSize;
        private float position[];
        private float height;
        private float width;

        public AccelerometerView(Context context)
        {
            super(context);
            SurfaceHolder holder = getHolder();
            holder.addCallback(this);
            setFocusable(true); // make sure we get key events
            setFocusableInTouchMode(true); // make sure we get touch events
            mLine.setColor(0xFFFF0000);
            
            mLine.setTextAlign(Paint.Align.CENTER);   
            
            mSpeedFrame.setStyle(Style.STROKE);
            mSpeedFrame.setStrokeWidth(2);
            mSpeedFrame.setColor(0xFFFF0000);
            
            mSpeedPnt.setStyle(Style.STROKE);
            mSpeedPnt.setColor(0xFFFFFF00);
            
            arrow_up = BitmapFactory.decodeResource(getResources(), R.drawable.up);
            arrow_down = BitmapFactory.decodeResource(getResources(), R.drawable.down);
            arrow_left = BitmapFactory.decodeResource(getResources(), R.drawable.left);
            arrow_right = BitmapFactory.decodeResource(getResources(), R.drawable.right);
        }

        public void surfaceCreated(SurfaceHolder holder) {
        }         
        
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int arg1, int width, int height)
        {
            this.width = width/2;
            this.height = height/2;            
            this.holder = holder;
            
            textSize = FONT_SIZE_PER_PX*width;
            position = new float[14];
            // speed bar
            position[0] = this.width - (width - arrow_up.getWidth())/2; // left 
            position[1] = this.width + (width - arrow_up.getWidth())/2; // right
            position[2] = this.height + textSize; // top
            position[3] = this.height + textSize*2 + 10; // bottom
            position[4] = position[0] + (width - arrow_up.getWidth())/3; // first third
            position[5] = position[0] + ((width - arrow_up.getWidth())/3)*2; // second third
            position[9] = (position[2] + position[3])/2; // yellow speed bar
            // text
            position[6] = this.height-textSize/2; // first line
            position[7] = this.height+textSize/2; // second line
            position[8] = this.height+textSize*2; // Speed
            // arrows
            position[10] = ((this.width*2)-arrow_up.getWidth())/2; // up/down center
            position[11] = (this.height*2)-arrow_down.getHeight(); // down bottom pos
            position[12] = ((this.height*2)-arrow_left.getHeight())/2; // left/right center
            position[13] = (this.width*2)-arrow_right.getWidth(); // right pos
            
            mLine.setTextSize(textSize);
            mSpeedPnt.setStrokeWidth(position[3] - position[2] - 2);
            drawArrows();
        }
        
        private void drawBase(Canvas c)
        {
            c.drawColor(Color.BLACK);
            c.drawText("Hold your phone in landscape mode.", width, position[6], mLine);
            c.drawText("Press back to exit.", width, position[7], mLine);
            c.drawText("Speed", width, position[8], mLine);
            
            c.drawLine(position[0], position[2], position[1], position[2], mSpeedFrame);
            c.drawLine(position[0], position[3], position[1], position[3], mSpeedFrame);
            
            c.drawLine(position[0], position[2], position[0], position[3], mSpeedFrame);
            c.drawLine(position[1], position[2], position[1], position[3], mSpeedFrame);
        }
        
        public void drawArrows()
        {
            Canvas c = holder.lockCanvas();
            if(c == null)
                return;
            
            drawBase(c);
            
            switch(mMovementFlags)
            {
                case controlAPI.MOVE_NONE:
                    holder.unlockCanvasAndPost(c);
                    return;
                case controlAPI.MOVE_FORWARD:
                    c.drawBitmap(arrow_up, position[10], 0, null);
                    break;
                case controlAPI.MOVE_BACKWARD:
                    c.drawBitmap(arrow_down, position[10], position[11], null);
                    break;
                case controlAPI.MOVE_LEFT:
                    c.drawBitmap(arrow_left, 0, position[12], null);
                    break;
                case controlAPI.MOVE_RIGHT:
                    c.drawBitmap(arrow_right, position[13], position[12], null);
                    break;
                default:
                {
                    if((mMovementFlags & controlAPI.MOVE_FORWARD) != 0)
                        c.drawBitmap(arrow_up, position[10], 0, null);
                    else
                        c.drawBitmap(arrow_down, position[10], position[11], null);
                    
                    if((mMovementFlags & controlAPI.MOVE_LEFT) != 0)
                        c.drawBitmap(arrow_left, 0, position[12], null);
                    else
                        c.drawBitmap(arrow_right, position[13], position[12], null);
                    break;
                }
            }
            switch(mSpeed)
            {
                case 0:
                default:
                    break;
                case 50:
                    c.drawLine(position[0], position[9], position[4], position[9], mSpeedPnt);
                    break;
                case 100:
                    c.drawLine(position[0], position[9], position[5], position[9], mSpeedPnt);
                    break;
                case 127:
                    c.drawLine(position[0], position[9], position[1], position[9], mSpeedPnt);
                    break;
            }
            holder.unlockCanvasAndPost(c);
        }
    }
    
    private AccelerometerView view;
    private AccelerometerListener accelerometerListener;
    private WakeLock lock;
    private Context context;
    private AlertDialog alertDialog;
    
    private boolean started;
    private byte mMovementFlags;
    private byte mSpeed;
    private boolean pawsClosed;
    private boolean reelUp;
}