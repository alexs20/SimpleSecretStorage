package com.wolandsoft.sss.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.LruCache;
import android.widget.Toast;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.NoSuchPaddingException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class AppCentral extends ContextWrapper {
    private static AppCentral mThisInstance;
    private static final int ENTRIES_CACHE_SIZE = 100;
    private KeyStoreManager mKSManager;
    private SQLiteStorage mSQLtStorage;
    private LruCache<Integer, SecretEntry> mEntryCache;

    private AppCentral(Context context) {
        super(context.getApplicationContext());
        try {
            mKSManager = new KeyStoreManager(this);
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException
                | IOException | InvalidKeyException | InvalidAlgorithmParameterException
                | KeyStoreException | NoSuchPaddingException | NoSuchProviderException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            LogEx.e(e.getMessage(), e);
        }
        try {
            mSQLtStorage = new SQLiteStorage(this);
        } catch (StorageException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            LogEx.e(e.getMessage(), e);
        }
        mEntryCache = new LruCache<>(ENTRIES_CACHE_SIZE);
    }

    public static void init(Context context) {
        if (mThisInstance == null) {
            mThisInstance = new AppCentral(context);
        }
    }

    public static AppCentral getInstance() {
        return mThisInstance;
    }

    public KeyStoreManager getKeyStoreManager() {
        return mKSManager;
    }

    public SQLiteStorage getSQLiteStorage() {
        return mSQLtStorage;
    }

    public LruCache<Integer, SecretEntry> getEntriesCache() {
        return mEntryCache;
    }
}
