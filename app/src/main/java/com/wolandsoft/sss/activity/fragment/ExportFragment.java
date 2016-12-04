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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.activity.fragment.dialog.FileDialogFragment;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.external.IExternal;
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.LogEx;

import java.io.File;
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
public class ExportFragment extends Fragment implements FileDialogFragment.OnDialogToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {
    private static final int DONE_DIALOG = 1;
    private static final String OUTPUT_FILE_NAME = "secret_export_%1$s.zip";
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.US);

    private ArrayAdapter<String> mExtEngAdapter;

    private Spinner mSprExtEngine;
    private TextView mTxtDestinationPath;
    private EditText mEdtPassword1;
    private EditText mEdtPassword2;
    private EditText mEdtPasswordOpen;
    private RelativeLayout mLayWait;
    private FloatingActionButton mBtnApply;

    private boolean mIsShowPwd;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ExternalFactory extFactory = ExternalFactory.getInstance(context);
        List<String> engines = Arrays.asList(extFactory.getAvailableIds());
        mExtEngAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, engines);
        mExtEngAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_export, container, false);

        mSprExtEngine = (Spinner) view.findViewById(R.id.sprExtEngine);
        mEdtPassword1 = (EditText) view.findViewById(R.id.edtPassword);
        mEdtPassword2 = (EditText) view.findViewById(R.id.edtPasswordRepeat);
        mEdtPasswordOpen = (EditText) view.findViewById(R.id.edtPasswordOpen);
        mTxtDestinationPath = (TextView) view.findViewById(R.id.txtDestinationPath);
        Button btnSelectDest = (Button) view.findViewById(R.id.btnSelectDest);
        btnSelectDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestinationSelectClicked();
            }
        });
        mLayWait = (RelativeLayout) view.findViewById(R.id.layWait);

        if (savedInstanceState == null) {

        } else {
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

    private void setOpenPasswordView(View view) {
        TableRow tr = (TableRow) view.findViewById(R.id.trPasswordOpen);
        tr.setVisibility(mIsShowPwd ? View.VISIBLE : View.GONE);
        tr = (TableRow) view.findViewById(R.id.trPassword);
        tr.setVisibility(mIsShowPwd ? View.GONE : View.VISIBLE);
        tr = (TableRow) view.findViewById(R.id.trPasswordRepeat);
        tr.setVisibility(mIsShowPwd ? View.GONE : View.VISIBLE);
    }

    private void onDestinationSelectClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = FileDialogFragment.newInstance(false);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, 9);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    private void onApplyClicked() {
        ExportArgs args = new ExportArgs();
        String selectedEngine = mSprExtEngine.getSelectedItem().toString();
        try {
            args.engine = ExternalFactory.getInstance(getContext()).getExternal(selectedEngine);
        } catch (ExternalException e) {
            LogEx.e(e.getMessage(), e);
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (mIsShowPwd) {
            args.password = mEdtPasswordOpen.getText().toString();
        } else {
            args.password = mEdtPassword1.getText().toString();
            String pwd2 = mEdtPassword2.getText().toString();
            if (!args.password.equals(pwd2)) {
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
        if (args.password.length() == 0) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_password, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }
        String destination = mTxtDestinationPath.getText().toString();
        if (!destination.startsWith("/")) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_destination_directory_selected, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }
        args.destination = new File(destination, String.format(OUTPUT_FILE_NAME, format.format(new Date())));
        while (args.destination.exists()) {
            args.destination = new File(destination, String.format(OUTPUT_FILE_NAME, format.format(new Date())));
        }

        new AsyncTask<ExportArgs, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mLayWait.setVisibility(View.VISIBLE);
                mBtnApply.setEnabled(false);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                mLayWait.setVisibility(View.GONE);
                if (aBoolean) {
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_info,
                            R.string.label_export, R.string.message_export_process_completed, false, null);
                    fragment.setCancelable(true);
                    fragment.setTargetFragment(ExportFragment.this, DONE_DIALOG);
                    transaction.addToBackStack(null);
                    fragment.show(transaction, DialogFragment.class.getName());
                }
            }

            @Override
            protected Boolean doInBackground(ExportArgs... params) {
                try {
                    params[0].engine.doExport(AppCentral.getInstance().getSQLiteStorage(),
                            AppCentral.getInstance().getKeyStoreManager(),
                            params[0].destination.toURI(), params[0].password);
                } catch (ExternalException e) {
                    LogEx.e(e.getMessage(), e);
                    return false;
                }
                return true;
            }
        }.execute(args);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enabling search icon
        setHasOptionsMenu(true);
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
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(String.valueOf(R.id.showPwd), mIsShowPwd);
    }

    @Override
    public void onFileSelected(File path) {
        mTxtDestinationPath.setText(path.toString());
    }

    @Override
    public void onDialogResult(int requestCode, int result, Bundle args) {
        if (requestCode == DONE_DIALOG) {
            getFragmentManager().popBackStack(ExportFragment.class.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    class ExportArgs {
        IExternal engine;
        String password;
        File destination;
    }
}
