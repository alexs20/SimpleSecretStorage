package com.wolandsoft.sss.entity;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import com.wolandsoft.sss.db.DatabaseHelper;
import com.wolandsoft.sss.db.UserTable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static junit.framework.Assert.assertTrue;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
@RunWith(AndroidJUnit4.class)
public class UserTest implements UserTable {

    private final static String USER_ID = "alexs20@gmail.com";
    private final static String USER_NAME = "Alexander Shulgin";
    private final static String USER_PWD = "12345678";

    private final static String NOT_USER_ID = "email@example.com";
    private final static String NOT_USER_PWD = "qwerty";

    @Before
    public void cleanDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    /**
     * Test user entity creation and retrieval
     *
     * @throws IOException
     */
    @Test
    public void add_lookup_User_entity() throws IOException {
        Context context = InstrumentationRegistry.getTargetContext();
        //add user entity to the DB
        User user = User.getModel(context).add(USER_ID, USER_NAME, USER_PWD);
        assertTrue(user != null);
        assertTrue(user.getID() == USER_ID);
        assertTrue(user.getDisplayName() == USER_NAME);
        //retrieve user entity from the DB by ID/email
        user = User.getModel(context).find(USER_ID);
        assertTrue(user != null);
        assertTrue(user.getID().equals(USER_ID));
        assertTrue(user.getDisplayName().equals(USER_NAME));
        //try to retrieve non-existent user entity from the DB by ID/email
        user = User.getModel(context).find(NOT_USER_ID);
        assertTrue(user == null);
        //retrieve user entity from the DB by ID/email and password
        user = User.getModel(context).find(USER_ID, USER_PWD);
        assertTrue(user != null);
        assertTrue(user.getID().equals(USER_ID));
        assertTrue(user.getDisplayName().equals(USER_NAME));
        //retrieve user entity from the DB by ID/email and invalid password
        user = User.getModel(context).find(USER_ID, NOT_USER_PWD);
        assertTrue(user == null);
    }

}
