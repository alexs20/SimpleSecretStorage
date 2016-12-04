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
import java.util.Arrays;
import java.util.List;

/**
 * Import from external file.
 *
 * @author Alexander Shulgin
 */
public class ImportFragment extends Fragment implements FileDialogFragment.OnDialogToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {
    private final static int DONE_DIALOG = 1;
    private ArrayAdapter<String> mExtEngAdapter;

    private Spinner mSprExtEngine;
    private Spinner mSprConflictResolution;
    private TextView mTxtSourcePath;
    private EditText mEdtPassword;
    private EditText mEdtPasswordOpen;
    private RelativeLayout mLayWait;
    private boolean mIsShowPwd;
    private FloatingActionButton mBtnApply;

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

        View view = inflater.inflate(R.layout.fragment_import, container, false);

        mSprExtEngine = (Spinner) view.findViewById(R.id.sprExtEngine);
        mSprConflictResolution = (Spinner) view.findViewById(R.id.sprConflictResolution);
        mEdtPassword = (EditText) view.findViewById(R.id.edtPassword);
        mEdtPasswordOpen = (EditText) view.findViewById(R.id.edtPasswordOpen);
        mTxtSourcePath = (TextView) view.findViewById(R.id.txtSourcePath);
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
            actionBar.setTitle(R.string.title_import);
        return view;
    }

    private void setOpenPasswordView(View view) {
        TableRow tr = (TableRow) view.findViewById(R.id.trPasswordOpen);
        tr.setVisibility(mIsShowPwd ? View.VISIBLE : View.GONE);
        tr = (TableRow) view.findViewById(R.id.trPassword);
        tr.setVisibility(mIsShowPwd ? View.GONE : View.VISIBLE);
    }

    private void onDestinationSelectClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = FileDialogFragment.newInstance(true);
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
            args.password = mEdtPassword.getText().toString();
        }
        String source = mTxtSourcePath.getText().toString();
        if (!source.startsWith("/")) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_source_file_selected, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }
        args.source = new File(source);
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
        String conflictRes = mSprConflictResolution.getSelectedItem().toString();
        if (conflictRes.equals(getString(R.string.label_merge))) {
            args.conflictResolution = IExternal.ConflictResolution.merge;
        } else {
            args.conflictResolution = IExternal.ConflictResolution.overwrite;
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
                            R.string.label_export, R.string.message_import_process_completed, false, null);
                    fragment.setCancelable(true);
                    fragment.setTargetFragment(ImportFragment.this, DONE_DIALOG);
                    transaction.addToBackStack(null);
                    fragment.show(transaction, DialogFragment.class.getName());
                }
            }

            @Override
            protected Boolean doInBackground(ExportArgs... params) {
                try {
                    params[0].engine.doImport(AppCentral.getInstance().getSQLiteStorage(),
                            AppCentral.getInstance().getKeyStoreManager(), params[0].conflictResolution,
                            params[0].source.toURI(), params[0].password);
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
        mTxtSourcePath.setText(path.toString());
    }

    @Override
    public void onDialogResult(int requestCode, int result, Bundle args) {
        if (requestCode == DONE_DIALOG) {
            Fragment parent = getTargetFragment();
            if (parent instanceof OnFragmentToFragmentInteract) {
                ((OnFragmentToFragmentInteract) parent).onImportCompleted();
            } else {
                throw new ClassCastException(
                        String.format(
                                getString(R.string.internal_exception_must_implement),
                                parent.toString(),
                                OnFragmentToFragmentInteract.class.getName()
                        )
                );
            }
            getFragmentManager().popBackStack(ImportFragment.class.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    interface OnFragmentToFragmentInteract {
        void onImportCompleted();
    }

    class ExportArgs {
        IExternal engine;
        String password;
        File source;
        IExternal.ConflictResolution conflictResolution;
    }
}
