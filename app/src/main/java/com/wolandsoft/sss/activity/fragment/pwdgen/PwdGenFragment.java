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
package com.wolandsoft.sss.activity.fragment.pwdgen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.util.KeySharedPreferences;

import java.util.Arrays;
import java.util.Random;

/**
 * Password generator.
 *
 * @author Alexander Shulgin
 */
public class PwdGenFragment extends Fragment {
    private SeekBar mSeekLcChars;
    private SeekBar mSeekUpChars;
    private SeekBar mSeekNumChars;
    private SeekBar mSeekSpChars;
    private TextView mTxtPwdPreview;
    private KeySharedPreferences mPref;
    private Random mRandom;
    private OnFragmentToFragmentInteract mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parent = getTargetFragment();
        if (parent instanceof OnFragmentToFragmentInteract) {
            mListener = (OnFragmentToFragmentInteract) parent;
        } else {
            throw new ClassCastException(String.format(
                    getString(R.string.internal_exception_must_implement), parent.toString(), OnFragmentToFragmentInteract.class.getName()));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRandom = new Random();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPref = new KeySharedPreferences(shPref, getContext());
        View view = inflater.inflate(R.layout.fragment_pwdgen, container, false);

        TextView txtLcChars = (TextView) view.findViewById(R.id.txtLcChars);
        mSeekLcChars = (SeekBar) view.findViewById(R.id.seekLcChars);
        int min = getResources().getInteger(R.integer.pref_pwdgen_lowercase_chars_value_min);
        int max = getResources().getInteger(R.integer.pref_pwdgen_lowercase_chars_value_max);
        mSeekLcChars.setMax(max - min);
        mSeekLcChars.setOnSeekBarChangeListener(new OnCharNumbersChangeListener(getContext(),
                txtLcChars, R.plurals.label_number_of_lowercase_chars, min));

        TextView txtUpChars = (TextView) view.findViewById(R.id.txtUpChars);
        mSeekUpChars = (SeekBar) view.findViewById(R.id.seekUpChars);
        min = getResources().getInteger(R.integer.pref_pwdgen_uppercase_chars_value_min);
        max = getResources().getInteger(R.integer.pref_pwdgen_uppercase_chars_value_max);
        mSeekUpChars.setMax(max - min);
        mSeekUpChars.setOnSeekBarChangeListener(new OnCharNumbersChangeListener(getContext(),
                txtUpChars, R.plurals.label_number_of_uppercase_chars, min));

        TextView txtNumChars = (TextView) view.findViewById(R.id.txtNumChars);
        mSeekNumChars = (SeekBar) view.findViewById(R.id.seekNumChars);
        min = getResources().getInteger(R.integer.pref_pwdgen_numeric_chars_value_min);
        max = getResources().getInteger(R.integer.pref_pwdgen_numeric_chars_value_max);
        mSeekNumChars.setMax(max - min);
        mSeekNumChars.setOnSeekBarChangeListener(new OnCharNumbersChangeListener(getContext(),
                txtNumChars, R.plurals.label_number_of_numeric_chars, min));

        TextView txtSpChars = (TextView) view.findViewById(R.id.txtSpChars);
        mSeekSpChars = (SeekBar) view.findViewById(R.id.seekSpChars);
        min = getResources().getInteger(R.integer.pref_pwdgen_special_chars_value_min);
        max = getResources().getInteger(R.integer.pref_pwdgen_special_chars_value_max);
        mSeekSpChars.setMax(max - min);
        mSeekSpChars.setOnSeekBarChangeListener(new OnCharNumbersChangeListener(getContext(),
                txtSpChars, R.plurals.label_number_of_special_chars, min));

        mTxtPwdPreview = (TextView) view.findViewById(R.id.txtPwdPreview);

        if (savedInstanceState == null) {
            int lcChars = mPref.getInt(R.string.pref_pwdgen_lowercase_chars_key, R.integer.pref_pwdgen_lowercase_chars_value);
            mSeekLcChars.setProgress(lcChars);

            int ucChars = mPref.getInt(R.string.pref_pwdgen_uppercase_chars_key, R.integer.pref_pwdgen_uppercase_chars_value);
            mSeekUpChars.setProgress(ucChars);

            int numChars = mPref.getInt(R.string.pref_pwdgen_numeric_chars_key, R.integer.pref_pwdgen_numeric_chars_value);
            mSeekNumChars.setProgress(numChars);

            int spChars = mPref.getInt(R.string.pref_pwdgen_special_chars_key, R.integer.pref_pwdgen_special_chars_value);
            mSeekSpChars.setProgress(spChars);

            onRefreshClicked();
        } else {
            mTxtPwdPreview.setText(savedInstanceState.getString(String.valueOf(R.id.txtPwdPreview)));
        }

        FloatingActionButton btnApply = (FloatingActionButton) view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onApplyClicked();
            }
        });

        FloatingActionButton btnRefresh = (FloatingActionButton) view.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefreshClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_generate_password);
        return view;
    }

    private void onRefreshClicked() {
        int lcChars = mSeekLcChars.getProgress() + getResources().getInteger(R.integer.pref_pwdgen_lowercase_chars_value_min);
        int ucChars = mSeekUpChars.getProgress() + getResources().getInteger(R.integer.pref_pwdgen_uppercase_chars_value_min);
        int numChars = mSeekNumChars.getProgress() + getResources().getInteger(R.integer.pref_pwdgen_numeric_chars_value_min);
        int spChars = mSeekSpChars.getProgress() + getResources().getInteger(R.integer.pref_pwdgen_special_chars_value_min);

        char[] pwd = new char[lcChars + ucChars + numChars + spChars];
        Arrays.fill(pwd, (char) 0);
        genFromRange(pwd, 0x61, 0x7A, lcChars);
        genFromRange(pwd, 0x41, 0x5A, ucChars);
        genFromRange(pwd, 0x30, 0x39, numChars);
        genFromRange(pwd, new char[]{'!', '@', '#', '$', '%', '^', '&', '*'}, spChars);

        mTxtPwdPreview.setText(String.valueOf(pwd));
    }

    private void genFromRange(char[] dest, int from, int to, int count) {
        char[] src = new char[to - from + 1];
        for (int i = 0; i < src.length; i++) {
            src[i] = (char) (mRandom.nextInt(to - from) + from);
        }
        genFromRange(dest, src, count);
    }

    private void genFromRange(char[] dest, char[] src, int count) {
        for (int i = 0; i < count; i++) {
            int idx = mRandom.nextInt(dest.length);
            while (true) {
                if (dest[idx] == 0) {
                    dest[idx] = src[mRandom.nextInt(src.length)];
                    break;
                }
                idx++;
                if (idx >= dest.length) {
                    idx = 0;
                }
            }
        }
    }

    private void onApplyClicked() {
        KeySharedPreferences.KeyableEditor editor = mPref.edit();
        editor.putInt(R.string.pref_pwdgen_lowercase_chars_key, mSeekLcChars.getProgress());
        editor.putInt(R.string.pref_pwdgen_uppercase_chars_key, mSeekUpChars.getProgress());
        editor.putInt(R.string.pref_pwdgen_numeric_chars_key, mSeekNumChars.getProgress());
        editor.putInt(R.string.pref_pwdgen_special_chars_key, mSeekSpChars.getProgress());
        editor.apply();

        getFragmentManager().popBackStackImmediate();//complete the pop in order to restore the parent fragment as we are going to call it back
        mListener.onPasswordGenerate(mTxtPwdPreview.getText().toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(String.valueOf(R.id.txtPwdPreview), mTxtPwdPreview.getText().toString());
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    public interface OnFragmentToFragmentInteract {
        void onPasswordGenerate(String password);
    }
}
