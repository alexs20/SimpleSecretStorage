/*
    Copyright 2016 Alexander Shulgin

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
import android.widget.ListView;
import android.widget.TextView;

import com.wolandsoft.sss.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Alert dialog to be presented as fragment element.
 *
 * @author Alexander Shulgin
 */
public class FileDialogFragment extends DialogFragment {
    private static final String KEY_IS_FILE_CHOOSER = "file_chooser";
    private static final String KEY_CURRENT_PATH = "current_path";
    private final File mBasePath = Environment.getExternalStorageDirectory();
    private OnDialogToFragmentInteract mListener;
    private TextView mTxtSelectedFile;
    private FolderListAdapter mAdapter;
    private File mCurrentPath = mBasePath;
    private boolean mIsFileChooser = false;

    public static FileDialogFragment newInstance(boolean isFileChooser) {
        FileDialogFragment fragment = new FileDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_IS_FILE_CHOOSER, isFileChooser);
        fragment.setArguments(args);
        return fragment;
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
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            mIsFileChooser = args.getBoolean(KEY_IS_FILE_CHOOSER);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_file_list, (ViewGroup) null);
        ListView listView = (ListView) v.findViewById(R.id.lstItems);
        mTxtSelectedFile = (TextView) v.findViewById(R.id.txtTitle);
        if (savedInstanceState != null) {
            mCurrentPath = (File) savedInstanceState.getSerializable(KEY_CURRENT_PATH);
        }
        //mTxtSelectedFile.setText(getResources().getString(R.string.label_path_prefix) + mCurrentPath.getName());
        List<ListItem> list = loadFileList(mCurrentPath);
        mAdapter = new FolderListAdapter(list);
        listView.setAdapter(mAdapter);
        mTxtSelectedFile.setText(mIsFileChooser ? R.string.label_select_file : R.string.label_select_directory);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);
        if (!mIsFileChooser) {
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String selectedPath = mCurrentPath.getPath().substring(mBasePath.getPath().length());
                                    mListener.onFileSelected(mCurrentPath, selectedPath);
                                }
                            });
                            dialog.dismiss();
                        }
                    }
            );
        }
        final AlertDialog dialog = builder.create();
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ListItem li = (ListItem) mAdapter.getItem(position);
                        if (li.isBackButton) {
                            mCurrentPath = mCurrentPath.getParentFile();
                        } else {
                            mCurrentPath = new File(mCurrentPath, li.label);
                        }
                        final String selectedPath = mCurrentPath.getPath().substring(mBasePath.getPath().length());
                        if (li.isFile) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onFileSelected(mCurrentPath, selectedPath);
                                }
                            });
                            dialog.dismiss();
                            return;
                        }
                        mTxtSelectedFile.setText(selectedPath.length() > 0 ? selectedPath :
                                getString(mIsFileChooser ? R.string.label_select_file : R.string.label_select_directory));
                        List<ListItem> list = loadFileList(mCurrentPath);
                        mAdapter.updateModel(list);
                    }
                });
        return dialog;
    }

    private List<ListItem> loadFileList(File path) {
        File[] fileList;
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return (sel.isDirectory() || mIsFileChooser) && !filename.startsWith(".");
                }

            };
            fileList = path.listFiles(filter);
        } else {
            fileList = new File[0];
        }
        List<ListItem> list = new ArrayList<>();
        if (!path.equals(mBasePath)) {
            ListItem li = new ListItem();
            li.iconId = R.mipmap.img24dp_back;
            li.iconDescId = R.string.label_back;
            li.isBackButton = true;
            li.label = getString(R.string.label_back);
            list.add(li);
        }
        for (File file : fileList) {
            ListItem li = new ListItem();
            li.label = file.getName();
            if (file.isFile()) {
                li.isFile = true;
                li.iconId = R.mipmap.img24dp_file;
                li.iconDescId = R.string.label_file;
            }
            list.add(li);
        }
        Collections.sort(list);
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
        void onFileSelected(File path, String uiPath);
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
                convertView = mInflater.inflate(R.layout.dialog_file_include_card, parent, false);
            }
            ListItem item = (ListItem) getItem(position);
            ImageView img = (ImageView) convertView.findViewById(R.id.imgIcon);
            img.setImageResource(item.iconId);
            img.setContentDescription(parent.getContext().getString(item.iconDescId));
            TextView txt = (TextView) convertView.findViewById(R.id.txtFile);
            txt.setText(item.label);
            return convertView;
        }
    }

    private static class ListItem implements Comparable<ListItem> {
        boolean isFile = false;
        boolean isBackButton = false;
        String label;
        int iconId = R.mipmap.img24dp_directory;
        int iconDescId = R.string.label_directory;

        @Override
        public int compareTo(ListItem o) {
            if (o.isBackButton)
                return 1;

            if (o.isFile != isFile)
                return o.isFile ? 0 : 1;

            return label.compareTo(o.label);
        }
    }
}
