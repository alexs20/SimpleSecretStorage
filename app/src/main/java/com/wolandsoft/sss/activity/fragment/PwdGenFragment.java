package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.common.InputFilterMinMax;
import com.wolandsoft.sss.util.KeySharedPreferences;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class PwdGenFragment extends Fragment {
    private EditText mEdtLcChars;
    private EditText mEdtUpChars;
    private EditText mEdtNumChars;
    private EditText mEdtSpChars;
    private TextView mTxtPwdPreview;
    private KeySharedPreferences mPref;
    private Random mRandom;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mRandom = new Random();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPref = new KeySharedPreferences(shPref, getContext());
        View view = inflater.inflate(R.layout.fragment_pwdgen, container, false);

        mEdtLcChars = (EditText) view.findViewById(R.id.edtLcChars);
        int min = getResources().getInteger(R.integer.pref_pwdgen_lowercase_chars_value_min);
        int max = getResources().getInteger(R.integer.pref_pwdgen_lowercase_chars_value_max);
        mEdtLcChars.setFilters(new InputFilter[]{new InputFilterMinMax(min, max)});

        mEdtUpChars = (EditText) view.findViewById(R.id.edtUpChars);
        min = getResources().getInteger(R.integer.pref_pwdgen_uppercase_chars_value_min);
        max = getResources().getInteger(R.integer.pref_pwdgen_uppercase_chars_value_max);
        mEdtUpChars.setFilters(new InputFilter[]{new InputFilterMinMax(min, max)});

        mEdtNumChars = (EditText) view.findViewById(R.id.edtNumChars);
        min = getResources().getInteger(R.integer.pref_pwdgen_numeric_chars_value_min);
        max = getResources().getInteger(R.integer.pref_pwdgen_numeric_chars_value_max);
        mEdtNumChars.setFilters(new InputFilter[]{new InputFilterMinMax(min, max)});

        mEdtSpChars = (EditText) view.findViewById(R.id.edtSpChars);
        min = getResources().getInteger(R.integer.pref_pwdgen_special_chars_value_min);
        max = getResources().getInteger(R.integer.pref_pwdgen_special_chars_value_max);
        mEdtSpChars.setFilters(new InputFilter[]{new InputFilterMinMax(min, max)});

        mTxtPwdPreview = (TextView) view.findViewById(R.id.txtPwdPreview);

        if (savedInstanceState == null) {
            int lcChars = mPref.getInt(R.string.pref_pwdgen_lowercase_chars_key, R.integer.pref_pwdgen_lowercase_chars_value);
            mEdtLcChars.setText(String.valueOf(lcChars));

            int ucChars = mPref.getInt(R.string.pref_pwdgen_uppercase_chars_key, R.integer.pref_pwdgen_uppercase_chars_value);
            mEdtUpChars.setText(String.valueOf(ucChars));

            int numChars = mPref.getInt(R.string.pref_pwdgen_numeric_chars_key, R.integer.pref_pwdgen_numeric_chars_value);
            mEdtNumChars.setText(String.valueOf(numChars));

            int spChars = mPref.getInt(R.string.pref_pwdgen_special_chars_key, R.integer.pref_pwdgen_special_chars_value);
            mEdtSpChars.setText(String.valueOf(spChars));

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
        int lcChars = Integer.parseInt(mEdtLcChars.getText().toString());
        int ucChars = Integer.parseInt(mEdtUpChars.getText().toString());
        int numChars = Integer.parseInt(mEdtNumChars.getText().toString());
        int spChars = Integer.parseInt(mEdtSpChars.getText().toString());

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
        editor.putInt(R.string.pref_pwdgen_lowercase_chars_key, Integer.parseInt(mEdtLcChars.getText().toString()));
        editor.putInt(R.string.pref_pwdgen_uppercase_chars_key, Integer.parseInt(mEdtUpChars.getText().toString()));
        editor.putInt(R.string.pref_pwdgen_numeric_chars_key, Integer.parseInt(mEdtNumChars.getText().toString()));
        editor.putInt(R.string.pref_pwdgen_special_chars_key, Integer.parseInt(mEdtSpChars.getText().toString()));
        editor.apply();

        Fragment parent = getTargetFragment();
        if (parent instanceof OnFragmentToFragmentInteract) {
            ((OnFragmentToFragmentInteract) parent).onPasswordGenerate(mTxtPwdPreview.getText().toString());
        } else {
            throw new ClassCastException(
                    String.format(
                            getString(R.string.internal_exception_must_implement),
                            parent.toString(),
                            OnFragmentToFragmentInteract.class.getName()
                    )
            );
        }
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(String.valueOf(R.id.txtPwdPreview), mTxtPwdPreview.getText().toString());
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    interface OnFragmentToFragmentInteract {
        void onPasswordGenerate(String password);
    }
}
