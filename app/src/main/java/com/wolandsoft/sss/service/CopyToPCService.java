package com.wolandsoft.sss.service;


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
import com.wolandsoft.sss.common.TheApp;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class CopyToPCService extends IntentService {
    public static final String KEY_DATA_TO_COPY = "data";
    private static final String SERVICE_TAG = CopyToPCService.class.getSimpleName();
    private static final String ALL_HOSTS_MC_ADDRESS = "224.0.0.1";

    public CopyToPCService() {
        super(SERVICE_TAG);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onHandleIntent(Intent intent) {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() || BuildConfig.DEBUG) {
            DatagramSocket socket = null;
            try {
                SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(this);
                KeySharedPreferences ksPref = new KeySharedPreferences(shPref, this);
                String encodedParInfo = ksPref.getString(R.string.pref_pc_receiver_encoded_key, (Integer) null);
                String parInfo = TheApp.getKeyStoreManager().decrupt(encodedParInfo);
                int sep = parInfo.indexOf(":");
                int port = Integer.valueOf(parInfo.substring(0, sep));
                String aesKeyB64 = parInfo.substring(sep + 1);
                // recreate key
                byte[] aesKeyBuff = Base64.decode(aesKeyB64, Base64.DEFAULT);
                SecretKey aesKey = new SecretKeySpec(aesKeyBuff, "AES");
                Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
                String msgData = intent.getStringExtra(KEY_DATA_TO_COPY);
                byte[] clearTextBuff = msgData.getBytes();
                byte[] cipherTextBuff = aesCipher.doFinal(clearTextBuff);
                socket = new DatagramSocket(port);
                InetAddress group = InetAddress.getByName(ALL_HOSTS_MC_ADDRESS);
                //Sending to Multicast Group
                DatagramPacket packet = new DatagramPacket(cipherTextBuff, cipherTextBuff.length, group, port);
                socket.send(packet);
            } catch (Exception e) {
                LogEx.e(e.getMessage(), e);
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }
}
