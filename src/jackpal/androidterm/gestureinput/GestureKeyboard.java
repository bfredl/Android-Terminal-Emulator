package jackpal.androidterm.gestureinput;

public class GestureKeyboard {

    private int mRows, mCols;
    private GKAction[][] mActions;
    private boolean mReady = false;

    int mWidth, mHeight;

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

    public void resize(int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    public void onTouchDown(float x, float y) {
        mStartPos = locate(x,y);
        mMoving = mStartPos >= 0;
    }

    public void onTouchUp(float x, float y) {
        if(! mMoving ) {
            return;
        }
        mMoving = false;
        int endPos = locate(x,y);
        if( endPos == -1) {
            return;
        }
        performAction(mStartPos, endPos);
    }

    public void cancel() {
        mMoving = false;
    }

    private int locate(float x, float y) {
        int c = (int)(x*mCols/mWidth);
        int r = (int)(y*mRows/mHeight);
        return mCols*r+c;    
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

    public static interface ActionListener {
        void onAction(int type, String str);
    }
}


