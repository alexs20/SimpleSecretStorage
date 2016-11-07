package com.wolandsoft.sss.storage;

import com.wolandsoft.sss.entity.SecretEntry;

import java.util.List;
import java.util.UUID;

/**
 * Created by Alexander on 10/14/2016.
 */

public interface IStorage {

    /**
     * Initialize and unlock data storage.
     *
     * @param password The password to unlock the storage. If no password set yet then {@code null} should be provided.
     * @throws StorageException On any storage error.
     */
    void startup(String password) throws StorageException;

    /**
     * Close storage.
     */
    void shutdown();

    /**
     * Check storage status.
     *
     * @return {@code true} if storage was successfully initialized and unlocked.
     */
    boolean isActive();

    List<SecretEntry> find(String criteria, boolean isASC, int offset, int limit) throws StorageException;

    SecretEntry get(UUID id) throws StorageException;

    /**
     * Store {@link SecretEntry}
     * @param entry The entry object to store.
     * @return Old version of the entry if any.
     * @throws StorageException
     */
    SecretEntry put(SecretEntry entry) throws StorageException;

    /**
     * Update unlock password with new one.</br>.
     *
     * @param password New password or {@code null} to remove password protection.
     * @throws StorageException
     */
    void setPassword(String password) throws StorageException;

    /**
     * Storage identifier.
     *
     * @return Unique identifier across all possible storage implementations.
     */
    String getID();
}
