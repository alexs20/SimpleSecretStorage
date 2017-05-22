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
package com.wolandsoft.sss.external.json;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.external.AExternal;
import com.wolandsoft.sss.external.EConflictResolution;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.storage.IStorage;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Export - import engine.<br/>
 * Operate with JSON formatted file compressed in ZIP using AES 256 encryption.
 *
 * @author Alexander Shulgin
 */

public class JsonAes256Zip extends AExternal {
    private static final String JSON_FILE_ENTRY_NAME = "secret.json";
    private static final String PROTECTED = "Protected";
    private static final String KEY = "Entry";
    private static final String VALUE = "Value";
    private static final String CREATED = "Created";
    private static final String UPDATED = "Updated";
    private static final String KEY_ID = "ID";
    private static final String DATA = "Data";
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);

    public JsonAes256Zip(Context base) {
        super(base);
    }

    @Override
    public void doExport(IStorage storage,
                         URI destination, String password, Object... extra) throws ExternalException {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            throw new ExternalException(getString(R.string.exception_no_storage_permission));
        }
        File jsonFile = new File(destination);
        if (jsonFile.exists()) {
            if (!jsonFile.delete()) {
                throw new ExternalException(getString(R.string.exception_no_storage_permission));
            }
        }
        try {
            ZipFile zip = new ZipFile(jsonFile);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
            parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
            parameters.setPassword(password);
            parameters.setSourceExternalStream(true);
            parameters.setFileNameInZip(JSON_FILE_ENTRY_NAME);


            List<Object> jsonEntries = new LinkedList<>();
            List<Integer> entries = storage.findRecords(null);
            for (int id : entries) {
                SecretEntry entry = storage.getRecord(id);
                Map<String, Object> jsonEntry = new LinkedHashMap<>();
                jsonEntry.put(KEY_ID, String.valueOf(entry.getID()));
                jsonEntry.put(CREATED, format.format(new Date(entry.getCreated())));
                jsonEntry.put(UPDATED, format.format(new Date(entry.getUpdated())));
                List<Object> jsonAttrs = new LinkedList<>();
                for (SecretEntryAttribute attr : entry) {
                    Map<String, Object> jsonAttr = new LinkedHashMap<>();
                    jsonAttr.put(KEY, attr.getKey());
                    if (attr.isProtected()) {
                        jsonAttr.put(PROTECTED, true);
                    }
                    jsonAttr.put(VALUE, attr.getValue());
                    jsonAttrs.add(jsonAttr);
                }
                jsonEntry.put(DATA, jsonAttrs);
                jsonEntries.add(jsonEntry);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String outStr = gson.toJson(jsonEntries);

            InputStream in = IOUtils.toInputStream(outStr);
            zip.addStream(in, parameters);

        } catch (ZipException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public void doImport(IStorage toStorage,
                         EConflictResolution conflictRes, URI source, String password, Object... extra) throws ExternalException {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            throw new ExternalException(getString(R.string.exception_no_storage_permission));
        }

        File jsonFile = new File(source);
        if (!jsonFile.exists()) {
            throw new ExternalException(String.format(getString(R.string.exception_no_file_found), source));
        }

        try {
            ZipFile zip = new ZipFile(jsonFile);
            if (!zip.isValidZipFile()) {
                throw new ExternalException(String.format(getString(R.string.exception_invalid_input_file), source));
            }
            zip.setPassword(password);
            FileHeader header = zip.getFileHeader(JSON_FILE_ENTRY_NAME);
            InputStream in = zip.getInputStream(header);
            String dataString = IOUtils.toString(in, "UTF-8");

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            List<Map<String, Object>> entriesList = gson.fromJson(dataString,
                    new TypeToken<List<Map<String, Object>>>() {
                    }.getType());
            for (Map entryMap : entriesList) {
                int id = Integer.parseInt(entryMap.get(KEY_ID).toString());
                SecretEntry entry = new SecretEntry(id,
                        format.parse(entryMap.get(CREATED).toString()).getTime(),
                        format.parse(entryMap.get(UPDATED).toString()).getTime());
                Object objItem = entryMap.get(DATA);
                if (objItem instanceof List<?>) {
                    List<?> attrsList = (List<?>) objItem;
                    for (Object objAttr : attrsList) {
                        if (objAttr instanceof Map<?, ?>) {
                            Map attrMap = (Map) objAttr;
                            String key = attrMap.containsKey(KEY) ? attrMap.get(KEY).toString() : "";
                            boolean isProtected = attrMap.containsKey(PROTECTED) ? Boolean.valueOf(attrMap.get(PROTECTED).toString()) : false;
                            String value = attrMap.containsKey(VALUE) ? attrMap.get(VALUE).toString() : "";
                            SecretEntryAttribute attr = new SecretEntryAttribute(key, value, isProtected);
                            entry.add(attr);
                        }
                    }
                    SecretEntry oldEntry = toStorage.getRecord(id);
                    if (oldEntry == null || EConflictResolution.overwrite == conflictRes) {
                        toStorage.putRecord(entry);
                    } else {
                        long created = oldEntry.getCreated() > entry.getCreated() ? entry.getCreated() : oldEntry.getCreated();
                        long updated = oldEntry.getUpdated() < entry.getUpdated() ? entry.getUpdated() : oldEntry.getUpdated();
                        SecretEntry mergedEntry = new SecretEntry(id, created, updated);
                        for (SecretEntryAttribute oldAttr : oldEntry) {
                            mergedEntry.add(oldAttr);
                            for (int i = 0; i < entry.size(); i++) {
                                SecretEntryAttribute attr = entry.get(i);
                                if (attr.getKey().equals(oldAttr.getKey())) {
                                    if (!attr.getValue().equals(oldAttr.getValue())) {
                                        mergedEntry.add(attr);
                                    }
                                    entry.remove(i);
                                    break;
                                }
                            }
                        }
                        for (SecretEntryAttribute attr : entry) {
                            mergedEntry.add(attr);
                        }
                        toStorage.putRecord(mergedEntry);
                    }
                }
            }
        } catch (ZipException | IOException | ParseException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }
}
