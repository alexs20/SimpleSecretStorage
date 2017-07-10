/*
    Copyright 2017 Alexander Shulgin

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
package com.wolandsoft.sss.activity.fragment.pwdgen;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Characters selector seek bar listener
 *
 * @author Alexander Shulgin
 */
public class OnCharNumbersChangeListener extends ContextWrapper implements OnSeekBarChangeListener {

    private TextView mTxtView;
    private int mStrResId;
    private int mMinVal;

    public OnCharNumbersChangeListener(Context context, TextView mTxtView, int mStrResId, int mMinVal) {
        super(context);
        this.mTxtView = mTxtView;
        this.mStrResId = mStrResId;
        this.mMinVal = mMinVal;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Resources res = getResources();
        String text = res.getQuantityString(mStrResId, mMinVal + progress, mMinVal + progress);
        mTxtView.setText(text);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
