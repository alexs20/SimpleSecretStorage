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

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Alexander Shulgin
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SQLiteStorageEntryTest {

    private SQLiteStorage storage;
    private SecretEntry entry;

    @Before
    public void setupDB() throws Exception {
        entry = new SecretEntry();
        entry.add(new SecretEntryAttribute("Name", "Test1", false));
        entry.add(new SecretEntryAttribute("URL", "http://test.example.com", false));
        entry.add(new SecretEntryAttribute("Password", "12345", true));

        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        storage = new SQLiteStorage(context);
    }

    @After
    public void cleanDB() throws Exception {
        storage.close();
    }

    @Test
    public void test_s0_get_null() throws StorageException {
        SecretEntry se = storage.get(1);
        assertNull(se);
    }

    @Test
    public void test_s0_put() throws StorageException {
        SecretEntry se = storage.put(entry);
        assertNotNull(se);
        assertTrue(se.getID() > 0);
        assertTrue(se.getCreated() > 0);
        assertTrue(se.getUpdated() > 0);
        assertEquals(entry.size(), se.size());
    }

    @Test
    public void test_s1_put_and_get() throws StorageException {
        SecretEntry out = storage.put(entry);
        assertNotNull(out);
        SecretEntry se = storage.get(out.getID());
        assertNotNull(se);
        assertEquals(out.getID(), se.getID());
        assertEquals(out.getCreated(), se.getCreated());
        assertEquals(out.getUpdated(), se.getUpdated());
        assertEquals(entry.size(), se.size());
        for (int i = 0; i < entry.size(); i++) {
            SecretEntryAttribute inSeAttr = entry.get(i);
            SecretEntryAttribute outSeAttr = se.get(i);
            assertEquals(inSeAttr.getKey(), outSeAttr.getKey());
            assertEquals(inSeAttr.getValue(), outSeAttr.getValue());
            assertEquals(inSeAttr.isProtected(), outSeAttr.isProtected());
        }
    }

    @Test
    public void test_s2_put_update_and_get() throws StorageException {
        SecretEntry out = storage.put(entry);
        assertNotNull(out);
        for (int i = 0; i < entry.size(); i++) {
            SecretEntryAttribute inSeAttr = out.get(i);
            inSeAttr.setValue(inSeAttr.getValue() + "_changed");
            inSeAttr.setProtected(false);
        }
        SecretEntry se = storage.put(out);
        assertNotNull(se);
        se = storage.get(out.getID());
        assertNotNull(se);
        assertEquals(out.getID(), se.getID());
        assertEquals(out.size(), se.size());
        for (int i = 0; i < entry.size(); i++) {
            SecretEntryAttribute inSeAttr = entry.get(i);
            SecretEntryAttribute outSeAttr = se.get(i);
            assertEquals(inSeAttr.getKey(), outSeAttr.getKey());
            assertEquals(inSeAttr.getValue(), outSeAttr.getValue());
            assertEquals(inSeAttr.isProtected(), outSeAttr.isProtected());
        }
    }

    @Test
    public void test_s3_delete() throws StorageException {
        SecretEntry out = storage.put(entry);
        assertNotNull(out);
        storage.delete(out.getID());
        out = storage.get(out.getID());
        assertNull(out);
    }
}
