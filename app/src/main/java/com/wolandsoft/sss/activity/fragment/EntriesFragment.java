package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.storage.IStorageProvider;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.LogEx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class EntriesFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private IStorageProvider mStorageProvider;
    private CustomAdapter mAdapter;

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
            mStorageProvider = (IStorageProvider) context;
            mAdapter = new CustomAdapter(context,  mStorageProvider.getSQLiteStorage());
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    String.format(getString(R.string.internal_exception_must_implement), context.toString(),
                            IStorageProvider.class.getName()));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entries, container, false);
        //connect to add button
        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        ListView entriesList = (ListView)view.findViewById(R.id.entriesList);
        entriesList.setAdapter(mAdapter);
        entriesList.setOnScrollListener(mAdapter);
        LogEx.i("onCreateView");
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
        mListener = null;
        LogEx.i("onDetach");
    }

    @Override
    public void onResume() {
        super.onResume();
        LogEx.i("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        LogEx.i("onPause");
    }

    public interface OnFragmentInteractionListener {

        /**
         * Triggered when user select an entry.
         */
        void onEntrySelected(UUID entryId);
    }

    public class CustomAdapter extends BaseAdapter implements AbsListView.OnScrollListener {
        private ArrayList<SecretEntry> mLoadedEntries;
        private int mCount = 0;
        private int mShift = 0;
        private Context mContext;
        private SQLiteStorage mStorage;
        private Handler mLoader;

        public CustomAdapter(Context context, SQLiteStorage storage) {
            this.mContext = context;
            this.mStorage = storage;
            this.mLoadedEntries = new ArrayList<>();

            HandlerThread thread = new HandlerThread(CustomAdapter.class.getName());
            thread.start();
            this.mLoader = new Handler(thread.getLooper());

            onScroll(null, 0, 50, 50);
        }

        public int getCount() {
            return mCount;
        }

        public Object getItem(int arg0) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View row = inflater.inflate(R.layout.fragment_entries_item, parent, false);
            TextView title = (TextView) row.findViewById(R.id.txtTitle);
            ImageView image = (ImageView) row.findViewById(R.id.imgIcon);
                SecretEntry entry = mLoadedEntries.get(position);
                title.setText(entry.get(0).getValue());

            image.setImageResource(R.mipmap.img_add);

            return (row);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean isLoadMore = firstVisibleItem + visibleItemCount >= mCount;

            if(isLoadMore) {
                try {
                    List<SecretEntry> entries = mStorage.find(null, true, mCount, visibleItemCount);
                    mLoadedEntries.addAll(entries);
                    mCount += visibleItemCount;
                    notifyDataSetChanged();
                } catch (StorageException e) {
                    LogEx.e(e.getMessage(), e);
                }
            }
        }
    }
}
