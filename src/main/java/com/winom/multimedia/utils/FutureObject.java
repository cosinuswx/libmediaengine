package com.winom.multimedia.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public class FutureObject<T> implements Future<T> {
    private T mObject;
    private FutureTask<T> mFutureTask;

    public void setResult(T object) {
        createFutureTaskIfNeed();
        mObject = object;
        mFutureTask.run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        createFutureTaskIfNeed();
        return mFutureTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        createFutureTaskIfNeed();
        return mFutureTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        createFutureTaskIfNeed();
        return mFutureTask.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        createFutureTaskIfNeed();
        return mFutureTask.get();
    }

    @Override
    public T get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException {
        createFutureTaskIfNeed();
        return mFutureTask.get();
    }

    synchronized private void createFutureTaskIfNeed() {
        if (mFutureTask != null) {
            return;
        }

        mFutureTask = new FutureTask<>(() -> mObject);
    }
}
