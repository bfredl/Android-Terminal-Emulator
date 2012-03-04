package jackpal.androidterm.gestureinput;
import android.util.Log;
import java.io.*;
import java.util.*;
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
            if(mTyped.length() == 1) {
                char x = mTyped.charAt(0);
                if(x < ' ') {
                    mDescr = String.format("^%c", x+'@');
                }
            }
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
        return asPos(c,r);
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

    public int columns() {
        return mCols;
    }

    public int rows() {
        return mRows;
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

    private static class Shape {
        int repeatCount;
        int repeatOffset;
        int[] pattern;
        Shape(int c, int o, int[] p) {
            repeatCount = c;
            repeatOffset = o; 
            pattern = p;
        }
    }
    private Map<String, Shape> mShapes = new HashMap();

    MapFileReader(Reader r, GestureKeyboard target) {
        super(r);
        mTarget = target;
        //resetSyntax();
        eolIsSignificant(true);
        whitespaceChars(' ', ' ');
        whitespaceChars('\t', '\t');
        wordChars('a','z');
        wordChars('A','Z');
        wordChars('0','9');
        ordinaryChar('.');
        ordinaryChar('-');
        ordinaryChar('/'); //meh
        ordinaryChar('\\'); //meh
        quoteChar('"');
        quoteChar('\'');
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
        sval = sval.intern();
        if(isPos()) {
            parseMapping();
        } else if(sval == "shape") {
            parseShape();
        } else if(mShapes.containsKey(sval)) {
            parseShapeInvoke(mShapes.get(sval));
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

    void parseShape() {
        nextTok();
        if(ttype != TT_WORD) 
            fail();
        String name = sval;
        nextTok();
        if(ttype != TT_WORD) 
            fail();
        int offset = -100;;
        if( sval.equals("r")) {
            offset = 1; 
        } else if( sval.equals("c")) { 
            offset = mTarget.columns();
        } else {
            fail();
        }

        nextTok();
        if(ttype != TT_NUMBER) 
            fail();
        int count = (int)nval;
        List<Integer> pat = new ArrayList();
        while(true) {
            nextTok();
            if(isPos()) {
                pat.add(asPos());
            } else if(ttype == '!') {
                // reverse it!
                int s = pat.size();
                pat.add(pat.get(s-1));
                pat.add(pat.get(s-2));
            } else {
                break;
            }
        }
        pushBack();
        Log.d(TAG, String.format("shape %d %d %s", count, offset, pat) );
        mShapes.put(name, new Shape(count, offset, intArray(pat)));
    }

    void parseShapeInvoke(Shape s) {
        nextTok();
        int basepos = -100;
        if(isPos()) {
            basepos = asPos();
        } else if(ttype == TT_WORD && sval.length() == 1) {
            basepos = sval.charAt(0) - 'A'; 
        } else if(ttype == TT_NUMBER) {
            basepos = mTarget.columns()*(int)nval;
        } else {
            fail();
        }
        for(int i = 0; i < s.repeatCount; i++) {
            int pos0 = basepos+i*s.repeatOffset;
            for(int k = 0; k < s.pattern.length-1; k+=2) {
                int pos1 = posAdd(pos0, s.pattern[k]);
                int pos2 = posAdd(pos0, s.pattern[k+1]);
                String a1 = parseAction();
                if(a1 == null) {
                    return;
                }
                Log.d(TAG, String.format("si %d %d %s", pos1, pos2, a1) );
                mTarget.mapGesture(pos1, pos2, a1);
            }
        }

    }

    private static int[] intArray(List<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }

    // reuses NOT, pushes back on fail
    String parseAction() {
        nextTok();
        if(ttype == '"' || ttype == '\'') {
            return sval; 
        } else if(ttype == TT_WORD) {
            if(isPos()) {
                pushBack();
                return null;
            } else {
                return sval;
            }
        } else if(ttype == '^') {
            nextTok();
            if(ttype == TT_WORD && sval.length() == 1) {
                char ch = sval.charAt(0);
                if('A' <= ch && ch <= 'Z') {
                    return String.format("%c",ch-'A'+1);
                } else {
                    fail(); return null; // FIXME
                }
            } else if(ttype == '[') {
                return "\033";
            } else {
                fail(); return null;//FIXME
            }
        } else if(ttype == TT_EOF || ttype == TT_EOL) {
            pushBack();
            return null;
        } else {
            Log.d(TAG, String.format("ttype %d %c", ttype, ttype));
            return String.format("%c",ttype);
        }
    }


    // reuses
    boolean isPos() {
        return ttype == TT_WORD && Pattern.matches("[A-Za-z][0-9]", sval);
    }

    // reuses
    int asPos() {
        char c0 = sval.charAt(0);
        int c = c0 > 'Z' ? c0 - 'a' : c0 - 'A';
        char r0 = sval.charAt(1);
        int r = r0 - '0';
        return mTarget.asPos(c,r);
    }

    int posAdd(int p0, int p1) {
        int c = mTarget.columns(), r = mTarget.rows();
        return c*((p0/c+p1/c ) % r) + (p0 + p1) % c;
    }
}

