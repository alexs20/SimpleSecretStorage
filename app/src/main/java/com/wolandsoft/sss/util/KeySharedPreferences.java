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
package com.wolandsoft.sss.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.util.TypedValue;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Utility adapter of {@link SharedPreferences} that allows to fetch preference values by resource id.
 *
 * @author Alexander Shulgin
 */

@SuppressWarnings("ALL")
public class KeySharedPreferences extends ContextWrapper implements SharedPreferences {

    private SharedPreferences mSharedPreferences;
    private TypedValue mTmpValue; // cached instance

    /**
     * Creates Utility adapter for {@link SharedPreferences}
     *
     * @param sharedPreferences the instance of {@link KeySharedPreferences}
     * @param context           the instance of {@link Context}
     */
    public KeySharedPreferences(SharedPreferences sharedPreferences, Context context) {
        super(context);
        this.mSharedPreferences = sharedPreferences;
    }

    @Override
    public Map<String, ?> getAll() {
        return mSharedPreferences.getAll();
    }

    @Override
    public String getString(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    /**
     * Retrieve a String value from the preferences.
     *
     * @param keyId      The resource id of the name of the preference to retrieve.
     * @param defValueId The resource id of the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue. Throws ClassCastException if there is a
     * preference with this name that is not a String.
     */
    public String getString(int keyId, Integer defValueId) {
        if (defValueId == null) {
            return getString(getString(keyId), null);
        }
        return getString(getString(keyId), getResources().getString(defValueId));
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mSharedPreferences.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    /**
     * Retrieve an int value from the preferences.
     *
     * @param keyId      The resource id of the name of the preference to retrieve.
     * @param defValueId The resource id of the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or value with id defValueId. Throws ClassCastException if
     * there is a preference with this name that is not an int.
     */
    public int getInt(int keyId, int defValueId) {
        return getInt(getString(keyId), getResources().getInteger(defValueId));
    }

    /**
     * Retrieve an int value from the preferences.
     *
     * @param keyId    The resource id of the name of the preference to retrieve.
     * @param defValue The the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or value with id defValueId. Throws ClassCastException if
     * there is a preference with this name that is not an int.
     */
    public int getInt(int keyId, Integer defValue) {
        return getInt(getString(keyId), defValue);
    }

    /**
     * Retrieve an array of int values from the preferences.
     *
     * @param key           The name of the preference to retrieve.
     * @param defValueArray The value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValueArray. Throws ClassCastException if there is a
     * preference with this name that is not an int.
     */
    public int[] getIntArray(String key, int[] defValueArray) {
        String savedString = getString(key, null);
        if (savedString != null) {
            StringTokenizer st = new StringTokenizer(savedString, ",");
            int[] savedList = new int[st.countTokens()];
            for (int i = 0; i < savedList.length; i++) {
                savedList[i] = Integer.parseInt(st.nextToken());
            }
            return savedList;
        } else {
            return defValueArray;
        }
    }

    /**
     * Retrieve an array of int values from the preferences.
     *
     * @param keyId      The resource id of the name of the preference to retrieve.
     * @param defValueId The resource id of the array of int values to return if this preference does not exist.
     * @return Returns the preference value if it exists, or array retrieved from resources by defValueId. Throws
     * ClassCastException if there is a preference with this name that is not an int.
     */
    public int[] getIntArray(int keyId, int defValueId) {
        return getIntArray(getString(keyId), getResources().getIntArray(defValueId));
    }

    /**
     * Retrieve an array of int values from the preferences.
     *
     * @param keyId The resource id of the name of the preference to retrieve.
     * @return Returns the preference value if it exists, or array retrieved from resources by defValueId. Throws
     * ClassCastException if there is a preference with this name that is not an int.
     */
    public int[] getIntArray(int keyId) {
        return getIntArray(getString(keyId), new int[0]);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mSharedPreferences.getLong(key, defValue);
    }

    /**
     * Retrieve a long value from the preferences.
     *
     * @param keyId      The resource id of the name of the preference to retrieve.
     * @param defValueId The resource id of the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue. Throws ClassCastException if there is a
     * preference with this name that is not a long.
     */
    public long getLong(int keyId, int defValueId) {
        return getLong(getString(keyId), getResources().getInteger(defValueId));
    }

    /**
     * Retrieve a long value from the preferences.
     *
     * @param keyId    The resource id of the name of the preference to retrieve.
     * @param defValue The the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue. Throws ClassCastException if there is a
     * preference with this name that is not a long.
     */
    public long getLong(int keyId, Long defValue) {
        return getLong(getString(keyId), defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    /**
     * Retrieve a float value from the preferences.
     *
     * @param keyId      The resource id of the name of the preference to retrieve.
     * @param defValueId The resource id of the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue. Throws ClassCastException if there is a
     * preference with this name that is not a float.
     */
    public float getFloat(int keyId, int defValueId) {
        TypedValue value = mTmpValue;
        if (value == null) {
            mTmpValue = value = new TypedValue();
        }
        getResources().getValue(defValueId, value, true);
        if (value.type != TypedValue.TYPE_FLOAT) {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(defValueId) + " type #0x" + Integer.toHexString(value.type) + " is not valid");
        }
        return getFloat(getString(keyId), value.getFloat());
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    /**
     * Retrieve a boolean value from the preferences.
     *
     * @param keyId      The resource id of the name of the preference to retrieve.
     * @param defValueId The resource id of the value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue. Throws ClassCastException if there is a
     * preference with this name that is not a boolean.
     */
    public boolean getBoolean(int keyId, int defValueId) {
        return getBoolean(getString(keyId), getResources().getBoolean(defValueId));
    }

    /**
     * Retrieve a boolean value from the preferences.
     *
     * @param keyId    The resource id of the name of the preference to retrieve.
     * @param defValue The value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue. Throws ClassCastException if there is a
     * preference with this name that is not a boolean.
     */
    public boolean getBoolean(int keyId, boolean defValue) {
        return getBoolean(getString(keyId), defValue);
    }

    @Override
    public boolean contains(String key) {
        return mSharedPreferences.contains(key);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public KeyableEditor edit() {
        return new KeyableEditor(mSharedPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Utility adapter for {@link Editor} that allows to edit preference values by resource id.
     *
     * @author Alexander Shulgin /alexs20@gmail.com/
     */
    public class KeyableEditor implements Editor {

        private Editor mEditor;

        private KeyableEditor(Editor editor) {
            this.mEditor = editor;
        }

        @Override
        public KeyableEditor putString(String key, String value) {
            mEditor.putString(key, value);
            return this;
        }

        /**
         * Set a String value in the preferences editor, to be written back once {@link #commit} or {@link #apply} are
         * called.
         *
         * @param keyId The resource id of the name of the preference to modify.
         * @param value The new value for the preference. Supplying {@code null} as the value is equivalent to calling
         *              {@link #remove(String)} with this key.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor putString(int keyId, String value) {
            putString(getString(keyId), value);
            return this;
        }

        @Override
        public KeyableEditor putStringSet(String key, Set<String> values) {
            mEditor.putStringSet(key, values);
            return this;
        }

        @Override
        public KeyableEditor putInt(String key, int value) {
            mEditor.putInt(key, value);
            return this;
        }

        /**
         * Set an int value in the preferences editor, to be written back once {@link #commit} or {@link #apply} are
         * called.
         *
         * @param keyId The resource id of the name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor putInt(int keyId, int value) {
            putInt(getString(keyId), value);
            return this;
        }

        /**
         * Set an array of int values in the preferences editor, to be written back once {@link #commit} or
         * {@link #apply} are called.
         *
         * @param key   The name of the preference to modify.
         * @param array The new array of int values for the preference.
         */
        public KeyableEditor putIntArray(String key, int[] array) {
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    str.append(",");
                }
                str.append(array[i]);
            }
            putString(key, str.toString());
            return this;
        }

        /**
         * Set an array of int values in the preferences editor, to be written back once {@link #commit} or
         * {@link #apply} are called.
         *
         * @param keyId The resource id of the name of the preference to modify.
         * @param array The new array of int values for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor putIntArray(int keyId, int[] array) {
            putIntArray(getString(keyId), array);
            return this;
        }

        @Override
        public KeyableEditor putLong(String key, long value) {
            mEditor.putLong(key, value);
            return this;
        }

        /**
         * Set a long value in the preferences editor, to be written back once {@link #commit} or {@link #apply} are
         * called.
         *
         * @param keyId The resource id of the name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor putLong(int keyId, long value) {
            putLong(getString(keyId), value);
            return this;
        }

        @Override
        public KeyableEditor putFloat(String key, float value) {
            mEditor.putFloat(key, value);
            return this;
        }

        /**
         * Set a float value in the preferences editor, to be written back once {@link #commit} or {@link #apply} are
         * called.
         *
         * @param keyId The resource id of the name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor putFloat(int keyId, float value) {
            putFloat(getString(keyId), value);
            return this;
        }

        @Override
        public KeyableEditor putBoolean(String key, boolean value) {
            mEditor.putBoolean(key, value);
            return this;
        }

        /**
         * Set a boolean value in the preferences editor, to be written back once {@link #commit} or {@link #apply} are
         * called.
         *
         * @param keyId The resource id of the name of the preference to modify.
         * @param value The new value for the preference.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor putBoolean(int keyId, boolean value) {
            putBoolean(getString(keyId), value);
            return this;
        }

        @Override
        public KeyableEditor remove(String key) {
            mEditor.remove(key);
            return this;
        }

        /**
         * Mark in the editor that a preference value should be removed, which will be done in the actual preferences
         * once {@link #commit} is called.
         * <p/>
         * <p/>
         * Note that when committing back to the preferences, all removals are done first, regardless of whether you
         * called remove before or after put methods on this editor.
         *
         * @param keyId The name of the preference to remove.
         * @return Returns a reference to the same Editor object, so you can chain put calls together.
         */
        public KeyableEditor remove(int keyId) {
            mEditor.remove(getString(keyId));
            return this;
        }

        @Override
        public KeyableEditor clear() {
            mEditor.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return mEditor.commit();
        }

        @Override
        public void apply() {
            mEditor.apply();
        }

    }
}
