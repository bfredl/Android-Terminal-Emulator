package jackpal.androidterm.gestureinput;
import android.util.Log;
import java.io.*;
import java.util.regex.Pattern;
public class GestureKeyboard {
    private final String TAG = "GestureKeyboard";

    protected int mRows, mCols;
    private GKAction[][] mActions;
    private boolean mReady = false;

    protected int mPadX, mPadY;
    protected int mWidth, mHeight;

    protected boolean mMoving = false;
    protected int mStartPos;
    protected int mCurPos;


    private ActionListener mListener;
    
    public abstract static class GKAction {
        abstract public String describe();
        abstract void perform(); 
    }

    private class TypeAction extends GKAction {
        String mTyped,mDescr;
        TypeAction(String typed) {
            mTyped = typed;
            mDescr = typed; //FIXME
        }
        public String describe() {
            return mDescr;
        }
        public void perform() {
            mListener.writeStr(mTyped);
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

    public void mapGesture(int pos0, int pos1, String s) {
        mActions[pos0][pos1] = new TypeAction(s);
    }

    public void mapGesture(int pos0, int pos1, String s, String rev) {
        mapGesture(pos0, pos1, s);
        mapGesture(pos1, pos0, rev);
    }
    public void mapGesture(int c0, int r0, int c1, int r1, String s) {
        int n = mCols;
        mActions[n*r0+c0][n*r1+c1] = new TypeAction(s);
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
        mStartPos = mCurPos = locate(x,y);
        Log.d(TAG, "onTouchDown " + mStartPos);
        mMoving = mStartPos >= 0;
        displayAction(mStartPos, mStartPos);
        return mMoving;
    }

    public boolean onTouchUp(float x, float y) {
        if(! mMoving ) {
            Log.d(TAG, "onTouchUP");
            return false;
        }
        mMoving = false;
        display("");
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
        mCurPos = locate(x,y);
        displayAction(mStartPos, mCurPos);
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
            a.perform();
    }

    private void displayAction(int startPos, int endPos) {
        if(startPos < 0 || endPos < 0 ) {
            display("");
            return;
        }
        GKAction a = mActions[startPos][endPos];
        if(a != null) {
            display(a.describe());
        } else {
            display("");
        }
    }

    public boolean isMoving() {
        return mMoving;
    }

    private void displayPos(int pos) {
        mCurPos = pos;
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

    public int asPos(int c, int r) {
        if( 0 <= c && c < mCols && 0<=r && r< mRows) 
            return mCols*r+c;    
        else 
            return -1;
    }

    private void display(String msg) {
        mListener.displayMsg(msg);
    }
    public static interface ActionListener {
        void displayMsg(String msg);
        void writeStr(String typed);
    }

    public void readFile(String filename) {
        MapFileReader fr;
        try {
            fr = new MapFileReader(new FileReader(filename),this);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        fr.parseFile();
    }

}

class MapFileReader extends StreamTokenizer {
    private static final String TAG = "MapFileReader";
    private GestureKeyboard mTarget;
    MapFileReader(Reader r, GestureKeyboard target) {
        super(r);
        mTarget = target;
        eolIsSignificant(true);
        whitespaceChars(' ', ' ');
        whitespaceChars('\t', '\t');
        wordChars('a','z');
        wordChars('A','Z');
        wordChars('0','9');
        ordinaryChar('.');
        ordinaryChar('-');
        quoteChar('"');
        commentChar('#');
    }
    void fail() {
        throw new RuntimeException("Meh" + lineno());
    }

    void nextTok() {
        try {
            nextToken();
        } catch(IOException e ) {
            throw new RuntimeException(e);
        }
    }

    void parseFile() {
        while(true) {
            nextTok();
            if( ttype == TT_EOL) {
                continue;
            } else if(ttype == TT_EOF) {
                break;
            } else if(parseLine()) {
                continue;
            } else {
                fail();
            }

        }
    }

    // reuses
    boolean parseLine() {
        if(ttype != TT_WORD) {
            return false;
        }
        if(isPos()) {
            parseMapping();
        } else if(sval.equals("pairs")) {
            parsePairs();
        } else if(sval.equals("sing")) {
            parseSingles();
        } else {
            fail();
        }
        return true;
    }

    // reuses
    void parseMapping() {
        int pos = asPos();
        nextTok();
        if(ttype == TT_WORD && isPos()) {
            int pos2 = asPos();
            String a1 = parseAction();
            if(a1 == null) {
                fail();
            }
            String a2 = parseAction();
            if(a2 != null) {
                mTarget.mapGesture(pos, pos2, a1, a2);
            } else {
                mTarget.mapGesture(pos, pos2, a1);
            }
        } else {
            pushBack();
            String a = parseAction();
            if(a != null) {
                mTarget.mapGesture(pos, pos, a);
            } else {
                fail();
            }
        }
    }

    void parsePairs() {
        nextTok();
        if(ttype != TT_WORD && ttype != TT_NUMBER) {
            fail();
        }
        boolean col;
        int posu, r=-100, c=-100;
        if( ttype == TT_WORD) {
            char s = sval.charAt(0);
            col = true;
            c = s - 'A';
        } else {
            col = false;
            r = (int)nval;
            Log.d(TAG, "nval " + r);
        }
        for (int v = 0; v < 3; v++) {
            String a1 = parseAction();
            Log.d(TAG, "a1 " + a1);
            String a2 = parseAction();
            Log.d(TAG, "a2 " + a2);
            if(col) {
                mTarget.mapGesture(c, v, c, v+1, a1, a2);
            } else {
                mTarget.mapGesture(v, r, v+1, r, a1, a2);
            }
        }
    } 

    void parseSingles() {
        nextTok();
        if(ttype != TT_WORD && ttype != TT_NUMBER) {
            fail();
        }
        boolean col;
        int posu, r=-100, c=-100;
        if( ttype == TT_WORD) {
            char s = sval.charAt(0);
            col = true;
            c = s - 'A';
        } else {
            col = false;
            r = (int)nval;
        }
        for (int v = 0; v < 4; v++) {
            String a1 = parseAction();
            if(col) {
                r = v;
            } else {
                c = v;
            }
            mTarget.mapGesture(r, c, r, c, a1);
        }
    }
    // reuses NOT
    String parseAction() {
        nextTok();
        if(ttype == '"') {
            return sval; 
        } else if(ttype == TT_WORD) {
            if(isPos()) {
                pushBack();
                return null;
            } else {
                return sval;
            }
        } else if(ttype == TT_EOF || ttype == TT_EOL) {
            pushBack();
            return null;
        } else {
            Log.d(TAG, "ttype " + ttype);
            return String.format("%c",ttype);
        }
    }


    
    boolean isPos() {
        return Pattern.matches("[A-Za-z][0-9]", sval);
    }

    int asPos() {
        char c0 = sval.charAt(0);
        int c = c0 > 'Z' ? c0 - 'a' : c0 - 'A';
        char r0 = sval.charAt(1);
        int r = r0 - '0';
        return mTarget.asPos(c,r);
    }
}

