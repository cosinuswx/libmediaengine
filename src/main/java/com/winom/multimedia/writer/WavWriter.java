package com.winom.multimedia.writer;

import android.media.AudioFormat;

import com.winom.multimedia.exceptions.ProcessException;
import com.winom.multimedia.exceptions.ReleaseException;
import com.winom.multimedia.exceptions.SetupException;
import com.winom.multimedia.pipeline.Provider;
import com.winom.multimedia.pipeline.Stage;
import com.winom.multimedia.source.Frame;
import com.winom.multimedia.utils.MediaConstants;
import com.winom.multimedia.utils.MediaUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavWriter extends Stage {
    private final String mFilePath;
    private final int mSampleRate;
    private final int mChannelCount;
    private final Provider<Frame> mAudioProvider;

    private DataOutputStream mOutputStream;
    private long mWroteCount = 0;

    public WavWriter(String filepath, int sampleRate, int channelCnt, Provider<Frame> provider) {
        mFilePath = filepath;
        mSampleRate = sampleRate;
        mChannelCount = channelCnt;
        mAudioProvider = provider;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void setup() throws SetupException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(44);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(0x46464952);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0x45564157);
        byteBuffer.putInt(0x20746d66);
        byteBuffer.putInt(16);
        byteBuffer.putShort((short)1);
        byteBuffer.putShort((short) mChannelCount);
        byteBuffer.putInt(mSampleRate);
        byteBuffer.putInt((mSampleRate * mChannelCount * MediaConstants.BYTE_COUNT_PRE_SAMPLE));
        byteBuffer.putShort((short) (mChannelCount * MediaConstants.BYTE_COUNT_PRE_SAMPLE));
        byteBuffer.putShort((short) (MediaConstants.AUDIO_FORMAT == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8));
        byteBuffer.putInt(0x61746164);
        byteBuffer.putInt(0);

        try {
            mOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mFilePath)));
            mOutputStream.write(byteBuffer.array());
            mWroteCount = 44;
        } catch (IOException e) {
            throw new SetupException("Open output stream failed.", e);
        }

        setState(State.SETUPED);
    }

    @Override
    public void processFrame() throws ProcessException {
        Frame frame = mAudioProvider.dequeueOutputBuffer();
        if (frame == null) {
            return;
        }

        if (MediaUtils.hasEosFlag(frame.flags)) {
            setState(State.DONE);
            return;
        }

        frame.buffer.position(0);
        try {
            mOutputStream.write(frame.buffer.array(), 0, frame.size);
            mWroteCount += frame.size;
        } catch (IOException e) {
            throw new ProcessException("Write data to failed.", e);
        } finally {
            mAudioProvider.enqueueOutputBuffer(frame);
        }
    }

    @Override
    public void release() throws ReleaseException {
        try {
            mOutputStream.close();
        } catch (IOException e) {
            throw new ReleaseException("Close file failed.", e);
        }

        RandomAccessFile ras = null;
        try {
            ras = new RandomAccessFile(mFilePath, "rw");
            ras.seek(4);

            // 文件总长度
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt((int) (mWroteCount - 8));
            ras.write(byteBuffer.array());

            // 数据总长度
            byteBuffer.rewind();
            byteBuffer.putInt((int) (mWroteCount - 42));
            ras.seek(40);
            ras.write(byteBuffer.array());
        } catch (IOException e) {
            throw new ReleaseException("Write file header failed.", e);
        } finally {
            MediaUtils.closeQuietly(ras);
        }
    }
}
