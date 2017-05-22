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

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES cipher
 *
 * @author Alexander Shulgin
 */
public class AESCipher {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "aes_key";
    private static final String CIPHER_MODE = String.format("%s/%s/%s",
            KeyProperties.KEY_ALGORITHM_AES,
            KeyProperties.BLOCK_MODE_GCM,
            KeyProperties.ENCRYPTION_PADDING_NONE);
    private static final String IV_ALGORITHM = "SHA1PRNG";
    private static final int KEY_SIZE = 256;
    private SecretKey mAesKey;
    private String mKeyAlias;

    /**
     * Initialize.
     */
    public AESCipher() {
        this(KEY_ALIAS);
    }

    /**
     * Initialize.
     *
     * @param keyAlias a key alias.
     */
    public AESCipher(String keyAlias) {
        mKeyAlias = keyAlias;
        try {
            KeyStore keystore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keystore.load(null);
            if (!keystore.containsAlias(mKeyAlias)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                keyGenerator.init(new KeyGenParameterSpec.Builder(mKeyAlias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_SIZE)
                        .build());
                mAesKey = keyGenerator.generateKey();
            } else {
                mAesKey = (SecretKey) keystore.getKey(mKeyAlias, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public AESCipher(byte[] aesKeyBytes) throws GeneralSecurityException {
        mAesKey = new SecretKeySpec(aesKeyBytes, KeyProperties.KEY_ALGORITHM_AES);
    }

    /**
     * Cipher value using public key stored in keystore.
     *
     * @param payload value to encrypt.
     * @return encrypted value
     * @throws GeneralSecurityException
     */
    public byte[] cipher(byte[] payload) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, mAesKey);
        byte[] encrypted = cipher.doFinal(payload);
        byte[] iv = cipher.getIV();
        int tLen = (encrypted.length - payload.length) * 8;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(tLen);
            dos.writeInt(iv.length);
            dos.write(iv);
            dos.writeInt(encrypted.length);
            dos.write(encrypted);
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
    }

    /**
     * Decipher value using private key stored in keystore.
     *
     * @param payload encrypted value
     * @return decrypted vale
     * @throws GeneralSecurityException
     */
    public byte[] decipher(byte[] payload) throws GeneralSecurityException {
        int tLen;
        byte[] iv;
        byte[] encrypted;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            DataInputStream dis = new DataInputStream(bais);
            tLen = dis.readInt();
            int size = dis.readInt();
            iv = new byte[size];
            dis.readFully(iv);
            size = dis.readInt();
            encrypted = new byte[size];
            dis.read(encrypted);
        } catch (IOException e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
        GCMParameterSpec ivSpec = new GCMParameterSpec(tLen, iv);
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        cipher.init(Cipher.DECRYPT_MODE, mAesKey, ivSpec);
        return cipher.doFinal(encrypted);
    }

    /**
     * Delete generated key rfom keystore.<br/>
     * Only for testing.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void deleteKey() {
        try {
            KeyStore keystore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keystore.deleteEntry(mKeyAlias);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
