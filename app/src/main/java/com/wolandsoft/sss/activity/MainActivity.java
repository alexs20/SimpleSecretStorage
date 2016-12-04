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
package com.wolandsoft.sss.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.EntriesFragment;
import com.wolandsoft.sss.activity.fragment.ExportFragment;
import com.wolandsoft.sss.activity.fragment.ImportFragment;
import com.wolandsoft.sss.activity.fragment.PinFragment;
import com.wolandsoft.sss.activity.fragment.SettingsFragment;
import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * Main UI class of the app.
 *
 * @author Alexander Shulgin
 */
public class MainActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        PinFragment.OnFragmentToFragmentInteract {
    private static final int REQUEST_IMPORT = 1;
    private static final int REQUEST_EXPORT = 2;
    private DrawerLayout mDrawerLayout;
    private NavigationView mDrawerView;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCentral.init(getApplicationContext());
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = new EntriesFragment();
            transaction.replace(R.id.content_fragment, fragment, EntriesFragment.class.getName());
            transaction.commit();
        }

        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        //drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerView = (NavigationView) findViewById(R.id.nvView);
        //drawer items selection listener
        mDrawerView.setNavigationItemSelectedListener(
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(this);
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, this);
        mIsLocked = ksPref.getBoolean(R.string.pref_pin_enabled_key, R.bool.pref_pin_enabled_value);
        //pin protection enabled
        if (mIsLocked) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = PinFragment.newInstance(false);
            transaction.replace(R.id.content_fragment, fragment, PinFragment.class.getName());
            transaction.addToBackStack(PinFragment.class.getName());
            transaction.commit();
            controlDrawerAvailability();
            return;
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

    public void selectDrawerItem(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        switch (menuItem.getItemId()) {
            case R.id.navExport:
            case R.id.navImport:
                String[] requiredPermissions = new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
                boolean askForPermissions = false;
                for (String permission : requiredPermissions) {
                    int permissionGranted = ContextCompat.checkSelfPermission(this, permission);
                    if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
                        askForPermissions = true;
                        break;
                    }
                }
                if (askForPermissions) {
                    ActivityCompat.requestPermissions(this, requiredPermissions,
                            menuItem.getItemId() == R.id.navExport ? REQUEST_EXPORT : REQUEST_IMPORT);
                } else {
                    switch (menuItem.getItemId()) {
                        case R.id.navExport:
                            openExportFragment();
                            break;
                        case R.id.navImport:
                            openImportFragment();
                            break;
                    }
                }
                break;
            case R.id.navSettings:
                Fragment target = getSupportFragmentManager().findFragmentByTag(EntriesFragment.class.getName());
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment fragment = new SettingsFragment();
                fragment.setTargetFragment(target, 0);
                transaction.replace(R.id.content_fragment, fragment);
                transaction.addToBackStack(SettingsFragment.class.getName());
                transaction.commit();
                break;
        }

    }

    private void openImportFragment() {
        Fragment target = getSupportFragmentManager().findFragmentByTag(EntriesFragment.class.getName());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new ImportFragment();
        fragment.setTargetFragment(target, 0);
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(ImportFragment.class.getName());
        transaction.commit();
    }

    private void openExportFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new ExportFragment();
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(ExportFragment.class.getName());
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackStackChanged() {
        controlDrawerAvailability();
    }

    public void controlDrawerAvailability() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_EXPORT:
            case REQUEST_IMPORT:
                break;
        }
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onPinProvided(String pin) {
        try {
            SharedPreferences shPref = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this);
            KeySharedPreferences ksPref = new KeySharedPreferences(shPref, this);
            String storedPin = ksPref.getString(R.string.pref_pin_key, R.string.label_ellipsis);
            storedPin = AppCentral.getInstance().getKeyStoreManager().decrupt(storedPin);
            mIsLocked = !pin.equals(storedPin);
            if (!mIsLocked) {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.show();
                }
                controlDrawerAvailability();
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            LogEx.e(e.getMessage(), e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if(!mIsLocked){
            super.onBackPressed();
        }
    }
}
