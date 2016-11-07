package com.wolandsoft.sss.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DB helper with automatic database creation.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class DatabaseHelper extends SQLiteOpenHelper implements UserTable {

    public static final String DATABASE_NAME = "I2XDB";
    public static final int DATABASE_VERSION = 1;
    //Create database SQL
    private static final String DATABASE_CREATE =
            "CREATE TABLE " + DATABASE_TABLE + " (" +
                    KEY_EMAIL + " TEXT PRIMARY KEY," +
                    KEY_NAME + " TEXT," +
                    KEY_PWDH + " TEXT );";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
