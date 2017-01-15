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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * AES cipher with IV in payload
 *
 * @author Alexander Shulgin
 */
public class AESIVCipher extends AESCipher {

    public AESIVCipher(byte[] aesKeyBytes) throws GeneralSecurityException {
        super(aesKeyBytes);
    }

    public byte[] cipher(byte[] data) throws GeneralSecurityException {
        byte[] iv = generateIV();
        byte[] chipered = cipher(iv, data);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(iv.length);
            dos.write(iv);
            dos.writeInt(chipered.length);
            dos.write(chipered);
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
    }

    public byte[] decipher(byte[] data) throws GeneralSecurityException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            int size = dis.readInt();
            byte[] iv = new byte[size];
            dis.readFully(iv);
            size = dis.readInt();
            byte[] ciphered = new byte[size];
            dis.readFully(ciphered);
            return decipher(iv, ciphered);
        } catch (IOException e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
    }
}
