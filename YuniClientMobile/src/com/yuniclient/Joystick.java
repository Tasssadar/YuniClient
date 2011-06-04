package com.yuniclient;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class Joystick
{
    private static final float PI = (float)3.141592;
    
    public Joystick()
    {
        Reset();
    }
    public void Reset()
    {
        mMovementFlags = 0;
        mSpeed = 0;
        mSpinAtPlace = false;
        mFingerDown = false;
    }
    
    public byte[] touchEvent(int action, float x, float y, int width, int height)
    {
        if(action == MotionEvent.ACTION_UP)
        {
            byte[] flags = {0, 0};
            Reset();
            return flags;
        }
        else
        {
            float dta[] = calculateFloats(x, y, width, height);

            byte speed = 127;
            if(dta[0] < 33)
                speed = 50;
            else if(dta[0] < 66)
                speed = 100;
            
            byte[] flags = {0, speed };
            
            float ang = dta[1];
            
            if(ang > 5.18362 || ang <  1.09956) // 0.7 PI range
                flags[0] |= controlAPI.MOVE_FORWARD;
            else if(ang > 2.04203 && ang < 4.24115) 
                flags[0] |= controlAPI.MOVE_BACKWARD;
            
            if(ang > 0.471236 && ang < 2.67036)
                flags[0] |= controlAPI.MOVE_RIGHT;
            else if(ang > 3.61283 && ang <  5.81195)
                flags[0] |= controlAPI.MOVE_LEFT;
            
            if((flags[0] & controlAPI.MOVE_RIGHT) != 0 || (flags[0] & controlAPI.MOVE_LEFT) != 0)
            {
                if(!mFingerDown)
                    mSpinAtPlace = true;
                
                if(!mSpinAtPlace)
                {
                    if(ang >= PI*1.5 || ang <= PI*0.5)
                        flags[0] |= controlAPI.MOVE_FORWARD;
                    else
                        flags[0] |= controlAPI.MOVE_BACKWARD;
                }
            }
            if(!mFingerDown)
                mFingerDown = true;
            
            if(flags[0] == mMovementFlags && flags[1] == mSpeed)
                return null;
            
            mMovementFlags = flags[0];
            mSpeed = flags[1];
            return flags;
        }
    }
    
    public class MTView extends SurfaceView implements SurfaceHolder.Callback
    {
        private int width, height;
        Paint mLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        Context context = null;
        Message msg = null; 

        public MTView(Context context) {
            super(context);
            this.context = context;
            SurfaceHolder holder = getHolder();
            holder.addCallback(this);
            setFocusable(true); // make sure we get key events
            setFocusableInTouchMode(true); // make sure we get touch events
            mLine.setColor(0xFFFF0000);
            mLine.setStyle(Style.STROKE);
            mLine.setStrokeWidth(3);
            mCircle.setColor(0xFFFFFF00);
            mCircle.setStyle(Style.FILL);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            Canvas c = getHolder().lockCanvas();
            if (c == null)
                return true;
            
            drawBaseCircles(c);
            
            if (event.getAction() != MotionEvent.ACTION_UP)
            {
                c.drawCircle(event.getX(), event.getY(), 40, mCircle);
                c.drawLine(0, event.getY(), width*2, event.getY(), mLine);
                c.drawLine(event.getX(), 0, event.getX(), height*2, mLine);    
            }
            getHolder().unlockCanvasAndPost(c);
            
            msg = new Message();
            msg.obj = event;
            msg.arg1 = width;
            msg.arg2 = height;
            ((YuniClient)context).ballHandler.sendMessage(msg);
            return true;
        }


        public void surfaceCreated(SurfaceHolder holder) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int arg1, int width, int height)
        {
            this.width = width/2;
            this.height = height/2;
            
            Canvas c = holder.lockCanvas();
            drawBaseCircles(c);
            holder.unlockCanvasAndPost(c);
        }
        
        private void drawBaseCircles(Canvas c)
        {
            c.drawColor(Color.BLACK);
            c.drawCircle(width, height, width, mLine);
            c.drawCircle(width, height, (float) (width*0.66), mLine);
            c.drawCircle(width, height, (float) (width*0.33), mLine);
            c.drawLine(width-20, height, width+20, height, mLine);
            c.drawLine(width, height-20, width, height+20, mLine);
        }
    }
    
    private native float[] calculateFloats(float x, float y, float width, float height);
    
    private byte mMovementFlags;
    private byte mSpeed;
    private boolean mSpinAtPlace;
    private boolean mFingerDown;
}