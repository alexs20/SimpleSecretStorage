/*
    Copyright 2016 Alexander Shulgin

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
package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;

/**
 * Pin input fragment
 *
 * @author Alexander Shulgin
 */
public class PinFragment extends Fragment {
    private final static String ARG_NEW = "new";

    private OnFragmentToFragmentInteract mListener = null;
    //ui elements
    private TextInputEditText mEdtPin;
    private TextInputEditText mEdtPin2;
    private boolean mIsNew;

    public static PinFragment newInstance(boolean isNew) {
        PinFragment fragment = new PinFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_NEW, isNew);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle args = getArguments();
        mIsNew = args.getBoolean(ARG_NEW);
        Fragment parent = getTargetFragment();
        if (parent != null) {
            if (parent instanceof OnFragmentToFragmentInteract) {
                mListener = (OnFragmentToFragmentInteract) parent;
            } else {
                throw new ClassCastException(
                        String.format(
                                getString(R.string.internal_exception_must_implement),
                                parent.toString(),
                                OnFragmentToFragmentInteract.class.getName()
                        )
                );
            }
        } else {
            if (context instanceof OnFragmentToFragmentInteract) {
                mListener = (OnFragmentToFragmentInteract) context;
            } else {
                throw new ClassCastException(
                        String.format(
                                getString(R.string.internal_exception_must_implement),
                                context.toString(),
                                OnFragmentToFragmentInteract.class.getName()
                        )
                );
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pin, container, false);

        int titleId = R.string.title_master_pin;
        if (mIsNew) {
            mEdtPin = (TextInputEditText) view.findViewById(R.id.edtNewPin);
            mEdtPin2 = (TextInputEditText) view.findViewById(R.id.edtPin2);
            TextInputLayout lay = (TextInputLayout) view.findViewById(R.id.layPin);
            lay.setVisibility(View.GONE);
            mEdtPin2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        onOkClicked();
                    }
                    return false;
                }
            });
        } else {
            mEdtPin = (TextInputEditText) view.findViewById(R.id.edtPin);
            TextInputLayout lay = (TextInputLayout) view.findViewById(R.id.layNew);
            lay.setVisibility(View.GONE);
            lay = (TextInputLayout) view.findViewById(R.id.layRepeat);
            lay.setVisibility(View.GONE);
            titleId = R.string.app_name;
        }

        mEdtPin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onOkClicked();
                }
                return false;
            }
        });

        FloatingActionButton btnApply = (FloatingActionButton) view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(titleId);
        return view;
    }

    private void onOkClicked() {
        View currentFocus = getActivity().getCurrentFocus();
        if (currentFocus != null) {
            //hide soft keyboard
            InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        String pin = mEdtPin.getText().toString();
        if (pin.length() < 4) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_pin_should_be_4chars, false, null);
            fragment.setCancelable(true);
            fragment.show(transaction, null);
            return;
        }
        if (mIsNew) {
            String pin2 = mEdtPin2.getText().toString();
            if (!pin.equals(pin2)) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                        R.string.label_error, R.string.message_repeated_pin_no_the_same, false, null);
                fragment.setCancelable(true);
                fragment.show(transaction, null);
                return;
            }
        }
        getFragmentManager().popBackStack();
        mListener.onPinProvided(pin);

    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    public interface OnFragmentToFragmentInteract {
        void onPinProvided(String pin);
    }
}
