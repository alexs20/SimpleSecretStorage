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
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.common.TheApp;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

import java.util.Collections;

/**
 * Single secret entry edit.
 *
 * @author Alexander Shulgin
 */
public class EntryFragment extends Fragment implements AttributeFragment.OnFragmentToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {
    private final static String ARG_ITEM_ID = "item_id";
    private static final int DELETE_ATTRIBUTE_CONFIRMATION_DIALOG = 1;

    private final static String ARG_POSITION = "position";
    private RVAdapter mRVAdapter;
    private View mView;
    private boolean mIsShowPwd = false;
    private KeySharedPreferences mKsPref;

    public static EntryFragment newInstance(int entryId) {
        EntryFragment fragment = new EntryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ITEM_ID, entryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        int entryId = args.getInt(ARG_ITEM_ID);

        mRVAdapter = new RVAdapter(new RVAdapter.OnRVAdapterActionListener() {
            @Override
            public void onEntryAttributeDelete(int position) {
                EntryFragment.this.onEntryAttributeDelete(position);
            }

            @Override
            public void onEntryAttributeEdit(int position) {
                EntryFragment.this.onEntryAttributeEdit(position);
            }

            @Override
            public void onEntryAttributeNavigate(SecretEntryAttribute attr) {
                EntryFragment.this.onEntryAttributeNavigate(attr);
            }

            @Override
            public void onEntryAttributeCopy(SecretEntryAttribute attr) {
                EntryFragment.this.onEntryAttributeCopy(attr);
            }
        }, entryId, TheApp.getSQLiteStorage(), TheApp.getKeyStoreManager());

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mKsPref = new KeySharedPreferences(shPref, getContext());

        if (savedInstanceState != null) {
            mIsShowPwd = savedInstanceState.getBoolean(String.valueOf(R.id.mnuShowPwd));
        } else {
            mIsShowPwd = mKsPref.getBoolean(R.string.pref_protected_field_default_visibility_key,
                    R.bool.pref_protected_field_default_visibility_value);
        }
        mRVAdapter.setProtectedVisible(mIsShowPwd);

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
        rView.setAdapter(mRVAdapter);
        mRVAdapter.updateModel();
        if (savedInstanceState == null) { // means first time
            boolean isAutoCopy = mKsPref.getBoolean(R.string.pref_auto_copy_protected_field_key, R.bool.pref_auto_copy_protected_field_value);
            if (isAutoCopy) {
                SecretEntry entry = mRVAdapter.getEntry();
                for (SecretEntryAttribute attr : entry) {
                    if (attr.isProtected()) {
                        onEntryAttributeCopy(attr);
                        break;
                    }
                }
            }
        }

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
        Fragment fragment = AttributeFragment.newInstance(mRVAdapter.getItemCount(), null);
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
                    mRVAdapter.deleteItem(idx);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    //restoring presence of deleted attribute
                    mRVAdapter.notifyItemChanged(idx);
                }
                break;
        }
    }

    @Override
    public void onAttributeUpdate(int idx, SecretEntryAttribute attr) {
        mRVAdapter.updateItem(attr, idx);
    }

    private void onEntryAttributeDelete(int position) {
        //swipe left/right to delete + delete from popup
        if (mRVAdapter.getItemCount() == 1) {
            //can not delete last element
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_can_not_delete_last_attribute, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE_CONFIRMATION_DIALOG);
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
        } else if (position == 0 && mRVAdapter.getEntry().get(1).isProtected()) {
            //can not perform delete operation when result leads to make a protected field be moved to the top
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                    R.string.label_error, R.string.message_can_not_delete_before_protected_attribute, false, null);
            fragment.setCancelable(true);
            fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE_CONFIRMATION_DIALOG);
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
        } else {
            //confirmation dialog
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
    }

    private void onEntryAttributeEdit(int position) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(position, mRVAdapter.getEntry().get(position));
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

    private void onEntryAttributeCopy(SecretEntryAttribute attr) {
        String text = attr.getValue();
        if (attr.isProtected()) {
            if (attr.getValue() != null && attr.getValue().length() > 0) {
                text = TheApp.getKeyStoreManager().decrupt(attr.getValue());
            }
        }
        if (text.length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(attr.getKey(), text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), attr.getKey() + " " + getString(R.string.label_copied), Toast.LENGTH_LONG).show();
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
            mRVAdapter.setProtectedVisible(mIsShowPwd);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {
        private final OnRVAdapterActionListener mOnActionListener;
        private KeyStoreManager mKs;
        private SQLiteStorage mDb;
        private SecretEntry mEntry;
        private boolean mIsProtectedVisible = false;

        RVAdapter(OnRVAdapterActionListener listener, int itemId,
                  SQLiteStorage db, KeyStoreManager ks) {
            mOnActionListener = listener;
            mDb = db;
            mKs = ks;
            mEntry = new SecretEntry(itemId, 0, 0);
        }

        public boolean onItemReorder(int fromPosition, int toPosition) {
            LogEx.d("onItemReorder ( ", fromPosition, ", ", toPosition, " )");
            //need to be sure that protected field will not move to the top
            if ((toPosition == 0 && mEntry.get(fromPosition).isProtected()) ||
                    (fromPosition == 0 && mEntry.get(1).isProtected())) {
                return false;
            }
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(mEntry, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(mEntry, i, i - 1);
                }
            }
            notifyItemMoved(fromPosition, toPosition);
            updateEntryInDb();
            return true;
        }

        public void onItemDismiss(int position) {
            mOnActionListener.onEntryAttributeDelete(position);
        }

        @Override
        public RVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entry_include_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.itemView.setLongClickable(true);
            final ViewHolder viewHolder = holder;
            final SecretEntryAttribute attr = mEntry.get(position);

            holder.mTxtKey.setText(attr.getKey());
            if (!attr.isProtected()) {
                holder.mTxtValue.setText(attr.getValue());
                holder.mImgProtected.setVisibility(View.GONE);
            } else {
                holder.mTxtValue.setText("");
                if (attr.getValue() != null && attr.getValue().length() > 0) {
                    if (mIsProtectedVisible) {
                        String plain = mKs.decrupt(attr.getValue());
                        holder.mTxtValue.setText(plain);
                    } else {
                        holder.mTxtValue.setText(R.string.label_hidden_password);
                    }
                }
            }
            holder.mImgMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPopup(v, viewHolder.getLayoutPosition(), attr);
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnActionListener.onEntryAttributeEdit(viewHolder.getLayoutPosition());
                }
            });
        }

        @SuppressWarnings("UnusedParameters")
        private void openPopup(final View v, final int position, final SecretEntryAttribute attr) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.fragment_entry_card_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.mnuCopy:
                            mOnActionListener.onEntryAttributeCopy(attr);
                            break;
                        case R.id.mnuNavigate:
                            mOnActionListener.onEntryAttributeNavigate(attr);
                            break;
                    }
                    return false;
                }
            });
            if (attr.getValue().startsWith("http://") || attr.getValue().startsWith("https://")) {
                MenuItem mnuNavigate = popup.getMenu().findItem(R.id.mnuNavigate);
                mnuNavigate.setVisible(true);
            }
            MenuPopupHelper menuHelper = new MenuPopupHelper(v.getContext(), (MenuBuilder) popup.getMenu(), v);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        }

        @Override
        public int getItemCount() {
            return mEntry.size();
        }

        SecretEntry getEntry() {
            return mEntry;
        }

        void updateEntryInDb() {
            mDb.put(mEntry);
        }

        void deleteItem(int idx) {
            mEntry.remove(idx);
            notifyItemRemoved(idx);
            updateEntryInDb();
        }

        void updateItem(SecretEntryAttribute attr, int idx) {
            if (idx < mEntry.size()) {
                mEntry.set(idx, attr);
                notifyItemChanged(idx);
            } else {
                mEntry.add(attr);
                notifyItemInserted(idx);
            }
            updateEntryInDb();
        }

        void updateModel() {
            if (mDb != null && mKs != null) {
                mEntry = mDb.get(mEntry.getID());
                notifyDataSetChanged();
            }
        }

        void setProtectedVisible(boolean isProtectedVisible) {
            mIsProtectedVisible = isProtectedVisible;
            for (int i = 0; i < mEntry.size(); i++) {
                if (mEntry.get(i).isProtected()) {
                    notifyItemChanged(i);
                }
            }
        }

        //ItemTouchHelper extension
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
                @Override
                public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                    return makeMovementFlags(dragFlags, swipeFlags);
                }

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    return onItemReorder(viewHolder.getAdapterPosition(), target.getAdapterPosition());
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
            void onEntryAttributeDelete(int idx);

            void onEntryAttributeEdit(int idx);

            void onEntryAttributeNavigate(SecretEntryAttribute attr);

            void onEntryAttributeCopy(SecretEntryAttribute attr);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mTxtKey;
            final TextView mTxtValue;
            final ImageView mImgProtected;
            final ImageView mImgMenu;

            ViewHolder(View view) {
                super(view);
                mTxtKey = (TextView) view.findViewById(R.id.txtKey);
                mTxtValue = (TextView) view.findViewById(R.id.txtValue);
                mImgProtected = (ImageView) view.findViewById(R.id.imgProtected);
                mImgMenu = (ImageView) view.findViewById(R.id.imgMenu);
            }
        }
    }
}
