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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.ISharedObjects;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.PredefinedAttribute;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * Single secret entry edit.
 *
 * @author Alexander Shulgin
 */
public class EntryFragment extends Fragment implements AttributeFragment.OnFragmentToFragmentInteract,
        AlertDialogFragment.OnDialogToFragmentInteract {
    private final static String ARG_ENTRY = "entry";

    private static final int DELETE_ENTRY_CONFIRMATION_DIALOG = 1;
    private static final int DELETE_ATTRIBUTE_CONFIRMATION_DIALOG = 2;

    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
    private final static String ARG_POSITION = "position";
    private OnFragmentToFragmentInteract mListener;
    private RecyclerView mRecyclerView;
    private SecretEntryAdapter mRVAdapter;
    private View mView;
    private int mClickedPosition;
    private KeyStoreManager mKSManager;
    private boolean mIsShowPwd = false;

    public static EntryFragment newInstance(SecretEntry entry) {
        EntryFragment fragment = new EntryFragment();
        if (entry != null) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_ENTRY, entry);
            fragment.setArguments(args);
        }
        return fragment;
    }

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
        mKSManager = sharedObj.getKeyStoreManager();

        Fragment parent = getTargetFragment();
        if (parent instanceof OnFragmentToFragmentInteract) {
            mListener = (OnFragmentToFragmentInteract) parent;
        } else {
            throw new ClassCastException(
                    String.format(
                            getString(R.string.internal_exception_must_implement),
                            parent.toString(),
                            OnFragmentToFragmentInteract.class.getName()
                    )
            );
        }

        SecretEntry entry;
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            entry = (SecretEntry) args.getParcelable(ARG_ENTRY);
        } else {
            entry = new SecretEntry();
            for (PredefinedAttribute attr : PredefinedAttribute.values()) {
                String key = getString(attr.getKeyResID());
                String value = "";
                if (attr.isProtected()) {
                    try {
                        value = mKSManager.encrypt(value);
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        LogEx.e(e.getMessage(), e);
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
                entry.add(new SecretEntryAttribute(key, value, attr.isProtected()));
            }
        }

        mRVAdapter = new SecretEntryAdapter(entry, new OnSecretEntryAttributeActionListener() {
            @Override
            public void onSecretEntryAttributeDelete(int position) {
                EntryFragment.this.onSecretEntryAttributeDelete(position);
            }

            @Override
            public void onSecretEntryAttributeClick(View view, int position) {
                EntryFragment.this.onSecretEntryAttributeClick(view, position);
            }
        }, mKSManager);

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
        mIsShowPwd = ksPref.getBoolean(R.string.pref_protected_field_default_visibility_key,
                R.bool.pref_protected_field_default_visibility_value);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_entry, container, false);

        mRecyclerView = (RecyclerView) mView.findViewById(R.id.rvAttrList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        registerForContextMenu(mRecyclerView);
        mRecyclerView.setAdapter(mRVAdapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mRVAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

        if (savedInstanceState != null) {
            mIsShowPwd = savedInstanceState.getBoolean(String.valueOf(R.id.showPwd));
        }
        mRVAdapter.setProtectedVisible(mIsShowPwd);

        FloatingActionButton btnAdd = (FloatingActionButton) mView.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        FloatingActionButton btnApply = (FloatingActionButton) mView.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onApplyClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_secret_fields);

        return mView;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ENTRY, mRVAdapter.getSecretEntry());
        outState.putBoolean(String.valueOf(R.id.showPwd), mIsShowPwd);
    }

    private void onAddClicked() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(mRVAdapter.getSecretEntry().size(), null);
        fragment.setTargetFragment(EntryFragment.this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
    }

    private void onDeleteClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                R.string.label_delete_entry, R.string.message_are_you_sure, true, null);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, DELETE_ENTRY_CONFIRMATION_DIALOG);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }


    private void onApplyClicked() {
        mListener.onEntryUpdate(mRVAdapter.getSecretEntry());
        getFragmentManager().popBackStack();
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Bundle args) {
        switch (requestCode) {
            case DELETE_ENTRY_CONFIRMATION_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    mListener.onEntryDelete(mRVAdapter.getSecretEntry().getID());
                    getFragmentManager().popBackStack(EntryFragment.class.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                break;
            case DELETE_ATTRIBUTE_CONFIRMATION_DIALOG:
                int position = args.getInt(ARG_POSITION);
                if (resultCode == Activity.RESULT_OK) {
                    mRVAdapter.getSecretEntry().remove(position);
                    mRVAdapter.notifyItemRemoved(position);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    //restoring presence of deleted attribute
                    mRVAdapter.notifyItemChanged(position);
                }
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onAttributeUpdate(int pos, SecretEntryAttribute attr) {
        SecretEntry se = mRVAdapter.getSecretEntry();
        if (pos < se.size()) {
            se.set(pos, attr);
            mRVAdapter.notifyItemChanged(pos);
        } else {
            se.add(attr);
            mRVAdapter.notifyItemInserted(pos);
        }
    }

    private void onSecretEntryAttributeDelete(int position) {
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
        } else if (position == 0 && mRVAdapter.getSecretEntry().get(1).isProtected()) {
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
                    R.string.label_delete_field, R.string.message_are_you_sure, true, extras);
            fragment.setCancelable(true);
            fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE_CONFIRMATION_DIALOG);
            transaction.addToBackStack(null);
            fragment.show(transaction, DialogFragment.class.getName());
        }
    }

    private void onSecretEntryAttributeEdit(int position) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(position, mRVAdapter.getSecretEntry().get(position));
        fragment.setTargetFragment(EntryFragment.this, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(EntryFragment.class.getName());
        transaction.commit();
        mView.showContextMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_entry_options_menu, menu);
        if (mRVAdapter.getSecretEntry().getID() == 0) {
            MenuItem deleteMenuItem = menu.findItem(R.id.mnuDeleteEntry);
            deleteMenuItem.setVisible(false);
        }
        MenuItem showPwdMenuItem = menu.findItem(R.id.showPwd);
        showPwdMenuItem.setChecked(mIsShowPwd);
        showPwdMenuItem.setIcon(mIsShowPwd ? R.mipmap.img24dp_no_eye_w : R.mipmap.img24dp_eye_w);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enabling delete icon
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mnuDeleteEntry) {
            onDeleteClicked();
            return true;
        }
        if (id == R.id.showPwd) {
            item.setChecked(!item.isChecked());
            item.setIcon(item.isChecked() ? R.mipmap.img24dp_no_eye_w : R.mipmap.img24dp_eye_w);
            mIsShowPwd = item.isChecked();
            mRVAdapter.setProtectedVisible(mIsShowPwd);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onSecretEntryAttributeClick(View view, int position) {
        mRecyclerView.showContextMenuForChild(view);
        mClickedPosition = position;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.fragment_entry_card_popup, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        SecretEntryAttribute attr = mRVAdapter.getSecretEntry().get(mClickedPosition);
        switch (item.getItemId()) {
            case R.id.menuDelete:
                onSecretEntryAttributeDelete(mClickedPosition);
                return true;
            case R.id.menuEdit:
                onSecretEntryAttributeEdit(mClickedPosition);
                return true;
            case R.id.menuCopy:
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                String text = attr.getValue();
                if (attr.isProtected()) {
                    if (attr.getValue() != null && attr.getValue().length() > 0) {
                        try {
                            text = mKSManager.decrupt(attr.getValue());
                        } catch (BadPaddingException | IllegalBlockSizeException e) {
                            LogEx.e(e.getMessage(), e);
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                }
                ClipData clip = ClipData.newPlainText(attr.getKey(), text);
                clipboard.setPrimaryClip(clip);
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    interface OnFragmentToFragmentInteract {
        void onEntryUpdate(SecretEntry entry);

        void onEntryDelete(int id);
    }

    public interface ItemTouchHelperAdapter {

        boolean onItemMove(int fromPosition, int toPosition);

        void onItemDismiss(int position);
    }

    interface OnSecretEntryAttributeActionListener {
        void onSecretEntryAttributeDelete(int position);

        void onSecretEntryAttributeClick(View view, int position);
    }

    static class SecretEntryAdapter extends RecyclerView.Adapter<SecretEntryAdapter.ViewHolder> implements ItemTouchHelperAdapter {
        private SecretEntry mEntry;
        private OnSecretEntryAttributeActionListener mListener;
        private KeyStoreManager mKsMgr;
        private boolean mIsProtectedVisible = false;

        SecretEntryAdapter(SecretEntry entry, OnSecretEntryAttributeActionListener listener,
                           KeyStoreManager ksMgr) {
            mEntry = entry;
            mListener = listener;
            mKsMgr = ksMgr;
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
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
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
            mListener.onSecretEntryAttributeDelete(position);
        }

        @Override
        public SecretEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entry_include_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final int pos = position;
            SecretEntryAttribute attr = mEntry.get(position);
            holder.mTxtKey.setText(attr.getKey());
            if (!attr.isProtected()) {
                holder.mTxtValue.setText(attr.getValue());
                holder.mImgProtected.setVisibility(View.GONE);
            } else {
                holder.mTxtValue.setText("");
                if (attr.getValue() != null && attr.getValue().length() > 0) {
                    if (mIsProtectedVisible) {
                        try {
                            String plain = mKsMgr.decrupt(attr.getValue());
                            holder.mTxtValue.setText(plain);
                        } catch (BadPaddingException | IllegalBlockSizeException e) {
                            LogEx.e(e.getMessage(), e);
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    } else {
                        holder.mTxtValue.setText(R.string.label_hidden_password);
                    }
                }
            }

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSecretEntryAttributeClick(v, pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mEntry.size();
        }

        SecretEntry getSecretEntry() {
            return mEntry;
        }

        void setProtectedVisible(boolean isProtectedVisible) {
            mIsProtectedVisible = isProtectedVisible;
            for (int i = 0; i < mEntry.size(); i++) {
                if (mEntry.get(i).isProtected()) {
                    notifyItemChanged(i);
                }
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View mView;
            TextView mTxtKey;
            TextView mTxtValue;
            ImageView mImgProtected;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mTxtKey = (TextView) view.findViewById(R.id.txtKey);
                mTxtValue = (TextView) view.findViewById(R.id.txtValue);
                mImgProtected = (ImageView) view.findViewById(R.id.imgProtected);
            }
        }
    }

    static class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private ItemTouchHelperAdapter mAdapter;

        ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
        }
    }
}
