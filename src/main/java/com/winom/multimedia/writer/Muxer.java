package com.winom.multimedia.writer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.pipeline.Provider;
import com.winom.multimedia.pipeline.Stage;
import com.winom.multimedia.source.Frame;
import com.winom.multimedia.utils.FutureObject;
import com.winom.multimedia.utils.MeLog;
import com.winom.multimedia.utils.MediaUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Muxer extends Stage {
    private static final String TAG = "Muxer";

    private final String mOutputPath;
    private final List<TrackWriter> mTrackWriters;

    private MediaMuxer mMediaMuxer;

    public Muxer(String filePath) {
        mOutputPath = filePath;
        mTrackWriters = new ArrayList<>();
    }

    public void addTrackProvider(FutureObject<MediaFormat> mediaFormat, Provider<Frame> provider) {
        if (mState != State.INIT) {
            throw new RuntimeException("only can add provider in init state");
        }
        mTrackWriters.add(new TrackWriter(mediaFormat, provider));
    }

    @Override
    public void setup() throws SetupException {
        try {
            mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            throw new SetupException("can't updateExtractor MediaMuxer.", e);
        }

        try {
            for (TrackWriter writer : mTrackWriters) {
                MediaFormat mediaFormat = writer.getOutputMediaFormat();
                int trackIndex = mMediaMuxer.addTrack(mediaFormat);
                MeLog.i(TAG, "track[%d]: %s", trackIndex, mediaFormat);
                writer.setTrackIndex(trackIndex);
            }
        } catch (Exception e) {
            throw new SetupException("add track failed.", e);
        }

        mMediaMuxer.start();
        setState(State.SETUPED);
        MeLog.i(TAG, "muxer configured");
    }

    @Override
    public void processFrame() {
        boolean isAllWriterEnd = true;
        for (TrackWriter writer : mTrackWriters) {
            if (!writer.isWriteEnd()) {
                isAllWriterEnd = false;
                writer.processFrame();
            }
        }

        if (isAllWriterEnd) {
            setState(State.DONE);
        }
    }

    @Override
    public void release() {
        MeLog.i(TAG, "release muxer");
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    private class TrackWriter {
        private final FutureObject<MediaFormat> mFutureMediaFormat;
        private final Provider<Frame> mFrameProvider;
        private final MediaCodec.BufferInfo mBufferInfo;

        private int mTrackIndex;
        private boolean mIsWriteEnd;
        private long mLastFramePts;

        private TrackWriter(FutureObject<MediaFormat> futureMediaFormat, Provider<Frame> provider) {
            mFutureMediaFormat = futureMediaFormat;
            mIsWriteEnd = false;
            mFrameProvider = provider;
            mBufferInfo = new MediaCodec.BufferInfo();
            mLastFramePts = 0;
        }

        private boolean isWriteEnd() {
            return mIsWriteEnd;
        }

        private void setTrackIndex(int index) {
            mTrackIndex = index;
        }

        private MediaFormat getOutputMediaFormat() throws ExecutionException, InterruptedException {
            return mFutureMediaFormat.get();
        }

        @SuppressWarnings("ConstantConditions")
        private void processFrame() {
            Frame frame = mFrameProvider.dequeueOutputBuffer();
            if (frame == null) {
                return;
            }

            do {
                if (MediaUtils.hasEosFlag(frame.flags)) {
                    MeLog.i(TAG, "meet BUFFER_FLAG_END_OF_STREAM");
                    mIsWriteEnd = true;
                    break;
                }

                if ((frame.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    MeLog.i(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    break;
                }

                if (frame.presentationTimeUs < mLastFramePts) {
                    MeLog.w(TAG, "out of order frame. %d <= %d", frame.presentationTimeUs, mLastFramePts);
                    break;
                } else if (frame.presentationTimeUs == mLastFramePts) {
                    frame.presentationTimeUs += TimeUnit.MILLISECONDS.toMicros(1);
                }
                mLastFramePts = frame.presentationTimeUs;

                mBufferInfo.offset = frame.offset;
                mBufferInfo.size = frame.size;
                mBufferInfo.presentationTimeUs = frame.presentationTimeUs;
                mBufferInfo.flags = frame.flags;
                mMediaMuxer.writeSampleData(mTrackIndex, frame.buffer, mBufferInfo);
                MeLog.v(TAG, "[%d] size: %d, time: %d, flag: %d",
                        mTrackIndex, frame.size, mBufferInfo.presentationTimeUs, mBufferInfo.flags);
            } while (false);

            mFrameProvider.enqueueOutputBuffer(frame);
        }
    }
}
