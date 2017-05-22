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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database implementation that stores {@link SecretEntry} objects.
 *
 * @author Alexander Shulgin
 */
public class SQLiteStorage extends ContextWrapper implements IStorage, Closeable {
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
        sb.append(" ORDER BY R.").append(SecretEntryAttributeTable.FLD_VALUE);
        try (Cursor cursor = mRoDb.rawQuery(sb.toString(), args.toArray(new String[0]))) {
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
        StringBuilder sb = new StringBuilder();
        mRwDb.beginTransaction();
        try {
            //deleteRecord entry itself
            sb.append("DELETE FROM ").append(SecretEntryTable.TBL_NAME)
                    .append(" WHERE ").append(SecretEntryTable.FLD_ID).append("=?");
            mRwDb.execSQL(sb.toString(), new String[]{String.valueOf(id)});
            //deleteRecord attributes
            sb.setLength(0);
            sb.append("DELETE FROM ").append(SecretEntryAttributeTable.TBL_NAME)
                    .append(" WHERE ").append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=?");
            mRwDb.execSQL(sb.toString(), new String[]{String.valueOf(id)});
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
        StringBuilder sb = new StringBuilder();
        long updated = System.currentTimeMillis();
        mRwDb.beginTransaction();
        try {
            if (id > 0) { //presume that row already exist
                SecretEntry oldEntry = readEntry(id);
                if (oldEntry != null) {
                    //update entry
                    sb.append("UPDATE ").append(SecretEntryTable.TBL_NAME).append(" SET ")
                            .append(SecretEntryTable.FLD_UPDATED).append("=? WHERE ")
                            .append(SecretEntryTable.FLD_ID).append("=?");
                    mRwDb.execSQL(sb.toString(), new String[]{String.valueOf(updated), String.valueOf(id)});
                    result = new SecretEntry(id, oldEntry.getCreated(), updated);
                    mRwDb.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                    //deleteRecord old attributes
                    sb.setLength(0);
                    sb.append("DELETE FROM ").append(SecretEntryAttributeTable.TBL_NAME).append(" WHERE ")
                            .append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=?");
                    mRwDb.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                } else { //no row, means inserting with id ... importing
                    sb.append("INSERT INTO ").append(SecretEntryTable.TBL_NAME).append(" (rowid,")
                            .append(SecretEntryTable.FLD_CREATED).append(",")
                            .append(SecretEntryTable.FLD_UPDATED).append(") VALUES (?,?,?)");
                    String[] args = {String.valueOf(id), String.valueOf(updated), String.valueOf(updated)};
                    mRwDb.execSQL(sb.toString(), args);
                    result = new SecretEntry(id, updated, updated);
                }
            } else {
                sb.append("INSERT INTO ").append(SecretEntryTable.TBL_NAME).append(" ( ")
                        .append(SecretEntryTable.FLD_CREATED).append(", ")
                        .append(SecretEntryTable.FLD_UPDATED).append(" ) VALUES ( ?, ?)");
                String[] args = {String.valueOf(updated), String.valueOf(updated)};
                mRwDb.execSQL(sb.toString(), args);
                try (Cursor cursor = mRwDb.rawQuery("SELECT last_insert_rowid()", null)) {
                    cursor.moveToFirst();
                    result = new SecretEntry(cursor.getInt(0), updated, updated);
                }
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
                        attr.isProtected() ? mCipher.cipher(attr.getValue()) : null};
                mRwDb.execSQL(sql, args);
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
        @SuppressWarnings("StringBufferReplaceableByString") StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(SecretEntryTable.TBL_NAME).append(" WHERE ")
                .append(SecretEntryTable.FLD_ID).append("=?");
        String[] args = {String.valueOf(id)};
        try (Cursor cursor = mRoDb.rawQuery(sb.toString(), args)) {
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
            @SuppressWarnings("StringBufferReplaceableByString") StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM ").append(SecretEntryAttributeTable.TBL_NAME).append(" WHERE ")
                    .append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=? ")
                    .append(" ORDER BY ").append(SecretEntryAttributeTable.FLD_ORDER_ID);
            String[] args = {String.valueOf(entry.getID())};
            try (Cursor cursor = mRoDb.rawQuery(sb.toString(), args)) {
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
