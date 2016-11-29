package com.wolandsoft.sss.activity;

import android.Manifest;
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
import android.view.MenuItem;
import android.view.View;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.EntriesFragment;
import com.wolandsoft.sss.activity.fragment.ExportFragment;
import com.wolandsoft.sss.activity.fragment.ImportFragment;

/**
 * Main UI class of the app.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class MainActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int REQUEST_IMPORT = 1;
    private static final int REQUEST_EXPORT = 2;
    private DrawerLayout mDrawer;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mPendingRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = new EntriesFragment();
            transaction.replace(R.id.content_fragment, fragment, EntriesFragment.class.getName());
            transaction.commit();
        }

        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();
        //drawer
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        // Setup drawer view
        nvDrawer.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return false;
                    }
                });

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawer,         /* DrawerLayout object */
                R.string.label_open_drawer,  /* "open drawer" description */
                R.string.label_close_drawer  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null)
                        actionBar.setTitle(R.string.app_name);
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null)
                    actionBar.setTitle(R.string.label_options);
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawer.addDrawerListener(mDrawerToggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
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
        if (mPendingRequestCode > 0) {
            switch (mPendingRequestCode) {
                case REQUEST_EXPORT:
                    openExportFragment();
                    break;
                case REQUEST_IMPORT:
                    openImportFragment();
                    break;
            }
            mPendingRequestCode = 0;
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
        mDrawer.closeDrawers();

        switch (menuItem.getItemId()) {
            case R.id.navExport:
            case R.id.navImport:
                String[] requiredPermissions = new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
                boolean needUpdates = false;
                for (String permission : requiredPermissions) {
                    int permissionGranted = ContextCompat.checkSelfPermission(this, permission);
                    if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
                        needUpdates = true;
                        break;
                    }
                }
                if (needUpdates) {
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
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp() {
        //Enable Up button only  if there are entries in the back stack
        boolean callback = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (mDrawerToggle != null)
            mDrawerToggle.setDrawerIndicatorEnabled(!callback);
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
                boolean canContinue = true;
                for (int permission : grantResults) {
                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        canContinue = false;
                        break;
                    }
                }
                if (canContinue) {
                    mPendingRequestCode = requestCode;
                }
                break;
        }
    }
}
