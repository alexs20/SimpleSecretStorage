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

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Base64 wrapped AES cipher
 *
 * @author Alexander Shulgin
 */

public class TextCipher extends AESCipher {

    /**
     * Initialize.
     */
    public TextCipher() {
        super();
    }

    /**
     * Initialize.
     *
     * @param keyAlias a key alias.
     */
    public TextCipher(String keyAlias) {
        super(keyAlias);
    }

    /**
     * Initialize.
     *
     * @param aesKeyBytes an aes key.
     */
    public TextCipher(byte[] aesKeyBytes) throws GeneralSecurityException {
        super(aesKeyBytes);
    }

    public String cipher(String text) {
        try {
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            byte[] textCiphered = cipher(textBytes);
            return Base64.encodeToString(textCiphered, Base64.DEFAULT);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String decipher(String secret) {
        try {
            byte[] textCiphered = Base64.decode(secret, Base64.DEFAULT);
            byte[] textBytes = decipher(textCiphered);
            return new String(textBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
