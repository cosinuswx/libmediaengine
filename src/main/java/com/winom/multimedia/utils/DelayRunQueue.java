package com.winom.multimedia.utils;

import java.util.ArrayList;
import java.util.List;

public class DelayRunQueue {
    private final List<Runnable> mDelayList = new ArrayList<>();

    public void addRunnable(Runnable runnable) {
        synchronized (mDelayList) {
            mDelayList.add(runnable);
        }
    }

    public void rerun() {
        synchronized (mDelayList) {
            if (mDelayList.size() == 0) {
                return;
            }

            int oldSize = mDelayList.size();
            for (int i = 0; i < mDelayList.size(); ++i) {
                mDelayList.get(i).run();
            }

            // 检查下同步执行过程中有没有新增，如果有的话就报错
            if (oldSize != mDelayList.size()) {
                throw new RuntimeException("can't add runnable to list when it call all runnable!");
            } else {
                mDelayList.clear();
            }
        }
    }

    public void clear() {
        synchronized (mDelayList) {
            mDelayList.clear();
        }
    }
}
