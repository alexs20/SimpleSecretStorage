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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.wolandsoft.sss.service.proxy.ServiceProxy;

import java.io.IOException;

/**
 * Proxy wrapper for {@link PcCommService}.
 */

public class PcCommServiceProxy extends ContextWrapper implements ServiceProxy {

    public PcCommServiceProxy(Context base) {
        super(base);
    }

    public void ping(PairedDevice target) {
        Intent intent = new Intent(PcCommService.ACTION_PING, null, this, PcCommService.class);
        intent.putExtra(PcCommService.KEY_DEVICE, target);
        startService(intent);
    }

    public void sendData(PairedDevice target, String title, String data) {
        Intent intent = new Intent(PcCommService.ACTION_PAYLOAD, null, this, PcCommService.class);
        intent.putExtra(PcCommService.KEY_DEVICE, target);
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
