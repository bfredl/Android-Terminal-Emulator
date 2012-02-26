package jackpal.androidterm.gestureinput;
import jackpal.androidterm.gestureinput.GestureKeyboard;
import 	android.view.MotionEvent;
import android.util.Log;
import android.graphics.*;

public class AndroidGestureKeyboard extends GestureKeyboard {
    private final String TAG = "AGestureKeyboard";
    Paint mFramePaint, mActivePaint, mStartPaint;
    public AndroidGestureKeyboard(int rows, int cols, ActionListener l) {
        super(rows, cols, l);
        //demoMap();
        readFile("/sdcard/mapping.txt");
        mFramePaint = new Paint();
        mFramePaint.setARGB(128,128,128,128);
        mActivePaint = new Paint();
        mActivePaint.setARGB(255,0,255,128);
        mStartPaint = new Paint();
        mStartPaint.setARGB(255,255,0,0);
    }

    private boolean mCaptured = false;
    private int mDrawedState;

    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getAction() & MotionEvent.ACTION_MASK;
        
        //Log.d(TAG, "onTouchEvent " + a);
        if(a == MotionEvent.ACTION_DOWN) {
            mCaptured = onTouchDown(ev.getX(), ev.getY()); 
            return mCaptured;
        } else {
            if(!mCaptured) 
                return false;
        }
        if(a == MotionEvent.ACTION_UP) {
            onTouchUp(ev.getX(), ev.getY());
        } else if(a == MotionEvent.ACTION_MOVE) {
            onTouchMove(ev.getX(), ev.getY());
        } else {
            return true;
        }
        return true;
    }

    public void demoMap() {
        mapGesture(0,0,1,0,"[", "]");
        mapGesture(2,0,3,0,"=", "_");
        mapGesture(1,1,1,1,"\033[A");
        mapGesture(1,2,1,2,"\033[B");
        mapGesture(0,2,0,3,"\033", "\t");
    }

    private void drawRect(Canvas c, int x, int y, int w, int h, Paint p) {
        c.drawLine(x,y,x+w,y,p);
        c.drawLine(x+w,y,x+w,y+h,p);
        c.drawLine(x+w,y+h,x,y+h,p);
        c.drawLine(x,y+h,x,y,p);
    }

    private void drawPos(Canvas ca, int pos, Paint p) {
        if(pos >= 0) {
            int c = pos % mCols;
            int r = pos / mCols;
            float w1 = (float)mWidth/mCols;
            float h1 = (float)mHeight/mRows;
            drawRect(ca, (int)(mPadX+c*w1), (int)(mPadY+r*h1),(int)w1,(int)h1, p);
        }
    }

    public void draw(Canvas c) {
        drawRect(c,mPadX, mPadY, mWidth, mHeight, mFramePaint);
        if(mMoving) {
            drawPos(c,mStartPos,mStartPaint);
            //drawPos(c,mCurPos,mActivePaint);
        }
        mDrawedState = drawState(); 
    }

    private int drawState() {
        int s;
        if( mMoving ) {
            s = 1;
            s = mRows*mCols*s+mStartPos;
            //s = mRows*mCols*s+mCurPos;
        } else {
            s = 0;
        }
        return s;
    }

    public boolean needsDraw() {
        return mDrawedState != drawState();
    }

}
