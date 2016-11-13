package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.LogEx;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class EntryFragment2 extends Fragment  {
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
    private final static String ARG_ENTRY = "entry";
    private OnFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private SecretEntryAdapter mRecyclerViewAdapter;

    public static EntryFragment2 newInstance(SecretEntry entry) {
        EntryFragment2 fragment = new EntryFragment2();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ENTRY, entry);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        try {
//            mListener = (OnFragmentInteractionListener) context;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(
//                    String.format(getString(R.string.internal_exception_must_implement), context.toString(),
//                            OnFragmentInteractionListener.class.getName()));
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entry2, container, false);

        Bundle args = getArguments();
        SecretEntry entry = (SecretEntry) args.getSerializable(ARG_ENTRY);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.rvAttrList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mRecyclerViewAdapter = new SecretEntryAdapter(entry);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        //registerForContextMenu(mRecyclerView);

        ItemTouchHelper.Callback callback = new  ItemTouchHelperCallback(mRecyclerViewAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

        //connect to add button
//        FloatingActionButton btnDelete = (FloatingActionButton) view.findViewById(R.id.btnDelete);
//        btnDelete.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onDeleteClicked();
//            }
//        });
//
//        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
//        btnDelete.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onAddClicked();
//            }
//        });

        return view;
    }
    

    private void onAddClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;//AttributeFragment.newInstance(null);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void onDeleteClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = new EntryFragment2();
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void openPopup(View view){
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

        /**
         * Triggered when user select an entry.
         */
        void onEntrySelected(UUID entryId);
    }

    static class SecretEntryAdapter extends RecyclerView.Adapter<SecretEntryAdapter.ViewHolder>
    implements ItemTouchHelperAdapter {
        public int longPressItem;
        private SecretEntry mEntry;

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

        // Provide a suitable constructor (depends on the kind of dataset)
        public SecretEntryAdapter(SecretEntry entry) {
            mEntry = entry;
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
            if(!attr.isProtected()){
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

        private void openPopup(View v, int position){
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
    }

    static class ItemTouchHelperCallback extends ItemTouchHelper.Callback{

        private ItemTouchHelperAdapter mAdapter;
        public ItemTouchHelperCallback (ItemTouchHelperAdapter adapter){
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
    public interface ItemTouchHelperAdapter {

        boolean onItemMove(int fromPosition, int toPosition);

        void onItemDismiss(int position);
    }
}
