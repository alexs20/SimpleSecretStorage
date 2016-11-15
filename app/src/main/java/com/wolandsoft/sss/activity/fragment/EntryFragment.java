package com.wolandsoft.sss.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
    private static final int DIALOG_FRAGMENT = 1;
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
    private final static String ARG_ENTRY = "entry";
    private OnFragmentInteractionListener mListener;
    private SecretEntryAdapter mRecyclerViewAdapter;
    private SecretEntry mSecretEntry;

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

        Bundle args = getArguments();
        if (args != null) {
            mSecretEntry = (SecretEntry) args.getSerializable(ARG_ENTRY);
        } else {
            mSecretEntry = new SecretEntry();
            for (PredefinedAttribute attr : PredefinedAttribute.values()) {
                mSecretEntry.add(new SecretEntryAttribute(getString(attr.getKeyResID()), "", attr.isProtected()));
            }
        }

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rvAttrList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mRecyclerViewAdapter = new SecretEntryAdapter(mSecretEntry);
        recyclerView.setAdapter(mRecyclerViewAdapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mRecyclerViewAdapter);
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
        if (mSecretEntry.getCreated() == 0) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeleteClicked();
                }
            });
        }
        return view;
    }

    private void onAddClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = AttributeFragment.newInstance(mSecretEntry.getID(), -1, null);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void onDeleteClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                R.string.label_delete_all, R.string.message_are_you_sure, true);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, DIALOG_FRAGMENT);
        transaction.addToBackStack(null);
        fragment.show(transaction, "DIALOG");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DIALOG_FRAGMENT:
                if (resultCode == Activity.RESULT_OK) {
                    mListener.onEntryDeleted(mSecretEntry.getID());
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // nothing
                }
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void openPopup(View view) {
        // Create a PopupMenu, giving it the clicked view for an anchor
        PopupMenu popup = new PopupMenu(getActivity(), view);

        // Inflate our menu resource into the PopupMenu's Menu
        popup.getMenuInflater().inflate(R.menu.fragment_entry_include_card_popup, popup.getMenu());

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_remove:
                        // Remove the item from the adapter
                        //adapter.remove(item);
                        return true;
                }
                return false;
            }
        });

        // Finally show the PopupMenu
        popup.show();
    }


    public interface OnFragmentInteractionListener {

        void onEntryDeleted(UUID seeID);

        void onEntryUpdated(SecretEntry entry);
    }

    public interface ItemTouchHelperAdapter {

        boolean onItemMove(int fromPosition, int toPosition);

        void onItemDismiss(int position);
    }

    static class SecretEntryAdapter extends RecyclerView.Adapter<SecretEntryAdapter.ViewHolder>
            implements ItemTouchHelperAdapter {
        public int longPressItem;
        private SecretEntry mEntry;

        // Provide a suitable constructor (depends on the kind of dataset)
        public SecretEntryAdapter(SecretEntry entry) {
            mEntry = entry;
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
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
            mEntry.remove(position);
            notifyItemRemoved(position);
            //notifyDataSetChanged();
        }

        // Create new views (invoked by the layout manager)
        @Override
        public SecretEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entry_include_card, parent, false);
            ViewHolder holder = new ViewHolder(card);
            return holder;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            SecretEntryAttribute attr = mEntry.get(position);
            holder.mTxtKey.setText(attr.getKey());
            holder.mTxtValue.setText(attr.getValue());
            if (!attr.isProtected()) {
                holder.mImgProtected.setVisibility(View.GONE);
            }
            holder.mImgMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPopup(v, position);
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mEntry.size();
        }

        private void openPopup(View v, int position) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);

            // Inflate our menu resource into the PopupMenu's Menu
            popup.getMenuInflater().inflate(R.menu.fragment_entry_include_card_popup, popup.getMenu());

            // Set a listener so we are notified if a menu item is clicked
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.menu_remove:
                            // Remove the item from the adapter
                            //adapter.remove(item);
                            return true;
                    }
                    return false;
                }
            });

            // Finally show the PopupMenu
            popup.show();
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public View mView;
            public TextView mTxtKey;
            public TextView mTxtValue;
            public ImageView mImgProtected;
            public ImageView mImgMenu;

            public ViewHolder(View view) {
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

        public ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
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
