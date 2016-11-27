package com.wolandsoft.sss.activity.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.util.LogEx;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Alert dialog to be presented as fragment element.
 *
 * @author Alexander Shulgin /alexs20@gmail.com/
 */
public class DirectoryDialogFragment extends DialogFragment {
    private static final String KEY_CURRENT_PATH = "current_path";
    private OnDialogToFragmentInteract mListener;

    private FolderListAdapter mAdapter;

    private File mBasePath = Environment.getExternalStorageDirectory();
    private File mCurrentPath = mBasePath;

    public static DirectoryDialogFragment newInstance(/*int iconId, int titleId, int messageId, boolean isYesNo, Bundle data*/) {
        DirectoryDialogFragment fragment = new DirectoryDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentPath = (File) savedInstanceState.getSerializable(KEY_CURRENT_PATH);
        }
        List<ListItem> list = loadFileList(mCurrentPath);
        mAdapter = new FolderListAdapter(list);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.label_select_directory) + "\n" + mCurrentPath.toString());
        builder.setAdapter(mAdapter, null);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onDirectorySelected(mCurrentPath);
                            }
                        });
                        dialog.dismiss();
                    }
                }
        );
        final AlertDialog dialog = builder.create();
        dialog.getListView().setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ListItem li = (ListItem) mAdapter.getItem(position);
                        if (li.isBackButton) {
                            mCurrentPath = mCurrentPath.getParentFile();
                        } else {
                            mCurrentPath = new File(mCurrentPath, li.label);
                        }
                        List<ListItem> list = loadFileList(mCurrentPath);
                        mAdapter.updateModel(list);
                        dialog.setTitle(getString(R.string.label_select_directory) + "\n" + mCurrentPath.toString());
                    }
                });
        return dialog;
    }

    private void navigate(File currentPath, boolean goInside) {

    }

    private List<ListItem> loadFileList(File path) {
        String[] fileList;
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isDirectory() && !filename.startsWith(".");
                }

            };
            fileList = path.list(filter);
        } else {
            fileList = new String[0];
        }
        List<ListItem> list = new ArrayList<>();
        if (!path.equals(mBasePath)) {
            ListItem li = new ListItem();
            li.iconId = R.mipmap.img24dp_back;
            li.isBackButton = true;
            li.label = getString(R.string.label_back);
            list.add(li);
        }
        for (String file : fileList) {
            ListItem li = new ListItem();
            li.label = file;
            list.add(li);
        }
        return list;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_CURRENT_PATH, mCurrentPath);
    }

    /**
     * This interface should be implemented by parent fragment in order to receive callbacks from this fragment.
     */
    public interface OnDialogToFragmentInteract {
        void onDirectorySelected(File path);
    }

    public static class FolderListAdapter extends BaseAdapter {
        private List<ListItem> mFileList;

        public FolderListAdapter(List<ListItem> fileList) {
            mFileList = fileList;
        }

        public void updateModel(List<ListItem> fileList) {
            mFileList = fileList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFileList.size();
        }

        @Override
        public Object getItem(int position) {
            return mFileList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater) parent.getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                convertView = mInflater.inflate(R.layout.dialog_folder_include_card, null);
            }
            ListItem item = (ListItem) getItem(position);
            ImageView img = (ImageView) convertView.findViewById(R.id.imgIcon);
            img.setImageResource(item.iconId);
            TextView txt = (TextView) convertView.findViewById(R.id.txtDirectory);
            txt.setText(item.label);
            return convertView;
        }
    }

    private static class ListItem {
        boolean isBackButton = false;
        String label;
        int iconId = R.mipmap.img24dp_directory;
    }
}
