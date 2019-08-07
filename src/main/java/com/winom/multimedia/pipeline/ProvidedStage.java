package com.winom.multimedia.pipeline;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.utils.DelayRunQueue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import androidx.annotation.CallSuper;

public abstract class ProvidedStage<T> extends Stage implements Provider<T> {
    protected final Queue<T> mWaitOutBuffers = new LinkedList<>();
    protected final Queue<T> mRecycledBuffers = new LinkedList<>();
    protected final DelayRunQueue mDelayRunQueue = new DelayRunQueue();
    protected int mBufferOutedCount = 0;

    @Override
    public T dequeueOutputBuffer() {
        synchronized (this) {
            T t = mWaitOutBuffers.poll();
            if (t != null) {
                mBufferOutedCount++;
            }
            return t;
        }
    }

    @Override
    public void enqueueOutputBuffer(T buffer) {
        synchronized (this) {
            mBufferOutedCount --;
            mRecycledBuffers.add(buffer);
        }
    }

    @CallSuper
    @Override
    public void processFrame() throws ProcessException {
        List<T> canReuseBuffers;
        synchronized (this) {
            canReuseBuffers = new ArrayList<>(mRecycledBuffers);
            mRecycledBuffers.clear();
        }
        recycleBuffers(canReuseBuffers);

        mDelayRunQueue.rerun();

        synchronized (this) {
            if (isAllDataReady() && noBufferKeepByUs()) {
                setState(State.DONE);
            }
        }
    }

    public void queueRunnable(Runnable runnable) {
        mDelayRunQueue.addRunnable(runnable);
    }

    protected abstract void recycleBuffers(List<T> canReuseBuffers);

    protected boolean noBufferKeepByUs() {
        synchronized (this) {
            return mRecycledBuffers.isEmpty() && mWaitOutBuffers.isEmpty() && mBufferOutedCount == 0;
        }
    }
}
