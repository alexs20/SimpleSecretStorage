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

    public synchronized int[] find(String criteria, boolean isASC) throws StorageException {
        if (dbHelper == null) {
            return new int[0];
        }
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
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
            try (Cursor cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]))) {
                int[] result = new int[cursor.getCount()];
                while (cursor.moveToNext()) {
                    result[cursor.getPosition()] = cursor.getInt(0);
                }
                return result;
            }
        }
    }

    public SecretEntry get(int id) throws StorageException {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            return readEntry(id, db);
        }
    }

    public void delete(long id) throws StorageException {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
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
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
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
                    try (Cursor cursor = db.rawQuery("SELECT last_insert_rowid()", null)) {
                        if (cursor.moveToFirst()) {
                            result = new SecretEntry(cursor.getInt(0), updated, updated);
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
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
        return result;
    }

    private SecretEntry readEntry(int id, SQLiteDatabase db) throws StorageException {
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
                    boolean isProtected = value == null;
                    if (isProtected) {
                        value = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_PROTECTED_VALUE));
                    }
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
