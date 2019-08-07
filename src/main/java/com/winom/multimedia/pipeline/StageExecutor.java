package com.winom.multimedia.pipeline;

import com.winom.multimedia.utils.FutureObject;
import com.winom.multimedia.utils.MeLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

@SuppressWarnings("WeakerAccess")
public class StageExecutor {
    private static final String TAG = "StageExecutor";

    /**
     * 该Executor的事件监听接口
     */
    public interface ExecutorListener {
        /**
         * <p>当所有任务都完成的时候回调</p>
         * @param executor 用来执行任务的Exectuor
         */
        void onAllTaskFinished(StageExecutor executor);

        /**
         * <p>当有任务执行失败的时候回调。此时，整个Executor中所有的任务都会被取消。</p>
         * @param executor 用来执行任务的Exectuor
         * @param failedTask 哪个任务触发的失败
         * @param error 失败的原因
         */
        void onTaskFailed(StageExecutor executor, StageTask failedTask, Throwable error);
    }

    private final Map<StageTask, Future> mTasks;
    private ExecutorListener mExecutorListener;
    private ExecutorService mExecutorService;

    public StageExecutor(@NonNull List<StageTask> stageTasks, ExecutorListener listener) {
        mExecutorListener = listener;
        mTasks = new HashMap<>();
        for (StageTask task : stageTasks) {
            mTasks.put(task, null);
        }
    }

    synchronized public void start() {
        mExecutorService = new FixedExecutorService(mTasks.keySet().size());
        for (Map.Entry<StageTask, Future> entry : mTasks.entrySet()) {
            Future future = mExecutorService.submit(entry.getKey());
            mTasks.put(entry.getKey(), future);
        }
    }

    synchronized public void cancel() {
        for (Map.Entry<StageTask, Future> entry : mTasks.entrySet()) {
            entry.getKey().cancel();
        }
    }

    synchronized public void pause() {
        for (Map.Entry<StageTask, Future> entry : mTasks.entrySet()) {
            entry.getKey().pause();
        }
    }

    synchronized public void resume() {
        for (Map.Entry<StageTask, Future> entry : mTasks.entrySet()) {
            entry.getKey().resume();
        }
    }

    synchronized private void onStageTaskFinished(Future future, Throwable error) {
        StageTask finishedTask = null;
        for (Map.Entry<StageTask, Future> entry : mTasks.entrySet()) {
            if (entry.getValue() == future) {
                finishedTask = entry.getKey();
                break;
            }
        }

        if (finishedTask == null) {
            throw new RuntimeException("task finished, but didn't find task in map");
        }

        MeLog.i(TAG, "task[%s] finished, error: %s", finishedTask.getTaskName(),
                android.util.Log.getStackTraceString(error));
        mTasks.remove(finishedTask);

        // 有任务失败，取消所有任务，回调接口
        if (error != null) {
            cancel();
            if (mExecutorListener != null) {
                notifyTaskIsFinished(error, finishedTask);
                mExecutorListener = null;
            }
            shutdownExecutor();
            return;
        }

        // 任务全部完成
        if (mTasks.isEmpty() && mExecutorListener != null) {
            notifyTaskIsFinished(null, null);
            mExecutorListener = null;
            shutdownExecutor();
        }
    }

    private void shutdownExecutor() {
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
    }

    private void notifyTaskIsFinished(Throwable error, StageTask failedTask) {
        if (mExecutorListener == null) {
            return;
        }

        // 这里回到执行，防止出现问题，先暂存下回调接口
        ExecutorListener listener = mExecutorListener;
        if (error == null) {
            listener.onAllTaskFinished(this);
        } else {
            listener.onTaskFailed(this, failedTask, error);
        }
    }

    private class FixedExecutorService extends ThreadPoolExecutor {

        public FixedExecutorService(int threadCount) {
            super(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t == null && r instanceof Future<?> && ((Future<?>) r).isDone()) {
                try {
                    ((Future<?>) r).get();
                } catch (CancellationException ce) {
                    t = ce;
                } catch (ExecutionException ee) {
                    t = ee.getCause();
                } catch (InterruptedException ie) {
                    // ignore/reset
                    Thread.currentThread().interrupt();
                }
            }
            onStageTaskFinished((Future) r, t);
        }
    }
}
