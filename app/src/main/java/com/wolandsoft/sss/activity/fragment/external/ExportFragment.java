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
package com.wolandsoft.sss.activity.fragment.external;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.activity.fragment.dialog.FileDialogFragment;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.service.external.ExternalServiceProxy;
import com.wolandsoft.sss.util.LogEx;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Export into external file.
 *
 * @author Alexander Shulgin
 */
public class ExportFragment extends BaseFragment implements
        FileDialogFragment.OnDialogToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract,
        ExternalServiceProxy.OnExportStatusListener {
    private static final String OUTPUT_FILE_NAME = "secret_export_%1$s.zip";
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);

    private ArrayAdapter<String> mExtEngAdapter;

    private Spinner mSprExtEngine;
    private EditText mEdtPassword1;
    private EditText mEdtPassword2;
    private EditText mEdtPasswordOpen;
    private FloatingActionButton mBtnApply;
    private TextView mTxtExtEngineLabel;
    private Button mBtnDestination;
    private RelativeLayout mLayoutWait;
    private RelativeLayout mLayoutForm;
    private RelativeLayout mLayoutPermissions;
    private boolean mIsShowPwd;
    private File mDestinationPath;
    private ExternalServiceProxy mExternal;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mExternal = new ExternalServiceProxy(context).addListener(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExternalFactory extFactory = ExternalFactory.getInstance(getContext());
        List<String> engines = Arrays.asList(extFactory.getAvailableIds());
        mExtEngAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, engines);
        mExtEngAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //enabling show pwd icon
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int[] attrs = new int[]{R.attr.colorAccent, android.R.attr.textColorSecondary};
        TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
        final int colorActive = a.getColor(0, Color.RED);
        final int colorPassive = a.getColor(1, Color.GRAY);
        a.recycle();

        View view = inflater.inflate(R.layout.fragment_export, container, false);
        mTxtExtEngineLabel = (TextView) view.findViewById(R.id.txtExtEngineLabel);
        mTxtExtEngineLabel.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                mTxtExtEngineLabel.setTextColor(b ? colorActive : colorPassive);
            }
        });
        mTxtExtEngineLabel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mTxtExtEngineLabel.setFocusable(false);
                mTxtExtEngineLabel.setFocusableInTouchMode(false);
                return false;
            }
        });
        mSprExtEngine = (Spinner) view.findViewById(R.id.sprExtEngine);
        mSprExtEngine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mTxtExtEngineLabel.setFocusable(true);
                mTxtExtEngineLabel.setFocusableInTouchMode(true);
                mTxtExtEngineLabel.requestFocus();
                return false;
            }
        });

        mEdtPassword1 = (EditText) view.findViewById(R.id.edtPassword);
        mEdtPassword2 = (EditText) view.findViewById(R.id.edtPasswordRepeat);
        mEdtPasswordOpen = (EditText) view.findViewById(R.id.edtPasswordOpen);

        mBtnDestination = (Button) view.findViewById(R.id.btnSelectDest);
        mBtnDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestinationSelectClicked();
            }
        });

        Button btnPermissions = (Button) view.findViewById(R.id.btnPermissions);
        btnPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(EXTERNAL_STORAGE_PERMISSIONS);
            }
        });

        mLayoutWait = (RelativeLayout) view.findViewById(R.id.layoutWait);
        mLayoutForm = (RelativeLayout) view.findViewById(R.id.layoutForm);
        mLayoutPermissions = (RelativeLayout) view.findViewById(R.id.layoutPermissions);

        if (savedInstanceState != null) {
            mIsShowPwd = savedInstanceState.getBoolean(String.valueOf(R.id.showPwd));
        }

        setOpenPasswordView(view);

        mSprExtEngine.setAdapter(mExtEngAdapter);

        mBtnApply = (FloatingActionButton) view.findViewById(R.id.btnApply);
        mBtnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onApplyClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_export);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean askForPermissions = false;
        for (String permission : EXTERNAL_STORAGE_PERMISSIONS) {
            int permissionGranted = ContextCompat.checkSelfPermission(getContext(), permission);
            if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
                askForPermissions = true;
                break;
            }
        }
        mLayoutForm.setVisibility(askForPermissions ? View.GONE : View.VISIBLE);
        mLayoutPermissions.setVisibility(askForPermissions ? View.VISIBLE : View.GONE);

        if (!askForPermissions) {
            if (mExternal.isServiceActive()) {
                mLayoutWait.setVisibility(View.VISIBLE);
                mBtnApply.setEnabled(false);
            } else {
                mLayoutWait.setVisibility(View.GONE);
                mBtnApply.setEnabled(true);
            }
        }
    }

    private void setOpenPasswordView(View view) {
        TextInputLayout til = (TextInputLayout) view.findViewById(R.id.tilPasswordOpen);
        til.setVisibility(mIsShowPwd ? View.VISIBLE : View.GONE);
        til = (TextInputLayout) view.findViewById(R.id.tilPassword);
        til.setVisibility(mIsShowPwd ? View.GONE : View.VISIBLE);
        til = (TextInputLayout) view.findViewById(R.id.tilPasswordRepeat);
        til.setVisibility(mIsShowPwd ? View.GONE : View.VISIBLE);
    }

    private void onDestinationSelectClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = FileDialogFragment.newInstance(false);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, 0);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    private void onApplyClicked() {
        String selectedEngine = mSprExtEngine.getSelectedItem().toString();
        String password;
        if (mIsShowPwd) {
            password = mEdtPasswordOpen.getText().toString();
        } else {
            password = mEdtPassword1.getText().toString();
            String pwd2 = mEdtPassword2.getText().toString();
            if (!password.equals(pwd2)) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                        R.string.label_error, R.string.message_password_not_the_same, false, null);
                fragment.setCancelable(true);
                fragment.setTargetFragment(this, 0); //response is going to be ignored
                transaction.addToBackStack(null);
                fragment.show(transaction, DialogFragment.class.getName());
                return;
            }
        }
        if (password.length() == 0) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_password, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }
        if (mDestinationPath == null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_destination_directory_selected, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }

        File destination = new File(mDestinationPath, String.format(OUTPUT_FILE_NAME, format.format(new Date())));
        while (destination.exists()) {
            destination = new File(mDestinationPath, String.format(OUTPUT_FILE_NAME, format.format(new Date())));
        }

        mLayoutWait.setVisibility(View.VISIBLE);
        mBtnApply.setEnabled(false);
        mExternal.doExport(selectedEngine, destination.toURI(), password);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_export_options_menu, menu);
        MenuItem item = menu.findItem(R.id.showPwd);
        item.setChecked(mIsShowPwd);
        item.setIcon(mIsShowPwd ? R.mipmap.img24dp_no_eye_w : R.mipmap.img24dp_eye_w);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.showPwd) {
            if (mIsShowPwd) {
                String pwd = mEdtPasswordOpen.getText().toString();
                mEdtPassword1.setText(pwd);
                mEdtPassword2.setText(pwd);
            } else {
                String pwd = mEdtPassword1.getText().toString();
                mEdtPasswordOpen.setText(pwd);
            }
            item.setChecked(!item.isChecked());
            item.setIcon(item.isChecked() ? R.mipmap.img24dp_no_eye_w : R.mipmap.img24dp_eye_w);
            mIsShowPwd = item.isChecked();
            setOpenPasswordView(getView());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(String.valueOf(R.id.showPwd), mIsShowPwd);
    }

    @Override
    public void onFileSelected(File path, String uiPath) {
        mBtnDestination.setText(uiPath);
        mDestinationPath = path;
    }

    @Override
    public void onDialogResult(int requestCode, int result, Bundle args) {

    }

    @Override
    public void onDestroy() {
        LogEx.d("onDestroy()");
        try {
            mExternal.close();
        } catch (IOException ignore) {
        }
        super.onDestroy();
    }

    @Override
    public void onExportStatus(boolean status) {
        mLayoutWait.setVisibility(View.GONE);
        mBtnApply.setEnabled(true);
        FragmentManager fragmentManager = getFragmentManager();
        if (status) {
            fragmentManager.popBackStack();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_info,
                    R.string.label_export, R.string.message_export_process_completed, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(ExportFragment.this, 0);
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
        } else {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_export, R.string.message_export_process_failed, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(ExportFragment.this, 0);
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
        }
    }
}
