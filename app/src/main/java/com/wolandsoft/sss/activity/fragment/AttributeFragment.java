package com.wolandsoft.sss.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.wolandsoft.sss.util.LogEx;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AttributeFragment extends Fragment {
    public static final int RESULT_UPDATE = 1;
    public final static String ARG_ATTR = "attr";
    public final static String ARG_ATTR_POS = "attr_pos";
    public static final int PWDGEN_FRAGMENT = 1;

    private TextView mTxtKey;
    private TextView mTxtValue;
    private ToggleButton mChkProtected;
    private FloatingActionButton mBtnGenerate;
    private int mAttrPos = -1;
    private String mKey;
    private String mValue;

    public static AttributeFragment newInstance(int attrPos, SecretEntryAttribute attr) {
        AttributeFragment fragment = new AttributeFragment();
        if (attr != null) {
            Bundle args = new Bundle();
            args.putSerializable(ARG_ATTR, attr);
            args.putInt(ARG_ATTR_POS, attrPos);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            SecretEntryAttribute attr = (SecretEntryAttribute) args.getSerializable(ARG_ATTR);
            mAttrPos = args.getInt(ARG_ATTR_POS);
        }
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
            Bundle args = getArguments();
            if (args != null && !args.isEmpty()) {
                SecretEntryAttribute attr = (SecretEntryAttribute) args.getSerializable(ARG_ATTR);
                mAttrPos = args.getInt(ARG_ATTR_POS);
                if (attr != null) {
                    mTxtKey.setText(attr.getKey());
                    if (!attr.isProtected()) {
                        mTxtValue.setText(attr.getValue());
                    } else {
                        if (attr.getValue() != null && attr.getValue().length() > 0) {
                            try {
                                String plain = AppCentral.getInstance().getKeyStoreManager().decrupt(attr.getValue());
                                mTxtValue.setText(plain);
                            } catch (BadPaddingException | IllegalBlockSizeException e) {
                                LogEx.e(e.getMessage(), e);
                            }
                        }
                    }
                    mChkProtected.setChecked(attr.isProtected());
                }
            }
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

    private void onGenerateClicked() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = PwdGenFragment.newInstance();
        fragment.setTargetFragment(this, PWDGEN_FRAGMENT);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(AttributeFragment.class.getName());
        transaction.commit();
    }

    private void onOkClicked() {
        InputMethodManager inputManager =
                (InputMethodManager) getContext().
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(
                getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

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
        getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_UPDATE, intent);
        getFragmentManager().popBackStack();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PWDGEN_FRAGMENT:
                if (resultCode == Activity.RESULT_OK) {
                    String pwd = data.getExtras().getString(PwdGenFragment.ARG_PWD);
                    mTxtValue.setText(pwd);
                }
                break;
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
