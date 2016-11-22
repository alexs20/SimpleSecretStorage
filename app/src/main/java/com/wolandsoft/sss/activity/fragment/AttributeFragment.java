package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AttributeFragment extends Fragment implements PwdGenFragment.OnFragmentToFragmentInteract {
    private final static String ARG_ATTR = "attr";
    private final static String ARG_ATTR_POS = "attr_pos";

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
        View currentFocus = getActivity().getCurrentFocus();
        if (currentFocus != null) {
            //nide soft keyboard
            InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        String protectedStr = mTxtValue.getText().toString();
        if (mChkProtected.isChecked()) {
            try {
                protectedStr = AppCentral.getInstance().getKeyStoreManager().encrypt(protectedStr);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                LogEx.e(e.getMessage(), e);
            }
        }
        SecretEntryAttribute attr = new SecretEntryAttribute(mTxtKey.getText().toString(), protectedStr, mChkProtected.isChecked());
        Fragment parent = getTargetFragment();
        if (parent instanceof OnFragmentToFragmentInteract) {
            ((OnFragmentToFragmentInteract) parent).onAttributeUpdate(mAttrPos, attr);
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
    public void onPasswordGenerate(String password) {
        mGeneratedPwd = password;
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    interface OnFragmentToFragmentInteract {
        void onAttributeUpdate(int pos, SecretEntryAttribute attr);
    }
}
