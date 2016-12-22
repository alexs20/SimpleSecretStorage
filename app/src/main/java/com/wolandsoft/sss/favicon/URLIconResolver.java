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
package com.wolandsoft.sss.favicon;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import com.jakewharton.disklrucache.DiskLruCache;
import com.wolandsoft.sss.AppConstants;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.util.LogEx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class URLIconResolver extends ContextWrapper {
    private static final String FAVICONS_DIRECTORY = "favicons";
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int SIZE_MEGABYTE = 1024 * 1024;
    private static final int SIZE_GIGABYTE = SIZE_MEGABYTE * 1024;
    private static final int CONNECT_TIMEOUT_MSEC = 15000;
    private static final int READ_TIMEOUT_MSEC = 10000;
    Bitmap bmp;

    private DiskLruCache mCache;

    public URLIconResolver(Context base) {
        super(base);
        boolean canUseExternalStorage = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            canUseExternalStorage = true;
            for (String permission : AppConstants.EXTERNAL_STORAGE_PERMISSIONS) {
                int permissionGranted = ContextCompat.checkSelfPermission(this, permission);
                if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
                    canUseExternalStorage = false;
                    break;
                }
            }
        }
        File cachePath = canUseExternalStorage ? getExternalCacheDir() : getCacheDir();
        int cacheSize = canUseExternalStorage ? SIZE_GIGABYTE : SIZE_MEGABYTE;
        try {
            mCache = DiskLruCache.open(new File(cachePath, FAVICONS_DIRECTORY), APP_VERSION, VALUE_COUNT, cacheSize);
        } catch (IOException e) {
            LogEx.e(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Bitmap resolve(SecretEntry entry, final OnURLIconResolveListener async) {
        List<URL> urls = new ArrayList<>();
        for (SecretEntryAttribute attr : entry) {
            if (!attr.isProtected()) {
                String val = attr.getValue();
                if (val != null && val.length() > 0) {
                    val = val.toLowerCase();
                    if (val.startsWith("http://") || val.startsWith("https://")) {
                        DiskLruCache.Snapshot snapshot = null;
                        try {
                            URL url = new URL(val);
                            urls.add(url); // storing urls in case that need to resolve
                            String key = url.getHost().toLowerCase().replace('.', '_');
                            snapshot = mCache.get(key);
                            if (snapshot != null) {
                                InputStream in = snapshot.getInputStream(0);
                                if (in != null) {
                                    BufferedInputStream buffIn = new BufferedInputStream(in);
                                    Bitmap bitmap = BitmapFactory.decodeStream(buffIn);
                                    return bitmap;
                                }
                            }
                        } catch (Exception e) {
                            LogEx.w(e.getMessage(), e);
                        } finally {
                            if (snapshot != null) {
                                snapshot.close();
                            }
                        }
                    }
                }
            }
        }

        //noinspection unchecked
        new AsyncTask<List<URL>, Void, AsyncTaskResult>() {
            @Override
            protected AsyncTaskResult doInBackground(List<URL>... params) {
                AsyncTaskResult result = new AsyncTaskResult();
                for (URL url : params[0]) {
                    result.key = url.getHost().replace('.', '_');
                    InputStream is = null;
                    try {
                        URL favUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "favicon.ico");
                        HttpURLConnection conn = (HttpURLConnection) favUrl.openConnection();
                        conn.setReadTimeout(READ_TIMEOUT_MSEC);
                        conn.setConnectTimeout(CONNECT_TIMEOUT_MSEC);
                        conn.setRequestMethod("GET");
                        conn.setDoInput(true);
                        // Starts the query
                        conn.connect();
                        int status = conn.getResponseCode();
                        LogEx.d("HttpURLConnection response is: ", status);
                        if (status != HttpURLConnection.HTTP_OK) {
                            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                                    || status == HttpURLConnection.HTTP_MOVED_PERM
                                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                                url = new URL(conn.getHeaderField("Location"));
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setReadTimeout(READ_TIMEOUT_MSEC);
                                conn.setConnectTimeout(CONNECT_TIMEOUT_MSEC);
                                conn.setRequestMethod("GET");
                                conn.setDoInput(true);
                                // Starts the query
                                conn.connect();
                                status = conn.getResponseCode();
                                LogEx.d("HttpURLConnection response after redirect is: ", status);
                            }
                        }
                        if (status == HttpURLConnection.HTTP_OK) {
                            is = conn.getInputStream();
                            if (is != null) {
                                result.bitmap = BitmapFactory.decodeStream(is);
                                if (result.bitmap != null) {
                                    return result;
                                }
                            }
                        }
                    } catch (IOException e) {
                        LogEx.w(e.getMessage(), e);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                LogEx.w(e.getMessage(), e);
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AsyncTaskResult result) {
                if (result != null) {
                    DiskLruCache.Editor editor = null;
                    try {
                        editor = mCache.edit(result.key);
                        if (editor == null) {
                            return;
                        }
                        OutputStream out = null;
                        try {
                            out = new BufferedOutputStream(editor.newOutputStream(0));
                            if (result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                mCache.flush();
                                editor.commit();

                            } else {
                                editor.abort();
                            }
                        } finally {
                            if (out != null) {
                                out.close();
                            }
                        }
                    } catch (IOException e) {
                        LogEx.w(e.getMessage(), e);
                    }
                    if (async != null) {
                        async.onURLIconResolved(result.bitmap);
                    }
                }
            }
        }.execute(urls);


        return null;
    }

    public interface OnURLIconResolveListener {
        void onURLIconResolved(Bitmap image);
    }

    static class AsyncTaskResult {
        String key;
        Bitmap bitmap;
    }
}
