package com.winom.multimedia.pipeline;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.utils.MeLog;

/**
 * 媒体处理管道中Stage
 */
public abstract class Stage {
    private static final String TAG = "Stage";
    protected static final int MAX_FRAME_COUNT = 3;

    protected enum State {
        /**
         * 该Stage为初始阶段
         */
        INIT,

        /**
         * 该Stage处理设置完成
         */
        SETUPED,

        /**
         * 该Stage的数据都准备好了
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

    /**
     * 设置该阶段的状态
     * @param newState 新状态
     */
    protected void setState(State newState) {
        mState = newState;
        if (State.DONE == mState) {
            MeLog.i(TAG, "[%s] is done", this);
        }
    }

    protected boolean isAllDataReady() {
        return mState == State.ALL_DATA_READY;
    }

    public boolean isDone() {
        return mState == State.DONE;
    }
}
