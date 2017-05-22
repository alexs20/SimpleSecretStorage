package com.wolandsoft.sss.service.pccomm;


import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.support.v7.preference.PreferenceManager;
import android.util.Base64;

import com.wolandsoft.sss.BuildConfig;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.security.AESCipher;
import com.wolandsoft.sss.security.TextCipher;
import com.wolandsoft.sss.util.LogEx;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Copy to PC service
 *
 * @author Alexander Shulgin
 */
public class PcCommService extends IntentService {
    public static final String ACTION_PING = "com.wolandsoft.sss.ACTION_PING";
    public static final String ACTION_PAYLOAD = "com.wolandsoft.sss.ACTION_PAYLOAD";
    public static final String KEY_ACTION = "action";
    public static final String KEY_DATA = "data";
    public static final String KEY_TITLE = "title";
    private static final String SERVICE_TAG = PcCommService.class.getSimpleName();
    private static final String ALL_HOSTS_MC_ADDRESS = "224.0.0.1";

    public PcCommService() {
        super(SERVICE_TAG);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onHandleIntent(Intent intent) {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() || BuildConfig.DEBUG) {
            try {
                SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(this);
                int port = shPref.getInt(getString(R.string.pref_pc_receiver_port_key), 0);
                String keyB64 = shPref.getString(getString(R.string.pref_pc_receiver_key_key), null);
                byte[] encodedKey = Base64.decode(keyB64, Base64.DEFAULT);
                byte[] aesKey = new TextCipher().decipher(encodedKey);
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
                try (DatagramSocket socket = new DatagramSocket(port)) {
                    InetAddress group = InetAddress.getByName(ALL_HOSTS_MC_ADDRESS);
                    //Sending to Multicast Group
                    DatagramPacket packet = new DatagramPacket(cipherTextBuff, cipherTextBuff.length, group, port);
                    socket.send(packet);
                }
            } catch (Exception e) {
                LogEx.e(e.getMessage(), e);
            }
        }
    }
}
