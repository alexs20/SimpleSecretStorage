package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntryAttribute;

import java.util.UUID;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AttributeFragment extends Fragment {
    private final static String ARG_ATTR = "attr";
    private final static String ARG_SE_UUID = "se_uuid";
    private final static String ARG_ATTR_POS = "attr_pos";
    private OnFragmentInteractionListener mListener;

    private TextView mTxtKey;
    private TextView mTxtValue;
    private ToggleButton mChkProtected;
    private UUID mSeUUID = null;
    private int mAttrPos = -1;

    public static AttributeFragment newInstance(UUID seUUID, int attrPos, SecretEntryAttribute attr) {
        AttributeFragment fragment = new AttributeFragment();
        if (attr != null) {
            Bundle args = new Bundle();
            args.putSerializable(ARG_ATTR, attr);
            args.putSerializable(ARG_SE_UUID, seUUID);
            args.putInt(ARG_ATTR_POS, attrPos);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    String.format(getString(R.string.internal_exception_must_implement), context.toString(),
                            OnFragmentInteractionListener.class.getName()));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attr, container, false);
        mTxtKey = (TextView) view.findViewById(R.id.txtKey);
        mTxtValue = (TextView) view.findViewById(R.id.txtValue);
        mChkProtected = (ToggleButton) view.findViewById(R.id.chkProtected);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null && !args.isEmpty()) {
                SecretEntryAttribute attr = (SecretEntryAttribute) args.getSerializable(ARG_ATTR);
                mSeUUID = (UUID) args.getSerializable(ARG_SE_UUID);
                mAttrPos = args.getInt(ARG_ATTR_POS);
                if(attr != null) {
                    mTxtKey.setText(attr.getKey());
                    mTxtValue.setText(attr.getValue());
                    mChkProtected.setChecked(attr.isProtected());
                }
            }
        }

        FloatingActionButton btnOk = (FloatingActionButton) view.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClicked();
            }
        });
        return view;
    }


    private void onOkClicked() {
        SecretEntryAttribute attr = new SecretEntryAttribute(
                mTxtKey.getText().toString(), mTxtValue.getText().toString(), mChkProtected.isChecked());
        mListener.onSecretEntryAttributeApply(mSeUUID, mAttrPos, attr);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public interface OnFragmentInteractionListener {
        void onSecretEntryAttributeApply(UUID seUUID, int attrPos, SecretEntryAttribute attr);
    }
}
