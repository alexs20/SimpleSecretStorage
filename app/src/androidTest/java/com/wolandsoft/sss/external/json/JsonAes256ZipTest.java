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
package com.wolandsoft.sss.external.json;

import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.external.IExternal;
import com.wolandsoft.sss.storage.DatabaseHelper;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.crypto.NoSuchPaddingException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

/**
 * @author Alexander Shulgin
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JsonAes256ZipTest {
    private static final String FILE_NAME = "export.zip";
    private static final String FILE_PASSWORD = "4RRt6#$";
    private static final String KEY_NAME = "Name";
    private static final String KEY_URL = "URL";
    private static final String KEY_PASSWORD = "Password";
    private static final String TEMPLATE_NAME = "thename%1$s";
    private static final String TEMPLATE_URL = "http://www.example%1$s.com/test";
    private static final String TEMPLATE_PASSWORD = "\\1/2|3[4]\"'56789%1$s";
    private static final int ENTRIES_COUNT = 1;

    private static SQLiteStorage storage;
    private static KeyStoreManager keystore;
    private static IExternal external;

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
        external = ExternalFactory.getInstance(context).getExternal(JsonAes256Zip.class.getSimpleName());
    }

    @AfterClass
    public static void cleanDB() throws Exception {
        storage.close();
    }

    @Test
    public void test_export_import() throws ExternalException, StorageException, InterruptedException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File location = new File(dir, FILE_NAME);
        if (location.exists()) {
            location.delete();
        }
        assertFalse(location.exists());
        external.doExport(storage, keystore, location.toURI(), FILE_PASSWORD);
        assertTrue(location.exists());

        List<Integer> entries = storage.find(null, true);
        assertEquals(ENTRIES_COUNT, entries.size());
        SecretEntry se = storage.get(entries.get(0));
        assertNotNull(se);
        SecretEntryAttribute attr = se.get(0);
        attr.setValue("CHANGED");
        storage.put(se);

        external.doImport(storage, keystore, IExternal.ConflictResolution.overwrite, location.toURI(), FILE_PASSWORD);
        entries = storage.find(null, true);
        assertEquals(ENTRIES_COUNT, entries.size());

        se = storage.get(entries.get(0));
        assertNotNull(se);
        attr = se.get(0);
        assertNotSame("CHANGED", attr.getValue());

        se = storage.get(entries.get(0));
        assertNotNull(se);
        attr = se.get(0);
        attr.setValue("CHANGED");
        storage.put(se);

        external.doImport(storage, keystore, IExternal.ConflictResolution.merge, location.toURI(), FILE_PASSWORD);
        entries = storage.find(null, true);
        assertEquals(ENTRIES_COUNT, entries.size());

        se = storage.get(entries.get(0));
        assertNotNull(se);
        int count = 0;
        for (SecretEntryAttribute attrNext : se) {
            if (attrNext.getKey().equals(KEY_NAME)) {
                count++;
            }
        }
        assertEquals(2, count);
    }

}
