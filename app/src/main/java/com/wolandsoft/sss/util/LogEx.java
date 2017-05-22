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
    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final boolean SHOW_SOURCE = BuildConfig.SHOW_SRC_IN_LOG;
//MessageFormat
    /**
     * Print debug information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void d(Object... args) {
        if (IS_DEBUG) {
            SrcInfo si = getSrcInfoBuilder();
            for (Object arg : args) {
                if (arg instanceof Throwable) {
                    Log.d(si.tag, si.sb.toString(), (Throwable) arg);
                    return;
                }
                si.sb.append(arg);
            }
            Log.d(si.tag, si.sb.toString());
        }
    }

    /**
     * Print warning information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void w(Object... args) {
        SrcInfo si = getSrcInfoBuilder();
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                Log.w(si.tag, si.sb.toString(), (Throwable) arg);
                return;
            }
            si.sb.append(arg);
        }
        Log.w(si.tag, si.sb.toString());
    }

    /**
     * Print error information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void e(Object... args) {
        SrcInfo si = getSrcInfoBuilder();
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                Log.e(si.tag, si.sb.toString(), (Throwable) arg);
                return;
            }
            si.sb.append(arg);
        }
        Log.e(si.tag, si.sb.toString());
    }

    /**
     * Print info information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void i(Object... args) {
        SrcInfo si = getSrcInfoBuilder();
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                Log.i(si.tag, si.sb.toString(), (Throwable) arg);
                return;
            }
            si.sb.append(arg);
        }
        Log.i(si.tag, si.sb.toString());
    }

    /**
     * Print verbose information.
     *
     * @param args Sequence of elements to concatenate and print. The last element could be an exception.
     */
    public static void v(Object... args) {
        if (IS_DEBUG) {
            SrcInfo si = getSrcInfoBuilder();
            for (Object arg : args) {
                if (arg instanceof Throwable) {
                    Log.v(si.tag, si.sb.toString(), (Throwable) arg);
                    return;
                }
                si.sb.append(arg);
            }
            Log.v(si.tag, si.sb.toString());
        }
    }

    /*
     * Build log header
     */
    private static SrcInfo getSrcInfoBuilder() {
        SrcInfo ret = new SrcInfo();
        ret.sb = new StringBuilder();
        Throwable th = new Throwable();
        StackTraceElement ste = th.getStackTrace()[2];
        String clsName = ste.getClassName();
        if (SHOW_SOURCE) {
            if (clsName.startsWith(PACKAGE_NAME)) {
                ret.sb.append(clsName.substring(PACKAGE_NAME.length() + 1));
            } else {
                ret.sb.append(clsName);
            }
            ret.sb.append(".").append(ste.getMethodName()).append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")").append("\n");
        }
        ret.tag = clsName.substring(clsName.lastIndexOf(".") + 1);
        return ret;
    }

    private static class SrcInfo {
        StringBuilder sb;
        String tag;
    }
}
