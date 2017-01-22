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
package com.wolandsoft.sss.activity.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;

import com.wolandsoft.sss.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base fragment
 *
 * @author Alexander Shulgin
 */
public class BaseFragment extends Fragment {
    public void requestPermission(String[] permissions) {
        Context ctx = getContext();
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        Set<String> askedPermissions = shPref.getStringSet(getString(R.string.pref_permission_asked_type_key), new HashSet<String>());
        boolean askDirect = true;
        for (String permission : permissions) {
            int permissionGranted = ContextCompat.checkSelfPermission(ctx, permission);
            if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
                askDirect = askDirect && (!askedPermissions.contains(permission) || shouldShowRequestPermissionRationale(permission));
            }
        }
        if (askDirect) {
            requestPermissions(permissions, 0);
            Collections.addAll(askedPermissions, permissions);
            shPref.edit().putStringSet(getString(R.string.pref_permission_asked_type_key), askedPermissions).apply();
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivityForResult(intent, 0);
        }
    }
}
