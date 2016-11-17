package com.wolandsoft.sss.common;

import android.app.Application;
import android.widget.Toast;

import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

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

public class AppCentral extends Application {

    private static KeyStoreManager mKSManager;

    public static KeyStoreManager getKeyStoreManager() {
        return mKSManager;
    }

    public AppCentral() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mKSManager = new KeyStoreManager(this);
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException
                | IOException | InvalidKeyException | InvalidAlgorithmParameterException
                | KeyStoreException | NoSuchPaddingException | NoSuchProviderException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            LogEx.e(e.getMessage(), e);
            System.exit(-1);
        }
    }
}
