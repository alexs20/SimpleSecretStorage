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
import com.wolandsoft.sss.security.AESCipher;
import com.wolandsoft.sss.util.LogEx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Copy to PC service
 *
 * @author Alexander Shulgin
 */
public class PcCommService extends IntentService {
    public static final int CMD_PING = 0;
    public static final int CMD_DATA = 1;
    public static final String KEY_DATA = "data";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CMD = "cmd";
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
            DatagramSocket socket = null;
            try {
                SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(this);
                int port = shPref.getInt(getString(R.string.pref_pc_receiver_port_key), 0);
                String keyB64 = shPref.getString(getString(R.string.pref_pc_receiver_key_key), null);
                byte[] encodedKey = Base64.decode(keyB64, Base64.DEFAULT);
                byte[] aesKey = TheApp.getCipher().decipher(encodedKey);
                System.out.println(Base64.encodeToString(aesKey, Base64.DEFAULT));
                AESCipher aesCipher = new AESCipher(aesKey);
                byte [] payload;
                if(CMD_PING == intent.getIntExtra(KEY_CMD, CMD_PING)){
                    payload = new byte [] {CMD_PING};
                } else {
                    String msgTitle = intent.getStringExtra(KEY_TITLE);
                    String msgData = intent.getStringExtra(KEY_DATA);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeByte(CMD_DATA);
                    byte [] msg = msgTitle.getBytes("UTF-8");
                    dos.writeInt(msg.length);
                    dos.write(msg);
                    msg = msgData.getBytes("UTF-8");
                    dos.writeInt(msg.length);
                    dos.write(msg);
                    dos.close();
                    payload = baos.toByteArray();
                }
                byte[] cipherTextBuff = aesCipher.cipher(payload);
                aesCipher.decipher(cipherTextBuff);
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
