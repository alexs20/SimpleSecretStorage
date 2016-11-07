package com.wolandsoft.sss.util;

import android.util.Log;

import com.wolandsoft.sss.AppConstants;
import com.wolandsoft.sss.BuildConfig;

import static android.content.ContentValues.TAG;

/**
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class LogEx {
    public static final boolean msIsDebug = BuildConfig.DEBUG;
    public static final boolean msIsSource = BuildConfig.SHOW_SRC_IN_LOG;
    private static final String TAG = AppConstants.APP_TAG;
    /**
     * Print debug information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void d(Object... args) {
        if (msIsDebug) {
            StringBuilder sb = getStringBuilderWithHeader();
            for (Object arg : args) {
                if (arg instanceof Throwable) {
                    Log.d(TAG, sb.toString(), (Throwable) arg);
                    return;
                }
                sb.append(arg);
            }
            Log.d(TAG, sb.toString());
        }
    }

    /**
     * Print warning information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void w(Object... args) {
        StringBuilder sb = getStringBuilderWithHeader();
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                Log.w(TAG, sb.toString(), (Throwable) arg);
                return;
            }
            sb.append(arg);
        }
        Log.w(TAG, sb.toString());
    }

    /**
     * Print error information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void e(Object... args) {
        StringBuilder sb = getStringBuilderWithHeader();
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                Log.e(TAG, sb.toString(), (Throwable) arg);
                return;
            }
            sb.append(arg);
        }
        Log.e(TAG, sb.toString());
    }

    /**
     * Print info information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void i(Object... args) {
        StringBuilder sb = getStringBuilderWithHeader();
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                Log.i(TAG, sb.toString(), (Throwable) arg);
                return;
            }
            sb.append(arg);
        }
        Log.e(TAG, sb.toString());
    }

    /**
     * Print verbose information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void v(Object... args) {
        if (msIsDebug) {
            StringBuilder sb = getStringBuilderWithHeader();
            for (Object arg : args) {
                if (arg instanceof Throwable) {
                    Log.v(TAG, sb.toString(), (Throwable) arg);
                    return;
                }
                sb.append(arg);
            }
            Log.d(TAG, sb.toString());
        }
    }

    /*
     * Build log header
     */
    private static StringBuilder getStringBuilderWithHeader() {
        StringBuilder sb = new StringBuilder();
        if (msIsSource) {
            Throwable th = new Throwable();
            StackTraceElement ste = th.getStackTrace()[2];
            String clsName = ste.getClassName();
            int dotIdx = clsName.indexOf(".");
            if (dotIdx != -1) {
                dotIdx = clsName.indexOf(".", ++dotIdx);
                if (dotIdx != -1) {
                    dotIdx = clsName.indexOf(".", ++dotIdx);
                    if (dotIdx != -1) {
                        sb.append(clsName.substring(++dotIdx));
                    }
                }
            }
            sb.append(".").append(ste.getMethodName()).append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")").append("\n");
        }
        return sb;
    }
}
