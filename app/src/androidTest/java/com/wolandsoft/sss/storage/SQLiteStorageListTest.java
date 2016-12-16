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
package com.wolandsoft.sss.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.NoSuchPaddingException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Alexander Shulgin
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SQLiteStorageListTest {
    private static final String KEY_NAME = "Name";
    private static final String KEY_URL = "URL";
    private static final String KEY_PASSWORD = "Password";
    private static final String TEMPLATE_NAME = "thename%1$s";
    private static final String TEMPLATE_URL = "http://www.example%1$s.com/test";
    private static final String TEMPLATE_PASSWORD = "123456789%1$s";
    private static final int ENTRIES_COUNT = 100;
    private static SQLiteStorage storage;
    private static KeyStoreManager keystore;

    @BeforeClass
    public static void setupDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        //security keystore initialization
        try {
            keystore = new KeyStoreManager(context);
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException
                | IOException | InvalidKeyException | InvalidAlgorithmParameterException
                | KeyStoreException | NoSuchPaddingException | NoSuchProviderException e) {
            LogEx.e(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        //db initialization
        try {
            storage = new SQLiteStorage(context);
        } catch (StorageException e) {
            LogEx.e(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        for (int i = 0; i < ENTRIES_COUNT; i++) {
            SecretEntry entry = new SecretEntry();
            entry.add(new SecretEntryAttribute(KEY_NAME, String.format(TEMPLATE_NAME, i), false));
            entry.add(new SecretEntryAttribute(KEY_URL, String.format(TEMPLATE_URL, i), false));
            String password = keystore.encrypt(String.format(TEMPLATE_PASSWORD, i));
            entry.add(new SecretEntryAttribute(KEY_PASSWORD, password, true));
            storage.put(entry);
        }
    }

    @AfterClass
    public static void cleanDB() throws Exception {
        storage.close();
    }

    @Test
    public void test_s0_get_id_all() throws StorageException {
        int[] seIds = storage.find(null, true);
        assertNotNull(seIds);
        assertEquals(ENTRIES_COUNT, seIds.length);
    }

    @Test
    public void test_s1_get_id_by_name() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 0);
        int[] seIds = storage.find(name, true);
        assertNotNull(seIds);
        assertEquals(1, seIds.length);

        SecretEntry se = storage.get(seIds[0]);
        assertNotNull(se);
        assertEquals(3, se.size());
        assertEquals(KEY_NAME, se.get(0).getKey());
        assertEquals(name, se.get(0).getValue());
    }

    @Test
    public void test_s1_get_id_by_url() throws StorageException {
        String name = String.format(TEMPLATE_URL, 0);
        int[] seIds = storage.find(name, true);
        assertNotNull(seIds);
        assertEquals(1, seIds.length);

        SecretEntry se = storage.get(seIds[0]);
        assertNotNull(se);
        assertEquals(3, se.size());
        assertEquals(KEY_URL, se.get(1).getKey());
        assertEquals(name, se.get(1).getValue());
    }

    @Test
    public void test_s1_get_id_by_name_many() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 7);
        int[] seIds = storage.find(name, true);
        assertNotNull(seIds);
        assertEquals(11, seIds.length);
    }
}
