package com.winom.multimedia.utils;

import android.media.AudioFormat;

import java.util.concurrent.TimeUnit;

public interface MediaConstants {
    int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    int BYTE_COUNT_PRE_SAMPLE = 2;
    int DURATION_PRE_AUDIO_FRAME = 20;

    long MS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);
    long MS_PER_MINUTE = TimeUnit.MINUTES.toMillis(1);
    long MS_PER_HOUR = TimeUnit.HOURS.toMillis(1);
    long MS_PER_DAY = TimeUnit.DAYS.toMillis(1);
}
