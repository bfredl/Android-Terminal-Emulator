package jackpal.androidterm.gestureinput;
import android.util.Log;
public class GestureKeyboard {
    private final String TAG = "GestureKeyboard";

    private int mRows, mCols;
    private GKAction[][] mActions;
    private boolean mReady = false;

    private int mPadX, mPadY;
    private int mWidth, mHeight;

    private boolean mMoving = false;
    private int mStartPos;


    private ActionListener mListener;
    
    public final int ACTION_WRITE = 1000;
    private class GKAction {
        int type = 0;
        String str = null;
        GKAction(int t, String s) {
            type = t; str = s;
        }
    }

    public GestureKeyboard() {
        mReady = false;
    }

    public GestureKeyboard(int rows, int cols, ActionListener l) {
        mRows = rows;
        mCols = cols;
        mActions = new GKAction[rows*cols][rows*cols];
        mListener = l;
        mReady = true;
    }

    public void mapGesture(int c0, int r0, int c1, int r1, String s) {
        int n = mCols;
        mActions[n*r0+c0][n*r1+c1] = new GKAction(ACTION_WRITE, s);
    }

    public void mapGesture(int c0, int r0, int c1, int r1, String s, String rev) {
        mapGesture(c0, r0, c1, r1, s);
        mapGesture(c1, r1, c0, r0, rev);
    }
    public void resize(int x, int y, int w, int h) {
        mPadX = x;
        mPadY = y;
        mWidth = w;
        mHeight = h;
    }

    public void resizeSquare(int w, int h, int m) {
        int s = Math.min(w,h)-2*m;
        mPadX = (w-s)/2;
        mPadY = (h-s)/2;
        mWidth = mHeight = s;
    }

    public boolean onTouchDown(float x, float y) {
        mStartPos = locate(x,y);
        Log.d(TAG, "onTouchDown " + mStartPos);
        mMoving = mStartPos >= 0;
        displayPos(mStartPos);
        return mMoving;
    }

    public boolean onTouchUp(float x, float y) {
        if(! mMoving ) {
            Log.d(TAG, "onTouchUP");
            return false;
        }
        mMoving = false;
        displayPos(-1);
        int endPos = locate(x,y);
        Log.d(TAG, "onTouchUP endp "+endPos);
        if( endPos == -1) {
            return true;
        }
        performAction(mStartPos, endPos);
        return true;
    }

    public boolean onTouchMove(float x, float y) {
        if( ! mMoving) {
            return false;
        }
        int pos = locate(x,y);
        displayPos(pos);
        return true;
    }

    public void cancel() {
        mMoving = false;
    }

    private int locate(float x, float y) {
        float c0 = (x-mPadX)*mCols/mWidth;
        float r0 = (y-mPadY)*mRows/mHeight;
        int c = (int)Math.floor(c0);
        int r = (int)Math.floor(r0);
        if( 0 <= c && c < mCols && 0<=r && r< mRows) 
            return mCols*r+c;    
        else 
            return -1;
    }

    private void performAction(int startPos, int endPos) {
        GKAction a = mActions[startPos][endPos];
        if(a != null) 
            performAction(a);
    }

    void performAction(GKAction a) {
        if(a.type >= 1000) {
            mListener.onAction(a.type, a.str);
        }
    }

    public boolean isMoving() {
        return mMoving;
    }

    private void displayPos(int pos) {
        if( !mMoving ) {
            display("");
        } else {
            display(formatPos(pos));
        }
    }
    
    private String formatPos(int pos) {
        if( pos < 0 ) {
            return "!";
        }
        int c = pos % mCols;
        int r = pos / mCols;
        return String.format("%c%d", 'A' + c, r);
    }


    private void display(String msg) {
        mListener.displayMsg(msg);
    }
    public static interface ActionListener {
        void onAction(int type, String str);
        void displayMsg(String msg);
    }
}


