package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.activity.fragment.dialog.DirectoryDialogFragment;
import com.wolandsoft.sss.activity.fragment.dialog.FileDialogFragment;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.util.KeySharedPreferences;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class ImportFragment extends Fragment implements FileDialogFragment.OnDialogToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract{
    private final static int DONE_DIALOG = 1;
    private KeySharedPreferences mPref;
    private ExternalFactory mExtFactory;
    private ArrayAdapter<String> mExtEngAdapter;

    private Spinner mSprExtEngine;

    private TextView mTxtSourcePath;
    private Button mBtnSelectDest;
    private EditText mEdtPassword;
    private EditText mEdtPasswordOpen;

    private boolean mIsShowPwd;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mExtFactory = ExternalFactory.getInstance(context);

        List<String> engines = Arrays.asList(mExtFactory.getAvailableIds());
        mExtEngAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, engines);
        mExtEngAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPref = new KeySharedPreferences(shPref, getContext());
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        mSprExtEngine = (Spinner)view.findViewById(R.id.sprExtEngine);
        mEdtPassword= (EditText) view.findViewById(R.id.edtPassword);
        mEdtPasswordOpen= (EditText) view.findViewById(R.id.edtPasswordOpen);
        mTxtSourcePath = (TextView) view.findViewById(R.id.txtSourcePath);

        mBtnSelectDest = (Button) view.findViewById(R.id.btnSelectDest);
        mBtnSelectDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestinationSelectClicked();
            }
        });


        if (savedInstanceState == null) {

        } else {
            mIsShowPwd = savedInstanceState.getBoolean(String.valueOf(R.id.showPwd));
        }

        setOpenPasswordView(view);

        mSprExtEngine.setAdapter(mExtEngAdapter);

        FloatingActionButton btnApply = (FloatingActionButton) view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onApplyClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.label_export);
        return view;
    }

    private void setOpenPasswordView(View view){
        TableRow tr = (TableRow) view.findViewById(R.id.trPasswordOpen);
        tr.setVisibility(mIsShowPwd ? View.VISIBLE : View.GONE);
        tr = (TableRow) view.findViewById(R.id.trPassword);
        tr.setVisibility(mIsShowPwd ? View.GONE : View.VISIBLE);
    }

    private void onDestinationSelectClicked(){
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = FileDialogFragment.newInstance(true);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, 9);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    private void onApplyClicked() {
        String exportEngine = mSprExtEngine.getSelectedItem().toString();
        String pwd;
        if(mIsShowPwd){
            pwd = mEdtPasswordOpen.getText().toString();
        } else {
            pwd = mEdtPassword.getText().toString();
        }
        String source = mTxtSourcePath.getText().toString();
        if(!source.startsWith("/")){
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_no_source_file_selected, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, 0); //response is going to be ignored
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
            return;
        }
        //TODO do export
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
            if(mIsShowPwd){
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

    }
}
