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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
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
            mAdapter = new CustomAdapter(context, mStorageProvider.getSQLiteStorage());
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

        ListView entriesList = (ListView) view.findViewById(R.id.entriesList);
        entriesList.setAdapter(mAdapter);
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

    public class CustomAdapter extends BaseAdapter {
        private List<SecretEntry> mLoadedEntries;
        private Context mContext;
        private SQLiteStorage mStorage;
        private Handler mLoader;
        private Handler mUpdater;

        public CustomAdapter(Context context, SQLiteStorage storage) {
            this.mContext = context;
            this.mStorage = storage;
            this.mLoadedEntries = Collections.synchronizedList(new ArrayList<SecretEntry>());

            HandlerThread thread = new HandlerThread(CustomAdapter.class.getName());
            thread.start();
            this.mLoader = new Handler(thread.getLooper());
            mUpdater = new Handler();

            mLoader.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        int offset = 0;
                        List<SecretEntry> entries = mStorage.find(null, true, offset, 10);
                        while (entries.size() > 0) {
                            mLoadedEntries.addAll(entries);
                            mUpdater.post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                            offset += 10;
                            entries = mStorage.find(null, true, offset, 10);
                        }
                    } catch (StorageException e) {
                        LogEx.e(e.getMessage(), e);
                    }
                }
            });
        }

        public int getCount() {
            return mLoadedEntries.size();
        }

        public Object getItem(int arg0) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.fragment_entries_item, parent, false);
            }
            TextView title = (TextView) convertView.findViewById(R.id.txtTitle);
            ImageView image = (ImageView) convertView.findViewById(R.id.imgIcon);
            SecretEntry entry = mLoadedEntries.get(position);
            title.setText(entry.get(0).getValue());

            image.setImageResource(R.mipmap.img_add);

            return convertView;
        }
    }
}
