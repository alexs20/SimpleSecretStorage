package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.storage.IStorageProvider;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.LogEx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class EntryFragment extends Fragment {
    private final static String ARG_ENTRY = "entry";
    private OnFragmentInteractionListener mListener;

    public static EntryFragment newInstance(SecretEntry entry) {
        EntryFragment fragment = new EntryFragment();
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
        View view = inflater.inflate(R.layout.fragment_entry, container, false);
        //connect to add button
        FloatingActionButton btnDelete = (FloatingActionButton) view.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeleteClicked();
            }
        });

        Bundle args = getArguments();
        SecretEntry entry = (SecretEntry) args.getSerializable(ARG_ENTRY);
        TableLayout tabLayout = (TableLayout) view.findViewById(R.id.tableLayout);
//        for(SecretEntryAttribute attr : entry) {
//            TableRow row = new TableRow(getContext());
//            TextView lbl = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.fragment_entry_include_textview, null);
//            row.addView(lbl);
//            lbl.setText(attr.getKey());
//            EditText edt = (EditText) LayoutInflater.from(getContext()).inflate(R.layout.fragment_entry_include_edittext, null);
//            row.addView(edt);
//            edt.setText(attr.getValue());
//            CheckBox chk = (CheckBox) LayoutInflater.from(getContext()).inflate(R.layout.fragment_entry_include_checkbox, null);
//            row.addView(chk);
//            chk.setChecked(attr.isProtected());
//            tabLayout.addView(row);
//        }
        return view;
    }


    private void onDeleteClicked() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment = new EntryFragment();
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

    public interface OnFragmentInteractionListener {

        /**
         * Triggered when user select an entry.
         */
        void onEntrySelected(UUID entryId);
    }
}
