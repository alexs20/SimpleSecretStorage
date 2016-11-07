package com.wolandsoft.sss.storage.db;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.wolandsoft.sss.storage.IStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.storage.StorageFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
@RunWith(Parameterized.class)
//@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SQLiteStorageInitTest {

    private final static String PASSWORD1 = "12345";
    private final static String PASSWORD2 = "98765";
    private String storageID;
    private IStorage storage;
    public SQLiteStorageInitTest(String storageID) {
        this.storageID = storageID;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"SQLiteStorage"},
        });
    }

    @Before
    public void setupDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        storage = StorageFactory.getInstance(context).getStorage(storageID);
    }

    @After
    public void cleanDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    @Test
    public void test_s0_start_new_wo_password() throws StorageException {
        storage.startup(null);
        assertTrue(storage.isActive());
        storage.shutdown();
    }

    @Test
    public void test_s0_start_new_w_password() throws StorageException {
        try {
            storage.startup(PASSWORD1);
            fail("New storage accepted password");
        } catch (StorageException expected) {

        }
        assertFalse(storage.isActive());
    }

    @Test
    public void test_s1_start_wo_password() throws StorageException {
        storage.startup(null);
        assertTrue(storage.isActive());
        storage.shutdown();

        storage.startup(null);
        assertTrue(storage.isActive());
        storage.shutdown();
    }

    @Test
    public void test_s1_start_w_password() throws StorageException {
        storage.startup(null);
        assertTrue(storage.isActive());
        storage.setPassword(PASSWORD1);
        storage.shutdown();

        storage.startup(PASSWORD1);
        assertTrue(storage.isActive());
        storage.shutdown();
    }

    @Test
    public void test_s1_start_w_invalid_password() throws StorageException {
        storage.startup(null);
        assertTrue(storage.isActive());
        storage.setPassword(PASSWORD1);
        storage.shutdown();

        try {
            storage.startup(PASSWORD2);
            fail("Storage accepted invalid");
        } catch (StorageException expected) {

        }
        assertFalse(storage.isActive());

        try {
            storage.startup(null);
            fail("Storage accepted no passwords");
        } catch (StorageException expected) {

        }
        assertFalse(storage.isActive());
    }

    @Test
    public void test_s2_remove_password() throws StorageException {
        storage.startup(null);
        assertTrue(storage.isActive());
        storage.setPassword(PASSWORD1);
        storage.shutdown();

        storage.startup(PASSWORD1);
        assertTrue(storage.isActive());
        storage.setPassword(null);
        storage.shutdown();

        storage.startup(null);
        assertTrue(storage.isActive());
        storage.shutdown();
    }


    @Test
    public void test_s2_change_password() throws StorageException {
        storage.startup(null);
        assertTrue(storage.isActive());
        storage.setPassword(PASSWORD1);
        storage.shutdown();

        storage.startup(PASSWORD1);
        assertTrue(storage.isActive());
        storage.setPassword(PASSWORD2);
        storage.shutdown();

        storage.startup(PASSWORD2);
        assertTrue(storage.isActive());
        storage.shutdown();
    }
}
