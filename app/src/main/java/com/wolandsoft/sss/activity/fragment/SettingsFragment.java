/*
    Copyright 2016 Alexander Shulgin

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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class SettingsFragment extends PreferenceFragmentCompat implements PinFragment.OnFragmentToFragmentInteract {
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


        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.label_settings);
    }

    private boolean onPinSwitch(boolean newState) {
        if (newState) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            Fragment fragment = PinFragment.newInstance(true);
            fragment.setTargetFragment(this, 0);
            transaction.replace(R.id.content_fragment, fragment);
            transaction.addToBackStack(PinFragment.class.getName());
            transaction.commit();
            return false;
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

    @Override
    public void onPinProvided(String pin) {
        if (pin != null && pin.length() == 4) {
            SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            KeySharedPreferences ksPref = new KeySharedPreferences(shPref, getContext());
            try {
                ksPref.edit()
                        .putBoolean(R.string.pref_pin_enabled_key, true)
                        .putString(R.string.pref_pin_key, AppCentral.getInstance().getKeyStoreManager().encrypt(pin))
                        .apply();
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                LogEx.e(e.getMessage(), e);
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
