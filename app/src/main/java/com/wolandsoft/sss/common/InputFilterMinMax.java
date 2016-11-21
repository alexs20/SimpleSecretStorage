package com.wolandsoft.sss.common;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class InputFilterMinMax implements InputFilter {
    private int mMin;
    private int mMax;

    public InputFilterMinMax(int min, int max) {
        mMin = min;
        mMax = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (isInRange(mMin, mMax, input))
                return null;
        } catch (NumberFormatException nfe) {
        }
        return "";
    }

    private boolean isInRange(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}
