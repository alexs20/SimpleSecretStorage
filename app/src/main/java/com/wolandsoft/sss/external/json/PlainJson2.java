package com.wolandsoft.sss.external.json;

import android.content.Context;
import android.os.Environment;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.external.AExternal;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.storage.IStorage;
import com.wolandsoft.sss.storage.StorageException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.wolandsoft.sss.AppConstants.APP_SHORT_NAME;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class PlainJson2 extends AExternal {
    private static final String PUBLIC_DIRECTORY = APP_SHORT_NAME;
    private static final String EXTERNAL_FILE_NAME = "secret.json";
    private static final String PROTECTED = "Protected";
    private static final String CREATED = "Created";
    private static final String UPDATED = "Updated";
    private static final String UUID = "UUID";
    private static final String DATA = "Data";
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.US);

    public PlainJson2(Context base) {
        super(base);
    }

    @Override
    public void startup() throws ExternalException {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void doExport(IStorage fromStorage) throws ExternalException {
        File dir = Environment.getExternalStoragePublicDirectory(PUBLIC_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File jsonFile = new File(dir, EXTERNAL_FILE_NAME);
        if (jsonFile.exists()) {
            jsonFile.delete();
        }
        try (FileOutputStream fOut = new FileOutputStream(jsonFile);
             OutputStreamWriter out = new OutputStreamWriter(fOut)) {
            JSONArray jsonArray = new JSONArray();
            int offset = 0;
            List<SecretEntry> entries = fromStorage.find(null, true, offset, 10);
            while (entries.size() > 0) {
                for (SecretEntry entry : entries) {
                    JSONObject jsonEntry = new JSONObject();
                    jsonEntry.put(UUID, entry.getID());
                    jsonEntry.put(CREATED, format.format(new Date(entry.getCreated())));
                    jsonEntry.put(UPDATED, format.format(new Date(entry.getUpdated())));
                    JSONArray jsonAttrs = new JSONArray();
                    for (SecretEntryAttribute attr : entry) {
                        JSONObject jsonAttr = new JSONObject();
                        jsonAttr.put(attr.getKey(), attr.getValue());
                        if(attr.isProtected()) {
                            jsonAttr.put(PROTECTED, true);
                        }
                        jsonAttrs.put(jsonAttr);
                    }
                    jsonEntry.put(DATA, jsonAttrs);
                    jsonArray.put(jsonEntry);
                }
                offset += 10;
                entries = fromStorage.find(null, true, offset, 10);
            }
            out.write(jsonArray.toString(4));
        } catch (StorageException | JSONException | IOException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public void doImport(IStorage toStorage, boolean isOverwrite) throws ExternalException {

    }

    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }
}
