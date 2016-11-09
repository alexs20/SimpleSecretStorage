package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
        entriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View selectedView,
                                    int position, long id) {
                SecretEntry entry = (SecretEntry) mAdapter.getItem(position);
                if(entry != null) {
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    Fragment fragment = EntryFragment.newInstance(entry);
                    transaction.replace(R.id.content_fragment, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
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
            this.mLoadedEntries = new ArrayList<SecretEntry>();
            new AsyncTask<Void, Void, List<SecretEntry>>() {
                @Override
                protected List<SecretEntry> doInBackground(Void... params) {
                    try {
                        return mStorage.find(null, true, 0, Integer.MAX_VALUE);
                    } catch (StorageException e) {
                        LogEx.e(e.getMessage(), e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(List<SecretEntry> secretEntries) {
                    mLoadedEntries = secretEntries;
                    notifyDataSetChanged();
                }
            }.execute();
        }

        public int getCount() {
            return mLoadedEntries.size();
        }

        public Object getItem(int index) {
            return mLoadedEntries.get(index);
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
