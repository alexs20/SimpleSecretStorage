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
package com.wolandsoft.sss.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

/**
 * Security keystore adapter that provides simplified access to encryption and decryption functions using private key from android keystore
 *
 * @author Alexander Shulgin
 */

public class KeyStoreManager extends ContextWrapper {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ssspkey";
    private static final String CIPHER_MODE = "RSA/ECB/PKCS1Padding";
    private String alias;
    private KeyStore keystore;
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public KeyStoreManager(Context base) throws KeyStoreException, CertificateException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, NoSuchProviderException, InvalidAlgorithmParameterException {
        this(base, KEY_ALIAS);
    }

    public KeyStoreManager(Context base, String keyAlias) throws KeyStoreException, CertificateException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, NoSuchProviderException, InvalidAlgorithmParameterException {
        super(base);
        alias = keyAlias;
        keystore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keystore.load(null);
        if (!keystore.containsAlias(keyAlias)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
            X500Principal subj = new X500Principal("CN=" + keyAlias);
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 100);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                        keyAlias, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                        .setCertificateSubject(subj)
                        .setCertificateSerialNumber(BigInteger.ONE)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setKeyValidityStart(start.getTime())
                        .setKeyValidityEnd(end.getTime())
                        .build();
                generator.initialize(spec);
            } else {
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(this)
                        .setAlias(keyAlias)
                        .setSubject(subj)
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                generator.initialize(spec);
            }
            generator.generateKeyPair();
        }

        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(keyAlias, null);
        PublicKey publicKey = (PublicKey) privateKeyEntry.getCertificate().getPublicKey();
        PrivateKey privateKey = (PrivateKey) privateKeyEntry.getPrivateKey();

        encryptCipher = Cipher.getInstance(CIPHER_MODE);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        decryptCipher = Cipher.getInstance(CIPHER_MODE);
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
    }

    public String encrypt(String value) throws BadPaddingException, IllegalBlockSizeException {
        byte[] encoded = encryptCipher.doFinal(value.getBytes());
        return Base64.encodeToString(encoded, Base64.DEFAULT);
    }

    public String decrupt(String value) throws BadPaddingException, IllegalBlockSizeException {
        byte[] decoded = decryptCipher.doFinal(Base64.decode(value, Base64.DEFAULT));
        return new String(decoded);
    }

    public void deleteKey() throws KeyStoreException {
        keystore.deleteEntry(alias);
    }
}
