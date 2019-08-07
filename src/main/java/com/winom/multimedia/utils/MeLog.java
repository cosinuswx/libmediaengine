package com.winom.multimedia.utils;

import java.util.Locale;

public class MeLog {
    public interface ILog {
        void logWriter(int level, String tag, String text);
    }

    public final static int LEVEL_VERBOSE = 0;
    public final static int LEVEL_DEBUG = 1;
    public final static int LEVEL_INFO = 2;
    public final static int LEVEL_WARNING = 3;
    public final static int LEVEL_ERROR = 4;

    public static ILog sLogImpl = new EmptyLog();

    public static void setLogImpl(ILog impl) {
        sLogImpl = impl;
    }

    public static void v(String tag, String text) {
        sLogImpl.logWriter(LEVEL_VERBOSE, tag, text);
    }

    public static void d(String tag, String text) {
        sLogImpl.logWriter(LEVEL_DEBUG, tag, text);
    }

    public static void i(String tag, String text) {
        sLogImpl.logWriter(LEVEL_INFO, tag, text);
    }

    public static void w(String tag, String text) {
        sLogImpl.logWriter(LEVEL_WARNING, tag, text);
    }

    public static void e(String tag, String text) {
        sLogImpl.logWriter(LEVEL_ERROR, tag, text);
    }

    public static void e(String tag, String text, Throwable ex) {
        e(tag, text + "\n" + android.util.Log.getStackTraceString(ex));
    }

    public static void printStack(String tag, Throwable ex) {
        e(tag, android.util.Log.getStackTraceString(ex));
    }

    public static void v(String tag, String format, Object... args) {
        v(tag, String.format(Locale.ENGLISH, format, args));
    }

    public static void d(String tag, String format, Object... args) {
        d(tag, String.format(format, args));
    }

    public static void i(String tag, String format, Object... args) {
        i(tag, String.format(format, args));
    }

    public static void w(String tag, String format, Object... args) {
        w(tag, String.format(format, args));
    }

    public static void e(String tag, String format, Object... args) {
        e(tag, String.format(format, args));
    }

    static class EmptyLog implements ILog {

        @Override
        public void logWriter(int lvl, String tag, String text) {
        }
    }
}