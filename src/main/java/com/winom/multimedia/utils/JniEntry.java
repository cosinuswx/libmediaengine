package com.winom.multimedia.utils;

import java.nio.ByteBuffer;

public class JniEntry {
    static {
        System.loadLibrary("mediaengine");
    }

    public static native void byteBufferCopy(ByteBuffer srcBuf, ByteBuffer dstBuf, int size);
}
