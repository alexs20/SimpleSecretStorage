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
package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wolandsoft.sss.R;

/**
 * Pin input fragment
 *
 * @author Alexander Shulgin
 */
public class PinFragment extends Fragment {
    private final static String ARG_MSG_RES_ID = "msg_res_id";
    private final static String ARG_DELAY_MSEC = "delay_msec";
    private final static String KEY_PIN = "pin";

    private OnFragmentToFragmentInteract mListener = null;
    private int mMsgResId;
    private long mDelayMsec;
    private String mPin = "";
    private Handler mHandler;
    //ui elements
    private RelativeLayout mLayoutWait;
    private ImageView mImgPin1;
    private ImageView mImgPin2;
    private ImageView mImgPin3;
    private ImageView mImgPin4;
    private ImageButton mBtnClear;
    private ImageButton mBtnDelete;
    private ImageButton[] mBtnDigit;

    public static PinFragment newInstance(int msgResId, long delayMsec) {
        PinFragment fragment = new PinFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MSG_RES_ID, msgResId);
        args.putLong(ARG_DELAY_MSEC, delayMsec);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parent = getTargetFragment();
        if (parent != null) { //first try to use target fragment as a callback
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
        } else { //otherwise use an activity
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        Bundle args = getArguments();
        mMsgResId = args.getInt(ARG_MSG_RES_ID);
        mDelayMsec = args.getLong(ARG_DELAY_MSEC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pin, container, false);
        mLayoutWait = (RelativeLayout) view.findViewById(R.id.layoutWait);
        mImgPin1 = (ImageView) view.findViewById(R.id.imgPin1);
        mImgPin2 = (ImageView) view.findViewById(R.id.imgPin2);
        mImgPin3 = (ImageView) view.findViewById(R.id.imgPin3);
        mImgPin4 = (ImageView) view.findViewById(R.id.imgPin4);
        mBtnClear = (ImageButton) view.findViewById(R.id.btnClear);
        mBtnDelete = (ImageButton) view.findViewById(R.id.btnDelete);
        mBtnDigit = new ImageButton[10];
        mBtnDigit[0] = (ImageButton) view.findViewById(R.id.btnDig0);
        mBtnDigit[1] = (ImageButton) view.findViewById(R.id.btnDig1);
        mBtnDigit[2] = (ImageButton) view.findViewById(R.id.btnDig2);
        mBtnDigit[3] = (ImageButton) view.findViewById(R.id.btnDig3);
        mBtnDigit[4] = (ImageButton) view.findViewById(R.id.btnDig4);
        mBtnDigit[5] = (ImageButton) view.findViewById(R.id.btnDig5);
        mBtnDigit[6] = (ImageButton) view.findViewById(R.id.btnDig6);
        mBtnDigit[7] = (ImageButton) view.findViewById(R.id.btnDig7);
        mBtnDigit[8] = (ImageButton) view.findViewById(R.id.btnDig8);
        mBtnDigit[9] = (ImageButton) view.findViewById(R.id.btnDig9);

        if (savedInstanceState != null) {
            mPin = savedInstanceState.getString(KEY_PIN);
        }
        updatePinUI();
        TextView txtMessage = (TextView) view.findViewById(R.id.txtMessage);
        txtMessage.setText(mMsgResId);

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonClear();
            }
        });
        mBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonDelete();
            }
        });
        int idx = 0;
        for (ImageButton btn : mBtnDigit) {
            final int digit = idx++;
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onButtonDigit(digit);
                }
            });
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_master_pin);

        return view;
    }

    private void updatePinUI() {
        mImgPin1.setImageResource(mPin.length() > 0 ?
                R.mipmap.img48dp_star : R.mipmap.img48dp_underscore);
        mImgPin2.setImageResource(mPin.length() > 1 ?
                R.mipmap.img48dp_star : R.mipmap.img48dp_underscore);
        mImgPin3.setImageResource(mPin.length() > 2 ?
                R.mipmap.img48dp_star : R.mipmap.img48dp_underscore);
        mImgPin4.setImageResource(mPin.length() > 3 ?
                R.mipmap.img48dp_star : R.mipmap.img48dp_underscore);
    }

    private void onButtonClear() {
        mPin = "";
        updatePinUI();
    }

    private void onButtonDelete() {
        if (mPin.length() > 0) {
            mPin = mPin.substring(0, mPin.length() - 1);
        }
        updatePinUI();
    }

    private void onButtonDigit(int digit) {
        mPin = mPin + digit;
        updatePinUI();
        if (mPin.length() > 3) {
            final FragmentManager fragmentManager = getFragmentManager();
            if (mDelayMsec > 0) {
                mBtnClear.setEnabled(false);
                mBtnDelete.setEnabled(false);
                for (ImageButton btn : mBtnDigit) {
                    btn.setEnabled(false);
                }
                mLayoutWait.setVisibility(View.VISIBLE);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fragmentManager.popBackStackImmediate();
                        mListener.onPinProvided(mPin);
                    }
                }, mDelayMsec);
            } else {
                fragmentManager.popBackStackImmediate();
                mListener.onPinProvided(mPin);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PIN, mPin);
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    public interface OnFragmentToFragmentInteract {
        /**
         * Triggered when user complete entering 4 digits pin.
         *
         * @param pin pin number
         */
        void onPinProvided(String pin);
    }
}
