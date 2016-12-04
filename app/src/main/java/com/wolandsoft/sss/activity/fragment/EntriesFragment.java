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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.LruCache;
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
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.LogEx;


/**
 * List of secret entries with search capability.
 *
 * @author Alexander Shulgin
 */
public class EntriesFragment extends Fragment implements SearchView.OnQueryTextListener,
        EntryFragment.OnFragmentToFragmentInteract, ImportFragment.OnFragmentToFragmentInteract {
    private SecretEntriesAdapter mRVAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SQLiteStorage sqltStorage = AppCentral.getInstance().getSQLiteStorage();
        LruCache<Integer, SecretEntry> entriesCache = AppCentral.getInstance().getEntriesCache();

        SecretEntriesAdapter.OnSecretEntryClickListener icl = new SecretEntriesAdapter.OnSecretEntryClickListener() {
            @Override
            public void onSecretEntryClick(SecretEntry entry) {
                EntriesFragment.this.onSecretEntryClick(entry);
            }
        };
        mRVAdapter = new SecretEntriesAdapter(icl, sqltStorage, entriesCache);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entries, container, false);
        //init recycler view
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rvEntriesList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mRVAdapter);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enabling search icon
        setHasOptionsMenu(true);
    }

    private void onAddClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(null);
        fragment.setTargetFragment(this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    public void onSecretEntryClick(SecretEntry entry) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(entry);
        fragment.setTargetFragment(this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //searching
        mRVAdapter.updateSearchCriteria(newText);
        return true;
    }

    @Override
    public void onEntryUpdate(SecretEntry entry) {
        mRVAdapter.updateItem(entry);
    }

    @Override
    public void onEntryDelete(int id) {
        mRVAdapter.deleteItem(id);
    }

    @Override
    public void onImportCompleted() {
        mRVAdapter.refresh();
    }

    static class SecretEntriesAdapter extends RecyclerView.Adapter<SecretEntriesAdapter.ViewHolder> {
        private static final long DELAY_SEARCH_UPDATE = 1000;
        private int[] mSeIds = null;
        private OnSecretEntryClickListener mOnClickListener;
        private String mSearchCriteria = "";
        private Handler mHandler;
        private Runnable mSearchUpdate;
        private SQLiteStorage mSQLtStorage;
        private LruCache<Integer, SecretEntry> mEntriesCache;

        SecretEntriesAdapter(OnSecretEntryClickListener onClickListener, SQLiteStorage sqltStorage, LruCache<Integer, SecretEntry> entriesCache) {
            mOnClickListener = onClickListener;
            mHandler = new Handler();
            mSQLtStorage = sqltStorage;
            mEntriesCache = entriesCache;
            try {
                mSeIds = mSQLtStorage.find(mSearchCriteria, true);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
            }
        }

        void updateSearchCriteria(final String criteria) {
            mHandler.removeCallbacks(mSearchUpdate);
            mSearchUpdate = new Runnable() {
                @Override
                public void run() {
                    if (!criteria.equals(mSearchCriteria)) {
                        mSearchCriteria = criteria;
                        refresh();
                    }
                }
            };
            mHandler.postDelayed(mSearchUpdate, DELAY_SEARCH_UPDATE);
        }

        @Override
        public SecretEntriesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entries_include_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(SecretEntriesAdapter.ViewHolder holder, int position) {
            final SecretEntry entry = getItem(position);
            if (entry != null) {
                holder.mTxtTitle.setText(entry.get(0).getValue());
                int next = 0;
                while (++next < entry.size() && entry.get(next).isProtected()) ;
                if (next < entry.size() && !entry.get(next).isProtected()) {
                    holder.mTxtTitleSmall.setText(entry.get(next).getValue());
                    holder.mTxtTitleSmall.setVisibility(View.VISIBLE);
                } else {
                    holder.mTxtTitleSmall.setVisibility(View.GONE);
                }
                holder.mImgIcon.setImageResource(R.mipmap.img48dp_lock_g);
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnClickListener.onSecretEntryClick(entry);
                    }
                });
            } else {
                holder.mTxtTitle.setText(R.string.label_loading_ellipsis);
                holder.mImgIcon.setImageResource(R.mipmap.img24dp_wait_g);
            }
        }

        @Override
        public int getItemCount() {
            return mSeIds == null ? 0 : mSeIds.length;
        }

        void deleteItem(int id) {
            try {
                mSQLtStorage.delete(id);
                mEntriesCache.remove(id);
                refresh();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
            }
        }

        void refresh() {
            try {
                mSeIds = mSQLtStorage.find(mSearchCriteria, true);
                notifyDataSetChanged();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
            }
        }

        void updateItem(SecretEntry se) {
            try {
                se = mSQLtStorage.put(se);
                mEntriesCache.put(se.getID(), se);
                refresh();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
            }
        }

        @Nullable
        SecretEntry getItem(int position) {
            int id = mSeIds[position];
            SecretEntry entry = mEntriesCache.get(id);
            //on-demand loading items if not available
            if (entry == null) {
                new AsyncTask<Integer, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Integer... params) {
                        try {
                            int id = mSeIds[params[0]];
                            SecretEntry entry = mSQLtStorage.get(id);
                            if (entry != null) {
                                mEntriesCache.put(id, entry);
                                return params[0];
                            }
                        } catch (StorageException e) {
                            LogEx.e(e.getMessage(), e);
                        }
                        return -1;
                    }

                    @Override
                    protected void onPostExecute(Integer position) {
                        if (position > -1) {
                            SecretEntriesAdapter.this.notifyItemChanged(position);
                        }
                    }
                }.execute(position);
            }
            return entry;
        }

        interface OnSecretEntryClickListener {
            void onSecretEntryClick(SecretEntry entry);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View mView;
            TextView mTxtTitle;
            TextView mTxtTitleSmall;
            ImageView mImgIcon;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mTxtTitle = (TextView) view.findViewById(R.id.txtTitle);
                mTxtTitleSmall = (TextView) view.findViewById(R.id.txtTitleSmall);
                mImgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            }
        }
    }
}
