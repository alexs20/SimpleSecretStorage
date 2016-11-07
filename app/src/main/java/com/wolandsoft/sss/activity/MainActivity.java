package com.wolandsoft.sss.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.wolandsoft.sss.AppConstants;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.activity.fragment.EntriesFragment;
import com.wolandsoft.sss.storage.IStorageProvider;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.KeySharedPreferences;

import java.util.UUID;

/**
 * Main UI class of the app.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class MainActivity extends AppCompatActivity implements EntriesFragment.OnFragmentInteractionListener,IStorageProvider {

    private SQLiteStorage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //KeySharedPreferences pref = new KeySharedPreferences(getSharedPreferences(AppConstants.APP_TAG, Context.MODE_PRIVATE), this);
        //String storageId = pref.getString(R.string.key_storage_id, R.string.value_storage_id_default);

        try {
            mStorage = new SQLiteStorage(this);
        } catch (StorageException e) {
            finish();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new EntriesFragment();
        transaction.replace(R.id.content_fragment, fragment);
        transaction.commit();
    }

//    @Override
//    public void onUserLoginInfoProvided(User user) {
//        FragmentManager fragmentMgr = getSupportFragmentManager();
//        //no history stack required
//        fragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//        //load home fragment
//        FragmentTransaction transaction = fragmentMgr.beginTransaction();
//        Fragment fragment = new EntriesFragment();
//        transaction.replace(R.id.content_fragment, fragment);
//        transaction.commit();
//    }
//
//    @Override
//    public void onSignupFlowSelected() {
//        //load registration fragment
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        Fragment fragment = new RegisterFragment();
//        transaction.replace(R.id.content_fragment, fragment);
//        transaction.addToBackStack("LOGIN-TO-SIGNUP");
//        transaction.commit();
//    }
//
//    @Override
//    public void onUserSignupInfoProvided(User user) {
//        onUserLoginInfoProvided(user);
//    }
//
//    @Override
//    public AuthService getService() {
//        return mService;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (mService != null) {
//            //close live connections to the service
//            getApplicationContext().unbindService(mConnection);
//            mService = null;
//        }
//    }
//
//    @Override
//    public void onLogoutFlowSelected() {
//        FragmentManager fragmentMgr = getSupportFragmentManager();
//        //no history stack required
//        fragmentMgr.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//        //load login screen after logout
//        FragmentTransaction transaction = fragmentMgr.beginTransaction();
//        Fragment fragment = new LoginFragment();
//        transaction.replace(R.id.content_fragment, fragment);
//        transaction.commit();
//    }

    @Override
    public void onEntrySelected(UUID entryId) {

    }

    @Override
    public SQLiteStorage getSQLiteStorage() {
        return mStorage;
    }
}
