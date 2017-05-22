/*
    Copyright 2017 Alexander Shulgin

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

import android.support.annotation.Nullable;

import com.wolandsoft.sss.entity.SecretEntry;

import java.io.Closeable;
import java.util.List;

/**
 * Storage that stores {@link SecretEntry} objects.
 *
 * @author Alexander Shulgin
 */
public interface IStorage {
    /**
     * Get list of {@link SecretEntry} IDs that matches the search criteria.
     * If search criteria is {@code null} then all elements returned.
     *
     * @param criteria A search criteria or {@code null}.
     * @return List of {@link SecretEntry} IDs.
     */
    List<Integer> findRecords(@Nullable String criteria);

    /**
     * Get {@link SecretEntry} by ID.
     *
     * @param id ID of {@link SecretEntry}.
     * @return instance of {@link SecretEntry} or {@code null} if not found.
     */
    SecretEntry getRecord(int id);

    /**
     * Delete {@link SecretEntry} by ID.
     *
     * @param id ID of {@link SecretEntry}.
     */
    void deleteRecord(int id);

    /**
     * Store {@link SecretEntry}.
     *
     * @param entry {@link SecretEntry} object to store.
     * @return updated entry where {@link SecretEntry#getID()} and {@link SecretEntry#getCreated()}
     * values are assigned for the new entries and {@link SecretEntry#getUpdated()} value updated for the others.
     */
    SecretEntry putRecord(SecretEntry entry);

}
