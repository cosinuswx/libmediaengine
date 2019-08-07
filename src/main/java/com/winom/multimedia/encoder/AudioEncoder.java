package com.winom.multimedia.encoder;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.winom.multimedia.pipeline.Provider;
import com.winom.multimedia.source.Frame;

public class AudioEncoder extends Encoder {
    private static final String MIME_TYPE = "audio/mp4a-latm";

    public AudioEncoder(MediaFormat mediaFormat, Provider<Frame> provider) {
        super(mediaFormat, provider);
    }

    public static MediaFormat buildMediaFormat(int sampleRate, int channelCnt, int bitRate) {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, channelCnt);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        return mediaFormat;
    }
}
