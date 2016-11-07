package com.wolandsoft.sss.external;

/**
 * Created by Alexander on 10/15/2016.
 */

public class ExternalException extends Exception {
    public ExternalException(String message) {
        super(message);
    }

    public ExternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
