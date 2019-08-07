package com.winom.multimedia.pipeline;

import android.os.SystemClock;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.ReleaseException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.utils.MeLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StageTask implements Runnable {
    private static final String TAG = "StageTask";
    private static final int DEFAULT_FRAME_PROCESS_INTERVAL = 3;

    private final List<Stage> mStages;
    private final String mTaskName;
    private final int mFrameProcessInterval;

    private boolean mIsPaused = false;
    private boolean mIsCanceled = false;

    public StageTask(String runnableName) {
        this(runnableName, null, DEFAULT_FRAME_PROCESS_INTERVAL);
    }

    public StageTask(String runnableName, Stage stage) {
        this(runnableName, Collections.singletonList(stage), DEFAULT_FRAME_PROCESS_INTERVAL);
    }

    public StageTask(String runnableName, List<Stage> stages) {
        this(runnableName, stages, DEFAULT_FRAME_PROCESS_INTERVAL);
    }

    public StageTask(String runnableName, List<Stage> stages, int frameProcessInterval) {
        mTaskName = runnableName;
        mFrameProcessInterval = frameProcessInterval;
        if (stages != null) {
            mStages = new ArrayList<>(stages);
        } else {
            mStages = new ArrayList<>();
        }
    }

    public void addStage(Stage stage) {
        mStages.add(stage);
    }

    public void cancel() {
        mIsCanceled = true;
        MeLog.i(TAG, "cancel task(%s)", mTaskName);
    }

    public void pause() {
        mIsPaused = true;
        MeLog.i(TAG, "pause task(%s)", mTaskName);
    }

    public void resume() {
        mIsPaused = false;
        MeLog.i(TAG, "resume task(%s)", mTaskName);
    }

    public String getTaskName() {
        return mTaskName;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(mTaskName);
        long startTime = SystemClock.elapsedRealtime();

        try {
            setup();
        } catch (SetupException e) {
            throw new RuntimeException(e);
        }

        try {
            MeLog.i(TAG, "start process task(%s)", mTaskName);
            processFrames();
        } catch (ProcessException e) {
            throw new RuntimeException(e);
        }

        try {
            release();
        } catch (ReleaseException e) {
            throw new RuntimeException(e);
        }
        MeLog.i(TAG, "[%s] stage cost: %d", mTaskName, (SystemClock.elapsedRealtime() - startTime));
    }

    private void processFrames() throws ProcessException {
        boolean isAllDone = false;
        do {
            if (mIsCanceled) {
                break;
            }

            long frameStartTime = SystemClock.elapsedRealtime();

            if (!mIsPaused) {
                isAllDone = true;
                for (Stage stage : mStages) {
                    if (!stage.isDone()) {
                        isAllDone = false;
                        stage.processFrame();
                    }
                }
            }

            // 如果一帧的处理时长太短，增加sleep，防止占用太高CPU。
            long frameCost = SystemClock.elapsedRealtime() - frameStartTime;
            if (frameCost < mFrameProcessInterval) {
                try {
                    Thread.sleep(mFrameProcessInterval - frameCost);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (!isAllDone);
    }

    private void setup() throws SetupException {
        MeLog.i(TAG, "setup task(%s)", mTaskName);
        for (Stage stage : mStages) {
            stage.setup();
        }
        MeLog.i(TAG, "task(%s) setupped", mTaskName);
    }

    private void release() throws ReleaseException {
        MeLog.i(TAG, "release task(%s)", mTaskName);
        for (Stage stage : mStages) {
            stage.release();
        }
        MeLog.i(TAG, "task(%s) released", mTaskName);
    }
}
