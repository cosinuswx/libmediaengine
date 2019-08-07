package com.winom.multimedia.pipeline;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.utils.MeLog;

/**
 * 针对于每一帧进行处理的模块
 */
public abstract class Stage {
    private static final String TAG = "Stage";
    protected static final int MAX_FRAME_COUNT = 3;

    protected enum State {
        INIT,
        SETUPED,

        /**
         * 所有数据都准备好了，下一个节点读取完成后就算结束
         */
        ALL_DATA_READY,

        /**
         * 这个Stage处理完成了
         */
        DONE
    }

    protected State mState = State.INIT;

    /**
     * 初始化设置
     */
    public abstract void setup() throws SetupException;

    /**
     * <p>处理一帧</p>
     * 该方法中不允许等待
     */
    public abstract void processFrame() throws ProcessException;

    /**
     * 释放持有的资源
     */
    public abstract void release();

    public boolean isDone() {
        return mState == State.DONE;
    }

    protected void setState(State state) {
        mState = state;
        if (State.DONE == mState) {
            MeLog.i(TAG, "[%s] is done", this);
        }
    }

    protected boolean isAllDataReady() {
        return mState == State.ALL_DATA_READY || mState == State.DONE;
    }
}
