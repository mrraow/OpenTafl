package com.manywords.softworks.tafl.ui.player.external.network.server.threads;

import com.manywords.softworks.tafl.ui.player.external.network.server.NetworkServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jay on 5/22/16.
 */
public class NetworkPriorityTaskQueue {
    private NetworkServer mServer;

    private final List<Runnable> mHighPriorityQueue;
    private final List<Runnable> mStandardPriorityQueue;
    private final List<Runnable> mLowPriorityTaskQueue;

    public NetworkPriorityTaskQueue() {
        mHighPriorityQueue = new ArrayList<>(16);
        mStandardPriorityQueue = new ArrayList<>(32);
        mLowPriorityTaskQueue = new ArrayList<>(16);
    }

    public void pushHighPriorityTask(Runnable task) {
        synchronized (mHighPriorityQueue) {
            mHighPriorityQueue.add(task);
        }

        mServer.notifyThreadIfNecessary();
    }

    public void pushStandardPriorityTask(Runnable task) {
        synchronized (mStandardPriorityQueue) {
            mStandardPriorityQueue.add(task);
        }

        mServer.notifyThreadIfNecessary();
    }

    public void pushLowPriorityTask(Runnable task) {
        synchronized (mLowPriorityTaskQueue) {
            mLowPriorityTaskQueue.add(task);
        }

        mServer.notifyThreadIfNecessary();
    }

    public Runnable getTask() {
        // Take a task from the highest priority queue with tasks, unless a lower priority's queue is 4^depth times larger
        // than the queue in question.
        if(mHighPriorityQueue.size() > 0
                && !((mStandardPriorityQueue.size() > mHighPriorityQueue.size() * 4) || (mLowPriorityTaskQueue.size() > mHighPriorityQueue.size() * 16))) {
            synchronized (mHighPriorityQueue) {
                return mHighPriorityQueue.remove(0);
            }
        }
        else if(mStandardPriorityQueue.size() > 0 && !(mLowPriorityTaskQueue.size() > mStandardPriorityQueue.size() * 4)) {
            synchronized (mStandardPriorityQueue) {
                return mStandardPriorityQueue.remove(0);
            }
        }
        else if(mLowPriorityTaskQueue.size() > 0){
            synchronized (mLowPriorityTaskQueue) {
                return mLowPriorityTaskQueue.remove(0);
            }
        }

        return null;
    }
}
