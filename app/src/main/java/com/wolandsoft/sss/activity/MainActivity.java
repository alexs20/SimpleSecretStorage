package com.wolandsoft.sss.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
        FragmentManager.OnBackStackChangedListener {
    private DrawerLayout mDrawer;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = new EntriesFragment();
            transaction.replace(R.id.content_fragment, fragment);
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
                if(getSupportFragmentManager().getBackStackEntryCount() == 0) {
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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.navExport:
                fragment = new ExportFragment();
                break;
            case R.id.navImport:
                fragment = new ImportFragment();
                break;
        }
        transaction.replace(R.id.content_fragment, fragment);
        transaction.addToBackStack(null);
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
}
