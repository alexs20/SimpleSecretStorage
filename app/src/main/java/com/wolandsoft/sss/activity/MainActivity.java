package com.wolandsoft.sss.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.AttributeFragment;
import com.wolandsoft.sss.activity.fragment.EntriesFragment2;
import com.wolandsoft.sss.entity.SecretEntryAttribute;

import java.util.UUID;

/**
 * Main UI class of the app.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class MainActivity extends AppCompatActivity implements EntriesFragment2.OnFragmentInteractionListener
        , AttributeFragment.OnFragmentInteractionListener, FragmentManager.OnBackStackChangedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = new EntriesFragment2();
            transaction.replace(R.id.content_fragment, fragment);
            transaction.commit();
        }

        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();
        //KeySharedPreferences pref = new KeySharedPreferences(getSharedPreferences(AppConstants.APP_TAG, Context.MODE_PRIVATE), this);
        //String storageId = pref.getString(R.string.key_storage_id, R.string.value_storage_id_default);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canback = getSupportFragmentManager().getBackStackEntryCount()>0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(canback);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }
    @Override
    public void onEntrySelected(UUID entryId) {

    }

    @Override
    public void onSecretEntryAttributeApply(int sePos, int attrPos, SecretEntryAttribute attr) {

    }


}
