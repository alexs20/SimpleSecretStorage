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
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.LruCache;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.LogEx;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Sqlite database implementation of secret entry storage.
 *
 * @author Alexander Shulgin
 */
public class SQLiteStorage extends ContextWrapper implements Closeable {
    private DatabaseHelper dbHelper;
    private LruCache<Integer, SecretEntry> mCache;

    public SQLiteStorage(Context base) throws StorageException {
        super(base);
        dbHelper = new DatabaseHelper(this);
        int cacheSize = getResources().getInteger(R.integer.pref_entries_cache_size);
        mCache = new LruCache<>(cacheSize);
    }

    @Override
    public synchronized void close() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
        if (mCache != null) {
            mCache = null;
        }
    }

    public synchronized List<Integer> find(String criteria, boolean isASC) throws StorageException {
        if (dbHelper == null) {
            return new ArrayList<>();
        }
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            String[] keywords = null;
            if (criteria != null) {
                keywords = criteria.split("\\s");
            }
            List<String> args = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ").append(keywords != null && keywords.length > 0 ? "DISTINCT " : "")
                    .append("R.").append(SecretEntryAttributeTable.FLD_ENTRY_ID).append(" FROM ")
                    .append(SecretEntryAttributeTable.TBL_NAME).append(" AS R");
            if (keywords != null && keywords.length > 0) {
                sb.append(" INNER JOIN ").append(SecretEntryAttributeTable.TBL_NAME)
                        .append(" AS F ON (R.").append(SecretEntryAttributeTable.FLD_ENTRY_ID)
                        .append("=F.").append(SecretEntryAttributeTable.FLD_ENTRY_ID)
                        .append(" AND  F.").append(SecretEntryAttributeTable.FLD_VALUE).append(" IS NOT NULL AND(");
                boolean isNone = true;
                for (String keyword : keywords) {
                    if (isNone) {
                        isNone = false;
                        sb.append(" F.");
                    } else {
                        sb.append(" OR F.");
                    }
                    sb.append(SecretEntryAttributeTable.FLD_VALUE).append(" LIKE ?");
                    args.add("%" + keyword + "%");
                }
                sb.append("))");
            }
            sb.append(" WHERE R.").append(SecretEntryAttributeTable.FLD_ORDER_ID).append("=0");
            sb.append(" ORDER BY R.").append(SecretEntryAttributeTable.FLD_VALUE).append(isASC ? " ASC" : " DESC");
            cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]));
            ArrayList<Integer> result = new ArrayList<>();
            result.ensureCapacity(cursor.getCount());
            while (cursor.moveToNext()) {
                result.add(cursor.getInt(0));
            }
            return result;
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
    }

    public SecretEntry get(int id) throws StorageException {
        return get(id, null);
    }

    /**
     * Get {@link SecretEntry} by ID.</br>
     * Two modes:
     * 1) async is {@code null} - {@link SecretEntry} instance will be fetched in blocking mode.
     * 2) async in not {@code null} - {@link SecretEntry} instance will be returned from the cache
     * and if not found then {@code null} returned and later {@link OnSecretEntryRetrieveListener} invoked.
     *
     * @param id
     * @param async
     * @return instance of {@link SecretEntry} or {@code null}
     * @throws StorageException
     */
    public SecretEntry get(int id, final OnSecretEntryRetrieveListener async) throws StorageException {
        SecretEntry entry = mCache.get(id);
        if (entry != null) {
            return entry;
        }
        if (async == null) {
            entry = getFromDb(id);
            if (entry != null) {
                mCache.put(id, entry);
            }
            return entry;
        } else {
            new AsyncTask<Integer, Void, SecretEntry>() {
                @Override
                protected SecretEntry doInBackground(Integer... params) {
                    try {
                        int id = params[0];
                        SecretEntry entry = getFromDb(id);
                        if (entry != null) {
                            mCache.put(id, entry);
                            return entry;
                        }
                    } catch (StorageException e) {
                        LogEx.e(e.getMessage(), e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(SecretEntry entry) {
                    if (entry != null) {
                        async.onSecretEntryRetrieved(entry);
                    }
                }
            }.execute(id);
            return null;
        }
    }

    private SecretEntry getFromDb(int id) throws StorageException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getReadableDatabase();
            return readEntry(id, db);
        } finally {
            if (db != null)
                db.close();
        }
    }

    public void delete(int id) throws StorageException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            try {
                StringBuilder sb = new StringBuilder();
                db.beginTransaction();
                //delete entry itself
                sb.append("DELETE FROM ").append(SecretEntryTable.TBL_NAME)
                        .append(" WHERE ").append(SecretEntryTable.FLD_ID).append("=?");
                db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                //delete attributes
                sb.setLength(0);
                sb.append("DELETE FROM ").append(SecretEntryAttributeTable.TBL_NAME)
                        .append(" WHERE ").append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=?");
                db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        } finally {
            if (db != null)
                db.close();
            mCache.remove(id);
        }
    }

    /**
     * Store entry in db.
     *
     * @param entry entry object to store
     * @return updated entry where {@link SecretEntry#getID()} and {@link SecretEntry#getCreated()}
     * values are assigned for new entries and {@link SecretEntry#getUpdated()} value updated.
     * @throws StorageException
     */
    public SecretEntry put(SecretEntry entry) throws StorageException {
        SecretEntry result = null;
        int id = entry.getID();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();
            try {
                StringBuilder sb = new StringBuilder();
                long updated = System.currentTimeMillis();
                db.beginTransaction();
                if (id > 0) { //presume that row already exist
                    SecretEntry oldEntry = readEntry(id, db);
                    if (oldEntry != null) {
                        //update entry
                        sb.append("UPDATE ").append(SecretEntryTable.TBL_NAME).append(" SET ")
                                .append(SecretEntryTable.FLD_UPDATED).append("=? WHERE ")
                                .append(SecretEntryTable.FLD_ID).append("=?");
                        db.execSQL(sb.toString(), new String[]{String.valueOf(updated), String.valueOf(id)});
                        result = new SecretEntry(id, oldEntry.getCreated(), updated);
                        db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                        //delete old attributes
                        sb.setLength(0);
                        sb.append("DELETE FROM ").append(SecretEntryAttributeTable.TBL_NAME).append(" WHERE ")
                                .append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=?");
                        db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                    } else { //no row, means inserting with id ... importing
                        sb.append("INSERT INTO ").append(SecretEntryTable.TBL_NAME).append(" (rowid,")
                                .append(SecretEntryTable.FLD_CREATED).append(",")
                                .append(SecretEntryTable.FLD_UPDATED).append(") VALUES (?,?,?)");
                        String[] args = {String.valueOf(id), String.valueOf(updated), String.valueOf(updated)};
                        db.execSQL(sb.toString(), args);
                        result = new SecretEntry(id, updated, updated);
                    }
                } else {
                    sb.append("INSERT INTO ").append(SecretEntryTable.TBL_NAME).append(" ( ")
                            .append(SecretEntryTable.FLD_CREATED).append(", ")
                            .append(SecretEntryTable.FLD_UPDATED).append(" ) VALUES ( ?, ?)");
                    String[] args = {String.valueOf(updated), String.valueOf(updated)};
                    db.execSQL(sb.toString(), args);
                    cursor = db.rawQuery("SELECT last_insert_rowid()", null);
                    if (cursor.moveToFirst()) {
                        result = new SecretEntry(cursor.getInt(0), updated, updated);
                    }
                }
                if (result == null) {
                    throw new StorageException(getString(R.string.exception_internal));
                }
                sb.setLength(0);
                sb.append("INSERT INTO ").append(SecretEntryAttributeTable.TBL_NAME).append(" ( ")
                        .append(SecretEntryAttributeTable.FLD_ENTRY_ID).append(", ")
                        .append(SecretEntryAttributeTable.FLD_ORDER_ID).append(", ")
                        .append(SecretEntryAttributeTable.FLD_KEY).append(", ")
                        .append(SecretEntryAttributeTable.FLD_VALUE).append(", ")
                        .append(SecretEntryAttributeTable.FLD_PROTECTED_VALUE).append(" ) VALUES ( ?, ?, ?, ?, ?)");
                String sql = sb.toString();

                for (int i = 0; i < entry.size(); i++) {
                    SecretEntryAttribute attr = entry.get(i);
                    String[] args = new String[]{
                            String.valueOf(result.getID()),
                            String.valueOf(i),
                            attr.getKey(),
                            attr.isProtected() ? null : attr.getValue(),
                            attr.isProtected() ? attr.getValue() : null};
                    db.execSQL(sql, args);
                    result.add(attr);
                }
                db.setTransactionSuccessful();
                mCache.put(result.getID(), result);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        } finally {
            if (cursor != null)
                cursor.close();
            if (db != null)
                db.close();
        }
        return result;
    }

    private SecretEntry readEntry(int id, SQLiteDatabase db) throws StorageException {
        SecretEntry entry = null;
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(SecretEntryTable.TBL_NAME).append(" WHERE ")
                .append(SecretEntryTable.FLD_ID).append("=?");
        String[] args = {String.valueOf(id)};
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sb.toString(), args);
            if (cursor.moveToFirst()) {
                entry = new SecretEntry(id,
                        cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_CREATED)),
                        cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UPDATED)));
                entry = readEntryAttributes(entry, db);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return entry;
    }

    private SecretEntry readEntryAttributes(SecretEntry entry, SQLiteDatabase db) throws StorageException {
        if (entry != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM ").append(SecretEntryAttributeTable.TBL_NAME).append(" WHERE ")
                    .append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=? ")
                    .append(" ORDER BY ").append(SecretEntryAttributeTable.FLD_ORDER_ID);
            String[] args = {String.valueOf(entry.getID())};
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sb.toString(), args);
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_KEY));
                    String value = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_VALUE));
                    boolean isProtected = value == null;
                    if (isProtected) {
                        value = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_PROTECTED_VALUE));
                    }
                    SecretEntryAttribute attr = new SecretEntryAttribute(key, value, isProtected);
                    entry.add(attr);
                }
            } catch (Exception e) {
                throw new StorageException(e.getMessage(), e);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return entry;
    }

    public interface OnSecretEntryRetrieveListener {
        void onSecretEntryRetrieved(SecretEntry entry);
    }
}
