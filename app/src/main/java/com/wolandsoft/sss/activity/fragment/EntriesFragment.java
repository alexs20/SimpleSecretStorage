package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.LogEx;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class EntriesFragment extends Fragment implements SearchView.OnQueryTextListener{

    private SQLiteStorage mStorage;
    private RecyclerView mRecyclerView;
    private SecretEntriesAdapter mRecyclerViewAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mStorage = new SQLiteStorage(context);
            SecretEntriesAdapter.OnSecretEntryClickListener icl = new SecretEntriesAdapter.OnSecretEntryClickListener(){
                @Override
                public void onSecretEntryClick(SecretEntry entry) {
                    EntriesFragment.this.onSecretEntryClick(entry);
                }
            };
            mRecyclerViewAdapter = new SecretEntriesAdapter(icl, mStorage);
        } catch (StorageException e) {
            LogEx.e(e.getMessage(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entries, container, false);
        //init recycler view
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rvEntriesList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        //connect to add button
        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });
        //restore title
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.app_name);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enabling search icon
        setHasOptionsMenu(true);
    }

    private void onAddClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(null);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onSecretEntryClick(SecretEntry entry) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(entry);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mStorage != null) {
            mStorage.close();
            mStorage = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_entries_options_menu, menu);
        //attaching search view
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        LogEx.d(newText);
        return false;
    }

    public static class SecretEntriesAdapter extends RecyclerView.Adapter<SecretEntriesAdapter.ViewHolder> {
        private static final int BG_LOAD_STEP = 10;
        private int mCount;
        private Map<Integer, SecretEntry> mEntries;
        private OnSecretEntryClickListener mOnClickListener;
        private SQLiteStorage mStorage;

        public SecretEntriesAdapter(OnSecretEntryClickListener onClickListener, SQLiteStorage storage) {
            mOnClickListener = onClickListener;
            this.mStorage = storage;
            mEntries = Collections.synchronizedMap(new HashMap<Integer, SecretEntry>());
            try {
                mCount = mStorage.count(null);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
            }
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        int pos = 0;
                        while (pos < mCount) {
                            if (!mEntries.containsKey(pos)) {
                                List<SecretEntry> entryList = mStorage.find(null, true, pos, 1);
                                if (entryList.isEmpty()) {
                                    break;
                                }
                                mEntries.put(pos, entryList.get(0));
                            }
                            pos++;
                        }
                    } catch (StorageException e) {
                        LogEx.e(e.getMessage(), e);
                    }
                    return null;
                }
            }.execute();
        }

        @Override
        public SecretEntriesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entries_include_card, parent, false);
            ViewHolder holder = new ViewHolder(card);
            return holder;
        }

        @Override
        public void onBindViewHolder(SecretEntriesAdapter.ViewHolder holder, final int position) {
            SecretEntry entry = getItem(position);
            holder.mTxtTitle.setText(entry.get(0).getValue());
            holder.mImgIcon.setImageResource(R.mipmap.img24dp_lock);
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SecretEntry entry = getItem(position);
                    mOnClickListener.onSecretEntryClick(entry);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mCount;
        }


        public SecretEntry getItem(int position) {
            if (!mEntries.containsKey(position)) {
                try {
                    List<SecretEntry> entryList = mStorage.find(null, true, position, 1);
                    mEntries.put(position, entryList.get(0));
                    if (entryList.isEmpty()) {
                        return null;
                    }
                } catch (StorageException e) {
                    LogEx.e(e.getMessage(), e);
                    return null;
                }
            }
            return mEntries.get(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public View mView;
            public TextView mTxtTitle;
            public ImageView mImgIcon;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mTxtTitle = (TextView) view.findViewById(R.id.txtTitle);
                mImgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            }
        }

        interface OnSecretEntryClickListener {
            void onSecretEntryClick(SecretEntry entry);
        }
    }
}
