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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.ISharedObjects;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.PredefinedAttribute;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.LogEx;

import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;


/**
 * List of secret entries with search capability.
 *
 * @author Alexander Shulgin
 */
public class EntriesFragment extends Fragment implements SearchView.OnQueryTextListener,
        EntryFragment.OnFragmentToFragmentInteract,
        ImportFragment.OnFragmentToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {
    private static final int DELETE_ITEM_CONFIRMATION_DIALOG = 1;
    private static final String KEY_ID = "id";
    private static final String KEY_SEARCH_PHRASE = "search_phrase";
    private ISharedObjects mHost;
    private RVAdapter mRVAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ISharedObjects) {
            mHost = (ISharedObjects) context;
        } else {
            throw new ClassCastException(String.format(getString(R.string.internal_exception_must_implement),
                    context.toString(), ISharedObjects.class.getName()));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RVAdapter.OnRVAdapterActionListener icl = new RVAdapter.OnRVAdapterActionListener() {
            @Override
            public void onRVItemClick(SecretEntry entry) {
                EntriesFragment.this.onSecretEntryClick(entry);
            }

            @Override
            public void onRVItemDismiss(SecretEntry entry) {
                EntriesFragment.this.onSecretEntryDelete(entry);
            }
        };
        String searchCriteria = "";
        int selectedItemId = 0;
        if (savedInstanceState != null) {
            searchCriteria = savedInstanceState.getString(KEY_SEARCH_PHRASE, searchCriteria);
        }
        //Recycler view adapter
        mRVAdapter = new RVAdapter(icl, mHost.getSQLiteStorage(), searchCriteria);
        //enabling action bar menu items
        setHasOptionsMenu(true);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_PHRASE, mRVAdapter.getSearchCriteria());
    }

    private void onAddClicked() {
        //create new entry
        SecretEntry entry = new SecretEntry();
        for (PredefinedAttribute attr : PredefinedAttribute.values()) {
            String key = getString(attr.getKeyResID());
            String value = "";
            if (attr.isProtected()) {
                try {
                    value = mHost.getKeyStoreManager().encrypt(value);
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    LogEx.e(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            entry.add(new SecretEntryAttribute(key, value, attr.isProtected()));
        }
        try {
            entry = mHost.getSQLiteStorage().put(entry);
        } catch (StorageException e) {
            LogEx.e(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(entry.getID());
        fragment.setTargetFragment(this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    private void onSecretEntryClick(SecretEntry entry) {
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
        inflater.inflate(R.menu.fragment_entries_options_menu, menu);
        //attaching search view
        MenuItem mnuSearch = menu.findItem(R.id.mnuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(mnuSearch);
        searchView.setOnQueryTextListener(this);
        if (!mRVAdapter.getSearchCriteria().isEmpty()) {
            mnuSearch.expandActionView();
            searchView.setQuery(mRVAdapter.getSearchCriteria(), true);
            searchView.clearFocus();
        }
        super.onCreateOptionsMenu(menu, inflater);
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
    public void onDialogResult(int requestCode, int resultCode, Bundle args) {
        switch (requestCode) {
            case DELETE_ITEM_CONFIRMATION_DIALOG:
                int id = args.getInt(KEY_ID);
                if (resultCode == Activity.RESULT_OK) {
                    mRVAdapter.deleteItem(id);
                } else {
                    mRVAdapter.reloadItem(id);
                }
                break;
        }
    }

    @Override
    public void onEntryUpdate(SecretEntry entry) {
        mRVAdapter.updateItem(entry);
    }

    @Override
    public void onImportCompleted() {
        mRVAdapter.reload();
    }

    static class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {
        private static final long DELAY_SEARCH_UPDATE = 1000;
        private final OnRVAdapterActionListener mOnActionListener;
        private final Handler mHandler;
        private final SQLiteStorage mSqLtStorage;
        private List<Integer> mItemIds = null;
        private String mSearchCriteria;
        private Runnable mSearchUpdate;

        RVAdapter(OnRVAdapterActionListener onActionListener,
                  SQLiteStorage sqLtStorage,
                  String searchCriteria) {
            mOnActionListener = onActionListener;
            mSqLtStorage = sqLtStorage;
            mSearchCriteria = searchCriteria;
            mHandler = new Handler();
            try {
                mItemIds = mSqLtStorage.find(mSearchCriteria, true);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        void updateSearchCriteria(final String criteria) {
            if (mSearchUpdate != null)
                mHandler.removeCallbacks(mSearchUpdate);
            mSearchUpdate = new Runnable() {
                @Override
                public void run() {
                    if (!criteria.equals(mSearchCriteria)) {
                        mSearchCriteria = criteria;
                        reload();
                    }
                }
            };
            mHandler.postDelayed(mSearchUpdate, DELAY_SEARCH_UPDATE);
        }

        @Override
        public RVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entries_include_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(RVAdapter.ViewHolder holder, int position) {
            holder.itemView.setLongClickable(true);
            final SecretEntry entry = getItem(position);

            String capitalTitle = entry.get(0).getValue();
            holder.mTxtTitle.setText(capitalTitle);
            int next = 0;
            while (++next < entry.size() && entry.get(next).isProtected()) ;
            if (next < entry.size() && !entry.get(next).isProtected()) {
                holder.mTxtTitleSmall.setText(entry.get(next).getValue());
                holder.mTxtTitleSmall.setVisibility(View.VISIBLE);
            } else {
                holder.mTxtTitleSmall.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnActionListener.onRVItemClick(entry);
                }
            });

            //make colored capital character
            String capChar = "?";
            capitalTitle = capitalTitle.trim();
            if (capitalTitle.length() > 0) {
                capChar = capitalTitle.substring(0, 1).toUpperCase();
            }
            ColorGenerator generator = ColorGenerator.DEFAULT;
            int color = generator.getColor(capitalTitle);
            TextDrawable drawable = TextDrawable.builder().beginConfig().bold().endConfig().buildRound(capChar, color);
            holder.mImgIcon.setImageDrawable(drawable);
        }

        @Override
        public int getItemCount() {
            return mItemIds.size();
        }

        private void onItemDismiss(int position) {
            SecretEntry item = getItem(position);
            mOnActionListener.onRVItemDismiss(item);
        }

        String getSearchCriteria() {
            return mSearchCriteria;
        }

        void deleteItem(int id) {
            //get position of the item
            int idx = mItemIds.indexOf(id);
            try {
                mSqLtStorage.delete(id);
                mItemIds.remove(idx);
                notifyItemRemoved(idx);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        void reload() {
            try {
                mItemIds = mSqLtStorage.find(mSearchCriteria, true);
                notifyDataSetChanged();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        void reloadItem(int id) {
            //get position of the item
            int idx = mItemIds.indexOf(id);
            notifyItemChanged(idx);
        }

        void updateItem(SecretEntry item) {
            try {
                mSqLtStorage.put(item);
                mItemIds = mSqLtStorage.find(mSearchCriteria, true);
                notifyDataSetChanged();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        @Nullable
        SecretEntry getItem(final int idx) {
            int id = mItemIds.get(idx);
            try {
                return mSqLtStorage.get(id);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        //ItemTouchHelper extension
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
                @Override
                public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                    return makeMovementFlags(0, swipeFlags);
                }

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    onItemDismiss(viewHolder.getAdapterPosition());
                }
            };
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        interface OnRVAdapterActionListener {
            void onRVItemClick(SecretEntry entry);

            void onRVItemDismiss(SecretEntry entry);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mTxtTitle;
            final TextView mTxtTitleSmall;
            final ImageView mImgIcon;

            ViewHolder(View view) {
                super(view);
                mTxtTitle = (TextView) view.findViewById(R.id.txtTitle);
                mTxtTitleSmall = (TextView) view.findViewById(R.id.txtTitleSmall);
                mImgIcon = (ImageView) view.findViewById(R.id.imgIcon);
            }
        }
    }
}
