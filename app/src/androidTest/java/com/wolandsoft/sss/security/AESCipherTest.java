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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Shulgin
 */
@RunWith(Parameterized.class)
//@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AESCipherTest {
    private final static String TEST_KEY = "test_aes_key";
    private static AESCipher mCipher;
    private final String pwd;

    public AESCipherTest(String pwd) {
        this.pwd = pwd;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"12345"}, {"98765"}, {"1234567890"}, {"qwertyuiop1234567890"},
                {"1q2w3e4r5t6y7u8i9o0pazsxdcfvgbhnjmk,l.;/[']\\=-"}
        });
    }

    @BeforeClass
    public static void setup() throws Exception {
        mCipher = new AESCipher(TEST_KEY);
    }

    @AfterClass
    public static void clean() throws Exception {
        mCipher.deleteKey();
    }

    @Test
    public void test_encrypt_decrupt() throws UnsupportedEncodingException, GeneralSecurityException {
        byte[] data = pwd.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = mCipher.cipher(data);
        assertFalse(Arrays.equals(encrypted, data));
        data = mCipher.decipher(encrypted);
        assertTrue(Arrays.equals(pwd.getBytes(StandardCharsets.UTF_8), data));
    }
}
