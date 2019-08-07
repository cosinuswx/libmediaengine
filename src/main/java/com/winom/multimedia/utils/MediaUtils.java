package com.winom.multimedia.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.exifinterface.media.ExifInterface;

public class MediaUtils {
    private final static String TAG = "MediaUtils";
    public static final String KEY_ROTATION = "rotation-degrees";

    public static long getVideoDuration(String filepath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filepath);
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.release();
        return Long.parseLong(duration);
    }

    public static boolean saveBmpToFile(Bitmap bmp, File file) {
        return saveBmpToFile(bmp, file, Bitmap.CompressFormat.JPEG);
    }

    public static boolean saveBmpToFile(Bitmap bmp, File file, Bitmap.CompressFormat format) {
        if (null == bmp || null == file) {
            MeLog.e(TAG, "bmp or file is null");
            return false;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(format, 100, baos);
        return writeToFile(baos.toByteArray(), file);
    }

    public static boolean writeToFile(byte[] data, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean mkdirs(String dir) {
        File file = new File(dir);
        if (file.exists()) {
            return file.isDirectory();
        }

        return file.mkdirs();
    }

    public static boolean hasEosFlag(int flags) {
        return (flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    public static String getStack(final boolean printLine) {
        StackTraceElement[] stes = new Throwable().getStackTrace();
        if ((stes == null) || (stes.length < 4)) {
            return "";
        }

        StringBuilder t = new StringBuilder();

        for (int i = 1; i < stes.length; i++) {
            t.append("[");
            t.append(stes[i].getClassName());
            t.append(":");
            t.append(stes[i].getMethodName());
            if (printLine) {
                t.append("(").append(stes[i].getLineNumber()).append(")]\n");
            } else {
                t.append("]\n");
            }
        }
        return t.toString();
    }

    public static int getImageRotation(String filepath) throws IOException {
        ExifInterface exifInterface = new ExifInterface(filepath);
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
        switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            return 90;
        case ExifInterface.ORIENTATION_ROTATE_180:
            return 180;
        case ExifInterface.ORIENTATION_ROTATE_270:
            return 270;
        default:
            return 0;
        }
    }

    public static MediaFormat createVideoFormatForImage(String filepath) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, options);

        // 图片需要旋转下
        int rotation = (getImageRotation(filepath) + 180) % 360;
        MediaFormat videoFormat = new MediaFormat();
        videoFormat.setInteger(MediaUtils.KEY_ROTATION, rotation);
        videoFormat.setInteger(MediaFormat.KEY_WIDTH, options.outWidth);
        videoFormat.setInteger(MediaFormat.KEY_HEIGHT, options.outHeight);

        return videoFormat;
    }

    public static void mediaEngineAssert(boolean condition, String assertMessage) {
        if (!condition) {
            throw new RuntimeException(assertMessage);
        }
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }
}
