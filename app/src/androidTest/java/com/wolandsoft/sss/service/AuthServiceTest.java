package com.wolandsoft.sss.service;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import com.wolandsoft.sss.db.DatabaseHelper;
import com.wolandsoft.sss.entity.User;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
@RunWith(AndroidJUnit4.class)
public class AuthServiceTest {
    private final static String USER_ID = "alexs20@gmail.com";
    private final static String USER_NAME = "Alexander Shulgin";
    private final static String USER_PWD = "12345678";

    private final static String NOT_USER_PWD = "qwerty";

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Before
    public void cleanDB() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    /**
     * Test service API.
     *
     * @throws TimeoutException
     * @throws AuthInvocationException
     */
    @Test
    public void login_logout_signup_via_AuthService() throws TimeoutException, AuthInvocationException {
        // Create the service Intent.
        Intent serviceIntent =
                new Intent(InstrumentationRegistry.getTargetContext(), AuthService.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder = mServiceRule.bindService(serviceIntent);

        // Get the reference to the service
        AuthService service = ((AuthService.LocalBinder) binder).getService();

        //test user id lookup
        assertTrue(service.isFreeID(USER_ID));

        //add new user to the system
        User user = service.signup(USER_ID, USER_NAME, USER_PWD);
        assertTrue(user != null);
        assertTrue(service.getAuthenticated() != null);
        assertFalse(service.isFreeID(USER_ID));

        service.logout();

        //perform user login
        user = service.login(USER_ID, USER_PWD);
        assertTrue(user != null);

        service.logout();

        //perform user login with bad password
        user = service.login(USER_ID, NOT_USER_PWD);
        assertTrue(user == null);
    }

}
