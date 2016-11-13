package com.wolandsoft.sss.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
        , AttributeFragment.OnFragmentInteractionListener {

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
        //KeySharedPreferences pref = new KeySharedPreferences(getSharedPreferences(AppConstants.APP_TAG, Context.MODE_PRIVATE), this);
        //String storageId = pref.getString(R.string.key_storage_id, R.string.value_storage_id_default);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onEntrySelected(UUID entryId) {

    }

    @Override
    public void onSecretEntryAttributeApply(int sePos, int attrPos, SecretEntryAttribute attr) {

    }
}
