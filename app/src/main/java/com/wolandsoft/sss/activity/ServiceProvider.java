package com.wolandsoft.sss.activity;

import com.wolandsoft.sss.service.AuthService;

/**
 * Interface that host activity should implement in order to provide access to the service API
 * in fragment classes.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public interface ServiceProvider {

    AuthService getService();
}
