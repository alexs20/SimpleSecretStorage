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
import android.content.res.ColorStateList;
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
import android.support.v7.widget.CardView;
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

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.ISharedObjects;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.LogEx;

import java.util.List;


/**
 * List of secret entries with search capability.
 *
 * @author Alexander Shulgin
 */
public class EntriesFragment extends Fragment implements SearchView.OnQueryTextListener,
        EntryFragment.OnFragmentToFragmentInteract, ImportFragment.OnFragmentToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {
    private static final int DELETE_ITEM_CONFIRMATION_DIALOG = 1;
    private static final String KEY_ID = "id";
    private RVAdapter mRVAdapter;
    private MenuItem mMnuSearch;
    private MenuItem mMnuDelete;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        ISharedObjects sharedObj;
        if (context instanceof ISharedObjects) {
            sharedObj = (ISharedObjects) context;
        } else {
            throw new ClassCastException(
                    String.format(
                            getString(R.string.internal_exception_must_implement),
                            context.toString(), ISharedObjects.class.getName()));
        }
        RVAdapter.OnRVAdapterActionListener icl = new RVAdapter.OnRVAdapterActionListener() {
            @Override
            public void onRVItemClick(SecretEntry entry) {
                EntriesFragment.this.onSecretEntryClick(entry);
            }

            @Override
            public void onRVItemSelect(SecretEntry entry) {
                EntriesFragment.this.onSecretEntrySelect(entry);
            }
        };

        mRVAdapter = new RVAdapter(icl, sharedObj.getSQLiteStorage());
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

    private void onSecretEntryClick(SecretEntry entry) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment.newInstance(entry);
        fragment.setTargetFragment(this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    private void onSecretEntrySelect(SecretEntry entry) {
        if (entry == null) {
            mMnuSearch.setVisible(true);
            mMnuDelete.setVisible(false);
        } else {
            mMnuSearch.setVisible(false);
            mMnuDelete.setVisible(true);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_entries_options_menu, menu);
        //attaching search view
        mMnuSearch = menu.findItem(R.id.mnuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(mMnuSearch);
        searchView.setOnQueryTextListener(this);

        mMnuDelete = menu.findItem(R.id.mnuDelete);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mnuDelete) {
            onDeleteItemClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void onDeleteItemClicked() {
        SecretEntry se = mRVAdapter.getSelectedItem();
        if (se != null) {
            Bundle data = new Bundle();
            data.putInt(KEY_ID, se.getID());
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                    R.string.label_delete_entry, R.string.message_item_confirmation, true, data);
            fragment.setCancelable(true);
            fragment.setTargetFragment(this, DELETE_ITEM_CONFIRMATION_DIALOG);
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
        }
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Bundle args) {
        switch (requestCode) {
            case DELETE_ITEM_CONFIRMATION_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    int id = args.getInt(KEY_ID);
                    mRVAdapter.deleteItem(id);
                }
                break;
        }
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

    static class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {
        private static final long DELAY_SEARCH_UPDATE = 1000;
        private List<Integer> mSeIds = null;
        private OnRVAdapterActionListener mOnActionListener;
        private String mSearchCriteria = "";
        private Handler mHandler;
        private Runnable mSearchUpdate;
        private SQLiteStorage mSQLtStorage;
        private int mSelectedItemPosition = -1;
        private int mActiveColor;

        RVAdapter(OnRVAdapterActionListener onActionListener, SQLiteStorage sqltStorage) {
            mOnActionListener = onActionListener;
            mHandler = new Handler();
            mSQLtStorage = sqltStorage;

            try {
                mSeIds = mSQLtStorage.find(mSearchCriteria, true);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
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
        public RVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entries_include_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(RVAdapter.ViewHolder holder, int position) {
            holder.itemView.setLongClickable(true);
            final SecretEntry entry = getItem(position);
            if (entry != null) {
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
                final int fPosition = position;
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int oldPosition = -1;
                        if (mSelectedItemPosition == fPosition) {
                            mSelectedItemPosition = -1;
                            mOnActionListener.onRVItemSelect(null);
                        } else {
                            oldPosition = mSelectedItemPosition;
                            mSelectedItemPosition = fPosition;
                            mOnActionListener.onRVItemSelect(entry);
                        }
                        notifyItemChanged(fPosition);
                        if (oldPosition > -1) {
                            notifyItemChanged(oldPosition);
                        }
                        return true;
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
            } else {
                holder.mTxtTitle.setText(R.string.label_loading_ellipsis);
                holder.mImgIcon.setImageResource(R.mipmap.img24dp_wait_g);
            }
            if (mSelectedItemPosition == position) {
                ((CardView) holder.itemView).setCardBackgroundColor(mSQLtStorage.getResources().getColor(R.color.colorAccent));
            } else {
                ((CardView) holder.itemView).setCardBackgroundColor(holder.mBackgroundColor);
            }
        }

        @Override
        public int getItemCount() {
            return mSeIds.size();
        }

        void clearSelection(){
            if (mSelectedItemPosition > -1) {
                int idx = mSelectedItemPosition;
                mSelectedItemPosition = -1;
                //notify the listener that selected item in not selected anymore
                notifyItemChanged(idx);
                mOnActionListener.onRVItemSelect(null);
            }
        }

        void deleteItem(int id) {
            clearSelection();
            //get position of the item
            int idx = mSeIds.indexOf(id);
            try {
                mSQLtStorage.delete(id);
                mSeIds.remove(idx);
                notifyItemRemoved(idx);
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        void refresh() {
            clearSelection();
            try {
                mSeIds = mSQLtStorage.find(mSearchCriteria, true);
                notifyDataSetChanged();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        void updateItem(SecretEntry se) {
            try {
                se = mSQLtStorage.put(se);
                refresh();
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        @Nullable
        SecretEntry getItem(final int idx) {
            int id = mSeIds.get(idx);
            try {
                SecretEntry entry = mSQLtStorage.get(id, new SQLiteStorage.OnSecretEntryRetrieveListener() {
                    @Override
                    public void onSecretEntryRetrieved(SecretEntry entry) {
                        notifyItemChanged(idx);
                    }
                });
                return entry;
            } catch (StorageException e) {
                LogEx.e(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        SecretEntry getSelectedItem() {
            if (mSelectedItemPosition > -1) {
                return getItem(mSelectedItemPosition);
            }
            return null;
        }

        interface OnRVAdapterActionListener {
            void onRVItemClick(SecretEntry entry);

            void onRVItemSelect(SecretEntry entry);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTxtTitle;
            TextView mTxtTitleSmall;
            ImageView mImgIcon;
            ColorStateList mBackgroundColor;

            ViewHolder(View view) {
                super(view);
                mTxtTitle = (TextView) view.findViewById(R.id.txtTitle);
                mTxtTitleSmall = (TextView) view.findViewById(R.id.txtTitleSmall);
                mImgIcon = (ImageView) view.findViewById(R.id.imgIcon);
                mBackgroundColor = ((CardView) itemView).getCardBackgroundColor();
            }
        }
    }
}
