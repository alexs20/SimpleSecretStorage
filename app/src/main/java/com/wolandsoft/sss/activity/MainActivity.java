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
package com.wolandsoft.sss.activity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.EntriesFragment;
import com.wolandsoft.sss.activity.fragment.ExportFragment;
import com.wolandsoft.sss.activity.fragment.ImportFragment;
import com.wolandsoft.sss.activity.fragment.PinFragment;
import com.wolandsoft.sss.activity.fragment.SettingsFragment;
import com.wolandsoft.sss.common.TheApp;
import com.wolandsoft.sss.service.ScreenMonitorService;
import com.wolandsoft.sss.service.ServiceManager;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main UI class of the app.
 *
 * @author Alexander Shulgin
 */
public class MainActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        PinFragment.OnFragmentToFragmentInteract {
    //some shared objects
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = new EntriesFragment();
            transaction.replace(R.id.content_fragment, fragment, EntriesFragment.class.getName());
            transaction.commit();
            ServiceManager.manageService(this, ScreenMonitorService.class, false);
        }

        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        //drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView drawerView = (NavigationView) findViewById(R.id.nvView);
        //drawer items selection listener
        drawerView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return false;
                    }
                });
        //drawer button customization
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.label_open_drawer, R.string.label_close_drawer) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null && actionBar.isShowing())
                        actionBar.setTitle(R.string.app_name);
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null && actionBar.isShowing())
                    actionBar.setTitle(R.string.label_options);
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        controlDrawerAvailability();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(this);
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, this);
        mIsLocked = ksPref.getBoolean(R.string.pref_pin_enabled_key, R.bool.pref_pin_enabled_value) && !ServiceManager.isServiceRunning(this, ScreenMonitorService.class);
        //pin protection enabled
        if (mIsLocked) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            if (getSupportFragmentManager().findFragmentByTag(PinFragment.class.getName()) == null) {
                int delaySec = ksPref.getInt(getString(R.string.pref_pin_delay_key), 0);
                openPinValidationFragment(delaySec);
            }
            controlDrawerAvailability();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...
        return super.onOptionsItemSelected(item);
    }

    private void selectDrawerItem(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        switch (menuItem.getItemId()) {
            case R.id.navExport: {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment fragment = new ExportFragment();
                transaction.replace(R.id.content_fragment, fragment, ExportFragment.class.getName());
                transaction.addToBackStack(ExportFragment.class.getName());
                transaction.commit();
                break;
            }
            case R.id.navImport: {
                Fragment target = getSupportFragmentManager().findFragmentByTag(EntriesFragment.class.getName());
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment fragment = new ImportFragment();
                fragment.setTargetFragment(target, 0);
                transaction.replace(R.id.content_fragment, fragment, ImportFragment.class.getName());
                transaction.addToBackStack(ImportFragment.class.getName());
                transaction.commit();
                break;
            }
            case R.id.navSettings: {
                Fragment target = getSupportFragmentManager().findFragmentByTag(EntriesFragment.class.getName());
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment fragment = new SettingsFragment();
                fragment.setTargetFragment(target, 0);
                transaction.replace(R.id.content_fragment, fragment, SettingsFragment.class.getName());
                transaction.addToBackStack(SettingsFragment.class.getName());
                transaction.commit();
                break;
            }
        }

    }

    private void openPinValidationFragment(int delaySec) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = PinFragment.newInstance(R.string.label_enter_pin, TimeUnit.SECONDS.toMillis(delaySec));
        transaction.replace(R.id.content_fragment, fragment, PinFragment.class.getName());
        transaction.addToBackStack(PinFragment.class.getName());
        transaction.commit();
    }

    @Override
    public void onBackStackChanged() {
        if (LogEx.IS_DEBUG) {
            LogEx.d("onBackStackChanged()");
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (int i = 0; i < fragments.size(); i++) {
                Fragment fragment = fragments.get(i);
                if (fragment != null) {
                    LogEx.d("Fragment ", i, ": ", fragment.getClass().getName(), "; Tag: ", fragment.getTag());
                }
            }
        }
        controlDrawerAvailability();
    }

    private void controlDrawerAvailability() {
        boolean isEmptyStack = getSupportFragmentManager().getBackStackEntryCount() == 0;
        mDrawerToggle.setDrawerIndicatorEnabled(isEmptyStack && !mIsLocked);
        mDrawerLayout.setDrawerLockMode(isEmptyStack && !mIsLocked ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onPinProvided(String pin) {
        SharedPreferences shPref = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this);
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, this);
        String storedPin = ksPref.getString(R.string.pref_pin_key, R.string.label_ellipsis);
        storedPin = TheApp.getCipher().decipherText(storedPin);
        mIsLocked = !pin.equals(storedPin);
        if (!mIsLocked) {
            //resetting pin response delay to zero.
            ksPref.edit().putInt(R.string.pref_pin_delay_key, 0).apply();
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            controlDrawerAvailability();
            ServiceManager.manageService(this, ScreenMonitorService.class, true);
            return;
        }
        //incrementing pin response delay on each invalid pin provided.
        int delaySec = ksPref.getInt(getString(R.string.pref_pin_delay_key), 0);
        delaySec++;
        ksPref.edit().putInt(R.string.pref_pin_delay_key, delaySec).apply();
        openPinValidationFragment(delaySec);
    }

    @Override
    public void onBackPressed() {
        if (!mIsLocked) {
            super.onBackPressed();
        }
    }
}
