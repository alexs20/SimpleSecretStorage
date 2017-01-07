/*
    Copyright 2016, 2017 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.wolandsoft.sss.util;

import android.util.Log;

import com.wolandsoft.sss.AppConstants;
import com.wolandsoft.sss.BuildConfig;

/**
 * Simplified Logger's adapter that allow reduce impact of multiple string concatenations for logging
 * and provides some extra information such source source's code line number.
 *
 * @author Alexander Shulgin
 */
@SuppressWarnings("unused")
public class LogEx {
    public static final boolean IS_DEBUG = BuildConfig.DEBUG;
    public static final boolean SHOW_SOURCE = BuildConfig.SHOW_SRC_IN_LOG;
    public static final String TAG = AppConstants.APP_TAG;

    /**
     * Print debug information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void d(Object... args) {
        if (IS_DEBUG) {
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
        Log.i(TAG, sb.toString());
    }

    /**
     * Print verbose information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void v(Object... args) {
        if (IS_DEBUG) {
            StringBuilder sb = getStringBuilderWithHeader();
            for (Object arg : args) {
                if (arg instanceof Throwable) {
                    Log.v(TAG, sb.toString(), (Throwable) arg);
                    return;
                }
                sb.append(arg);
            }
            Log.v(TAG, sb.toString());
        }
    }

    /*
     * Build log header
     */
    private static StringBuilder getStringBuilderWithHeader() {
        StringBuilder sb = new StringBuilder();
        if (SHOW_SOURCE) {
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
