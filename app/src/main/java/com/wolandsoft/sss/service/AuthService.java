package com.wolandsoft.sss.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.wolandsoft.sss.entity.User;

/**
 * Provides synchronously login / logout / signup functionality
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AuthService extends Service {
    private final static String TAG = AuthService.class.toString();
    private final static String KEY_LAST_USER = "last_user";
    private final IBinder mBinder = new LocalBinder();
    private User mLastUser = null;
    private SharedPreferences mPref;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPref = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        //check for previously logged-in user and restore it in case the service has been killed by the system and then restarted
        String lastUserID = mPref.getString(KEY_LAST_USER, null);
        if (lastUserID != null) {
            mLastUser = User.getModel(this).find(lastUserID);
            if (mLastUser == null) {
                //something is wrong, restoring state integrity
                mPref.edit().putString(KEY_LAST_USER, null).apply();
            }
        }
    }

    /**
     * Retrieve last authenticated or registered user entity
     *
     * @return user entity or {@null}
     */
    @Nullable
    public User getAuthenticated() {
        return mLastUser;
    }

    /**
     * Register new user and login
     *
     * @param id       user id/email
     * @param name     display name
     * @param password password
     * @return User's entity
     * @throws AuthInvocationException On any error such as empty input parameters, duplicate entity, active user.
     */
    public User signup(String id, String name, String password) throws AuthInvocationException {
        if (id == null || id.trim().isEmpty() ||
                name == null || name.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new AuthInvocationException("Empty parameter");
        }
        if (User.getModel(this).find(id) != null) {
            throw new AuthInvocationException("User exist");
        }
        if (mLastUser != null) {
            throw new AuthInvocationException("User logged in");
        }
        mLastUser = User.getModel(this).add(id, name, password);
        mPref.edit().putString(KEY_LAST_USER, id).apply();
        return mLastUser;
    }

    /**
     * User login
     *
     * @param id       user id/email
     * @param password password
     * @return user entity or {@code null} if not authenticated
     * @throws AuthInvocationException On any error such as empty input parameters, active user.
     */
    @Nullable
    public User login(String id, String password) throws AuthInvocationException {
        if (id == null || id.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new AuthInvocationException("Empty parameter");
        }
        mLastUser = User.getModel(this).find(id, password);
        if (mLastUser != null) {
            mPref.edit().putString(KEY_LAST_USER, id).apply();
        }
        return mLastUser;
    }

    /**
     * Check that user is not registered
     *
     * @param id user id/email
     * @return {@code true} when ID is available for registration
     * @throws AuthInvocationException On any error such as empty input parameters
     */
    public boolean isFreeID(String id) throws AuthInvocationException {
        if (id == null || id.trim().isEmpty()) {
            throw new AuthInvocationException("Empty parameter");
        }
        return User.getModel(this).find(id) == null;
    }

    /**
     * Log off a user from a session
     *
     * @throws AuthInvocationException On any error such as user not logged in.
     */
    public void logout() throws AuthInvocationException {
        mLastUser = null;
        mPref.edit().putString(KEY_LAST_USER, null).apply();
    }

    /**
     * Service binder impementation
     */
    public class LocalBinder extends Binder {
        public AuthService getService() {
            return AuthService.this;
        }
    }
}
