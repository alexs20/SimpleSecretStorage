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
package com.wolandsoft.sss.activity.fragment.entries;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.entry.EntryFragment;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.PredefinedAttribute;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.service.core.CoreServiceProxy;
import com.wolandsoft.sss.service.external.ExternalServiceProxy;
import com.wolandsoft.sss.util.LogEx;

import java.io.IOException;


/**
 * List of secret entries with search capability.
 *
 * @author Alexander Shulgin
 */
public class EntriesFragment extends Fragment implements
        SearchView.OnQueryTextListener,
        AlertDialogFragment.OnDialogToFragmentInteract,
        CoreServiceProxy.OnCoreServiceReadyListener,
        ExternalServiceProxy.OnImportStatusListener {
    private static final int DELETE_ITEM_CONFIRMATION_DIALOG = 1;
    private static final String KEY_ID = "id";
    private static final String KEY_SEARCH_PHRASE = "search_phrase";
    private RecyclerViewAdapter mRecViewAdapter;
    private CoreServiceProxy mCore;
    private ExternalServiceProxy mExternal;

    @Override
    public void onAttach(Context context) {
        LogEx.d("onAttach()");
        super.onAttach(context);
        mCore = new CoreServiceProxy(context).addListener(this);
        mExternal = new ExternalServiceProxy(context).addListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogEx.d("onCreate()");
        super.onCreate(savedInstanceState);
        String searchCriteria = "";
        if (savedInstanceState != null) {
            searchCriteria = savedInstanceState.getString(KEY_SEARCH_PHRASE, searchCriteria);
        }
        //Recycler view adapter
        mRecViewAdapter = new RecyclerViewAdapter(mCore){
            @Override
            void onItemDismiss(SecretEntry entry) {
                onSecretEntryDelete(entry);
            }

            @Override
            void onItemClick(SecretEntry entry) {
                onSecretEntryClick(entry);
            }
        };
        mRecViewAdapter.updateSearchCriteria(searchCriteria);
        //enabling action bar menu items
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        LogEx.d("onDestroy()");
        try {
            mCore.close();
        } catch (IOException ignore) {
        }
        try {
            mExternal.close();
        } catch (IOException ignore) {
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogEx.d("onCreateView()");
        View view = inflater.inflate(R.layout.fragment_entries, container, false);
        //init recycler view
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rvEntriesList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mRecViewAdapter);
        mRecViewAdapter.setSearchLocked(false);
        mRecViewAdapter.updateModel();
        //connect to add button
        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });
        //restore title
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.app_name);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        LogEx.d("onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_PHRASE, mRecViewAdapter.getSearchCriteria());
    }

    private void onAddClicked() {
        if (mCore.isServiceActive()) {
            //preserve search criteria
            mRecViewAdapter.setSearchLocked(true);
            //create new entry
            SecretEntry entry = new SecretEntry();
            for (PredefinedAttribute attr : PredefinedAttribute.values()) {
                String key = getString(attr.getKeyResID());
                String value = attr.getValueResID() != 0 ? getString(attr.getValueResID()) : "";
                entry.add(new SecretEntryAttribute(key, value, attr.isProtected()));
            }
            entry = mCore.putRecord(entry);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Fragment fragment = EntryFragment.newInstance(entry.getID());
            transaction.replace(R.id.content_fragment, fragment);
            transaction.addToBackStack(EntryFragment.class.getName());
            transaction.commit();
        }
    }

    private void onSecretEntryClick(SecretEntry entry) {
        //preserve search criteria
        mRecViewAdapter.setSearchLocked(true);
        //open entry view
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(entry.getID());
        fragment.setTargetFragment(this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    private void onSecretEntryDelete(SecretEntry item) {
        Bundle data = new Bundle();
        data.putInt(KEY_ID, item.getID());
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                R.string.label_delete_entry, R.string.message_entry_confirmation, true, data);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, DELETE_ITEM_CONFIRMATION_DIALOG);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogEx.d("onCreateOptionsMenu()");
        inflater.inflate(R.menu.fragment_entries_options_menu, menu);
        //attaching search view
        MenuItem mnuSearch = menu.findItem(R.id.mnuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(mnuSearch);
        if (!mRecViewAdapter.getSearchCriteria().isEmpty()) {
            mnuSearch.expandActionView();
            searchView.setQuery(mRecViewAdapter.getSearchCriteria(), true);
            searchView.clearFocus();
        }
        searchView.setOnQueryTextListener(this);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //searching
        mRecViewAdapter.updateSearchCriteria(newText);
        return true;
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Bundle args) {
        switch (requestCode) {
            case DELETE_ITEM_CONFIRMATION_DIALOG:
                int id = args.getInt(KEY_ID);
                if (resultCode == Activity.RESULT_OK) {
                    mRecViewAdapter.deleteItem(id);
                } else {
                    mRecViewAdapter.reloadItem(id);
                }
                break;
        }
    }

    @Override
    public void onCoreServiceReady() {
        if (mRecViewAdapter != null) mRecViewAdapter.updateModel();
    }

    @Override
    public void onImportStatus(boolean status) {
        if (mRecViewAdapter != null && status) mRecViewAdapter.updateModel();
    }
}
