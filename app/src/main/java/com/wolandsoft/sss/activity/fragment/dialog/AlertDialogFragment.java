package com.wolandsoft.sss.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Alert dialog to be presented as fragment element.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class AlertDialogFragment extends DialogFragment {
    private static final String ARG_ICON = "icon";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_YESNO = "yes_no";
    private static final String ARG_DATA = "data";

    public AlertDialogFragment() {
        // Required empty public constructor
    }

    public static AlertDialogFragment newInstance(int iconId, int titleId, int messageId, boolean isYesNo, Bundle data) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ICON, iconId);
        args.putInt(ARG_TITLE, titleId);
        args.putInt(ARG_MESSAGE, messageId);
        args.putBoolean(ARG_YESNO, isYesNo);
        args.putBundle(ARG_DATA, data);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int iconId = getArguments().getInt(ARG_ICON);
        int titleId = getArguments().getInt(ARG_TITLE);
        int messageId = getArguments().getInt(ARG_MESSAGE);
        boolean isYesNo = getArguments().getBoolean(ARG_YESNO);
        Bundle data = getArguments().getBundle(ARG_DATA);
        final Intent intent = new Intent();
        if(data != null) {
            intent.putExtras(data);
        }
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setIcon(iconId)
                        .setTitle(titleId)
                        .setMessage(messageId);
        if (isYesNo) {
            builder.setPositiveButton(android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                            dialog.dismiss();
                        }
                    }
            ).setNegativeButton(android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
                            dialog.dismiss();
                        }
                    }
            );
        } else {
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
                            dialog.dismiss();
                        }
                    }
            );
        }
        return builder.create();
    }


}
