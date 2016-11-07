package com.wolandsoft.sss;

import com.wolandsoft.sss.external.json.PlainJsonTest;
import com.wolandsoft.sss.storage.SQLiteStorageEntryTest;
import com.wolandsoft.sss.storage.SQLiteStorageListTest;
import com.wolandsoft.sss.util.KeyStoreManagerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

// Runs all unit tests.
@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                KeyStoreManagerTest.class,
                SQLiteStorageEntryTest.class,
                SQLiteStorageListTest.class,
                PlainJsonTest.class
        })

public class UnitTestSuite {
}
