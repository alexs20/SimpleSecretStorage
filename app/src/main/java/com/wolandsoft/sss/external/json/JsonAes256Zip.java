package com.wolandsoft.sss.external.json;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.external.AExternal;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.storage.StorageException;
import com.wolandsoft.sss.util.KeyStoreManager;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class JsonAes256Zip extends AExternal {
    //private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String JSON_FILE_ENTRY_NAME = "secret.json";
    private static final String PROTECTED = "Protected";
    private static final String KEY = "Entry";
    private static final String VALUE = "Value";
    private static final String CREATED = "Created";
    private static final String UPDATED = "Updated";
    private static final String KEY_ID = "ID";
    private static final String DATA = "Data";
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public JsonAes256Zip(Context base) {
        super(base);
    }

    @Override
    public void doExport(SQLiteStorage storage, KeyStoreManager keystore, OnExternalInteract callback,
                         URI destination, String password, Object... extra) throws ExternalException {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //We don't have permission so prompt the user
//            ActivityCompat.requestPermissions(
//                        activity,
//                        PERMISSIONS_STORAGE,
//                        REQUEST_EXTERNAL_STORAGE
//                );
            callback.onPermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                throw new ExternalException(getString(R.string.exception_no_storage_permission));
            }
        }
        File jsonFile = new File(destination);
        if (jsonFile.exists()) {
            jsonFile.delete();
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
            int[] entries = storage.find(null, true);
            for (int id : entries) {
                SecretEntry entry = storage.get(id);
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
                        String plain = keystore.decrupt(attr.getValue());
                        jsonAttr.put(VALUE, plain);
                    } else {
                        jsonAttr.put(VALUE, attr.getValue());
                    }
                    jsonAttrs.add(jsonAttr);
                }
                jsonEntry.put(DATA, jsonAttrs);
                jsonEntries.add(jsonEntry);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String outStr = gson.toJson(jsonEntries);

            InputStream in = IOUtils.toInputStream(outStr);
            zip.addStream(in, parameters);

        } catch (StorageException | IllegalBlockSizeException | BadPaddingException | ZipException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public void doImport(SQLiteStorage toStorage, KeyStoreManager keystore, OnExternalInteract callback,
                         ConflictResolution conflictRes, URI source, String password, Object... extra) throws ExternalException {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //We don't have permission so prompt the user
//            ActivityCompat.requestPermissions(
//                        activity,
//                        PERMISSIONS_STORAGE,
//                        REQUEST_EXTERNAL_STORAGE
//                );
            callback.onPermissionRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                throw new ExternalException(getString(R.string.exception_no_storage_permission));
            }
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
            List<Map> entriesList = gson.fromJson(dataString, ArrayList.class);
            for (Map entryMap : entriesList) {
                int id = Integer.parseInt(entryMap.get(KEY_ID).toString());
                SecretEntry entry = new SecretEntry(id,
                        format.parse(entryMap.get(CREATED).toString()).getTime(),
                        format.parse(entryMap.get(UPDATED).toString()).getTime());
                List<Map> attrsList = (List<Map>) entryMap.get(DATA);
                for (Map attrMap : attrsList) {
                    boolean isProtected = attrMap.containsKey(PROTECTED) ? Boolean.valueOf(attrMap.get(PROTECTED).toString()) : false;
                    String value = attrMap.get(VALUE).toString();
                    if (isProtected) {
                        value = keystore.encrypt(value);
                    }
                    SecretEntryAttribute attr = new SecretEntryAttribute(
                            attrMap.get(KEY).toString(),
                            value,
                            isProtected);
                    entry.add(attr);
                }
                SecretEntry oldEntry = toStorage.get(id);
                if (oldEntry == null || ConflictResolution.overwrite == conflictRes) {
                    toStorage.put(entry);
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
                    toStorage.put(mergedEntry);
                }

            }
        } catch (ZipException | StorageException | IOException | ParseException | IllegalBlockSizeException | BadPaddingException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }


    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }
}
