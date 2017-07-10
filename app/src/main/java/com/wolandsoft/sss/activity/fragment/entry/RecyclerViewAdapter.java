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
package com.wolandsoft.sss.activity.fragment.entry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.service.core.CoreServiceProxy;
import com.wolandsoft.sss.service.pccomm.PairedDevice;
import com.wolandsoft.sss.service.pccomm.PairedDevices;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;

import java.util.Collections;

/**
 * Adapter for {@link RecyclerView} component.
 */
abstract class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private final CoreServiceProxy mCore;
    private SecretEntry mEntry;
    private boolean mIsProtectedVisible = false;

    RecyclerViewAdapter(CoreServiceProxy core, int itemId) {
        mCore = core;
        mEntry = new SecretEntry(itemId, 0, 0);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entry_include_card, parent, false);
        return new RecyclerViewHolder(card);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        holder.itemView.setLongClickable(true);
        final RecyclerViewHolder viewHolder = holder;
        final SecretEntryAttribute attr = mEntry.get(position);

        holder.mTxtKey.setText(attr.getKey());
        if (!attr.isProtected()) {
            holder.mTxtValue.setText(attr.getValue());
            holder.mImgProtected.setVisibility(View.GONE);
        } else {
            holder.mTxtValue.setText("");
            if (attr.getValue() != null && attr.getValue().length() > 0) {
                if (mIsProtectedVisible) {
                    holder.mTxtValue.setText(attr.getValue());
                } else {
                    if (attr.getValue().length() > 0)
                        holder.mTxtValue.setText(R.string.label_hidden_password);
                    else
                        holder.mTxtValue.setText("");
                }
            }
        }
        holder.mImgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPopup(v, viewHolder.getLayoutPosition(), attr);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemEdit(viewHolder.getLayoutPosition());
            }
        });
    }

    @SuppressWarnings("UnusedParameters")
    @SuppressLint("StringFormatInvalid")
    private void openPopup(final View v, final int position, final SecretEntryAttribute attr) {
        LogEx.d("openPopup(", v, position, attr, ")");
        Context context = v.getContext();
        PopupMenu popup = new PopupMenu(context, v);
        popup.getMenuInflater().inflate(R.menu.fragment_entry_card_popup, popup.getMenu());

        //creating dynamic menu entries with mappings to unique view IDs
        String copyTo = context.getResources().getString(R.string.label_copy_to_pc);
        final SparseArray<Integer> vidMap = new SparseArray<>();
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        KeySharedPreferences ksPref = new KeySharedPreferences(shPref, context);
        String devicesJson = ksPref.getString(R.string.pref_paired_devices_key, (Integer)null);
        final PairedDevices devices = PairedDevices.fromJson(devicesJson);
        for(int i = 0; i < devices.size(); i++){
            int vid = View.generateViewId();
            vidMap.put(vid, i);
            PairedDevice device = devices.get(i);
            popup.getMenu().add(0, vid, i, String.format(copyTo, device.mHost));
            popup.getMenu().findItem(vid).setIcon(R.mipmap.img24dp_pc);
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.mnuCopy:
                        onItemCopy(attr, null);
                        break;
                    case R.id.mnuNavigate:
                        onItemNavigate(attr);
                        break;
                    default:
                        int idx = vidMap.get(menuItem.getItemId());
                        PairedDevice device = devices.get(idx);
                        onItemCopy(attr, device);
                }
                return false;
            }
        });
        if (attr.getValue().startsWith("http://") || attr.getValue().startsWith("https://")) {
            MenuItem mnuNavigate = popup.getMenu().findItem(R.id.mnuNavigate);

            mnuNavigate.setVisible(true);
        }

        MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popup.getMenu(), v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    @Override
    public int getItemCount() {
        LogEx.d("getItemCount()");
        return mEntry.size();
    }

    SecretEntry getEntry() {
        return mEntry;
    }

    void deleteItem(int idx) {
        mEntry.remove(idx);
        mCore.putRecord(mEntry);
        notifyItemRemoved(idx);
    }

    void updateItem(SecretEntryAttribute attr, int index) {
        if (index < mEntry.size()) {
            mEntry.set(index, attr);
            mCore.putRecord(mEntry);
            notifyItemChanged(index);
        } else {
            mEntry.add(attr);
            mCore.putRecord(mEntry);
            notifyItemInserted(index);
        }
    }

    void updateModel() {
        LogEx.d("updateModel()");
        if (mCore.isServiceActive()) {
            mEntry = mCore.getRecord(mEntry.getID());
            notifyDataSetChanged();
        }
    }

    void setProtectedVisible(boolean isProtectedVisible) {
        mIsProtectedVisible = isProtectedVisible;
        for (int i = 0; i < mEntry.size(); i++) {
            if (mEntry.get(i).isProtected()) {
                notifyItemChanged(i);
            }
        }
    }

    //ItemTouchHelper extension
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        LogEx.d("onAttachedToRecyclerView(", recyclerView, ")");
        super.onAttachedToRecyclerView(recyclerView);
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return onItemReorder(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                onItemDelete(viewHolder.getAdapterPosition());
            }

        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    private boolean onItemReorder(int fromPosition, int toPosition) {
        LogEx.d("onItemReorder(", fromPosition, ",", toPosition, ")");
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mEntry, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mEntry, i, i - 1);
            }
        }
        mCore.putRecord(mEntry);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    abstract void onItemDelete(int index);

    abstract void onItemEdit(int index);

    abstract void onItemNavigate(SecretEntryAttribute attr);

    abstract void onItemCopy(SecretEntryAttribute attr, PairedDevice device);
}
