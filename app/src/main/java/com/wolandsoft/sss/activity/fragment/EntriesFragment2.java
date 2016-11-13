package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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

interface OnSecretEntryClickListener {
    void onSecretEntryClick(SecretEntry entry);
}

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class EntriesFragment2 extends Fragment implements OnSecretEntryClickListener {

    private OnFragmentInteractionListener mListener;
    private SQLiteStorage mStorage;
    private RecyclerView mRecyclerView;
    private SecretEntriesAdapter mRecyclerViewAdapter;

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
        try {
            mStorage = new SQLiteStorage(context);
            mRecyclerViewAdapter = new SecretEntriesAdapter(this, mStorage);
        } catch (StorageException e) {
            LogEx.e(e.getMessage(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entries2, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.rvEntriesList);
        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        //connect to add button
        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        return view;
    }

    /**
     * Add button event
     */
    private void onAddClicked() {

    }


    @Override
    public void onDetach() {
        super.onDetach();
        if (mStorage != null) {
            mStorage.close();
            mStorage = null;
        }
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

    @Override
    public void onSecretEntryClick(SecretEntry entry) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = EntryFragment2.newInstance(entry);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public interface OnFragmentInteractionListener {

        /**
         * Triggered when user select an entry.
         */
        void onEntrySelected(UUID entryId);
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
            holder.mImgIcon.setImageResource(R.mipmap.img_add);
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
    }
}
