/*
    Copyright 2016, 2017 Alexander Shulgin

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
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.security.TextCipher;
import com.wolandsoft.sss.util.LogEx;
import com.wolandsoft.sss.util.SQLFormat;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database implementation that stores {@link SecretEntry} objects.
 *
 * @author Alexander Shulgin
 */
public class SQLiteStorage extends ContextWrapper implements IStorage, Closeable {
    private final static String SQL_GET_ID_INSERTED = "SELECT last_insert_rowid()";
    private final static String SQL_GET_ALL = SQLFormat.format(
            "SELECT DISTINCT ENTRY.{id} FROM {secret_entry} AS ENTRY"
                    + " LEFT JOIN {secret_entry_attribute} AS ATTR ON ("
                    + " ATTR.{entry_id} = ENTRY.{id}"
                    + " AND ATTR.{order_id} = ("
                    + " SELECT F1_ATTR.{order_id} FROM {secret_entry_attribute} AS F1_ATTR"
                    + " WHERE F1_ATTR.{entry_id} = ENTRY.{id}"
                    + " AND F1_ATTR.{value} IS NOT NULL"
                    + " ORDER BY F1_ATTR.{order_id} LIMIT 1)"
                    + " )"
                    + "ORDER BY ATTR.{value};",
            "secret_entry, id, secret_entry_attribute, entry_id, order_id, value",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_ID,
            SecretEntryAttributeTable.TBL_NAME,
            SecretEntryAttributeTable.FLD_ENTRY_ID,
            SecretEntryAttributeTable.FLD_ORDER_ID,
            SecretEntryAttributeTable.FLD_VALUE);
    private final static String SQL_SEARCH = SQLFormat.format(
            "SELECT DISTINCT ENTRY.{id} FROM {secret_entry} AS ENTRY"
                    + " INNER JOIN {secret_entry_attribute} AS ATTR ON ("
                    + " ATTR.{entry_id} = ENTRY.{id}"
                    + " AND ATTR.{order_id} = ("
                    + " SELECT F1_ATTR.{order_id} FROM {secret_entry_attribute} AS F1_ATTR"
                    + " WHERE F1_ATTR.{entry_id} = ENTRY.{id}"
                    + " AND F1_ATTR.{value} IS NOT NULL"
                    + " ORDER BY F1_ATTR.{value} LIMIT 1)"
                    + " AND ATTR.{entry_id} = ("
                    + " SELECT F2_ATTR.{entry_id} FROM {secret_entry_attribute} AS F2_ATTR"
                    + " WHERE F2_ATTR.{entry_id} = ENTRY.{id}"
                    + " AND F2_ATTR.{value} IS NOT NULL"
                    + " AND F2_ATTR.{value} LIKE ?)"
                    + " )"
                    + "ORDER BY ATTR.{value};",
            "secret_entry, id, secret_entry_attribute, entry_id, order_id, value",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_ID,
            SecretEntryAttributeTable.TBL_NAME,
            SecretEntryAttributeTable.FLD_ENTRY_ID,
            SecretEntryAttributeTable.FLD_ORDER_ID,
            SecretEntryAttributeTable.FLD_VALUE);
    private final static String SQL_ENTRY_DELETE = SQLFormat.format(
            "DELETE FROM {secret_entry} WHERE {id} = ?;",
            "secret_entry, id",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_ID);
    private final static String SQL_ATTR_DELETE = SQLFormat.format(
            "DELETE FROM {secret_entry_attribute} WHERE {entry_id} = ?;",
            "secret_entry_attribute, entry_id",
            SecretEntryAttributeTable.TBL_NAME,
            SecretEntryAttributeTable.FLD_ENTRY_ID);
    private final static String SQL_ENTRY_UPDATE = SQLFormat.format(
            "UPDATE {secret_entry} SET {updated} = ?"
                    + " WHERE {id} = ?",
            "secret_entry, id, updated",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_ID,
            SecretEntryTable.FLD_UPDATED);
    private final static String SQL_ENTRY_INSERT_WID = SQLFormat.format(
            "INSERT INTO {secret_entry} (rowid, {created}, {updated})"
                    + " VALUES (?,?,?)",
            "secret_entry, created, updated",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_CREATED,
            SecretEntryTable.FLD_UPDATED);
    private final static String SQL_ENTRY_INSERT_WOID = SQLFormat.format(
            "INSERT INTO {secret_entry} ({created}, {updated})"
                    + " VALUES (?,?)",
            "secret_entry, created, updated",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_CREATED,
            SecretEntryTable.FLD_UPDATED);
    private final static String SQL_ATTR_INSERT_WOID = SQLFormat.format(
            "INSERT INTO {secret_entry_attribute} ({entry_id}, {order_id}, {key}, {value}, {protected_value})"
                    + " VALUES (?, ?, ?, ?, ?)",
            "secret_entry_attribute, entry_id, order_id, key, value, protected_value",
            SecretEntryAttributeTable.TBL_NAME,
            SecretEntryAttributeTable.FLD_ENTRY_ID,
            SecretEntryAttributeTable.FLD_ORDER_ID,
            SecretEntryAttributeTable.FLD_KEY,
            SecretEntryAttributeTable.FLD_VALUE,
            SecretEntryAttributeTable.FLD_PROTECTED_VALUE);
    private final static String SQL_ENTRY_SELECT = SQLFormat.format(
            "SELECT * FROM {secret_entry} WHERE {id} = ?;",
            "secret_entry, id",
            SecretEntryTable.TBL_NAME,
            SecretEntryTable.FLD_ID);
    private final static String SQL_ATTR_SELECT = SQLFormat.format(
            "SELECT * FROM {secret_entry_attribute} WHERE {entry_id} = ? ORDER BY {order_id}",
            "secret_entry_attribute, entry_id, order_id",
            SecretEntryAttributeTable.TBL_NAME,
            SecretEntryAttributeTable.FLD_ENTRY_ID,
            SecretEntryAttributeTable.FLD_ORDER_ID);

    private final DatabaseHelper mDBHelper;
    private final LruCache<Integer, SecretEntry> mCache;
    private final TextCipher mCipher;
    private final SQLiteDatabase mRoDb;
    private final SQLiteDatabase mRwDb;

    /**
     * Initialize database and cache.<br/>
     * The cache size determined by {@link R.integer#pref_db_cache_size pref_db_cache_size} property parameter.
     *
     * @param base An application context.
     */
    public SQLiteStorage(Context base) {
        super(base);
        LogEx.d("SQLiteStorage() ", this);
        mDBHelper = new DatabaseHelper(this);
        mRoDb = mDBHelper.getReadableDatabase();
        mRwDb = mDBHelper.getWritableDatabase();
        int cacheSize = getResources().getInteger(R.integer.pref_db_cache_size);
        mCache = new LruCache<>(cacheSize);
        mCipher = new TextCipher();
    }

    /**
     * Preparing this object for disposal.<br/>
     * Once closed this object can not be reused.
     */
    @Override
    public void close() {
        LogEx.d("close() ", this);
        mRoDb.close();
        mRwDb.close();
        mDBHelper.close();
        mCache.evictAll();
    }

    @Override
    public List<Integer> findRecords(@Nullable String criteria) {
        LogEx.d("findRecords( ", criteria, " )");
        ArrayList<Integer> result = new ArrayList<>();
        String sql;
        String[] params;
        if (criteria == null || criteria.trim().length() == 0) {
            sql = SQL_GET_ALL;
            params = new String[0];
        } else {
            sql = SQL_SEARCH;
            params = new String[]{'%' + criteria.trim() + '%'};
        }
        try (Cursor cursor = mRoDb.rawQuery(sql, params)) {
            result.ensureCapacity(cursor.getCount());
            while (cursor.moveToNext()) {
                result.add(cursor.getInt(0));
            }
        }
        return result;
    }

    @Override
    public SecretEntry getRecord(int id) {
        LogEx.d("getRecord( ", id, " )");
        SecretEntry entry = mCache.get(id);
        if (entry != null) {
            return entry;
        }
        entry = getFromDb(id);
        if (entry != null) {
            mCache.put(id, entry);
        }
        return entry;
    }

    private SecretEntry getFromDb(int id) {
        return readEntry(id);
    }

    @Override
    public void deleteRecord(int id) {
        LogEx.d("deleteRecord( ", id, " )");
        mRwDb.beginTransaction();
        try {
            //deleteRecord entry itself
            mRwDb.execSQL(SQL_ENTRY_DELETE, new String[]{String.valueOf(id)});
            //deleteRecord attributes
            mRwDb.execSQL(SQL_ATTR_DELETE, new String[]{String.valueOf(id)});
            mRwDb.setTransactionSuccessful();
        } finally {
            mRwDb.endTransaction();
        }
        mCache.remove(id);
    }

    @Override
    public SecretEntry putRecord(SecretEntry entry) {
        LogEx.d("putRecord( ", entry, " )");
        SecretEntry result = null;
        int id = entry.getID();
        long updated = System.currentTimeMillis();
        mRwDb.beginTransaction();
        try {
            if (id > 0) { //presume that row already exist
                SecretEntry oldEntry = readEntry(id);
                if (oldEntry != null) {
                    //update entry
                    mRwDb.execSQL(SQL_ENTRY_UPDATE, new String[]{String.valueOf(updated), String.valueOf(id)});
                    result = new SecretEntry(id, oldEntry.getCreated(), updated);
                    //deleteRecord old attributes
                    mRwDb.execSQL(SQL_ATTR_DELETE, new String[]{String.valueOf(id)});
                } else { //no row, means inserting with id ... importing
                    String[] args = {String.valueOf(id), String.valueOf(updated), String.valueOf(updated)};
                    mRwDb.execSQL(SQL_ENTRY_INSERT_WID, args);
                    result = new SecretEntry(id, updated, updated);
                }
            } else {
                String[] args = {String.valueOf(updated), String.valueOf(updated)};
                mRwDb.execSQL(SQL_ENTRY_INSERT_WOID, args);
                try (Cursor cursor = mRwDb.rawQuery(SQL_GET_ID_INSERTED, null)) {
                    cursor.moveToFirst();
                    result = new SecretEntry(cursor.getInt(0), updated, updated);
                }
            }
            for (int i = 0; i < entry.size(); i++) {
                SecretEntryAttribute attr = entry.get(i);
                String[] args = new String[]{
                        String.valueOf(result.getID()),
                        String.valueOf(i),
                        attr.getKey(),
                        attr.isProtected() ? null : attr.getValue(),
                        attr.isProtected() ? mCipher.cipher(attr.getValue()) : null};
                mRwDb.execSQL(SQL_ATTR_INSERT_WOID, args);
                result.add(attr);
            }
            mRwDb.setTransactionSuccessful();
            mCache.put(result.getID(), result);
        } finally {
            mRwDb.endTransaction();
        }
        return result;
    }

    private SecretEntry readEntry(int id) {
        SecretEntry entry = null;
        String[] args = {String.valueOf(id)};
        try (Cursor cursor = mRoDb.rawQuery(SQL_ENTRY_SELECT, args)) {
            if (cursor.moveToFirst()) {
                entry = new SecretEntry(id,
                        cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_CREATED)),
                        cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UPDATED)));
                entry = readEntryAttributes(entry);
            }
        }
        return entry;
    }

    private SecretEntry readEntryAttributes(SecretEntry entry) {
        if (entry != null) {
            String[] args = {String.valueOf(entry.getID())};
            try (Cursor cursor = mRoDb.rawQuery(SQL_ATTR_SELECT, args)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_KEY));
                    String value = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_VALUE));
                    boolean isProtected = value == null;
                    if (isProtected) {
                        value = mCipher.decipher(
                                cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_PROTECTED_VALUE)));
                    }
                    SecretEntryAttribute attr = new SecretEntryAttribute(key, value, isProtected);
                    entry.add(attr);
                }
            }
        }
        return entry;
    }
}
