/*
    Copyright 2017 Alexander Shulgin

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
package com.wolandsoft.sss.activity.fragment.pairs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;
import com.wolandsoft.sss.util.PairedDevices;

/**
 * List of paired devices.
 */
public class PairedDevicesFragment extends Fragment {
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private View mView;

    public static PairedDevicesFragment newInstance() {
        LogEx.d("newInstance()");
        PairedDevicesFragment fragment = new PairedDevicesFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        LogEx.d("onAttach()");
        super.onAttach(context);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogEx.d("onCreate()");
        super.onCreate(savedInstanceState);

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
        String devicesJson = ksPref.getString(R.string.pref_paired_devices_key, (Integer)null);
        PairedDevices devices = PairedDevices.fromJson(devicesJson);

        mRecyclerViewAdapter = new RecyclerViewAdapter(devices) {
            @Override
            void onItemDelete(int index) {
                onDeviceDelete(index);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_settings_pairs, container, false);

        RecyclerView rView = (RecyclerView) mView.findViewById(R.id.rvList);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(new LinearLayoutManager(getContext()));
        rView.setAdapter(mRecyclerViewAdapter);
        mRecyclerViewAdapter.updateModel();

        FloatingActionButton btnAdd = (FloatingActionButton) mView.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_paired_devices);

        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void onAddClicked() {

    }


    private void onDeviceDelete(int position) {

    }

}
