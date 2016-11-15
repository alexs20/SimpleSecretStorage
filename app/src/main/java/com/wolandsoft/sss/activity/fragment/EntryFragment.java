package com.wolandsoft.sss.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.entity.PredefinedAttribute;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class EntryFragment extends Fragment {
    private static final int DELETE_ENTRY = 1;
    private static final int DELETE_ATTRIBUTE = 2;
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
    private final static String ARG_ENTRY = "entry";
    private final static String ARG_POSITION = "position";
    private OnFragmentInteractionListener mListener;
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
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    String.format(getString(R.string.internal_exception_must_implement), context.toString(),
                            OnFragmentInteractionListener.class.getName()));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entry, container, false);

        SecretEntry entry;
        if(savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                entry = (SecretEntry) args.getSerializable(ARG_ENTRY);
            } else {
                entry = new SecretEntry();
                for (PredefinedAttribute attr : PredefinedAttribute.values()) {
                    entry.add(new SecretEntryAttribute(getString(attr.getKeyResID()), "", attr.isProtected()));
                }
            }
        } else {
            entry = (SecretEntry) savedInstanceState.getSerializable(ARG_ENTRY);
        }

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rvAttrList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mRVAdapter = new SecretEntryAdapter(entry, new OnSecretEntryAttributeActionListener() {
            @Override
            public void onSecretEntryAttributeDelete(int position) {
                if (mRVAdapter.getItemCount() == 1) {
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                            R.string.label_delete_field, R.string.message_can_not_delete_last_attribute, false, null);
                    fragment.setCancelable(true);
                    fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE);
                    transaction.addToBackStack(null);
                    fragment.show(transaction, "DIALOG");
                } else {
                    Bundle extras = new Bundle();
                    extras.putInt(ARG_POSITION, position);

                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                            R.string.label_delete_field, R.string.message_are_you_sure, true, extras);
                    fragment.setCancelable(true);
                    fragment.setTargetFragment(EntryFragment.this, DELETE_ATTRIBUTE);
                    transaction.addToBackStack(null);
                    fragment.show(transaction, "DIALOG");
                }
            }

            @Override
            public void onSecretEntryAttributeReorder(int fromPosition, int toPosition) {
                mListener.onEntryUpdated(mRVAdapter.getSecretEntry());
            }
        });
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
        if (entry.getCreated() == 0) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeleteClicked();
                }
            });
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_secret_fields);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ENTRY, mRVAdapter.getSecretEntry());
    }

    private void onAddClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(mRVAdapter.getSecretEntry().getID(), -1, null);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void onDeleteClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                R.string.label_delete_all, R.string.message_are_you_sure, true, null);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, DELETE_ENTRY);
        transaction.addToBackStack(null);
        fragment.show(transaction, "DIALOG");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DELETE_ENTRY:
                if (resultCode == Activity.RESULT_OK) {
                    mListener.onEntryDeleted(mRVAdapter.getSecretEntry().getID());
                }
                break;
            case DELETE_ATTRIBUTE:
                if (resultCode == Activity.RESULT_OK) {
                    int position = data.getExtras().getInt(ARG_POSITION);
                    mRVAdapter.getSecretEntry().remove(position);
                    mRVAdapter.notifyItemRemoved(position);
                    mListener.onEntryUpdated(mRVAdapter.getSecretEntry());
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    mRVAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {

        void onEntryDeleted(UUID seeID);

        void onEntryUpdated(SecretEntry entry);
    }

    public interface ItemTouchHelperAdapter {

        boolean onItemMove(int fromPosition, int toPosition);

        void onItemDismiss(int position);
    }

    interface OnSecretEntryAttributeActionListener {
        void onSecretEntryAttributeDelete(int position);

        void onSecretEntryAttributeReorder(int fromPosition, int toPosition);
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
            mListener.onSecretEntryAttributeReorder(fromPosition, toPosition);
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
            holder.mTxtValue.setText(attr.getValue());
            if (!attr.isProtected()) {
                holder.mImgProtected.setVisibility(View.GONE);
            }
            holder.mImgMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPopup(v, pos);
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

        private void openPopup(View v, final int position) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.fragment_entry_include_card_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.menu_remove:
                            mListener.onSecretEntryAttributeDelete(position);
                            return true;
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
