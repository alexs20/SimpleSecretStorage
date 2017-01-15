/*
    Copyright 2017 Alexander Shulgin

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
package com.wolandsoft.sss.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.VisibleForTesting;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

/**
 * Security keystore adapter that provides simplified access to encryption and decryption functions using private key from android keystore
 *
 * @author Alexander Shulgin
 */
public class RSAKSCipher extends ContextWrapper {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ssspkey";
    private static final String CIPHER_MODE = "RSA/ECB/PKCS1Padding";
    private static final String KEY_ALGORITHM_RSA = "RSA";
    private final String alias;
    private KeyStore keystore;
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    /**
     * Initialize keystore manager using predefined {@value #KEY_ALIAS} alias.
     *
     * @param base an application context.
     */
    public RSAKSCipher(Context base) {
        this(base, KEY_ALIAS);
    }

    /**
     * Initialize keystore manager.
     *
     * @param base     An application context.
     * @param keyAlias a key alias.
     */
    @SuppressLint("GetInstance")
    public RSAKSCipher(Context base, String keyAlias) {
        super(base);
        alias = keyAlias;
        try {
            keystore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keystore.load(null);
            if (!keystore.containsAlias(keyAlias)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
                X500Principal subj = new X500Principal("CN=" + keyAlias);
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 100);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
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
                    //noinspection deprecation : it works only for android version blow 23
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
            PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();
            PrivateKey privateKey = privateKeyEntry.getPrivateKey();

            encryptCipher = Cipher.getInstance(CIPHER_MODE);
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            decryptCipher = Cipher.getInstance(CIPHER_MODE);
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Cipher value using public key stored in keystore.
     *
     * @param data value to encrypt.
     * @return encrypted value
     * @throws GeneralSecurityException
     */
    public byte[] cipher(byte[] data) throws GeneralSecurityException {
        return encryptCipher.doFinal(data);
    }

    /**
     * Decipher value using private key stored in keystore.
     *
     * @param data encrypted value
     * @return decrypted vale
     * @throws GeneralSecurityException
     */
    public byte[] decipher(byte[] data) throws GeneralSecurityException {
        return decryptCipher.doFinal(data);
    }

    /**
     * Delete generated key rfom keystore.<br/>
     * Only for testing.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void deleteKey() {
        try {
            keystore.deleteEntry(alias);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
