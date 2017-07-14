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
package com.wolandsoft.sss.service.pccomm;


import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.wolandsoft.sss.BuildConfig;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.security.AESCipher;
import com.wolandsoft.sss.security.TextCipher;
import com.wolandsoft.sss.util.LogEx;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Copy to PC service
 */
public class PcCommService extends IntentService {
    public static final String ACTION_PING = "com.wolandsoft.sss.ACTION_PING";
    public static final String ACTION_PAYLOAD = "com.wolandsoft.sss.ACTION_PAYLOAD";
    public static final String KEY_ACTION = "action";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_DATA = "data";
    public static final String KEY_TITLE = "title";
    private static final String SERVICE_TAG = PcCommService.class.getSimpleName();

    public PcCommService() {
        super(SERVICE_TAG);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onHandleIntent(Intent intent) {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() || BuildConfig.DEBUG) {
            try {
                PairedDevice device = intent.getParcelableExtra(KEY_DEVICE);
                byte[] aesKey = new TextCipher().decipher(device.mKey);
                AESCipher aesCipher = new AESCipher(aesKey);
                JSONObject jsonObj = new JSONObject();
                if (ACTION_PING.equals(intent.getAction())) {
                    jsonObj.put(KEY_ACTION, ACTION_PING);
                } else {
                    jsonObj.put(KEY_ACTION, ACTION_PAYLOAD);
                    jsonObj.put(KEY_TITLE, intent.getStringExtra(KEY_TITLE));
                    jsonObj.put(KEY_DATA, intent.getStringExtra(KEY_DATA));
                }
                byte[] cipherTextBuff = aesCipher.cipher(jsonObj.toString().getBytes(StandardCharsets.UTF_8));
                try (Socket client = new Socket()) {
                    client.connect(new InetSocketAddress(InetAddress.getByAddress(device.mIp), device.mPort), getResources().getInteger(R.integer.pref_paired_device_connect_timeout));
                    OutputStream outStream = client.getOutputStream();
                    outStream.write(cipherTextBuff);
                    outStream.flush();
                }
            } catch (Exception e) {
                LogEx.e(e.getMessage(), e);
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
