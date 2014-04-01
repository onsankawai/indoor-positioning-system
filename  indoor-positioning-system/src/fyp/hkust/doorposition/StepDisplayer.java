package fyp.hkust.doorposition;

import java.util.ArrayList;

import android.os.SystemClock;



/**
 * Counts steps provided by StepDetector and passes the current
 * step count to the activity.
 */
public class StepDisplayer implements StepListener {

    private int mCount = 0;
    private long mPrevTimeMillis;
    private long mCurTimeMillis;

    public StepDisplayer() {
    	mPrevTimeMillis = SystemClock.uptimeMillis();
    	mCurTimeMillis = mPrevTimeMillis;
        notifyListener();
    }
    
    public long getTimeElapsedMillis() {
    	mCurTimeMillis = SystemClock.uptimeMillis();
    	return mCurTimeMillis - mPrevTimeMillis; 
    }
    
    public void updatePrevTimeMillis() {
    	mPrevTimeMillis = mCurTimeMillis;
    }

    public void setSteps(int steps) {
        mCount = steps;
        notifyListener();
    }
    public void onStep() {
        mCount ++;
        notifyListener();
    }
    public void reloadSettings() {
        notifyListener();
    }
    public void passValue() {
    }
    
    

    //-----------------------------------------------------
    // Listener
    
    public interface Listener {
        public void stepsChanged(int value);
    }
    private ArrayList<Listener> mListeners = new ArrayList<Listener>();

    public void addListener(Listener l) {
        mListeners.add(l);
    }
    public void notifyListener() {
        for (Listener listener : mListeners) {
            listener.stepsChanged((int)mCount);
        }
    }
      
}
