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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.wolandsoft.sss.AppConstants;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.activity.fragment.dialog.FileDialogFragment;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.external.IExternal;
import com.wolandsoft.sss.service.ExportImportService;
import com.wolandsoft.sss.service.ServiceManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Import from external file.
 *
 * @author Alexander Shulgin
 */
public class ImportFragment extends BaseFragment implements FileDialogFragment.OnDialogToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {

    private ArrayAdapter<String> mExtEngAdapter;

    private Spinner mSprExtEngine;
    private Spinner mSprConflictResolution;
    private EditText mEdtPassword;
    private EditText mEdtPasswordOpen;
    private FloatingActionButton mBtnApply;
    private TextView mTxtExtEngineLabel;
    private TextView mTxtConflictResLabel;
    private Button mBtnSource;
    private RelativeLayout mLayoutWait;
    private RelativeLayout mLayoutForm;
    private RelativeLayout mLayoutPermissions;
    private boolean mIsShowPwd;
    private File mSourcePath;
    private BroadcastReceiver mBrReceiver;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //
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
        //an import result status
        mBrReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ExportImportService.BROADCAST_EVENT_COMPLETED.equals(intent.getAction())) {
                    int task = intent.getIntExtra(ExportImportService.KEY_TASK, -1);
                    boolean status = intent.getBooleanExtra(ExportImportService.KEY_STATUS, false);
                    onServiceResult(task, status);
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int[] attrs = new int[]{R.attr.colorAccent, android.R.attr.textColorSecondary};
        TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
        final int colorActive = a.getColor(0, Color.RED);
        final int colorPassive = a.getColor(1, Color.GRAY);
        a.recycle();

        View view = inflater.inflate(R.layout.fragment_import, container, false);
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

        mTxtConflictResLabel = (TextView) view.findViewById(R.id.txtConflictResLabel);
        mTxtConflictResLabel.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                mTxtConflictResLabel.setTextColor(b ? colorActive : colorPassive);
            }
        });
        mTxtConflictResLabel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mTxtConflictResLabel.setFocusable(false);
                mTxtConflictResLabel.setFocusableInTouchMode(false);
                return false;
            }
        });
        mSprConflictResolution = (Spinner) view.findViewById(R.id.sprConflictResolution);
        mSprConflictResolution.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mTxtConflictResLabel.setFocusable(true);
                mTxtConflictResLabel.setFocusableInTouchMode(true);
                mTxtConflictResLabel.requestFocus();
                return false;
            }
        });

        mEdtPassword = (EditText) view.findViewById(R.id.edtPassword);
        mEdtPasswordOpen = (EditText) view.findViewById(R.id.edtPasswordOpen);
        mBtnSource = (Button) view.findViewById(R.id.btnSelectSource);
        mBtnSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSourceSelectClicked();
            }
        });

        Button btnPermissions = (Button) view.findViewById(R.id.btnPermissions);
        btnPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(AppConstants.EXTERNAL_STORAGE_PERMISSIONS);
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
            actionBar.setTitle(R.string.title_import);
        return view;
    }

    private void onServiceResult(int task, boolean status) {
        mLayoutWait.setVisibility(View.GONE);
        mBtnApply.setEnabled(false);
        if (task == ExportImportService.TASK_IMPORT) {
            FragmentManager fragmentManager = getFragmentManager();
            if (status) {
                fragmentManager.popBackStack();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_info,
                        R.string.label_import, R.string.message_import_process_completed, false, null);
                fragment.setCancelable(true);
                fragment.setTargetFragment(ImportFragment.this, 0);
                transaction.addToBackStack(null);
                fragment.show(transaction, DialogFragment.class.getName());
            } else {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                        R.string.label_import, R.string.message_import_process_failed, false, null);
                fragment.setCancelable(true);
                fragment.setTargetFragment(ImportFragment.this, 0);
                transaction.addToBackStack(null);
                fragment.show(transaction, DialogFragment.class.getName());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mBrReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean askForPermissions = false;
        for (String permission : AppConstants.EXTERNAL_STORAGE_PERMISSIONS) {
            int permissionGranted = ContextCompat.checkSelfPermission(getContext(), permission);
            if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
                askForPermissions = true;
                break;
            }
        }
        mLayoutForm.setVisibility(askForPermissions ? View.GONE : View.VISIBLE);
        mLayoutPermissions.setVisibility(askForPermissions ? View.VISIBLE : View.GONE);

        IntentFilter filter = new IntentFilter(ExportImportService.BROADCAST_EVENT_COMPLETED);
        getContext().registerReceiver(mBrReceiver, filter);
        if (!askForPermissions) {
            if (ServiceManager.isServiceRunning(getContext(), ExportImportService.class)) {
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
    }

    private void onSourceSelectClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = FileDialogFragment.newInstance(true);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, 9);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    private void onApplyClicked() {
        String selectedEngine = mSprExtEngine.getSelectedItem().toString();
        String password;
        if (mIsShowPwd) {
            password = mEdtPasswordOpen.getText().toString();
        } else {
            password = mEdtPassword.getText().toString();
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
        if (mSourcePath == null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_source_file_selected, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }
        String conflictRes = mSprConflictResolution.getSelectedItem().toString();
        if (conflictRes.equals(getString(R.string.label_merge))) {
            conflictRes = IExternal.ConflictResolution.merge.name();
        } else {
            conflictRes = IExternal.ConflictResolution.overwrite.name();
        }

        mLayoutWait.setVisibility(View.VISIBLE);
        mBtnApply.setEnabled(false);

        Intent intent = new Intent(getContext(), ExportImportService.class);
        intent.putExtra(ExportImportService.KEY_TASK, ExportImportService.TASK_IMPORT);
        intent.putExtra(ExportImportService.KEY_ENGINE, selectedEngine);
        intent.putExtra(ExportImportService.KEY_CONFLICT_RESOLUTION, conflictRes);
        intent.putExtra(ExportImportService.KEY_PASSWORD, password);
        intent.putExtra(ExportImportService.KEY_PATH, mSourcePath.getAbsolutePath());
        getContext().startService(intent);
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
                mEdtPassword.setText(pwd);
            } else {
                String pwd = mEdtPassword.getText().toString();
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
        mBtnSource.setText(uiPath);
        mSourcePath = path;
    }

    @Override
    public void onDialogResult(int requestCode, int result, Bundle args) {

    }
}
