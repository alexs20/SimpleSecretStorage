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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES cipher
 *
 * @author Alexander Shulgin
 */
public class AESCipher {
    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";
    private static final String ALGORITHM = "SHA1PRNG";
    private SecretKey mAesKey;
    private boolean mIsGenerated;

    public AESCipher(byte[] aesKeyBytes) throws GeneralSecurityException {
        if (aesKeyBytes != null) {
            mAesKey = new SecretKeySpec(aesKeyBytes, AES);
            mIsGenerated = false;
        } else {
            KeyGenerator keygen = KeyGenerator.getInstance(AES);
            mAesKey = keygen.generateKey();
            mIsGenerated = true;
        }
    }

    public byte[] getKey() {
        return mAesKey.getEncoded();
    }

    public boolean isGenerated() {
        return mIsGenerated;
    }

    public byte[] generateIV() throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecureRandom randomSecureRandom = SecureRandom.getInstance(ALGORITHM);
        byte[] iv = new byte[cipher.getBlockSize()];
        randomSecureRandom.nextBytes(iv);
        return iv;
    }

    public byte[] decipher(byte[] ivb, byte[] data) throws GeneralSecurityException {
        IvParameterSpec iv = new IvParameterSpec(ivb);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, mAesKey, iv);
        return cipher.doFinal(data);
    }

    public byte[] cipher(byte[] ivb, byte[] data) throws GeneralSecurityException {
        IvParameterSpec iv = new IvParameterSpec(ivb);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, mAesKey, iv);
        return cipher.doFinal(data);
    }
}
