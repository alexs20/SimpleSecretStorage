/*
    Copyright 2016, 2017 Alexander Shulgin

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
package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.common.TheApp;
import com.wolandsoft.sss.service.ScreenMonitorService;
import com.wolandsoft.sss.service.ServiceManager;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SettingsFragment extends PreferenceFragmentCompat implements PinFragment.OnFragmentToFragmentInteract {
    private static final String KEY_PIN = "pin";
    private String mPin = null;
    private SwitchPreferenceCompat mChkPinEnabled;
    private SwitchPreferenceCompat mChkReceiverEnabled;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        mChkPinEnabled = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_pin_enabled_key));
        mChkPinEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return onPinSwitch((Boolean) newValue);
            }
        });
        mChkReceiverEnabled = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_pc_receiver_enabled_key));
        mChkReceiverEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return onPCPairSwitch((Boolean) newValue);
            }
        });

        if (savedInstanceState != null) {
            mPin = savedInstanceState.getString(KEY_PIN);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.label_settings);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PIN, mPin);
    }

    private boolean onPinSwitch(boolean newState) {
        if (newState) {
            mPin = null;
            showPinFragment(R.string.label_enter_new_pin);
            return false;
        } else {
            ServiceManager.manageService(getContext(), ScreenMonitorService.class, false);
        }
        return true;
    }

    private boolean onPCPairSwitch(boolean newState) {
        if (newState) {
            IntentIntegrator integrator = new IntentIntegrator(getActivity());
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt(getString(R.string.message_scan_receiver_code));
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            Intent intent = integrator.createScanIntent();
            startActivityForResult(intent, IntentIntegrator.REQUEST_CODE);
            return false;
        } else {
            SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            shPref.edit().putString(getString(R.string.pref_pc_receiver_encoded_key), null).apply();
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                //canceled
            } else {
                try {
                    String encodedB64 = result.getContents();
                    byte[] aesKeyBuff = Base64.decode(encodedB64, Base64.DEFAULT);
                    SecretKey aesKey = new SecretKeySpec(aesKeyBuff, "AES");

                    SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                    KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
                    ksPref.edit()
                            .putString(R.string.pref_pc_receiver_encoded_key, TheApp.getKeyStoreManager().encrypt(encodedB64))
                            .putBoolean(R.string.pref_pc_receiver_enabled_key, true)
                            .apply();
                } catch (Exception e) {
                    LogEx.w(e.getMessage(), e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());

        if (mChkPinEnabled.isChecked() != ksPref.getBoolean(R.string.pref_pin_enabled_key, R.bool.pref_pin_enabled_value)) {
            mChkPinEnabled.setChecked(!mChkPinEnabled.isChecked());
        }

        if (mChkReceiverEnabled.isChecked() != ksPref.getBoolean(R.string.pref_pc_receiver_enabled_key, R.bool.pref_pc_receiver_enabled_value)) {
            mChkReceiverEnabled.setChecked(!mChkReceiverEnabled.isChecked());
        }
    }

    private void showPinFragment(final int msgResId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment fragment = PinFragment.newInstance(msgResId, 0);
        fragment.setTargetFragment(SettingsFragment.this, 0);
        transaction.replace(R.id.content_fragment, fragment, PinFragment.class.getName());
        transaction.addToBackStack(PinFragment.class.getName());
        transaction.commit();
    }

    @Override
    public void onPinProvided(String pin) {
        if (mPin == null) {
            mPin = pin;
            showPinFragment(R.string.label_repeat_new_pin);
        } else {
            if (mPin.equals(pin)) {
                SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
                ksPref.edit()
                        .putBoolean(R.string.pref_pin_enabled_key, true)
                        .putString(R.string.pref_pin_key, TheApp.getKeyStoreManager().encrypt(pin))
                        .apply();
                ServiceManager.manageService(getContext(), ScreenMonitorService.class, true);
                SwitchPreferenceCompat chk = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_pin_enabled_key));
                chk.setChecked(true);
            } else {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                DialogFragment fragment = AlertDialogFragment.newInstance(R.mipmap.img24dp_error,
                        R.string.label_error, R.string.message_repeated_pin_no_the_same, false, null);
                fragment.setCancelable(true);
                fragment.show(transaction, AlertDialogFragment.class.getName());
            }
        }
    }
}
