package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.ServiceProvider;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.User;
import com.wolandsoft.sss.service.AuthService;

/**
 * Login fragment.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class LoginFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private EditText mEdtUserID;
    private EditText mEdtPassword;

    private AuthService mService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        //connect to next button
        FloatingActionButton btnNext = (FloatingActionButton) view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClicked();
            }
        });
        //connect to new button
        FloatingActionButton btnNew = (FloatingActionButton) view.findViewById(R.id.btnNew);
        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNewClicked();
            }
        });
        //obtain user id and passwords fields
        mEdtUserID = (EditText) view.findViewById(R.id.edtUserID);
        mEdtPassword = (EditText) view.findViewById(R.id.edtPassword);

        return view;
    }

    /**
     * Validate user input.
     *
     * @param userID   user ID as email
     * @param password password
     * @return return 0 for valid input and error message id for invalid
     */
    private int validateInput(String userID, String password) {
        userID = userID.trim();

        //validate for empty user
        if (TextUtils.isEmpty(userID))
            return R.string.empty_user_id_error;

        //validate for valid email
        if (!Patterns.EMAIL_ADDRESS.matcher(userID).matches())
            return R.string.invalid_user_id_error;

        //validate for empty password
        if (TextUtils.isEmpty(password))
            return R.string.empty_password_error;

        return 0;
    }

    /**
     * Next button event
     */
    private void onNextClicked() {
        String userID = mEdtUserID.getText().toString();
        String password = mEdtPassword.getText().toString();
        User user = null;
        int error = validateInput(userID, password);
        if (error == 0) {
            try {
                user = mService.login(userID, password);
                if (user == null) {
                    error = R.string.unknown_user_id_or_password;
                }
            } catch (Exception e) {
                //should never happen, except for bugs
                Log.e(EntriesFragment.class.getSimpleName(), e.getMessage(), e);
                error = R.string.internal_error;
            }
        }
        if (error == 0) {
            //delegate to parent activity
            mListener.onUserLoginInfoProvided(user);
        } else {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img_error,
                    R.string.error_dialog_title, error);
            fragment.setCancelable(false);
            transaction.addToBackStack(null);
            fragment.show(transaction, "ALERT");
        }
    }

    /**
     * New button event
     */
    private void onNewClicked() {
        mListener.onSignupFlowSelected();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + OnFragmentInteractionListener.class.getSimpleName());
        }
        try {
            mService = ((ServiceProvider) context).getService();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + ServiceProvider.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        /**
         * Triggered when valid user id and password provided
         *
         * @param user registered user entity
         */
        void onUserLoginInfoProvided(User user);

        /**
         * Triggered when user press signup button
         */
        void onSignupFlowSelected();
    }
}
