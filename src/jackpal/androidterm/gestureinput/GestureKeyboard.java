package jackpal.androidterm.gestureinput;
import android.util.Log;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
public class GestureKeyboard {
    private final String TAG = "GestureKeyboard";

    protected int mRows, mCols;
    private LayoutManager mLayout;
    private List<String> mActiveGroups = new ArrayList();

    protected int mPadX, mPadY;
    protected int mWidth, mHeight;

    protected boolean mMoving = false;
    protected int mStartPos;
    protected int mCurPos;

    private ActionListener mListener;

    public GestureKeyboard(int rows, int cols, ActionListener l) {
        mRows = rows;
        mCols = cols;
        mLayout = new LayoutManager(rows, cols);
        mLayout.createGroup("base");
        mActiveGroups.add("base");
        mListener = l;
    }

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

    public GKAction typeAction(String s) {
        return new TypeAction(s);
    }


    private class SetGroupAction extends GKAction {
        String mGroup;
        SetGroupAction(String group) {
            mGroup = group;
        }
        public String describe() {
            return "@"+mGroup;
        }
        public void perform() {
            mActiveGroups.clear();
            mActiveGroups.add(mGroup); //FIXME stack flag
            mActiveGroups.add("base");
        }
    }

    public GKAction setGroupAction(String g) {
        return new SetGroupAction(g);
    }

    public void addGroup(String group) {
        mLayout.createGroup(group);
    }

    public void addMapping(String group, int pos0, int pos1, GKAction a) {
        if( group == null) {
            group = "base";
        }
        mLayout.group(group)[pos0][pos1] = a;
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

    private GKAction getAction(int pos0, int pos1) {
        return mLayout.lookup(mActiveGroups, pos0, pos1);
    }

    private void performAction(int startPos, int endPos) {
        GKAction a = getAction(startPos, endPos);
        if(a != null) 
            a.perform();
    }

    private void displayAction(int startPos, int endPos) {
        if(startPos < 0 || endPos < 0 ) {
            display("");
            return;
        }
        GKAction a = getAction(startPos, endPos);
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


    static class LayoutManager {

        private int mRows, mCols;
        private Map<String,GKAction[][]> mGroups = new HashMap();
        int size;

        LayoutManager(int rows, int cols) {
            mRows = rows;
            mCols = cols;
            size = mRows*mCols;
        }

        void createGroup(String name) {
            if( mGroups.containsKey(name))
                return;
            GKAction[][] group = new GKAction[size][size];
            mGroups.put(name,group);
        }

        GKAction[][] group(String name) {
            return mGroups.get(name);
        }

        GKAction lookup(List<String> groups, int pos1, int pos2) {
            for(String g : groups) {
                if(!mGroups.containsKey(g)) {
                    throw new IllegalArgumentException("No such group: " + g);
                }
                GKAction a = group(g)[pos1][pos2];
                if( a != null) {
                    return a;
                }
            }
            return null;
        }




   }


}
class MapFileReader extends StreamTokenizer {
    private static final String TAG = "MapFileReader";
    private GestureKeyboard mTarget;
    private String mCurGroup = null;

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
        throw new RuntimeException("Parse error on line " + lineno());
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
        } else if(sval == "group") {
            parseGroup();
        } else if(mShapes.containsKey(sval)) {
            parseShapeInvoke(mShapes.get(sval));
        } else {
            fail();
        }
        return true;
    }

    // reuses
    void parseMapping() {
        int pos0 = asPos();
        nextTok();
        if(isPos()) {
            int pos1 = asPos();
            if(!parseAndMapAction(pos0,pos1)) {
                fail();
            }
            // revese mapping if exists
            parseAndMapAction(pos1,pos0);
        } else {
            pushBack();
            if(!parseAndMapAction(pos0,pos0)) {
                fail();
            }
        }
    }


    void parseGroup() {
        nextTok();
        if(ttype != TT_WORD) 
            fail();
        mCurGroup = sval;
        mTarget.addGroup(mCurGroup);
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
                if(!parseAndMapAction(pos1,pos2)) {
                    return;
                }
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

    GestureKeyboard.GKAction type(String t) {
        return mTarget.typeAction(t);
    }
    // reuses NOT, pushes back on fail
    GestureKeyboard.GKAction parseAction() {
        nextTok();
        if(ttype == '"' ) {
            return type(sval);
        } else if(ttype == '\'') {
            return type("\033"+sval);
        } else if(isPos()) {
                pushBack();
                return null;
        } else if(ttype == TT_WORD) {
                return type(sval);
        } else if(ttype == '^') {
            nextTok();
            if(ttype == TT_WORD && sval.length() == 1) {
                char ch = sval.charAt(0);
                if('A' <= ch && ch <= 'Z') {
                    return type(String.format("%c",ch-'A'+1));
                } else {
                    fail(); 
                }
            } else if(ttype == '[') {
                return type("\033");
            } else {
                fail();
            }
        } else if(ttype == '@') {
            nextTok();
            if(ttype != TT_WORD) 
                fail();
            return mTarget.setGroupAction(sval);
        } else if(ttype == TT_EOF || ttype == TT_EOL) {
            pushBack();
            return null;
        } else {
            Log.d(TAG, String.format("ttype %d %c", ttype, ttype));
            return type(String.format("%c",ttype));
        }
        return null;
    }

    private boolean parseAndMapAction(int pos0, int pos1) {
        GestureKeyboard.GKAction a1 = parseAction();
        if(a1 == null) {
            return false;
        }
        Log.d(TAG, String.format("si %d %d %s", pos0, pos1, a1.describe()) );
        mTarget.addMapping(mCurGroup, pos0, pos1, a1);
        return true;
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

