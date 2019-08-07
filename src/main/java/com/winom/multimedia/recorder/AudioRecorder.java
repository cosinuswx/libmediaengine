package com.winom.multimedia.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder.AudioSource;
import android.os.Process;
import android.os.SystemClock;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.pipeline.ProvidedStage;
import com.winom.multimedia.source.Frame;
import com.winom.multimedia.utils.MeLog;
import com.winom.multimedia.utils.MediaConstants;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class AudioRecorder extends ProvidedStage<Frame> {
    private static final String TAG = "AudioRecorder";

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_FORMAT_IN_BYTE = 2;

    private final int mSampleRate;
    private final int mChannelCount;
    private final Queue<Frame> mFreeFrames = new LinkedList<>();

    private long mStartTick = -1;
    private int mByteCountPreMs;
    private long mByteCountRead = 0;
    private AudioRecord mAudioRecord;

    public AudioRecorder(int sampleRate, int channelCnt) {
        mSampleRate = sampleRate;
        mChannelCount = channelCnt;
    }

    @Override
    protected void recycleBuffers(List<Frame> canReuseBuffers) {
        mFreeFrames.addAll(canReuseBuffers);
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

        mByteCountPreMs = (int) (mSampleRate / MediaConstants.MS_PER_SECOND * mChannelCount * AUDIO_FORMAT_IN_BYTE);
        int bufferSize = MediaConstants.DURATION_PRE_AUDIO_FRAME * mByteCountPreMs;
        for (int i = 0; i < MAX_FRAME_COUNT; ++i) {
            mFreeFrames.add(new Frame(ByteBuffer.allocateDirect(bufferSize)));
        }

        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        setState(State.SETUPED);
    }

    @Override
    public void processFrame() throws ProcessException {
        super.processFrame();
        Frame frame = mFreeFrames.poll();
        if (frame == null) {
            return;
        }

        if (mState == State.ALL_DATA_READY) {
            frame.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            frame.size = 0;
        } else {
            frame.size = mAudioRecord.read(frame.buffer, frame.buffer.capacity());
            MeLog.d(TAG, "read buffer size: %d", frame.size);
        }

        // 根据开始时间还有读取的数据来计算时间戳
        if (mStartTick == -1) {
            long now = TimeUnit.MICROSECONDS.toMillis(SystemClock.elapsedRealtimeNanos());
            mStartTick = now - frame.size / mByteCountPreMs;
        }
        frame.presentationTimeUs = mStartTick + mByteCountRead / mByteCountPreMs;
        mByteCountRead += frame.size;

        enqueueProcessedBuffer(frame);
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
