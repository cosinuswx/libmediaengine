package com.winom.multimedia.pipeline;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.utils.DelayRunQueue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import androidx.annotation.CallSuper;

/**
 * 会有数据供给出来的Stage
 * @param <T> 供给处理的数据类型
 */
public abstract class ProvidedStage<T> extends Stage implements Provider<T> {
    /**
     * 该Stage已经处理好了，等待下一个Stage来读取
     */
    protected final Queue<T> mProcessedBuffers = new LinkedList<>();

    /**
     * 下一个Stage已经处理过的Buffer，归还回来的
     */
    protected final Queue<T> mRecycledBuffers = new LinkedList<>();

    /**
     * 其他线程抛过来任务，在{@link #processFrame()}中执行，保证在同一个线程来处理
     */
    protected final DelayRunQueue mDelayRunQueue = new DelayRunQueue();

    /**
     * 已被下一个Stage持有的buffer的个数
     */
    protected int mBufferOutedCount = 0;

    /**
     * 出队一个该Stage处理好的Buffer
     */
    @Override
    public T dequeueOutputBuffer() {
        synchronized (this) {
            T t = mProcessedBuffers.poll();
            if (t != null) {
                mBufferOutedCount++;
            }
            return t;
        }
    }

    /**
     * 将前面出队的buffer再进队
     * @param buffer 前面出队的buffer
     */
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
            if (isAllDataReady()               // 所有数据都准备完成了
                && mRecycledBuffers.isEmpty()  // 归还回来的buffer都已经做过回收处理
                && mProcessedBuffers.isEmpty() // 该Stage处理过的数据已经被消费了
                && mBufferOutedCount == 0) {   // 该Stage所有缓存都归还回来了
                setState(State.DONE);
            }
        }
    }

    /**
     * 添加一个处理好后的数据到队列中
     * @param buffer 处理好的数据
     */
    protected void enqueueProcessedBuffer(T buffer) {
        synchronized (this) {
            mProcessedBuffers.add(buffer);
        }
    }

    protected void queueRunnable(Runnable runnable) {
        mDelayRunQueue.addRunnable(runnable);
    }

    /**
     * 归还回来的数据缓存做回收处理
     * @param canReuseBuffers 下一个Stage归还回来的buffer
     */
    protected abstract void recycleBuffers(List<T> canReuseBuffers);
}
