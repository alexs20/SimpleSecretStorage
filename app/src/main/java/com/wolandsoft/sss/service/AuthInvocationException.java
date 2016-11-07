package com.wolandsoft.sss.service;

import android.os.RemoteException;

/**
 * Used by report communication problem with {@link AuthService}
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AuthInvocationException extends RemoteException {

    public AuthInvocationException() {
        super();
    }

    public AuthInvocationException(String message) {
        super(message);
    }
}
