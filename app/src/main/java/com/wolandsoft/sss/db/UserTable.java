package com.wolandsoft.sss.db;

/**
 * Definition of User's entity table and field names.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public interface UserTable {
    /**
     * User entity table name
     */
    String DATABASE_TABLE = "user";
    /**
     * User entity fields
     */
    String KEY_EMAIL = "email";
    String KEY_NAME = "name";
    String KEY_PWDH = "pwd_hash";
    //IStorage s;
}
