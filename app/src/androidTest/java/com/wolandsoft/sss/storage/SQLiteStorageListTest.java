package com.wolandsoft.sss.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wolandsoft.sss.util.AppCentral;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

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
    private static final int ENTRIES_COUNT = 1000;
    private static SQLiteStorage storage;

    @BeforeClass
    public static void setupDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        AppCentral.init(context);
        storage = AppCentral.getInstance().getSQLiteStorage();
        for (int i = 0; i < ENTRIES_COUNT; i++) {
            SecretEntry entry = new SecretEntry();
            entry.add(new SecretEntryAttribute(KEY_NAME, String.format(TEMPLATE_NAME, i), false));
            entry.add(new SecretEntryAttribute(KEY_URL, String.format(TEMPLATE_URL, i), false));
            String password = AppCentral.getInstance().getKeyStoreManager().encrypt(String.format(TEMPLATE_PASSWORD, i));
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
        int [] seIds = storage.find(null, true);
        assertNotNull(seIds);
        assertEquals(ENTRIES_COUNT, seIds.length);
    }

    @Test
    public void test_s1_get_id_by_name() throws StorageException {
        String name = String.format(TEMPLATE_NAME, 0);
        int [] seIds = storage.find(name, true);
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
        int [] seIds = storage.find(name, true);
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
        int [] seIds = storage.find(name, true);
        assertNotNull(seIds);
        assertEquals(111, seIds.length);
    }
}
