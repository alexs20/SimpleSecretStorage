package com.wolandsoft.sss.external.json;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.external.IExternal;
import com.wolandsoft.sss.storage.DatabaseHelper;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.AppCentral;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import java.util.Arrays;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JsonAesZipTest {
    private static final String KEY_NAME = "Name";
    private static final String KEY_URL = "URL";
    private static final String KEY_PASSWORD = "Password";
    private static final String TEMPLATE_NAME = "thename%1$s";
    private static final String TEMPLATE_URL = "http://www.example%1$s.com/test";
    private static final String TEMPLATE_PASSWORD = "\\1/2|3[4]\"'56789%1$s";
    private static final int ENTRIES_COUNT = 100;

    private SQLiteStorage storage;
    private IExternal external;

    @Before
    public void setupDB() throws Exception {
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
        external = ExternalFactory.getInstance(context).getExternal("JsonAesZip");
    }

    @After
    public void cleanDB() throws Exception {
        storage.close();
    }

    @Test
    public void test_doExport() throws ExternalException {
        //external.doExport(storage);
    }

    @Test
    public void test_doImport() throws ExternalException {
        //external.doImport(storage, true);
    }

}