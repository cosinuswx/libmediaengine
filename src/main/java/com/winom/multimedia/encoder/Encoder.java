package com.winom.multimedia.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.pipeline.ProvidedStage;
import com.winom.multimedia.pipeline.Provider;
import com.winom.multimedia.source.Frame;
import com.winom.multimedia.utils.FutureObject;
import com.winom.multimedia.utils.JniEntry;
import com.winom.multimedia.utils.MeLog;
import com.winom.multimedia.utils.MediaUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class Encoder extends ProvidedStage<Frame> {
    private final static String TAG = "Encoder";

    private final boolean mUseInputSurface;
    private final MediaFormat mMediaFormat;
    private final Provider<Frame> mRawFramesProvider;
    private final MediaCodec.BufferInfo mBufferInfo;
    private final FutureObject<Surface> mFutureInputSurface = new FutureObject<>();
    private final FutureObject<MediaFormat> mFutureOutputFormat = new FutureObject<>();

    private int mInputBufferIndex = -1;
    private MediaCodec mMediaCodec;

    public Encoder(MediaFormat mediaFormat, Provider<Frame> provider) {
        this(false, mediaFormat, provider);
    }

    public Encoder(boolean useInputSurface, MediaFormat mediaFormat, Provider<Frame> provider) {
        mUseInputSurface = useInputSurface;
        mMediaFormat = mediaFormat;
        mRawFramesProvider = provider;
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    @Override
    public void setup() throws SetupException {
        String mimeType = mMediaFormat.getString(MediaFormat.KEY_MIME);
        MeLog.i(TAG, "updateExtractor encoder for [%s]", mimeType);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(mimeType);
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            if (mUseInputSurface) {
                mFutureInputSurface.setResult(mMediaCodec.createInputSurface());
                MeLog.i(TAG, "created input surface");
            }
            mMediaCodec.start();
        } catch (IOException e) {
            throw new SetupException("updateExtractor MediaCodec for encoder failed.", e);
        }

        setState(State.SETUPED);
    }

    @Override
    public void processFrame() throws ProcessException {
        super.processFrame();
        if (mUseInputSurface) {
            checkForEos();
        } else {
            feedDataToMediaCodec();
        }
        drainEncodedFrame();
    }

    @Override
    public void release() {
        MeLog.i(TAG, "release encoder");
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    public FutureObject<Surface> getFutureInputSurface() {
        return mFutureInputSurface;
    }

    public FutureObject<MediaFormat> getFutureOutputFormat() {
        return mFutureOutputFormat;
    }

    private void checkForEos() {
        Frame frame = mRawFramesProvider.dequeueOutputBuffer();
        if (frame == null) {
            return;
        }

        if (MediaUtils.hasEosFlag(frame.flags)) {
            mMediaCodec.signalEndOfInputStream();
            MeLog.i(TAG, "Sent eos to Encoder");
        }
        mRawFramesProvider.enqueueOutputBuffer(frame);
    }

    private void feedDataToMediaCodec() {
        if (isAllDataReady()) {
            return;
        }

        // 如果之前没有获取到输入Buffer，则需要重新获取下。
        if (mInputBufferIndex < 0) {
            mInputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
        }
        if (mInputBufferIndex < 0) {
            return;
        }

        Frame frame = mRawFramesProvider.dequeueOutputBuffer();
        if (frame == null) {
            return;
        }

        if (MediaUtils.hasEosFlag(frame.flags)) {
            frame.size = 0;
        }

        ByteBuffer inputBuffer = mMediaCodec.getInputBuffers()[mInputBufferIndex];
        if (frame.size > 0) {
            JniEntry.byteBufferCopy(frame.buffer, inputBuffer, frame.size);
        }

        MeLog.v(TAG, "encoder queueInputBuffer %d, flags: %d", frame.presentationTimeUs, frame.flags);
        mMediaCodec.queueInputBuffer(mInputBufferIndex, inputBuffer.position(), frame.size,
                frame.presentationTimeUs, frame.flags);
        mInputBufferIndex = -1;

        mRawFramesProvider.enqueueOutputBuffer(frame);
    }

    @Override
    protected void recycleBuffers(List<Frame> canReuseBuffers) {
        for (Frame frame : canReuseBuffers) {
            mMediaCodec.releaseOutputBuffer(frame.bufferIndex, false);
        }
    }

    private void drainEncodedFrame() {
        synchronized (this) {
            if (mWaitOutBuffers.size() >= MAX_FRAME_COUNT) {
                return;
            }
        }

        int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
        if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            return;
        }

        if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            MeLog.i(TAG, "decoder output buffers changed");
            return;
        }

        if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            mFutureOutputFormat.setResult(mMediaCodec.getOutputFormat());
            MeLog.i(TAG, "encoder output format changed: %s", mMediaCodec.getOutputFormat());
            return;
        }

        if (encoderStatus < 0) {
            throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
        }

        ByteBuffer buffer;
        // 如果高版本机器通过getOutputBuffers读取数据，会得到一个inaccessible的ByteBuffer，无法访问其数据。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buffer = mMediaCodec.getOutputBuffer(encoderStatus);
        } else {
            buffer = mMediaCodec.getOutputBuffers()[encoderStatus];
        }
        MeLog.v(TAG, "encoder getOutputBuffer %d, flags: %d", mBufferInfo.presentationTimeUs, mBufferInfo.flags);
        Frame frame = new Frame(buffer, encoderStatus, mBufferInfo);

        if (MediaUtils.hasEosFlag(mBufferInfo.flags)) {
            mBufferInfo.size = 0;
            MeLog.i(TAG, "encoder meet eos");
            setState(State.ALL_DATA_READY);
        }

        synchronized (this) {
            mWaitOutBuffers.add(frame);
        }
    }
}
