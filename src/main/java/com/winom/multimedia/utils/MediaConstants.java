package com.winom.multimedia.utils;

import java.util.concurrent.TimeUnit;

public interface MediaConstants {
    int DURATION_PRE_AUDIO_FRAME = 20;

    long MS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);
    long MS_PER_MINUTE = TimeUnit.MINUTES.toMillis(1);
    long MS_PER_HOUR = TimeUnit.HOURS.toMillis(1);
    long MS_PER_DAY = TimeUnit.DAYS.toMillis(1);
}
