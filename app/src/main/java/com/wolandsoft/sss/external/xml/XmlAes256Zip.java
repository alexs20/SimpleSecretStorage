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
package com.wolandsoft.sss.external.xml;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Xml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.external.AExternal;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.security.TextCipher;
import com.wolandsoft.sss.storage.SQLiteStorage;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.R.attr.id;

/**
 * Export - import engine.<br/>
 * Operate with XML file compressed in ZIP with AES 256 encryption.
 *
 * @author Alexander Shulgin
 */

public class XmlAes256Zip extends AExternal {
    private static final String JSON_FILE_ENTRY_NAME = "secret.xml";
    private static final String RECORDS = "Records";
    private static final String RECORD = "Record";
    private static final String PROTECTED = "Protected";
    private static final String KEY = "Name";
    private static final String VALUE = "Value";
    private static final String CREATED = "Created";
    private static final String UPDATED = "Updated";
    private static final String KEY_ID = "ID";
    private static final String DATA = "Field";
    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);

    public XmlAes256Zip(Context base) {
        super(base);
    }

    @Override
    public void doExport(SQLiteStorage storage, TextCipher cipher,
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

            XmlSerializer serializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
                serializer.setOutput(writer);
                serializer.startDocument("UTF-8", true);
                serializer.startTag("", RECORDS);
                List<Integer> entries = storage.find(null);
                for (int id : entries) {
                    serializer.startTag("", RECORD);
                    SecretEntry entry = storage.get(id);
                    serializer.startTag("", KEY_ID);
                    serializer.text(String.valueOf(entry.getID()));
                    serializer.endTag("", KEY_ID);
                    serializer.startTag("", CREATED);
                    serializer.text(format.format(new Date(entry.getCreated())));
                    serializer.endTag("", CREATED);
                    serializer.startTag("", UPDATED);
                    serializer.text(format.format(new Date(entry.getUpdated())));
                    serializer.endTag("", UPDATED);
                    for (SecretEntryAttribute attr : entry) {
                        serializer.startTag("", DATA);
                        serializer.startTag("", KEY);
                        serializer.text(attr.getKey());
                        serializer.endTag("", KEY);
                        serializer.startTag("", VALUE);
                        if (attr.isProtected()) {
                            String plain = cipher.decipher(attr.getValue());
                            serializer.text(plain);
                        } else {
                            serializer.text(attr.getValue());
                        }
                        serializer.endTag("", VALUE);
                        serializer.startTag("", PROTECTED);
                        serializer.text(String.valueOf(attr.isProtected()));
                        serializer.endTag("", PROTECTED);
                        serializer.endTag("", DATA);
                    }
                    serializer.endTag("", RECORD);
                }
                serializer.endTag("", RECORDS);
                serializer.endDocument();
                String outStr =  writer.toString();

            InputStream in = IOUtils.toInputStream(outStr);
            zip.addStream(in, parameters);

        } catch (ZipException | IOException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public void doImport(SQLiteStorage toStorage, TextCipher cipher,
                         ConflictResolution conflictRes, URI source, String password, Object... extra) throws ExternalException {
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

            List<Map<String, Object>> entriesList = new ArrayList<>();
            List<Map<String, Object>> attrs = null;
            Map<String, Object> mpEntry = null;
            Map<String, Object> mpAttr = null;
            Map<String, Object> mpNow = null;
            String key = null;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(dataString));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                if (eventType == XmlPullParser.START_TAG) {
                    if(RECORD.equals(xpp.getName())){
                        mpEntry = new HashMap<>();
                        attrs = new ArrayList<>();
                        mpNow = mpEntry;
                    } else if(DATA.equals(xpp.getName())) {
                        mpAttr = new HashMap<>();
                        mpNow = mpAttr;
                    } else {
                        key = xpp.getName();
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if(RECORD.equals(xpp.getName())){
                        mpNow = null;
                        mpEntry.put(DATA, attrs);
                        entriesList.add(mpEntry);
                    } else if(DATA.equals(xpp.getName())) {
                        mpNow = mpEntry;
                        attrs.add(mpAttr);
                    } else {
                        key = null;
                    }
                }
                else if(eventType == XmlPullParser.TEXT) {
                    mpNow.put(key, xpp.getText().trim());
                }
                eventType = xpp.next();
            }

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
                            key = attrMap.containsKey(KEY) ? attrMap.get(KEY).toString() : "";
                            boolean isProtected = attrMap.containsKey(PROTECTED) ? Boolean.valueOf(attrMap.get(PROTECTED).toString()) : false;
                            String value = attrMap.containsKey(VALUE) ? attrMap.get(VALUE).toString() : "";
                            if (isProtected) {
                                value = cipher.cipher(value);
                            }
                            SecretEntryAttribute attr = new SecretEntryAttribute(key, value, isProtected);
                            entry.add(attr);
                        }
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
            }
        } catch (ZipException | IOException | ParseException | XmlPullParserException e) {
            throw new ExternalException(e.getMessage(), e);
        }
    }

    @Override
    public String getID() {
        return this.getClass().getSimpleName();
    }
}
