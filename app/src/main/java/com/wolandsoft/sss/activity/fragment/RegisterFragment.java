package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.ServiceProvider;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.User;
import com.wolandsoft.sss.service.AuthInvocationException;
import com.wolandsoft.sss.service.AuthService;

/**
 * New user registration fragment.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class RegisterFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private EditText mEdtUserID;
    private EditText mEdtDisplayName;
    private EditText mEdtPassword;
    private EditText mEdtPasswordRepeat;
    private ProgressBar mPasswordState;

    private AuthService mService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        //connect to next button
        FloatingActionButton btnNext = (FloatingActionButton) view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClicked();
            }
        });
        //obtain user id, display name and passwords fields
        mEdtUserID = (EditText) view.findViewById(R.id.edtUserID);
        mEdtDisplayName = (EditText) view.findViewById(R.id.edtDisplayName);
        mEdtPassword = (EditText) view.findViewById(R.id.edtPassword);
        mEdtPasswordRepeat = (EditText) view.findViewById(R.id.edtPasswordRepeat);

        TextWatcher twPassword = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordIndicator(s.toString());
            }
        };
        mEdtPassword.addTextChangedListener(twPassword);
        mEdtPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    updatePasswordIndicator(mEdtPassword.getText().toString());
                } else {
                    mPasswordState.setVisibility(View.INVISIBLE);
                }
            }
        });

        mPasswordState = (ProgressBar) view.findViewById(R.id.prbStrength);
        mPasswordState.setVisibility(View.INVISIBLE);

        return view;
    }

    private void updatePasswordIndicator(String password) {
        int passwordStrength = 1;

        if (password.length() > 5) {
            passwordStrength++;
        } // minimal pw length of 6
        if (password.toLowerCase() != password) {
            passwordStrength++;
        } // lower and upper case
        if (password.length() > 7) {
            passwordStrength++;
        } // good pw length of 8+
        int numDigits = getNumberDigits(password);
        if (numDigits > 0 && numDigits != password.length()) {
            passwordStrength++;
        } // contains digits and non-digits

        int progressValue = passwordStrength * 100 / 5;
        mPasswordState.setVisibility(password.length() > 0 ? View.VISIBLE : View.INVISIBLE);

        if (progressValue > 66) {
            mPasswordState.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        } else if (progressValue > 33) {
            mPasswordState.getProgressDrawable().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
        } else {
            mPasswordState.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        }
        mPasswordState.setProgress(progressValue);
    }

    private int getNumberDigits(String inString) {
        int numDigits = 0;
        int length = inString.length();
        for (int i = 0; i < length; i++) {
            if (Character.isDigit(inString.charAt(i))) {
                numDigits++;
            }
        }
        return numDigits;
    }

    /**
     * Validate user input.
     *
     * @param userID         user ID as email
     * @param displayName    display name
     * @param password       password
     * @param passwordRepeat password
     * @return return 0 for valid input and error message id for invalid
     */
    private int validateInput(String userID, String displayName, String password, String passwordRepeat) {
        userID = userID.trim();

        //validate for empty user
        if (TextUtils.isEmpty(userID))
            return R.string.empty_user_id_error;

        //validate for valid email
        if (!Patterns.EMAIL_ADDRESS.matcher(userID).matches())
            return R.string.invalid_user_id_error;

        //validate that user not taken
        if (mService == null)
            return R.string.service_connection_problem_error;

        try {
            if (!mService.isFreeID(userID))
                return R.string.taken_user_id_error;
        } catch (AuthInvocationException e) {
            //should never happen, except for bugs
            Log.e(EntriesFragment.class.getSimpleName(), e.getMessage(), e);
            return R.string.internal_error;
        }

        //validate for empty display name
        if (TextUtils.isEmpty(displayName))
            return R.string.empty_display_name_error;

        //validate for empty password
        if (TextUtils.isEmpty(password))
            return R.string.empty_password_error;

        //validate for the same repeated password
        if (!password.equals(passwordRepeat))
            return R.string.repeated_password_mismatch_error;

        return 0;
    }

    /**
     * Next button event
     */
    private void onNextClicked() {
        String userID = mEdtUserID.getText().toString();
        String displayName = mEdtDisplayName.getText().toString();
        String password = mEdtPassword.getText().toString();
        String passwordRepeat = mEdtPasswordRepeat.getText().toString();
        User user = null;
        int error = validateInput(userID, displayName, password, passwordRepeat);
        if (error == 0) {
            try {
                user = mService.signup(userID, displayName, password); // newer null
            } catch (Exception e) {
                //should never happen, except for bugs
                Log.e(EntriesFragment.class.getSimpleName(), e.getMessage(), e);
                error = R.string.internal_error;
            }
        }
        if (error == 0) {
            //delegate to parent activity
            mListener.onUserSignupInfoProvided(user);
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
         * Triggers when proper registration information supplied and user presses next button.
         *
         * @param user registered user entity
         */
        void onUserSignupInfoProvided(User user);
    }
}
