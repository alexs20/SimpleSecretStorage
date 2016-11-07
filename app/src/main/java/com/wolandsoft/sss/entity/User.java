package com.wolandsoft.sss.entity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import com.wolandsoft.sss.db.DatabaseHelper;
import com.wolandsoft.sss.db.UserTable;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Represents User entity without password and its model.
 * The instance of this entity can be obtained by using {@link #getModel(Context)} model object.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class User {
    private String mDisplayName;
    private String mID;

    /**
     * Create User's entity
     *
     * @param id          User's ID in email format
     * @param displayName User's display name
     */
    private User(String id, String displayName) {
        this.mID = id;
        this.mDisplayName = displayName;
    }

    /**
     * Get data access and modification storage model for the entity
     *
     * @param context Application context
     * @return new model instance
     */
    public static Model getModel(Context context) {
        return new Model(context);
    }

    /**
     * User's display name
     *
     * @return display name, newer {@null}
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * User's ID (aka email)
     *
     * @return id, never {@null}
     */
    public String getID() {
        return mID;
    }

    /**
     * Provides data storage manipulation api for the entity.
     * Use {@link #add(String, String, String)} for creating new entity,
     * {@link #find(String)} and {@link #find(String, String)} to fetch existed one.
     */
    public static class Model implements UserTable {
        private DatabaseHelper mDBHelper;

        private Model(Context context) {
            this.mDBHelper = new DatabaseHelper(context);
        }

        /**
         * Retrieve {@link User} entity by ID/email
         *
         * @param id user id/email
         * @return {@link User} entity or {@code null}
         */
        @Nullable
        public User find(String id) {
            SQLiteDatabase db = mDBHelper.getReadableDatabase();
            try {
                String sql = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_EMAIL + " = ?";
                Cursor cursor = db.rawQuery(sql, new String[]{id});
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor2User(cursor);
                }
                return null;
            } finally {
                db.close();
            }
        }

        /**
         * Retrieve {@link User} entity by ID/email and password
         *
         * @param id  user id/email
         * @param pwd password
         * @return {@link User} entity or {@code null}
         */
        @Nullable
        public User find(String id, String pwd) {
            SQLiteDatabase db = mDBHelper.getReadableDatabase();
            try {
                String sql = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_EMAIL + " = ? AND " + KEY_PWDH + " = ?";
                String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(pwd)));
                Cursor cursor = db.rawQuery(sql, new String[]{id, hexHash});
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor2User(cursor);
                }
                return null;
            } finally {
                db.close();
            }
        }

        /**
         * Add new entity
         *
         * @param id   user id/email
         * @param name display name
         * @param pwd  password
         * @return new {@link User} entity
         */
        public User add(String id, String name, String pwd) {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            try {
                String sql = "INSERT INTO " + DATABASE_TABLE +
                        "(" + KEY_EMAIL + "," +
                        KEY_NAME + "," +
                        KEY_PWDH +
                        ") VALUES (?,?,?)";
                String hexHash = new String(Hex.encodeHex(DigestUtils.sha1(pwd)));
                db.execSQL(sql, new Object[]{id, name, hexHash});
                User user = new User(id, name);
                return user;
            } finally {
                db.close();
            }
        }

        // read fields from {@link Cursor}
        private User cursor2User(Cursor cursor) {
            User user = new User(
                    cursor.getString(cursor.getColumnIndex(KEY_EMAIL)),
                    cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            return user;
        }
    }
}
