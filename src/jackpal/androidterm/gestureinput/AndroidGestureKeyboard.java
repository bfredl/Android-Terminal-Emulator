package jackpal.androidterm.gestureinput;
import jackpal.androidterm.gestureinput.GestureKeyboard;
import 	android.view.MotionEvent;
import android.util.Log;

public class AndroidGestureKeyboard extends GestureKeyboard {
    private final String TAG = "AGestureKeyboard";
    public AndroidGestureKeyboard(int rows, int cols, ActionListener l) {
        super(rows, cols, l);
        //demoMap();
        readFile("/sdcard/mapping.txt");
    }

    private boolean mCaptured = false;

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


            
}
