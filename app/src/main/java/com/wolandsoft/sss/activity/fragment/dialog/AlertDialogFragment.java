package com.wolandsoft.sss.activity.fragment.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
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

    public static AlertDialogFragment newInstance(int iconId, int titleId, int messageId) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ICON, iconId);
        args.putInt(ARG_TITLE, titleId);
        args.putInt(ARG_MESSAGE, messageId);
        fragment.setArguments(args);
        return fragment;
    }

    public AlertDialogFragment() {
        // Required empty public constructor
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
        return new AlertDialog.Builder(getActivity())
                .setIcon(iconId)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                )
                .create();
    }


}
