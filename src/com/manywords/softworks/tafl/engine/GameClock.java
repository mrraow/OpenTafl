package com.manywords.softworks.tafl.engine;

import com.manywords.softworks.tafl.rules.Rules;
import com.manywords.softworks.tafl.rules.Side;

import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jay on 2/20/16.
 */
public class GameClock {
    public static final String TIME_SPEC_REGEX = "\\s*(\\d+)(\\s\\d+/\\d+)?(\\s\\d+i)?";
    private final Game mGame;
    private final long mMainTimeMillis;
    private final long mIncrementMillis;
    private final long mOvertimeMillis;
    private final int mOvertimeCount;

    private GameClockCallback mCallback;

    private UpdateThread mUpdateThread;
    private boolean mOutOfTime = false;
    private boolean mRunning = false;

    private static final int ATTACKERS = 0;
    private static final int DEFENDERS = 1;
    private final ClockEntry[] mClocks = new ClockEntry[2];
    private int mCurrentPlayer = ATTACKERS;

    private long mLastStartTime = 0;

    public GameClock(Game g, Side attackers, Side defenders, TimeSpec timeSpec) {
        this(g, attackers, defenders, timeSpec.mainTime, timeSpec.incrementTime, timeSpec.overtimeTime, timeSpec.overtimeCount);
    }

    public GameClock(Game g, Side attackers, Side defenders, long mainTime, long incrementTime, long overtimeTime, int overtimeCount) {
        mGame = g;
        mMainTimeMillis = mainTime;
        mIncrementMillis = incrementTime;
        mOvertimeMillis = overtimeTime;
        mOvertimeCount = overtimeCount;

        mClocks[ATTACKERS] = new ClockEntry(this, attackers, mainTime, overtimeTime, overtimeCount);
        mClocks[DEFENDERS] = new ClockEntry(this, defenders, mainTime, overtimeTime, overtimeCount);
    }

    public TimeSpec toTimeSpec() {
        return new TimeSpec(mMainTimeMillis, mOvertimeMillis, mOvertimeCount, mIncrementMillis);
    }

    public void setCallback(GameClockCallback callback) {
        mCallback = callback;
    }

    public ClockEntry start(Side startingSide) {
        mRunning = true;
        mLastStartTime = System.currentTimeMillis();

        if(startingSide.isAttackingSide()) {
            mCurrentPlayer = ATTACKERS;
        }
        else {
            mCurrentPlayer = DEFENDERS;
        }

        if(mMainTimeMillis == 0 || mClocks[mCurrentPlayer].mMainTimeMillis == 0) {
            mClocks[mCurrentPlayer].mOvertimeMillis = mOvertimeMillis + mIncrementMillis;
        }
        else {
            mClocks[mCurrentPlayer].mMainTimeMillis += mIncrementMillis;
        }

        mUpdateThread = new UpdateThread();
        mUpdateThread.start();

        return mClocks[mCurrentPlayer];
    }

    public void stop() {
        mRunning = false;
        mUpdateThread.cancel();
    }

    /**
     * Change sides on the game clock, decrementing the current player's clock entry and
     * returning the new player's clock entry.
     * @return
     */
    public ClockEntry slap(boolean switchSides) {
        if(!mRunning) return null;

        ClockEntry clock;
        synchronized (mClocks) {
            updateClocks();

            if(switchSides) mCurrentPlayer = ++mCurrentPlayer % 2;

            clock = mClocks[mCurrentPlayer];
            if(clock.mMainTimeMillis > 0) {
                clock.mMainTimeMillis += mIncrementMillis;
            }
            else {
                clock.mOvertimeMillis = mOvertimeMillis + mIncrementMillis;
            }
        }
        mCallback.timeUpdate(mClocks[mCurrentPlayer].mSide);
        return mClocks[mCurrentPlayer];
    }

    public long getMainTime() {
        return mMainTimeMillis;
    }

    public long getOvertimeTime() {
        return mOvertimeMillis;
    }

    public int getOvertimeCount() {
        return mOvertimeCount;
    }

    public long getIncrementTime() {
        return mIncrementMillis;
    }

    public ClockEntry getClockEntry(Side side) {
        if(side.isAttackingSide()) return mClocks[ATTACKERS];
        else return mClocks[DEFENDERS];
    }

    public ClockEntry getClockEntry(boolean attackers) {
        if(attackers) return mClocks[ATTACKERS];
        else return mClocks[DEFENDERS];
    }

    private void updateClocks() {
        if(mOutOfTime) return;

        synchronized (mClocks) {
            ClockEntry currentEntry = mClocks[mCurrentPlayer];
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - mLastStartTime;
            mLastStartTime = currentTime;

            long leftover = 0;
            if (currentEntry.mMainTimeMillis > 0) {
                currentEntry.mMainTimeMillis -= elapsed;
                if(currentEntry.mMainTimeMillis < 0) {
                    leftover = -currentEntry.mMainTimeMillis;
                    currentEntry.mMainTimeMillis = 0;
                }
            }
            else {
                leftover = elapsed;
            }

            if(leftover == 0) return;

            if (currentEntry.mOvertimeMillis > 0 && currentEntry.mOvertimeCount > 0) {
                currentEntry.mOvertimeMillis -= leftover;

                if(currentEntry.mOvertimeMillis < 0) {
                    leftover = -currentEntry.mOvertimeMillis;

                    currentEntry.mOvertimeCount--;
                    if(currentEntry.mOvertimeCount > 0) {
                        // Don't care about the case where we lose two overtimes at once,
                        // because we check often enough that we'll never hit that.
                        currentEntry.mOvertimeMillis = mOvertimeMillis - leftover;
                    }
                    else {
                        mOutOfTime = true;
                    }
                }
            }
            else {
                mOutOfTime = true;
            }
        }
    }

    private class UpdateThread extends Thread {
        private boolean mRunning = true;
        private static final int UPDATE_INTERVAL = 100;

        @Override
        public void run() {
            while(mRunning) {
                try {
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    // no-op
                }

                updateClocks();
                mCallback.timeUpdate(mClocks[mCurrentPlayer].mSide);
                if(mOutOfTime) {
                    mCallback.timeExpired(mClocks[mCurrentPlayer].mSide);
                }
            }
        }

        public void cancel() {
            mRunning = false;
        }
    }

    public static class TimeSpec {
        public long mainTime = 0;
        public long overtimeTime = 0;
        public int overtimeCount = 0;
        public long incrementTime = 0;

        public TimeSpec(long mainTime, long overtimeTime, int overtimeCount, long incrementTime) {
            this.mainTime = mainTime;
            this.overtimeTime = overtimeTime;
            this.overtimeCount = overtimeCount;
            this.incrementTime = incrementTime;
        }

        public String toString() {
            return mainTime / 1000 + " " + overtimeTime / 1000 + "/" + overtimeCount + " " + incrementTime / 1000 + "i";
        }

        public String toGameNotationString() {
            return mainTime / 1000 + " " + overtimeTime / 1000 + "/" + overtimeCount;
        }

        public String toHumanString() {
            int mainTimeSeconds = (int) mainTime / 1000;
            int overtimeSeconds = (int) overtimeTime / 1000;

            int hours = mainTimeSeconds / 3600;
            int minutes = (mainTimeSeconds % 3600) / 60;
            int seconds = (mainTimeSeconds % 3600) % 60;
            String m = (minutes >= 10 ? "" + minutes : "0" + minutes);
            String s = (seconds >= 10 ? "" + seconds : "0" + seconds);
            String mainTime = hours + ":" + m + ":" + s;

            hours = overtimeSeconds / 3600;
            minutes = (overtimeSeconds % 3600) / 60;
            seconds = (overtimeSeconds % 3600) % 60;
            m = (minutes >= 10 ? "" + minutes : "0" + minutes);
            s = (seconds >= 10 ? "" + seconds : "0" + seconds);
            String overtimeTime = hours + ":" + m + ":" + s;

            String result = mainTime + " " + overtimeTime + "/" + overtimeCount;
            return result;
        }

        public String toMillisString() {
            return mainTime  + " " + overtimeTime + "/" + overtimeCount + " " + incrementTime + "i";
        }
    }

    public interface GameClockCallback {
        public void timeUpdate(Side currentSide);
        public void timeExpired(Side currentSide);
    }

    public static class ClockEntry {
        private GameClock mGameClock;
        private Side mSide;
        private long mMainTimeMillis;
        private long mOvertimeMillis;
        private int mOvertimeCount;

        public ClockEntry(GameClock clock, Side side, long mainTime, long overtimeTime, int overtimeCount) {
            mGameClock = clock;
            mSide = side;
            mMainTimeMillis = mainTime;
            mOvertimeMillis = overtimeTime;
            mOvertimeCount = overtimeCount;
        }

        public GameClock getClock() {
            return mGameClock;
        }

        public void setTime(TimeSpec ts) {
            mMainTimeMillis = ts.mainTime;
            mOvertimeMillis = ts.overtimeTime;
            mOvertimeCount = ts.overtimeCount;
        }

        public long getMainTime() {
            return mMainTimeMillis;
        }

        public long getOvertimeTime() { return mOvertimeMillis; }

        public int getOvertimeCount() {
            return mOvertimeCount;
        }

        public boolean mainTimeExpired() {
            return mMainTimeMillis <= 0;
        }

        public TimeSpec toTimeSpec() {
            return new TimeSpec(mMainTimeMillis, mOvertimeMillis, mOvertimeCount, 0);
        }

        public String toString() {
            String result = "";
            result += (mSide.isAttackingSide() ? "Attacker clock " : "Defender clock ") + mMainTimeMillis / 1000d + " " + mOvertimeMillis / 1000d + "/" + mOvertimeCount;
            return result;
        }

        public String toHumanReadableString() {
            int mainTimeSeconds = (int) mMainTimeMillis / 1000;
            int overtimeSeconds = (int) mOvertimeMillis / 1000;

            int hours = mainTimeSeconds / 3600;
            int minutes = (mainTimeSeconds % 3600) / 60;
            int seconds = (mainTimeSeconds % 3600) % 60;
            String m = (minutes >= 10 ? "" + minutes : "0" + minutes);
            String s = (seconds >= 10 ? "" + seconds : "0" + seconds);
            String mainTime = hours + ":" + m + ":" + s;

            hours = overtimeSeconds / 3600;
            minutes = (overtimeSeconds % 3600) / 60;
            seconds = (overtimeSeconds % 3600) % 60;
            m = (minutes >= 10 ? "" + minutes : "0" + minutes);
            s = (seconds >= 10 ? "" + seconds : "0" + seconds);
            String overtimeTime = hours + ":" + m + ":" + s;

            String result = mainTime + " " + overtimeTime + "/" + mOvertimeCount;
            return result;
        }
    }

    public static TimeSpec getTimeSpecForGameNotationString(String string) {
        Pattern p = Pattern.compile(TIME_SPEC_REGEX);
        Matcher m = p.matcher(string);

        if(m.lookingAt()) {
            for(int i = 0; i < m.groupCount(); i++) {
                System.out.println("group " + i + ": " + m.group(i));
            }
            long mainTime = Long.parseLong(m.group(1)) * 1000;

            long overtimeTime = 0;
            int overtimeCount = 0;
            if(m.groupCount() >= 2 && m.group(2) != null) {
                String trimmed = m.group(2).trim();
                String[] overtimeParts = trimmed.split("/");
                overtimeTime = Long.parseLong(overtimeParts[0]) * 1000;
                overtimeCount = Integer.parseInt(overtimeParts[1]);
            }

            long incrementTime = 0;
            if(m.groupCount() >= 4 && m.group(3) != null) {
                String trimmed = m.group(3).trim();
                incrementTime = Long.parseLong(trimmed) * 1000;
            }

            TimeSpec ts = new TimeSpec(mainTime, overtimeTime, overtimeCount, incrementTime);
            System.out.println(ts.toMillisString());
            return ts;
        }

        return null;
    }
}
