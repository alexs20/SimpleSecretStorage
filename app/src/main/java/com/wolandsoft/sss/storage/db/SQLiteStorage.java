package com.wolandsoft.sss.storage.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.storage.AStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.KeyStoreManager;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 10/15/2016.
 */

public class SQLiteStorage extends AStorage {
    private DatabaseHelper dbHelper;
    private KeyStoreManager keyStore;
    private boolean isActive = false;

    public SQLiteStorage(Context base) {
        super(base);
    }

    @Override
    public void startup(String password) throws StorageException {
        dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(MasterPasswordTable.TBL_NAME, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                if (password == null) {
                    shutdown();
                    throw new StorageException(getString(R.string.exception_invalid_password));
                }
                String storedHash = cursor.getString(cursor.getColumnIndex(MasterPasswordTable.FLD_PASSWORD));
                String inputHash = hash(password);
                if (!inputHash.equals(storedHash)) {
                    shutdown();
                    throw new StorageException(getString(R.string.exception_invalid_password));
                }
            } else {
                if (password != null) {
                    shutdown();
                    throw new StorageException(getString(R.string.exception_password_not_set));
                }
            }
            keyStore = new KeyStoreManager(this);
        } catch (Exception e) {
            shutdown();
            throw new StorageException(e.getMessage(), e);
        } finally {
            db.close();
        }
        isActive = true;
    }

    @Override
    public void shutdown() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
        isActive = false;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public List<SecretEntry> find(String criteria, boolean isASC, int offset, int limit) throws StorageException {
        //sqlite> explain select e.*, a.* from secret_entry as e
        //...> inner join secret_entry_attribute as a on (
        //...> e.uuid_msb=a.entry_uuid_msb and e.uuid_lsb=a.entry_uuid_lsb and a.key='Name')
        //...> inner join secret_entry_attribute as f on (
        //...> e.uuid_msb=f.entry_uuid_msb and e.uuid_lsb=f.entry_uuid_lsb and f.protected=0)
        //...> where f.value like '%example%7.%'
        //...> order by a.value
        //...> ;

        List<SecretEntry> result = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            List<String> args = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ").append(SecretEntryTable.FLD_UUID_MSB).append(",")
                    .append(SecretEntryTable.FLD_UUID_LSB).append(",")
                    .append(SecretEntryTable.FLD_CREATED).append(",")
                    .append(SecretEntryTable.FLD_UPDATED).append(" FROM ")
                    .append(SecretEntryTable.TBL_NAME)
                    .append(" INNER JOIN ").append(SecretEntryAttributeTable.TBL_NAME)
                    .append(" AS A ON (")
                    .append(SecretEntryTable.FLD_UUID_MSB).append("=A.").append(SecretEntryAttributeTable.FLD_ENTRY_UUID_MSB)
                    .append(" AND ")
                    .append(SecretEntryTable.FLD_UUID_LSB).append("=A.").append(SecretEntryAttributeTable.FLD_ENTRY_UUID_LSB)
                    .append(" AND ")
                    .append("A.").append(SecretEntryAttributeTable.FLD_ORDER_ID).append("=0)");
            if (criteria != null) {
                String[] keywords = criteria != null ? criteria.split("\\s") : null;
                if (keywords != null && keywords.length > 0) {
                    sb.append(" INNER JOIN ").append(SecretEntryAttributeTable.TBL_NAME)
                            .append(" AS F ON (")
                            .append(SecretEntryTable.FLD_UUID_MSB).append("=F.").append(SecretEntryAttributeTable.FLD_ENTRY_UUID_MSB)
                            .append(" AND ")
                            .append(SecretEntryTable.FLD_UUID_LSB).append("=F.").append(SecretEntryAttributeTable.FLD_ENTRY_UUID_LSB)
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
            sb.append(" ORDER BY A.").append(SecretEntryAttributeTable.FLD_VALUE).append(isASC ? " ASC" : " DESC")
                    .append(" LIMIT ").append(offset).append(",").append(limit);
            try (Cursor cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]))) {
                while (cursor.moveToNext()) {
                    UUID uuid = new UUID(cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UUID_MSB)),
                            cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UUID_LSB)));
                    SecretEntry entry = new SecretEntry(uuid);
                    entry.setCreated(cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_CREATED)));
                    entry.setUpdated(cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UPDATED)));
                    result.add(readEntryAttributes(entry, db));
                }
            }
        }
        return result;
    }

    @Override
    public SecretEntry get(UUID id) throws StorageException {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            return readEntry(id, db);
        }
    }

    @Override
    public SecretEntry put(SecretEntry entry) throws StorageException {
        if (!isActive()) {
            throw new StorageException(getString(R.string.exception_storage_not_ready));
        }
        SecretEntry oldEntry = null;
        long uuid_msb = entry.getID().getMostSignificantBits();
        long uuid_lsb = entry.getID().getLeastSignificantBits();
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            try {
                db.beginTransaction();
                oldEntry = readEntry(entry.getID(), db);
                long updated = System.currentTimeMillis();
                long created = oldEntry == null ? updated : oldEntry.getCreated();
                //delete old data
                String sql = "DELETE FROM " + SecretEntryTable.TBL_NAME + " WHERE " +
                        SecretEntryTable.FLD_UUID_MSB + " = ? AND " +
                        SecretEntryTable.FLD_UUID_LSB + " = ?";
                String[] args = {String.valueOf(uuid_msb), String.valueOf(uuid_lsb)};
                db.execSQL(sql, args);
                sql = "DELETE FROM " + SecretEntryAttributeTable.TBL_NAME + " WHERE " +
                        SecretEntryAttributeTable.FLD_ENTRY_UUID_MSB + " = ? AND " +
                        SecretEntryAttributeTable.FLD_ENTRY_UUID_LSB + " = ?";
                db.execSQL(sql, args);
                //insert new
                sql = "INSERT INTO " + SecretEntryTable.TBL_NAME + " ( " +
                        SecretEntryTable.FLD_UUID_MSB + ", " +
                        SecretEntryTable.FLD_UUID_LSB + ", " +
                        SecretEntryTable.FLD_CREATED + ", " +
                        SecretEntryTable.FLD_UPDATED + " ) VALUES ( ?, ?, ?, ?)";
                args = new String[]{String.valueOf(uuid_msb), String.valueOf(uuid_lsb),
                        String.valueOf(created), String.valueOf(updated)};
                db.execSQL(sql, args);
                sql = "INSERT INTO " + SecretEntryAttributeTable.TBL_NAME + " ( " +
                        SecretEntryAttributeTable.FLD_ENTRY_UUID_MSB + ", " +
                        SecretEntryAttributeTable.FLD_ENTRY_UUID_LSB + ", " +
                        SecretEntryAttributeTable.FLD_ORDER_ID + ", " +
                        SecretEntryAttributeTable.FLD_KEY + ", " +
                        SecretEntryAttributeTable.FLD_VALUE + ", " +
                        SecretEntryAttributeTable.FLD_PROTECTED + " ) VALUES ( ?, ?, ?, ?, ?, ?)";
                for (int i = 0; i < entry.size(); i++) {
                    SecretEntryAttribute attr = entry.get(i);
                    args = new String[]{String.valueOf(uuid_msb), String.valueOf(uuid_lsb),
                            String.valueOf(i), attr.getKey(), attr.isProtected() ? keyStore.encrypt(attr.getValue()) : attr.getValue(),
                            String.valueOf(attr.isProtected() ? 1 : 0)};
                    db.execSQL(sql, args);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage(), e);
        }
        return oldEntry;
    }

    @Override
    public void setPassword(String password) throws StorageException {
        if (!isActive()) {
            throw new StorageException(getString(R.string.exception_storage_not_ready));
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(MasterPasswordTable.TBL_NAME, null, null);
            if (password != null) {
                String inputHash = hash(password);
                ContentValues values = new ContentValues();
                values.put(MasterPasswordTable.FLD_PASSWORD, inputHash);
                db.insert(MasterPasswordTable.TBL_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException(e.getMessage(), e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }

    private String hash(String value) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(value.getBytes());
        char[] hex = Hex.encodeHex(digest.digest());
        return String.valueOf(hex);
    }

    private SecretEntry readEntry(UUID uuid, SQLiteDatabase db) throws StorageException {
        SecretEntry entry = null;
        String sql = "SELECT * FROM " + SecretEntryTable.TBL_NAME + " WHERE " +
                SecretEntryTable.FLD_UUID_MSB + " = ? AND " +
                SecretEntryTable.FLD_UUID_LSB + " = ?";
        String[] args = {String.valueOf(uuid.getMostSignificantBits()),
                String.valueOf(uuid.getLeastSignificantBits())};

        try (Cursor cursor = db.rawQuery(sql, args)) {
            if (cursor.moveToFirst()) {
                entry = new SecretEntry(uuid);
                entry.setCreated(cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_CREATED)));
                entry.setUpdated(cursor.getLong(cursor.getColumnIndex(SecretEntryTable.FLD_UPDATED)));
            }
        }
        return readEntryAttributes(entry, db);
    }

    private SecretEntry readEntryAttributes(SecretEntry entry, SQLiteDatabase db) throws StorageException {
        if (entry != null) {
            String sql = "SELECT * FROM " + SecretEntryAttributeTable.TBL_NAME + " WHERE " +
                    SecretEntryAttributeTable.FLD_ENTRY_UUID_MSB + " = ? AND " +
                    SecretEntryAttributeTable.FLD_ENTRY_UUID_LSB + " = ? " +
                    " ORDER BY " + SecretEntryAttributeTable.FLD_ORDER_ID;
            String[] args = {String.valueOf(entry.getID().getMostSignificantBits()),
                    String.valueOf(entry.getID().getLeastSignificantBits())};

            try (Cursor cursor = db.rawQuery(sql, args)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_KEY));
                    String value = cursor.getString(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_VALUE));
                    boolean isProtected = cursor.getInt(cursor.getColumnIndex(SecretEntryAttributeTable.FLD_PROTECTED)) != 0;
                    SecretEntryAttribute attr = new SecretEntryAttribute(key, isProtected ? keyStore.decrupt(value) : value, isProtected);
                    entry.add(attr);
                }
            } catch (Exception e) {
                throw new StorageException(e.getMessage(), e);
            }
        }
        return entry;
    }
}
