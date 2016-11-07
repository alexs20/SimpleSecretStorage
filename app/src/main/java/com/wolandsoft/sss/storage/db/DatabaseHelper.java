package com.wolandsoft.sss.storage.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.wolandsoft.sss.AppConstants.APP_SHORT_NAME;

/**
 * DB helper with automatic database creation.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = APP_SHORT_NAME;
    public static final int DATABASE_VERSION = 1;
    public static final ATableDefinition[] TABLES = {
            new MasterPasswordTable(),
            new SecretEntryTable(),
            new SecretEntryAttributeTable()
    };

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        for (ATableDefinition tableDef : TABLES) {
            for (String sql : tableDef.getCreateSQL()) {
                db.execSQL(sql);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int v = oldVersion + 1; v <= newVersion; v++) {
            for (ATableDefinition tableDef : TABLES) {
                for (String sql : tableDef.getUpdateSQLs(v)) {
                    db.execSQL(sql);
                }
            }
        }
    }
}
