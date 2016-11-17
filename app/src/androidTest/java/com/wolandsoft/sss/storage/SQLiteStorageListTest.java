package com.wolandsoft.sss.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wolandsoft.sss.common.AppCentral;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
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

    @BeforeClass
    public static void setupDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        storage = new SQLiteStorage(context);
        for (int i = 0; i < ENTRIES_COUNT; i++) {
            SecretEntry entry = new SecretEntry();
            entry.add(new SecretEntryAttribute(KEY_NAME, String.format(TEMPLATE_NAME, i), false));
            entry.add(new SecretEntryAttribute(KEY_URL, String.format(TEMPLATE_URL, i), false));
            String password = AppCentral.getKeyStoreManager().encrypt(String.format(TEMPLATE_PASSWORD, i));
            entry.add(new SecretEntryAttribute(KEY_PASSWORD, password, true));
            storage.put(entry);
        }
    }

    @AfterClass
    public static void cleanDB() throws Exception {
        storage.close();
    }

    @Test
    public void test_s0_get_first_by_name() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 0);
        List<SecretEntry> se = storage.find(name, true, 0, 99);
        assertNotNull(se);
        assertEquals(1, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(name, entry.get(0).getValue());
    }

    @Test
    public void test_s0_get_last_by_name() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 99);
        List<SecretEntry> se = storage.find(name, true, 0, 99);
        assertNotNull(se);
        assertEquals(1, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(name, entry.get(0).getValue());
    }

    @Test
    public void test_s1_get_first_by_url() throws StorageException {
        String name = String.format(TEMPLATE_URL, 0);
        List<SecretEntry> se = storage.find(name, true, 0, 99);
        assertNotNull(se);
        assertEquals(1, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_URL, entry.get(1).getKey());
        assertEquals(name, entry.get(1).getValue());
    }


    @Test
    public void test_s1_get_last_by_url() throws StorageException {
        String name = String.format(TEMPLATE_URL, 99);
        List<SecretEntry> se = storage.find(name, true, 0, 99);
        assertNotNull(se);
        assertEquals(1, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_URL, entry.get(1).getKey());
        assertEquals(name, entry.get(1).getValue());
    }

    @Test
    public void test_s3_get_mid_by_default() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 49);
        List<SecretEntry> se = storage.find(name, true, 0, 99);
        assertNotNull(se);
        assertEquals(1, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(name, entry.get(0).getValue());
    }

    @Test
    public void test_s4_get_first_10_by_name() throws StorageException {
        String name = String.format(TEMPLATE_NAME, "");
        List<SecretEntry> se = storage.find(name, true, 0, 10);
        assertNotNull(se);
        assertEquals(10, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(String.format(TEMPLATE_NAME, 0), entry.get(0).getValue());
    }

    @Test
    public void test_s4_get_first_10() throws StorageException {
        List<SecretEntry> se = storage.find(null, true, 0, 10);
        assertNotNull(se);
        assertEquals(10, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(String.format(TEMPLATE_NAME, 0), entry.get(0).getValue());
    }

    @Test
    public void test_s4_get_last_10() throws StorageException {
        List<SecretEntry> se = storage.find(null, false, 0, 10);
        assertNotNull(se);
        assertEquals(10, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(String.format(TEMPLATE_NAME, 99), entry.get(0).getValue());
    }

    @Test
    public void test_s4_get_next_10_by_name() throws StorageException {
        String name = String.format(TEMPLATE_NAME, "");
        List<SecretEntry> se = storage.find(name, true, 10, 10);
        assertNotNull(se);
        assertEquals(10, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        //0,1,10,11,12,13,14,15,16,17,[18]
        assertEquals(String.format(TEMPLATE_NAME, 18), entry.get(0).getValue());
    }

    @Test
    public void test_s4_get_next_10() throws StorageException {
        List<SecretEntry> se = storage.find(null, true, 10, 10);
        assertNotNull(se);
        assertEquals(10, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(String.format(TEMPLATE_NAME, 18), entry.get(0).getValue());
    }

    @Test
    public void test_s5_get_first_2_by_mixed_key() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 99);
        String url = String.format(TEMPLATE_URL, 0);
        List<SecretEntry> se = storage.find(name + " " + url, true, 0, 10);
        assertNotNull(se);
        assertEquals(2, se.size());
        SecretEntry entry = se.get(0);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(String.format(TEMPLATE_NAME, 0), entry.get(0).getValue());
        entry = se.get(1);
        assertEquals(3, entry.size());
        assertEquals(KEY_NAME, entry.get(0).getKey());
        assertEquals(String.format(TEMPLATE_NAME, 99), entry.get(0).getValue());
    }
}
