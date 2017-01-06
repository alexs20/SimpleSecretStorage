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

    private SQLiteStorage mStorage;
    private SecretEntry mEntry;

    @Before
    public void setupDB() throws Exception {
        mEntry = new SecretEntry();
        mEntry.add(new SecretEntryAttribute("Name", "Test1", false));
        mEntry.add(new SecretEntryAttribute("URL", "http://test.example.com", false));
        mEntry.add(new SecretEntryAttribute("Password", "12345", true));

        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        mStorage = new SQLiteStorage(context);
    }

    @After
    public void cleanDB() throws Exception {
        mStorage.close();
    }

    @Test
    public void test_00_get_null() {
        SecretEntry se = mStorage.get(1);
        assertNull(se);
    }

    @Test
    public void test_00_put() {
        SecretEntry se = mStorage.put(mEntry);
        assertNotNull(se);
        assertTrue(se.getID() > 0);
        assertTrue(se.getCreated() > 0);
        assertTrue(se.getUpdated() > 0);
        assertEquals(mEntry.size(), se.size());
    }

    @Test
    public void test_01_put_and_get() {
        SecretEntry out = mStorage.put(mEntry);
        assertNotNull(out);
        SecretEntry entry = mStorage.get(out.getID());
        assertNotNull(entry);
        assertEquals(out.getID(), entry.getID());
        assertEquals(out.getCreated(), entry.getCreated());
        assertEquals(out.getUpdated(), entry.getUpdated());
        assertEquals(mEntry.size(), entry.size());
        for (int i = 0; i < mEntry.size(); i++) {
            SecretEntryAttribute inSeAttr = mEntry.get(i);
            SecretEntryAttribute outSeAttr = entry.get(i);
            assertEquals(inSeAttr.getKey(), outSeAttr.getKey());
            assertEquals(inSeAttr.getValue(), outSeAttr.getValue());
            assertEquals(inSeAttr.isProtected(), outSeAttr.isProtected());
        }
    }

    @Test
    public void test_02_put_update_and_get() {
        SecretEntry out = mStorage.put(mEntry);
        assertNotNull(out);
        for (int i = 0; i < out.size(); i++) {
            SecretEntryAttribute inSeAttr = out.get(i);
            inSeAttr.setValue(inSeAttr.getValue() + "_changed");
        }
        SecretEntry entry1 = mStorage.put(out);
        assertNotNull(entry1);
        SecretEntry entry2 = mStorage.get(entry1.getID());
        assertNotNull(entry1);
        assertEquals(entry1.getID(), entry2.getID());
        assertEquals(entry1.size(), entry2.size());
        for (int i = 0; i < entry1.size(); i++) {
            SecretEntryAttribute inSeAttr = entry1.get(i);
            SecretEntryAttribute outSeAttr = entry2.get(i);
            assertEquals(inSeAttr.getKey(), outSeAttr.getKey());
            assertEquals(inSeAttr.getValue(), outSeAttr.getValue());
            assertEquals(inSeAttr.isProtected(), outSeAttr.isProtected());
        }
    }

    @Test
    public void test_03_delete() {
        SecretEntry out = mStorage.put(mEntry);
        assertNotNull(out);
        mStorage.delete(out.getID());
        out = mStorage.get(out.getID());
        assertNull(out);
    }
}
