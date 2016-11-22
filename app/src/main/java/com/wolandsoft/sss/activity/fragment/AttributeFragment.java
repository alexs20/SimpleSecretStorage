package com.wolandsoft.sss.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AttributeFragment extends Fragment implements PwdGenFragment.OnFragmentToFragmentInteract{
    public static final int RESULT_UPDATE = 1;
    public final static String ARG_ATTR = "attr";
    public final static String ARG_ATTR_POS = "attr_pos";

    //ui elements
    private TextView mTxtKey;
    private TextView mTxtValue;
    private ToggleButton mChkProtected;
    private FloatingActionButton mBtnGenerate;
    //model objects
    private int mAttrPos;
    private SecretEntryAttribute mAttr;
    //model adjustment
    private String mGeneratedPwd = null;
    //utils
    private KeyStoreManager mKsMgr;

    public static AttributeFragment newInstance(int attrPos, SecretEntryAttribute attr) {
        AttributeFragment fragment = new AttributeFragment();
        if (attr == null) {
            attr = new SecretEntryAttribute("", "", false);
        }
        Bundle args = new Bundle();
        args.putSerializable(ARG_ATTR, attr);
        args.putInt(ARG_ATTR_POS, attrPos);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle args = getArguments();
        mAttr = (SecretEntryAttribute) args.getSerializable(ARG_ATTR);
        mAttrPos = args.getInt(ARG_ATTR_POS);
        mKsMgr = AppCentral.getInstance().getKeyStoreManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attr, container, false);
        mTxtKey = (TextView) view.findViewById(R.id.txtKey);
        mTxtValue = (TextView) view.findViewById(R.id.txtValue);
        mChkProtected = (ToggleButton) view.findViewById(R.id.chkProtected);
        mBtnGenerate = (FloatingActionButton) view.findViewById(R.id.btnGenerate);

        if (savedInstanceState == null) {
            mTxtKey.setText(mAttr.getKey());
            if (mAttr.isProtected()) {
                if (mAttr.getValue() != null && mAttr.getValue().length() > 0) {
                    try {
                        String plain = mKsMgr.decrupt(mAttr.getValue());
                        mTxtValue.setText(plain);
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        LogEx.e(e.getMessage(), e);
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
        if(!mChkProtected.isChecked()){
            mBtnGenerate.setVisibility(View.GONE);
        }
        mChkProtected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mBtnGenerate.setVisibility(View.VISIBLE);
                } else {
                    mBtnGenerate.setVisibility(View.GONE);
                }

            }
        });
        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(mGeneratedPwd != null){
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
        InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        String protectedStr = mTxtValue.getText().toString();
        if (mChkProtected.isChecked()) {
            try {
                protectedStr = AppCentral.getInstance().getKeyStoreManager().encrypt(protectedStr);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                LogEx.e(e.getMessage(), e);
            }
        }
        SecretEntryAttribute attr = new SecretEntryAttribute(
                mTxtKey.getText().toString(), protectedStr, mChkProtected.isChecked());
        Intent intent = new Intent();
        Bundle args = new Bundle();
        args.putInt(ARG_ATTR_POS, mAttrPos);
        args.putSerializable(ARG_ATTR, attr);
        intent.putExtras(args);
        getFragmentManager().popBackStack();
        getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_UPDATE, intent);

    }

    @Override
    public void onPasswordGenerated(String password) {
        mGeneratedPwd = password;
    }
}
