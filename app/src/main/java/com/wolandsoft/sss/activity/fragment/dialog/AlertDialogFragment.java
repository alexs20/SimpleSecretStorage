package com.wolandsoft.sss.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.wolandsoft.sss.R;

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
    private OnDialogToFragmentInteract mListener;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Fragment parent = getTargetFragment();
        if (parent instanceof OnDialogToFragmentInteract) {
            mListener = (OnDialogToFragmentInteract) parent;
        } else {
            throw new ClassCastException(
                    String.format(
                            getString(R.string.internal_exception_must_implement),
                            parent.toString(),
                            OnDialogToFragmentInteract.class.getName()
                    )
            );
        }
        Bundle args = getArguments();
        int iconId = args.getInt(ARG_ICON);
        int titleId = args.getInt(ARG_TITLE);
        int messageId = args.getInt(ARG_MESSAGE);
        boolean isYesNo = args.getBoolean(ARG_YESNO);
        final Bundle data = args.getBundle(ARG_DATA);
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setIcon(iconId)
                        .setTitle(titleId)
                        .setMessage(messageId);
        if (isYesNo) {
            builder.setPositiveButton(android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    mListener.onDialogResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                                }
                            });
                            dialog.dismiss();
                        }
                    }
            ).setNegativeButton(android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    mListener.onDialogResult(getTargetRequestCode(), Activity.RESULT_CANCELED, data);
                                }
                            });
                            dialog.dismiss();
                        }
                    }
            );
        } else {
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    mListener.onDialogResult(getTargetRequestCode(), Activity.RESULT_CANCELED, data);
                                }
                            });
                            dialog.dismiss();
                        }
                    }
            );
        }
        return builder.create();
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    public interface OnDialogToFragmentInteract {
        void onDialogResult(int requestCode, int result, Bundle args);
    }

}
