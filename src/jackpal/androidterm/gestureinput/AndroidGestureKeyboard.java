package jackpal.androidterm.gestureinput;
import jackpal.androidterm.gestureinput.GestureKeyboard;
import 	android.view.MotionEvent;

public class AndroidGestureKeyboard extends GestureKeyboard {
    public AndroidGestureKeyboard(int rows, int cols, ActionListener l) {
        super(rows, cols, l);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getAction();
        
        if((a & MotionEvent.ACTION_DOWN) != 0) {
            onTouchDown(ev.getX(), ev.getY()); 
        } else if( (a & MotionEvent.ACTION_UP )!= 0) {
            onTouchUp(ev.getX(), ev.getY());
        }
        return true;
    }

    public void demoMap() {
        mapGesture(0,0,1,0,"[");
        mapGesture(1,0,0,0,"]");
    }

            
}
