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
import android.view.View;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.dialog.AlertDialogFragment;
import com.wolandsoft.sss.service.CoreService;
import com.wolandsoft.sss.service.ScreenMonitorService;
import com.wolandsoft.sss.service.ServiceManager;
import com.wolandsoft.sss.util.KeySharedPreferences;

public class SettingsFragment extends PreferenceFragmentCompat implements PinFragment.OnFragmentToFragmentInteract {
    private static final String KEY_PIN = "pin";
    private String mPin = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);
        SwitchPreferenceCompat chk = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_pin_enabled_key));

        chk.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return onPinSwitch((Boolean) newValue);
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

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
        SwitchPreferenceCompat chk = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_pin_enabled_key));
        if (chk.isChecked() != ksPref.getBoolean(R.string.pref_pin_enabled_key, false)) {
            chk.setChecked(!chk.isChecked());
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
    public void onPinProvided(String pin, CoreService service) {
        if (mPin == null) {
            mPin = pin;
            showPinFragment(R.string.label_repeat_new_pin);
        } else {
            if (mPin.equals(pin)) {
                SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
                ksPref.edit()
                        .putBoolean(R.string.pref_pin_enabled_key, true)
                        .putString(R.string.pref_pin_key, service.getKeyStoreManager().encrypt(pin))
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
