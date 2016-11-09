package com.wolandsoft.sss.external.json;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.wolandsoft.sss.AppConstants.APP_SHORT_NAME;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class PlainJson extends AExternal {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String PUBLIC_DIRECTORY = APP_SHORT_NAME;
    private static final String EXTERNAL_FILE_NAME = "secret.json";
    private static final String PROTECTED = "Protected";
    private static final String KEY = "Entry";
    private static final String VALUE = "Value";
    private static final String CREATED = "Created";
    private static final String UPDATED = "Updated";
    private static final String KEY_UUID = "UUID";
    private static final String DATA = "Data";
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.US);

    public PlainJson(Context base) {
        super(base);
    }

    @Override
    public void startup() throws ExternalException {
        // Check if we have write permission
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            throw new ExternalException(getString(R.string.exception_no_storage_permission));
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void doExport(SQLiteStorage fromStorage) throws ExternalException {
        File dir = Environment.getExternalStoragePublicDirectory(PUBLIC_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File jsonFile = new File(dir, EXTERNAL_FILE_NAME);
        if (jsonFile.exists()) {
            jsonFile.delete();
        }
        try (FileOutputStream fOut = new FileOutputStream(jsonFile);
             OutputStreamWriter out = new OutputStreamWriter(fOut, "UTF-8")) {

            List<Object> jsonEntries = new LinkedList<>();
            List<SecretEntry> entries = fromStorage.find(null, true, 0, Integer.MAX_VALUE);

                for (SecretEntry entry : entries) {
                    Map<String, Object> jsonEntry = new LinkedHashMap<>();
                    jsonEntry.put(KEY_UUID, entry.getID());
                    jsonEntry.put(CREATED, format.format(new Date(entry.getCreated())));
                    jsonEntry.put(UPDATED, format.format(new Date(entry.getUpdated())));
                    List<Object> jsonAttrs = new LinkedList<>();
                    for (SecretEntryAttribute attr : entry) {
                        Map<String, Object> jsonAttr = new LinkedHashMap<>();
                        jsonAttr.put(KEY, attr.getKey());
                        jsonAttr.put(VALUE, attr.getValue());
                        if (attr.isProtected()) {
                            jsonAttr.put(PROTECTED, true);
                        }
                        jsonAttrs.add(jsonAttr);
                    }
                    jsonEntry.put(DATA, jsonAttrs);
                    jsonEntries.add(jsonEntry);
                }

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String outStr = gson.toJson(jsonEntries);
            out.write(outStr);
        } catch (StorageException | IOException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public void doImport(SQLiteStorage toStorage, boolean isOverwrite) throws ExternalException {
        File dir = Environment.getExternalStoragePublicDirectory(PUBLIC_DIRECTORY);
        if (!dir.exists()) {
            String fullPath = dir.getAbsolutePath();
            String userPortionDir = fullPath.substring(fullPath.lastIndexOf(PUBLIC_DIRECTORY));
            throw new ExternalException(String.format(getString(R.string.exception_no_dir_found), userPortionDir));
        }
        File jsonFile = new File(dir, EXTERNAL_FILE_NAME);
        if (!jsonFile.exists()) {
            throw new ExternalException(String.format(getString(R.string.exception_no_file_found), EXTERNAL_FILE_NAME));
        }

        try (FileInputStream fIn = new FileInputStream(jsonFile);
             InputStreamReader in = new InputStreamReader(fIn);
             BufferedReader br = new BufferedReader(in)) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line).append('\n');
            }
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            List<Map> entriesList = gson.fromJson(json.toString(), ArrayList.class);
            for(Map entryMap : entriesList){
                UUID uuid = UUID.fromString(entryMap.get(KEY_UUID).toString());
                SecretEntry oldEntry = toStorage.get(uuid);
                if(oldEntry != null || isOverwrite) {
                    SecretEntry entry = new SecretEntry(uuid);
                    entry.setCreated(format.parse(entryMap.get(CREATED).toString()).getTime());
                    entry.setUpdated(format.parse(entryMap.get(UPDATED).toString()).getTime());
                    List<Map> attrsList = (List<Map>) entryMap.get(DATA);
                    for (Map attrMap : attrsList) {
                        boolean isProtected = attrMap.containsKey(PROTECTED) ? Boolean.valueOf(attrMap.get(PROTECTED).toString()) : false;
                        SecretEntryAttribute attr = new SecretEntryAttribute(
                                attrMap.get(KEY).toString(),
                                attrMap.get(VALUE).toString(),
                                isProtected);
                        entry.add(attr);
                    }
                    toStorage.put(entry);
                }
            }
// Check if we have write permission
            //int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            //if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            //  ActivityCompat.requestPermissions(
            //           activity,
            //           PERMISSIONS_STORAGE,
            //           REQUEST_EXTERNAL_STORAGE
            //   );
            //}
        } catch (StorageException | IOException | ParseException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }
}
