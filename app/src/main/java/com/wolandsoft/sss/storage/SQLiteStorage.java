package com.wolandsoft.sss.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */

public class SQLiteStorage extends ContextWrapper implements Closeable {
    private DatabaseHelper dbHelper;

    public SQLiteStorage(Context base) throws StorageException {
        super(base);
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public synchronized void close() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    public synchronized int count(String criteria) throws StorageException {
        if (dbHelper == null) {
            return 0;
        }
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            List<String> args = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT COUNT(DISTINCT ").append(SecretEntryTable.FLD_ID).append(") FROM ").append(SecretEntryTable.TBL_NAME);
            if (criteria != null) {
                criteria = criteria.trim();
                if (criteria.length() > 0) {
                    String[] keywords = criteria.split("\\s");
                    if (keywords.length > 0) {
                        sb.append(" INNER JOIN ").append(SecretEntryAttributeTable.TBL_NAME)
                                .append(" AS F ON (")
                                .append(SecretEntryTable.FLD_ID).append("=F.").append(SecretEntryAttributeTable.FLD_ENTRY_ID)
                                .append(" AND ")
                                .append("F.").append(SecretEntryAttributeTable.FLD_PROTECTED).append("=0)");
                        boolean isWhere = true;
                        for (String keyword : keywords) {
                            if (isWhere) {
                                isWhere = false;
                                sb.append(" WHERE F.");
                            } else {
                                sb.append(" OR F.");
                            }
                            sb.append(SecretEntryAttributeTable.FLD_VALUE).append(" LIKE ?");
                            args.add("%" + keyword + "%");
                        }
                    }
                }
            }
            try (Cursor cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]))) {
                if (cursor.moveToNext()) {
                    return cursor.getInt(0);
                }
            }
        }
        return 0;
    }

    public synchronized List<SecretEntry> find(String criteria, boolean isASC, int offset, int limit) throws StorageException {
        return find(criteria, isASC, offset, limit, false);
    }

    public synchronized List<SecretEntry> find(String criteria, boolean isASC, int offset, int limit, boolean isHeaderOnly) throws StorageException {
        List<SecretEntry> result = new ArrayList<>();
        if (dbHelper == null) {
            return result;
        }
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            List<String> args = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ").append(SecretEntryTable.FLD_ID).append(",")
                    .append(SecretEntryTable.FLD_CREATED).append(",")
                    .append(SecretEntryTable.FLD_UPDATED).append(" FROM ")
                    .append(SecretEntryTable.TBL_NAME)
                    .append(" INNER JOIN ").append(SecretEntryAttributeTable.TBL_NAME)
                    .append(" AS A ON (")
                    .append(SecretEntryTable.FLD_ID).append("=A.").append(SecretEntryAttributeTable.FLD_ENTRY_ID)
                    .append(" AND ")
                    .append("A.").append(SecretEntryAttributeTable.FLD_ORDER_ID).append("=0)");
            if (criteria != null) {
                String[] keywords = criteria.split("\\s");
                if (keywords.length > 0) {
                    sb.append(" INNER JOIN ").append(SecretEntryAttributeTable.TBL_NAME)
                            .append(" AS F ON (")
                            .append(SecretEntryTable.FLD_ID).append("=F.").append(SecretEntryAttributeTable.FLD_ENTRY_ID)
                            .append(" AND ")
                            .append("F.").append(SecretEntryAttributeTable.FLD_PROTECTED).append("=0)");
                    boolean isWhere = true;
                    for (String keyword : keywords) {
                        if (isWhere) {
                            isWhere = false;
                            sb.append(" WHERE F.");
                        } else {
                            sb.append(" OR F.");
                        }
                        sb.append(SecretEntryAttributeTable.FLD_VALUE).append(" LIKE ?");
                        args.add("%" + keyword + "%");
                    }
                }
            }
            sb.append(" GROUP BY ").append(SecretEntryTable.FLD_ID);
            sb.append(" ORDER BY A.").append(SecretEntryAttributeTable.FLD_VALUE).append(isASC ? " ASC" : " DESC");
            sb.append(" LIMIT ").append(offset).append(",").append(limit);
            try (Cursor cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]))) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_ID));
                    long created = cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_CREATED));
                    long updated = cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UPDATED));
                    SecretEntry entry = new SecretEntry(id, created, updated);
                    if (!isHeaderOnly) {
                        result.add(readEntryAttributes(entry, db));
                    }
                }
            }
        }
        return result;
    }

    public SecretEntry get(long id) throws StorageException {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            return readEntry(id, db);
        }
    }

    public void delete(long id) throws StorageException {
        SecretEntry result = null;
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            try {
                StringBuilder sb = new StringBuilder();
                db.beginTransaction();
                sb.append("DELETE FROM ").append(SecretEntryTable.TBL_NAME)
                        .append(" WHERE ").append(SecretEntryTable.FLD_ID).append("=?");
                db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                sb.append("DELETE FROM ").append(SecretEntryAttributeTable.TBL_NAME)
                        .append(" WHERE ").append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=?");
                db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
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
        long id = entry.getID();
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            try {
                StringBuilder sb = new StringBuilder();
                long updated = System.currentTimeMillis();
                db.beginTransaction();
                if (id > 0) { //presuming that row already exist
                    SecretEntry oldEntry = readEntry(id, db);
                    if (oldEntry == null) {
                        throw new StorageException(String.format(getString(R.string.exception_record_not_found), id));
                    }
                    sb.append("UPDATE ").append(SecretEntryTable.TBL_NAME).append(" SET ")
                            .append(SecretEntryTable.FLD_UPDATED).append("=? WHERE ")
                            .append(SecretEntryTable.FLD_ID).append("=?");
                    db.execSQL(sb.toString(), new String[]{String.valueOf(updated), String.valueOf(id)});
                    result = new SecretEntry(id, oldEntry.getCreated(), updated);
                    sb.setLength(0);
                    sb.append("DELETE FROM ").append(SecretEntryAttributeTable.TBL_NAME).append(" WHERE ")
                            .append(SecretEntryAttributeTable.FLD_ENTRY_ID).append("=?");
                    db.execSQL(sb.toString(), new String[]{String.valueOf(id)});
                } else {
                    sb.append("INSERT INTO ").append(SecretEntryTable.TBL_NAME).append(" ( ")
                            .append(SecretEntryTable.FLD_CREATED).append(", ")
                            .append(SecretEntryTable.FLD_UPDATED).append(" ) VALUES ( ?, ?)");
                    String[] args = {String.valueOf(updated), String.valueOf(updated)};
                    db.execSQL(sb.toString(), args);
                    try (Cursor cursor = db.rawQuery("SELECT last_insert_rowid()", null)) {
                        if (cursor.moveToFirst()) {
                            result = new SecretEntry(cursor.getLong(0), updated, updated);
                        }
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
                        .append(SecretEntryAttributeTable.FLD_PROTECTED).append(" ) VALUES ( ?, ?, ?, ?, ?)");
                for (int i = 0; i < entry.size(); i++) {
                    SecretEntryAttribute attr = entry.get(i);
                    String[] args = new String[]{String.valueOf(result.getID()), String.valueOf(i),
                            attr.getKey(), attr.getValue(), String.valueOf(attr.isProtected() ? 1 : 0)};
                    db.execSQL(sb.toString(), args);
                    result.add(attr);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
        return result;
    }

    private SecretEntry readEntry(long id, SQLiteDatabase db) throws StorageException {
        SecretEntry entry = null;
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(SecretEntryTable.TBL_NAME).append(" WHERE ")
                .append(SecretEntryTable.FLD_ID).append("=?");
        String[] args = {String.valueOf(id)};

        try (Cursor cursor = db.rawQuery(sb.toString(), args)) {
            if (cursor.moveToFirst()) {
                entry = new SecretEntry(id,
                        cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_CREATED)),
                        cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UPDATED)));
                entry = readEntryAttributes(entry, db);
            }
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

            try (Cursor cursor = db.rawQuery(sb.toString(), args)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_KEY));
                    String value = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_VALUE));
                    boolean isProtected = cursor.getInt(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_PROTECTED)) != 0;
                    SecretEntryAttribute attr = new SecretEntryAttribute(key, value, isProtected);
                    entry.add(attr);
                }
            } catch (Exception e) {
                throw new StorageException(e.getMessage(), e);
            }
        }
        return entry;
    }
}
