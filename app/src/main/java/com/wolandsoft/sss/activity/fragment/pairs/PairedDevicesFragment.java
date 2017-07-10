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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.security.TextCipher;
import com.wolandsoft.sss.service.pccomm.PairedDevice;
import com.wolandsoft.sss.service.pccomm.PairedDevices;
import com.wolandsoft.sss.service.pccomm.PcCommServiceProxy;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * List of paired devices.
 */
public class PairedDevicesFragment extends Fragment
    implements AlertDialogFragment.OnDialogToFragmentInteract{
    private final static String ARG_POSITION = "position";
    private static final int DELETE_PAIRED_DEVICE_CONFIRMATION_DIALOG = 1;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private TextCipher mCipher;
    private PcCommServiceProxy mPcComm;
    private KeySharedPreferences mKsPref;

    public static PairedDevicesFragment newInstance() {
        LogEx.d("newInstance()");
        return new PairedDevicesFragment();
    }

    @Override
    public void onAttach(Context context) {
        LogEx.d("onAttach()");
        super.onAttach(context);
        mCipher = new TextCipher();
        mPcComm = new PcCommServiceProxy(context);
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mKsPref = new KeySharedPreferences(shPref, getContext());

    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogEx.d("onCreate()");
        super.onCreate(savedInstanceState);

        String devicesJson = mKsPref.getString(R.string.pref_paired_devices_key, (Integer)null);
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
        View view = inflater.inflate(R.layout.fragment_settings_pairs, container, false);

        RecyclerView rView = (RecyclerView) view.findViewById(R.id.rvList);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(new LinearLayoutManager(getContext()));
        rView.setAdapter(mRecyclerViewAdapter);

        FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_paired_devices);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void onAddClicked() {
        IntentIntegrator integrator = new IntentIntegrator(getActivity());
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(getString(R.string.message_scan_receiver_code));
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(false);
        Intent intent = integrator.createScanIntent();
        startActivityForResult(intent, IntentIntegrator.REQUEST_CODE);
    }


    private void onDeviceDelete(int position) {
        Bundle extras = new Bundle();
        extras.putInt(ARG_POSITION, position);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_warning,
                R.string.label_delete_paired_device, R.string.message_delete_paired_device_confirmation, true, extras);
        fragment.setCancelable(true);
        fragment.setTargetFragment(this, DELETE_PAIRED_DEVICE_CONFIRMATION_DIALOG);
        transaction.addToBackStack(null);
        fragment.show(transaction, DialogFragment.class.getName());
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Bundle args) {
        switch (requestCode) {
            case DELETE_PAIRED_DEVICE_CONFIRMATION_DIALOG:
                int idx = args.getInt(ARG_POSITION);
                if (resultCode == Activity.RESULT_OK) {
                    PairedDevices devices = mRecyclerViewAdapter.getModel();
                    devices.remove(idx);
                    mRecyclerViewAdapter.notifyItemRemoved(idx);
                    String json = devices.toJson();
                    mKsPref.edit()
                            .putString(R.string.pref_paired_devices_key, json)
                            .apply();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    //restoring presence of deleted attribute
                    mRecyclerViewAdapter.notifyItemChanged(idx);
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                //canceled
            } else {
                Context ctx = getContext();
                if (ctx != null) {
                    try {
                        PairedDevice device = new PairedDevice();
                        String encodedB64 = result.getContents();
                        byte[] packet = Base64.decode(encodedB64, Base64.DEFAULT);
                        ByteArrayInputStream bais = new ByteArrayInputStream(packet);
                        DataInputStream dis = new DataInputStream(bais);
                        long crc = dis.readLong();
                        int size = dis.readInt();
                        byte[] payload = new byte[size];
                        dis.readFully(payload, 0, size);
                        dis.close();
                        Checksum checksum = new CRC32();
                        checksum.update(payload, 0, payload.length);
                        if (checksum.getValue() == crc) {
                            bais = new ByteArrayInputStream(payload);
                            dis = new DataInputStream(bais);
                            device.mPort = dis.readInt();
                            size = dis.readInt();
                            payload = new byte[size];
                            dis.readFully(payload, 0, size);
                            device.mHost = new String(payload, StandardCharsets.UTF_8);
                            size = dis.readInt();
                            payload = new byte[size];
                            dis.readFully(payload);
                            device.mKey = mCipher.cipher(payload);
                            dis.close();
                            PairedDevices devices = mRecyclerViewAdapter.getModel();
                            devices.add(device);
                            String json = devices.toJson();
                            mKsPref.edit()
                                    .putString(R.string.pref_paired_devices_key, json)
                                    .apply();
                            mRecyclerViewAdapter.notifyDataSetChanged();
                            mPcComm.ping(device);
                            return;
                        }
                    } catch (Exception e) {
                        LogEx.w(e.getMessage(), e);
                    }
                    Toast.makeText(getContext(), getString(R.string.message_invalid_qr_code), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
