package com.wolandsoft.sss.storage;

/**
 * Created by Alexander on 10/15/2016.
 */

public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
