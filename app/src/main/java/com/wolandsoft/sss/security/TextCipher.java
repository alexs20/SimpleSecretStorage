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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Base64;

import com.wolandsoft.sss.R;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class TextCipher extends RSAKSCipher {
    private AESIVCipher mAesCipher;

    public TextCipher(Context base) {
        super(base);
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(this);
        String aesKeyB64 = shPref.getString(getString(R.string.pref_aes_key_key), null);
        try {
            if (aesKeyB64 != null) {
                byte[] aesKeyCiphered = Base64.decode(aesKeyB64, Base64.DEFAULT);
                byte[] aesKey = decipher(aesKeyCiphered);
                mAesCipher = new AESIVCipher(aesKey);
            } else {
                mAesCipher = new AESIVCipher(null);
                byte[] aesKeyCiphered = cipher(mAesCipher.getKey());
                aesKeyB64 = Base64.encodeToString(aesKeyCiphered, Base64.DEFAULT);
                shPref.edit().putString(getString(R.string.pref_aes_key_key), aesKeyB64).apply();
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String cipherText(String text) {
        try {
            byte[] textBytes = text.getBytes("UTF-8");
            byte[] textCiphered = mAesCipher.cipher(textBytes);
            return Base64.encodeToString(textCiphered, Base64.DEFAULT);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String decipherText(String secret) {
        try {
            byte[] textCiphered = Base64.decode(secret, Base64.DEFAULT);
            byte[] textBytes = mAesCipher.decipher(textCiphered);
            return new String(textBytes, "UTF-8");
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
