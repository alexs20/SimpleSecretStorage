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
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.service.CoreService;

/**
 * Attribute edit fragment
 *
 * @author Alexander Shulgin
 */
public class AttributeFragment extends Fragment implements PwdGenFragment.OnFragmentToFragmentInteract,
        CoreService.CoreServiceStateListener {
    private final static String ARG_ATTR = "attr";
    private final static String ARG_ATTR_POS = "attr_pos";

    //ui elements
    private TextView mTxtKey;
    private TextView mTxtValue;
    private SwitchCompat mChkProtected;
    private FloatingActionButton mBtnGenerate;
    //model objects
    private int mAttrPos;
    private SecretEntryAttribute mAttr;
    //model adjustment
    private String mGeneratedPwd = null;
    //utils
    private CoreService.CoreServiceProvider mServiceProvider;
    private OnFragmentToFragmentInteract mListener;

    public static AttributeFragment newInstance(int attrPos, SecretEntryAttribute attr) {
        AttributeFragment fragment = new AttributeFragment();
        if (attr == null) {
            attr = new SecretEntryAttribute("", "", false);
        }
        Bundle args = new Bundle();
        args.putParcelable(ARG_ATTR, attr);
        args.putInt(ARG_ATTR_POS, attrPos);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CoreService.CoreServiceProvider) {
            mServiceProvider = (CoreService.CoreServiceProvider) context;
        } else {
            throw new ClassCastException(String.format(getString(R.string.internal_exception_must_implement),
                    context.toString(), CoreService.CoreServiceProvider.class.getName()));
        }
        Fragment parent = getTargetFragment();
        if (parent instanceof OnFragmentToFragmentInteract) {
            mListener = (OnFragmentToFragmentInteract) parent;
        } else {
            throw new ClassCastException(String.format(getString(R.string.internal_exception_must_implement),
                    parent.toString(), OnFragmentToFragmentInteract.class.getName())
            );
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mAttr = args.getParcelable(ARG_ATTR);
        mAttrPos = args.getInt(ARG_ATTR_POS);
        mServiceProvider.addCoreServiceStateListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attr, container, false);
        mTxtKey = (TextView) view.findViewById(R.id.txtKey);
        mTxtValue = (TextView) view.findViewById(R.id.txtValue);
        mChkProtected = (SwitchCompat) view.findViewById(R.id.chkProtected);
        mBtnGenerate = (FloatingActionButton) view.findViewById(R.id.btnGenerate);

        if (savedInstanceState == null) {
            mTxtKey.setText(mAttr.getKey());
            if (mAttr.isProtected()) {
                if (mAttr.getValue() != null && mAttr.getValue().length() > 0) {
                    CoreService service = mServiceProvider.getCoreService();
                    if (service != null) {
                        String plain = service.getKeyStoreManager().decrupt(mAttr.getValue());
                        mTxtValue.setText(plain);
                    }
                }
            } else {
                mTxtValue.setText(mAttr.getValue());
            }
            mChkProtected.setChecked(mAttr.isProtected());
        }

        FloatingActionButton btnApply = (FloatingActionButton) view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClicked();
            }
        });

        if (mAttrPos == 0) {
            mChkProtected.setVisibility(View.GONE);
            view.findViewById(R.id.lblProtected).setVisibility(View.GONE);
        }

        mBtnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGenerateClicked();
            }
        });
        if (!mChkProtected.isChecked()) {
            mBtnGenerate.setVisibility(View.GONE);
        }
        mChkProtected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mBtnGenerate.setVisibility(View.VISIBLE);
                } else {
                    mBtnGenerate.setVisibility(View.GONE);
                }

            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_secret_field);
        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (mGeneratedPwd != null) {
            mTxtValue.setText(mGeneratedPwd);
            mGeneratedPwd = null;
        }
    }

    private void onGenerateClicked() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = new PwdGenFragment();
        fragment.setTargetFragment(this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(AttributeFragment.class.getName());
        transaction.commit();
    }

    private void onOkClicked() {
        CoreService service = mServiceProvider.getCoreService();
        if (service != null || !mChkProtected.isChecked()) {
            View currentFocus = getActivity().getCurrentFocus();
            if (currentFocus != null) {
                //hide soft keyboard
                InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

            String protectedStr = mTxtValue.getText().toString();
            if (mChkProtected.isChecked()) {
                protectedStr = service.getKeyStoreManager().encrypt(protectedStr);
            }
            SecretEntryAttribute attr = new SecretEntryAttribute(mTxtKey.getText().toString(), protectedStr, mChkProtected.isChecked());
            getFragmentManager().popBackStackImmediate(); //complete the pop in order to restore the parent fragment as we are going to call it back
            mListener.onAttributeUpdate(mAttrPos, attr);
        }
    }

    @Override
    public void onPasswordGenerate(String password) {
        mGeneratedPwd = password;
    }

    @Override
    public void onCoreServiceReady(CoreService service) {
        if (mAttr.isProtected()) {
            if (mAttr.getValue() != null && mAttr.getValue().length() > 0) {
                String plain = service.getKeyStoreManager().decrupt(mAttr.getValue());
                mTxtValue.setText(plain);
            }
        }
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    interface OnFragmentToFragmentInteract {
        void onAttributeUpdate(int pos, SecretEntryAttribute attr);
    }
}
