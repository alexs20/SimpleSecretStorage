/*
    Copyright 2016, 2017 Alexander Shulgin

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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.List;

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
    private static final String TEMPLATE_NAME = "#%1$s_%2$s#";
    private static final String TEMPLATE_URL = "http://www.%1$s%2$s.com/test";
    private static final String TEMPLATE_PASSWORD = "%1$s123456789%2$s";
    private static final int ENTRIES_ABC_COUNT = 50;
    private static final int ENTRIES_XYZ_COUNT = 50;
    private static final String VAR_ABC = "abc";
    private static final String VAR_XYZ = "xyz";
    private static SQLiteStorage mStorage;

    @BeforeClass
    public static void setupDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        //security keystore initialization
        KeyStoreManager keystore = new KeyStoreManager(context);
        //db initialization
        mStorage = new SQLiteStorage(context);
        for (int i = 0; i < ENTRIES_ABC_COUNT; i++) {
            SecretEntry entry = new SecretEntry();
            entry.add(new SecretEntryAttribute(KEY_NAME, String.format(TEMPLATE_NAME, i, VAR_ABC), false));
            entry.add(new SecretEntryAttribute(KEY_URL, String.format(TEMPLATE_URL, i, VAR_ABC), false));
            String password = keystore.encrypt(String.format(TEMPLATE_PASSWORD, i, VAR_ABC));
            entry.add(new SecretEntryAttribute(KEY_PASSWORD, password, true));
            mStorage.put(entry);
        }
        for (int i = 0; i < ENTRIES_XYZ_COUNT; i++) {
            SecretEntry entry = new SecretEntry();
            entry.add(new SecretEntryAttribute(KEY_NAME, String.format(TEMPLATE_NAME, i, VAR_XYZ), false));
            entry.add(new SecretEntryAttribute(KEY_URL, String.format(TEMPLATE_URL, i, VAR_XYZ), false));
            String password = keystore.encrypt(String.format(TEMPLATE_PASSWORD, i, VAR_XYZ));
            entry.add(new SecretEntryAttribute(KEY_PASSWORD, password, true));
            mStorage.put(entry);
        }
    }

    @AfterClass
    public static void cleanDB() throws Exception {
        mStorage.close();
    }

    @Test
    public void test_00_get_id_all() {
        List<Integer> seIds = mStorage.find(null);
        assertNotNull(seIds);
        assertEquals(ENTRIES_ABC_COUNT + ENTRIES_XYZ_COUNT, seIds.size());
    }

    @Test
    public void test_01_get_id_by_name() {
        String name = String.format(TEMPLATE_NAME, 0, VAR_ABC);
        List<Integer> seIds = mStorage.find(name);
        assertNotNull(seIds);
        assertEquals(1, seIds.size());

        SecretEntry se = mStorage.get(seIds.get(0));
        assertNotNull(se);
        assertEquals(3, se.size());
        for (int i = 0; i < se.size(); i++) {
            if (KEY_NAME.equals(se.get(0).getKey())) {
                assertEquals(name, se.get(0).getValue());
                break;
            }
        }
    }

    @Test
    public void test_01_get_id_by_url() {
        String name = String.format(TEMPLATE_URL, 0, VAR_ABC);
        List<Integer> seIds = mStorage.find(name);
        assertNotNull(seIds);
        assertEquals(1, seIds.size());

        SecretEntry se = mStorage.get(seIds.get(0));
        assertNotNull(se);
        assertEquals(3, se.size());
        for (int i = 0; i < se.size(); i++) {
            if (KEY_URL.equals(se.get(0).getKey())) {
                assertEquals(name, se.get(0).getValue());
                break;
            }
        }
    }

    @Test
    public void test_02_get_id_by_name_many() {
        List<Integer> seIds = mStorage.find(VAR_ABC);
        assertNotNull(seIds);
        assertEquals(ENTRIES_ABC_COUNT, seIds.size());
    }
}
