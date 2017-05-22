package com.wolandsoft.sss.service.pccomm;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.wolandsoft.sss.service.proxy.ServiceProxy;

import java.io.IOException;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class PcCommServiceProxy extends ContextWrapper implements ServiceProxy {

    public PcCommServiceProxy(Context base) {
        super(base);
    }

    public void ping() {
        Intent intent = new Intent(PcCommService.ACTION_PING, null, this, PcCommService.class);
        startService(intent);
    }

    public void sendData(String title, String data) {
        Intent intent = new Intent(PcCommService.ACTION_PAYLOAD, null, this, PcCommService.class);
        intent.putExtra(PcCommService.KEY_TITLE, title);
        intent.putExtra(PcCommService.KEY_DATA, data);
        startService(intent);
    }

    @Override
    public boolean isServiceActive() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
