package com.wolandsoft.sss.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.storage.DatabaseHelper;
import com.wolandsoft.sss.storage.StorageException;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
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
        storage = new SQLiteStorage(context);
    }

    @After
    public void cleanDB() throws Exception {
        storage.close();
    }

    @Test
    public void test_s0_get_null() throws StorageException {
        SecretEntry se = storage.get(entry.getID());
        assertNull(se);
    }

    @Test
    public void test_s0_put() throws StorageException {
        SecretEntry se = storage.put(entry);
        assertNull(se);
    }

    @Test
    public void test_s1_put_and_get() throws StorageException {
        SecretEntry se = storage.put(entry);
        assertNull(se);
        se = storage.get(entry.getID());
        assertNotNull(se);
        assertEquals(entry.getID(), se.getID());
        assertEquals(entry.size(), se.size());
        for(int i = 0; i < entry.size(); i++){
            SecretEntryAttribute inSeAttr = entry.get(i);
            SecretEntryAttribute outSeAttr = se.get(i);
            assertEquals(inSeAttr.getKey(), outSeAttr.getKey());
            assertEquals(inSeAttr.getValue(), outSeAttr.getValue());
            assertEquals(inSeAttr.isProtected(), outSeAttr.isProtected());
        }
    }

    @Test
    public void test_s2_put_update_and_get() throws StorageException {
        SecretEntry se = storage.put(entry);
        assertNull(se);
        for(int i = 0; i < entry.size(); i++){
            SecretEntryAttribute inSeAttr = entry.get(i);
            inSeAttr.setValue(inSeAttr.getValue() + "_changed");
            inSeAttr.setProtected(false);
        }
        se = storage.put(entry);
        assertNotNull(se);
        se = storage.get(entry.getID());
        assertNotNull(se);
        assertEquals(entry.getID(), se.getID());
        assertEquals(entry.size(), se.size());
        for(int i = 0; i < entry.size(); i++){
            SecretEntryAttribute inSeAttr = entry.get(i);
            SecretEntryAttribute outSeAttr = se.get(i);
            assertEquals(inSeAttr.getKey(), outSeAttr.getKey());
            assertEquals(inSeAttr.getValue(), outSeAttr.getValue());
            assertEquals(inSeAttr.isProtected(), outSeAttr.isProtected());
        }
    }
}
