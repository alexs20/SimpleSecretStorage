/*
    Copyright 2016 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.wolandsoft.sss.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.wolandsoft.sss.AppConstants.APP_SHORT_NAME;

/**
 * DB helper with automatic database creation.
 *
 * @author Alexander Shulgin
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = APP_SHORT_NAME;
    private static final int DATABASE_VERSION = 1;
    private static final ATableDefinition[] TABLES = {
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
