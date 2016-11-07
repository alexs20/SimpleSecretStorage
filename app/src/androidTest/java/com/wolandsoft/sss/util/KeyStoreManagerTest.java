package com.wolandsoft.sss.util;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
@RunWith(Parameterized.class)
//@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KeyStoreManagerTest {
    private final static String TEST_ALIAS = "test_alias";
    private static KeyStoreManager ksMgr;
    private String pwd;

    public KeyStoreManagerTest(String pwd) {
        this.pwd = pwd;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"12345"}, {"98765"}
        });
    }

    @BeforeClass
    public static void setup() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        ksMgr = new KeyStoreManager(context, TEST_ALIAS);
    }

    @AfterClass
    public static void clean() throws Exception {
        ksMgr.deleteKey();
    }

    @Test
    public void test_encrypt_decrupt() throws BadPaddingException, IllegalBlockSizeException {
        String encrypted = ksMgr.encrypt(pwd);
        assertNotEquals(encrypted, pwd);
        String decrypted = ksMgr.decrupt(encrypted);
        assertEquals(decrypted, pwd);
    }
}
