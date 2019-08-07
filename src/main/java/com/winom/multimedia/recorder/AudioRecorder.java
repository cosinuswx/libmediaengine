package com.winom.multimedia.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder.AudioSource;
import android.os.SystemClock;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.pipeline.ProvidedStage;
import com.winom.multimedia.source.Frame;
import com.winom.multimedia.utils.MeLog;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AudioRecorder extends ProvidedStage<Frame> {
    private static final String TAG = "AudioRecorder";

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_FORMAT_IN_BYTE = 2;

    private final int mSampleRate;
    private final int mChannelCount;
    private final Queue<Frame> mFreeFrames = new LinkedList<>();

    private AudioRecord mAudioRecord;

    public AudioRecorder(int sampleRate, int channelCnt) {
        mSampleRate = sampleRate;
        mChannelCount = channelCnt;
    }

    @Override
    protected void recycleBuffers(List<Frame> canReuseBuffers) {
        synchronized (this) {
            mFreeFrames.addAll(canReuseBuffers);
        }
    }

    @Override
    public void setup() throws SetupException {
        int channelConfig = (mChannelCount == 1) ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        int minBufSize = AudioRecord.getMinBufferSize(mSampleRate, channelConfig, AUDIO_FORMAT);
        mAudioRecord = new AudioRecord(AudioSource.MIC, mSampleRate, channelConfig, AUDIO_FORMAT, 8 * minBufSize);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            throw new SetupException("init AudioRecord failed.");
        }
        mAudioRecord.startRecording();

        int bufferSize = mSampleRate * 20 / 1000 * mChannelCount * AUDIO_FORMAT_IN_BYTE;
        synchronized (this) {
            for (int i = 0; i < MAX_FRAME_COUNT; ++i) {
                mFreeFrames.add(new Frame(ByteBuffer.allocateDirect(bufferSize)));
            }
        }

        setState(State.SETUPED);
    }

    @Override
    public void processFrame() throws ProcessException {
        super.processFrame();

        Frame frame;
        synchronized (this) {
            frame = mFreeFrames.poll();
            if (frame == null) {
                return;
            }
        }

        if (mState == State.ALL_DATA_READY) {
            frame.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            frame.size = 0;
        } else {
            frame.size = mAudioRecord.read(frame.buffer, frame.buffer.capacity());
            frame.presentationTimeUs = SystemClock.elapsedRealtimeNanos() / 1000;
            MeLog.d(TAG, "read buffer size: %d", frame.size);
        }

        synchronized (this) {
            mWaitOutBuffers.add(frame);
        }
    }

    public void stop() {
        queueRunnable(() -> setState(State.ALL_DATA_READY));
    }

    @Override
    public void release() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
    }
}
