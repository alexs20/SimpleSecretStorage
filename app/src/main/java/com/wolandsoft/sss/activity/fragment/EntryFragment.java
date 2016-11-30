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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.PredefinedAttribute;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.LogEx;

import java.io.Serializable;
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
    private SecretEntryAdapter mRVAdapter;

    public static EntryFragment newInstance(SecretEntry entry) {
        EntryFragment fragment = new EntryFragment();
        if (entry != null) {
            Bundle args = new Bundle();
            args.putSerializable(ARG_ENTRY, (Serializable) entry.clone());
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
            entry = (SecretEntry) args.getSerializable(ARG_ENTRY);
        } else {
            entry = new SecretEntry();
            for (PredefinedAttribute attr : PredefinedAttribute.values()) {
                entry.add(new SecretEntryAttribute(getString(attr.getKeyResID()), "", attr.isProtected()));
            }
        }

        mRVAdapter = new SecretEntryAdapter(entry, new OnSecretEntryAttributeActionListener() {
            @Override
            public void onSecretEntryAttributeDelete(int position) {
                //swipe left/right to delete + delete from popup
                if (mRVAdapter.getItemCount() == 1) {
                    //can not delete last element
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                            R.string.label_delete_field, R.string.message_can_not_delete_last_attribute, false, null);
                    fragment.setCancelable(true);
                    fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE_CONFIRMATION_DIALOG);
                    transaction.addToBackStack(null);
                    fragment.show(transaction, DialogFragment.class.getName());
                } else if (position == 0 && mRVAdapter.getSecretEntry().get(1).isProtected()) {
                    //can not perform delete operation when result leads to make a protected field be moved to the top
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                            R.string.label_delete_field, R.string.message_can_not_delete_before_protected_attribute, false, null);
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

            @Override
            public void onSecretEntryAttributeEdit(int position) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = AttributeFragment.newInstance(position, mRVAdapter.getSecretEntry().get(position));
                fragment.setTargetFragment(EntryFragment.this, 0);
                transaction.replace(R.id.content_fragment, fragment);
                transaction.addToBackStack(EntryFragment.class.getName());
                transaction.commit();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entry, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rvAttrList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerView.setAdapter(mRVAdapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mRVAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        FloatingActionButton btnDelete = (FloatingActionButton) view.findViewById(R.id.btnDelete);
        if (mRVAdapter.getSecretEntry().getCreated() == 0) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeleteClicked();
                }
            });
        }

        FloatingActionButton btnApply = (FloatingActionButton) view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onApplyClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_secret_fields);

        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ENTRY, mRVAdapter.getSecretEntry());
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
                R.string.label_delete_all, R.string.message_are_you_sure, true, null);
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
                if (resultCode == Activity.RESULT_OK) {
                    int position = args.getInt(ARG_POSITION);
                    mRVAdapter.getSecretEntry().remove(position);
                    mRVAdapter.notifyItemRemoved(position);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    //restoring presence of deleted attribute
                    mRVAdapter.notifyDataSetChanged();
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

        void onSecretEntryAttributeEdit(int position);
    }

    static class SecretEntryAdapter extends RecyclerView.Adapter<SecretEntryAdapter.ViewHolder> implements ItemTouchHelperAdapter {
        private SecretEntry mEntry;
        private OnSecretEntryAttributeActionListener mListener;

        SecretEntryAdapter(SecretEntry entry, OnSecretEntryAttributeActionListener listener) {
            mEntry = entry;
            mListener = listener;
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
            final SecretEntryAttribute attr = mEntry.get(position);
            holder.mTxtKey.setText(attr.getKey());
            if (!attr.isProtected()) {
                holder.mTxtValue.setText(attr.getValue());
                holder.mImgProtected.setVisibility(View.GONE);
            } else {
                holder.mTxtValue.setText("");
                if (attr.getValue() != null && attr.getValue().length() > 0) {
                    try {
                        String plain = AppCentral.getInstance().getKeyStoreManager().decrupt(attr.getValue());
                        holder.mTxtValue.setText(plain);
                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                        LogEx.e(e.getMessage(), e);
                        Toast.makeText(holder.mView.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
            holder.mImgMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPopup(v, pos, attr);
                }
            });
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSecretEntryAttributeEdit(pos);
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

        private void openPopup(final View v, final int position, final SecretEntryAttribute attr) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.fragment_entry_card_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.menuDelete:
                            mListener.onSecretEntryAttributeDelete(position);
                            return true;
                        case R.id.menuEdit:
                            mListener.onSecretEntryAttributeEdit(position);
                            return true;
                        case R.id.menuCopy:
                            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            String text = attr.getValue();
                            if (attr.isProtected()) {
                                if (attr.getValue() != null && attr.getValue().length() > 0) {
                                    try {
                                        text = AppCentral.getInstance().getKeyStoreManager().decrupt(attr.getValue());
                                    } catch (BadPaddingException | IllegalBlockSizeException e) {
                                        LogEx.e(e.getMessage(), e);
                                    }
                                }
                            }
                            ClipData clip = ClipData.newPlainText(attr.getKey(), text);
                            clipboard.setPrimaryClip(clip);
                            break;
                    }
                    return false;
                }
            });

            popup.show();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View mView;
            TextView mTxtKey;
            TextView mTxtValue;
            ImageView mImgProtected;
            private ImageView mImgMenu;

            ViewHolder(View view) {
                super(view);
                mView = view;
                mTxtKey = (TextView) view.findViewById(R.id.txtKey);
                mTxtValue = (TextView) view.findViewById(R.id.txtValue);
                mImgProtected = (ImageView) view.findViewById(R.id.imgProtected);
                mImgMenu = (ImageView) view.findViewById(R.id.imgMenu);
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
