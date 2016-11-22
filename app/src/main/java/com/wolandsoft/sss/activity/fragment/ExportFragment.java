package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.util.KeySharedPreferences;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class ExportFragment extends Fragment {
    private KeySharedPreferences mPref;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPref = new KeySharedPreferences(shPref, getContext());
        View view = inflater.inflate(R.layout.fragment_export, container, false);


        if (savedInstanceState == null) {

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
            actionBar.setTitle(R.string.label_export);
        return view;
    }

    private void onApplyClicked() {

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    interface OnFragmentToFragmentInteract {

    }
}
