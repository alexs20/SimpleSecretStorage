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
package com.wolandsoft.sss.activity.fragment.entry;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.attribute.AttributeFragment;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.service.core.CoreServiceProxy;
import com.wolandsoft.sss.service.pccomm.PairedDevice;
import com.wolandsoft.sss.service.pccomm.PcCommServiceProxy;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

/**
 * Single secret entry edit.
 *
 * @author Alexander Shulgin
 */
public class EntryFragment extends Fragment implements
        AttributeFragment.OnFragmentToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract,
        CoreServiceProxy.OnCoreServiceReadyListener {
    private final static String ARG_ITEM_ID = "item_id";
    private static final int DELETE_ATTRIBUTE_CONFIRMATION_DIALOG = 1;

    private final static String ARG_POSITION = "position";
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private View mView;
    private boolean mIsShowPwd = false;
    private CoreServiceProxy mCore;
    private PcCommServiceProxy mPcComm;

    public static EntryFragment newInstance(int entryId) {
        LogEx.d("newInstance(" + entryId + ")");
        EntryFragment fragment = new EntryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ITEM_ID, entryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        LogEx.d("onAttach()");
        super.onAttach(context);
        mCore = new CoreServiceProxy(context).addListener(this);
        mPcComm = new PcCommServiceProxy(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        int entryId = args.getInt(ARG_ITEM_ID);

        mRecyclerViewAdapter = new RecyclerViewAdapter(mCore, entryId) {
            @Override
            void onItemDelete(int index) {
                onEntryAttributeDelete(index);
            }

            @Override
            void onItemEdit(int index) {
                onEntryAttributeEdit(index);
            }

            @Override
            void onItemNavigate(SecretEntryAttribute attr) {
                onEntryAttributeNavigate(attr);
            }

            @Override
            void onItemCopy(SecretEntryAttribute attr, PairedDevice device) {
                onEntryAttributeCopy(attr, device);
            }
        };

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        KeySharedPreferences pref = new KeySharedPreferences(shPref, getContext());

        if (savedInstanceState != null) {
            mIsShowPwd = savedInstanceState.getBoolean(String.valueOf(R.id.mnuShowPwd));
        } else {
            mIsShowPwd = pref.getBoolean(R.string.pref_protected_field_default_visibility_key,
                    R.bool.pref_protected_field_default_visibility_value);
        }
        mRecyclerViewAdapter.setProtectedVisible(mIsShowPwd);

        //enabling actionbar icons
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_entry, container, false);

        RecyclerView rView = (RecyclerView) mView.findViewById(R.id.rvAttrList);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(new LinearLayoutManager(getContext()));
        rView.setAdapter(mRecyclerViewAdapter);
        mRecyclerViewAdapter.updateModel();

        FloatingActionButton btnAdd = (FloatingActionButton) mView.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_secret_fields);

        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(String.valueOf(R.id.mnuShowPwd), mIsShowPwd);
    }

    private void onAddClicked() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(mRecyclerViewAdapter.getItemCount(), null);
        fragment.setTargetFragment(EntryFragment.this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Bundle args) {
        switch (requestCode) {
            case DELETE_ATTRIBUTE_CONFIRMATION_DIALOG:
                int idx = args.getInt(ARG_POSITION);
                if (resultCode == Activity.RESULT_OK) {
                    mRecyclerViewAdapter.deleteItem(idx);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    //restoring presence of deleted attribute
                    mRecyclerViewAdapter.notifyItemChanged(idx);
                }
                break;
        }
    }

    @Override
    public void onAttributeUpdate(int idx, SecretEntryAttribute attr) {
        mRecyclerViewAdapter.updateItem(attr, idx);
    }

    private void onEntryAttributeDelete(int position) {
        Bundle extras = new Bundle();
        extras.putInt(ARG_POSITION, position);

        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                R.string.label_delete_field, R.string.message_attribute_confirmation, true, extras);
        fragment.setCancelable(true);
        fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE_CONFIRMATION_DIALOG);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    private void onEntryAttributeEdit(int position) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(position, mRecyclerViewAdapter.getEntry().get(position));
        fragment.setTargetFragment(EntryFragment.this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
        mView.showContextMenu();
    }

    private void onEntryAttributeNavigate(SecretEntryAttribute attr) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attr.getValue()));
        startActivity(browserIntent);
    }

    private void onEntryAttributeCopy(SecretEntryAttribute attr, PairedDevice device) {
        String text = attr.getValue();
        if (text.length() > 0) {
            if (device != null) {
                mPcComm.sendData(device, attr.getKey(), text);
            } else {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(attr.getKey(), text);
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(getContext(), attr.getKey() + " " + getString(R.string.label_copied), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_entry_options_menu, menu);

        MenuItem showPwdMenuItem = menu.findItem(R.id.mnuShowPwd);
        showPwdMenuItem.setChecked(mIsShowPwd);
        showPwdMenuItem.setIcon(mIsShowPwd ? R.mipmap.img24dp_no_eye_w : R.mipmap.img24dp_eye_w);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mnuShowPwd) {
            item.setChecked(!item.isChecked());
            item.setIcon(item.isChecked() ? R.mipmap.img24dp_no_eye_w : R.mipmap.img24dp_eye_w);
            mIsShowPwd = item.isChecked();
            mRecyclerViewAdapter.setProtectedVisible(mIsShowPwd);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCoreServiceReady() {
        mRecyclerViewAdapter.updateModel();
    }
}
