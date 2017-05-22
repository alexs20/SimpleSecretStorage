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
package com.wolandsoft.sss.activity.fragment.entries;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.service.core.CoreServiceProxy;
import com.wolandsoft.sss.util.LogEx;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for {@link RecyclerView} component.
 *
 * @author Alexander Shulgin
 */
abstract class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private static final long DELAY_SEARCH_UPDATE = 1000;
    private final Handler mHandler;
    private final CoreServiceProxy mCore;
    private List<Integer> mItemIds = null;
    private String mSearchCriteria = "";
    private Runnable mSearchUpdate;
    private boolean mSearchLocked = false;

    RecyclerViewAdapter(CoreServiceProxy core) {
        mCore = core;
        mHandler = new Handler();
        mItemIds = Collections.emptyList();
    }

    void updateSearchCriteria(final String criteria) {
        LogEx.d("updateSearchCriteria(", criteria, ")");
        if (!mSearchLocked) {
            if (mSearchUpdate != null) {
                mHandler.removeCallbacks(mSearchUpdate);
            }
            mSearchUpdate = new Runnable() {
                @Override
                public void run() {
                    if (!criteria.equals(mSearchCriteria)) {
                        mSearchCriteria = criteria;
                        updateModel();
                    }
                }
            };
            mHandler.postDelayed(mSearchUpdate, DELAY_SEARCH_UPDATE);
        }
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LogEx.d("onCreateViewHolder(", parent, ",", viewType, ")");
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_entries_include_card, parent, false);
        return new RecyclerViewHolder(card);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        LogEx.d("onBindViewHolder(", holder, ",", position, ")");
        holder.itemView.setLongClickable(true);
        final SecretEntry entry = getItem(position);
        holder.mTxtTitle.setVisibility(View.INVISIBLE);
        holder.mTxtTitleSmall.setVisibility(View.GONE);
        String capTitle = "?";
        int next = 0;
        while (next < entry.size() && entry.get(next).isProtected()) {
            next++;
        }
        if (next < entry.size()) {
            capTitle = entry.get(next).getValue().trim();
            holder.mTxtTitle.setText(capTitle);
            holder.mTxtTitle.setVisibility(View.VISIBLE);
            next++;
        }
        while (next < entry.size() && entry.get(next).isProtected()) {
            next++;
        }
        if (next < entry.size()) {
            holder.mTxtTitleSmall.setText(entry.get(next).getValue());
            holder.mTxtTitleSmall.setVisibility(View.VISIBLE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick(entry);
            }
        });
        //make a colored capital character
        ColorGenerator generator = ColorGenerator.DEFAULT;
        int color = generator.getColor(capTitle);
        TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .bold()
                .endConfig()
                .buildRound(getIconChars(capTitle), color);
        holder.mImgIcon.setImageDrawable(drawable);
    }

    private String getIconChars(String title) {
        StringBuilder sb = new StringBuilder();
        String[] parts = title.trim().split(" ");
        String part = parts[0];
        if (part.length() == 0) {
            return "?";
        }
        String oneChar = part.substring(0, 1).toUpperCase();
        sb.append(oneChar);
        if (parts.length > 1) {
            part = parts[parts.length - 1];
            if (part.length() > 0) {
                oneChar = part.substring(0, 1).toUpperCase();
                sb.append(oneChar);
            }
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        LogEx.d("getItemCount()");
        return mItemIds.size();
    }

    String getSearchCriteria() {
        LogEx.d("getSearchCriteria()");
        return mSearchCriteria;
    }

    void setSearchLocked(boolean state) {
        LogEx.d("setSearchLocked(", state, ")");
        mSearchLocked = state;
    }

    void deleteItem(int id) {
        LogEx.d("deleteItem(", id, ")");
        int index = mItemIds.indexOf(id);
        mCore.deleteRecord(id);
        mItemIds.remove(index);
        notifyItemRemoved(index);
    }

    void updateModel() {
        LogEx.d("updateModel()");
        if (mCore.isServiceActive()) {
            mItemIds = mCore.findRecords(mSearchCriteria);
            notifyDataSetChanged();
        }
    }

    void reloadItem(int id) {
        LogEx.d("reloadItem(", id, ")");
        int index = mItemIds.indexOf(id);
        notifyItemChanged(index);
    }

    private SecretEntry getItem(int index) {
        LogEx.d("getItem(", index, ")");
        int id = mItemIds.get(index);
        SecretEntry entry = mCore.getRecord(id);

        LogEx.d(entry);
        return entry;
    }

    //ItemTouchHelper extension
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        LogEx.d("onAttachedToRecyclerView(", recyclerView, ")");
        super.onAttachedToRecyclerView(recyclerView);
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(0, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int index = viewHolder.getAdapterPosition();
                SecretEntry item = getItem(index);
                onItemDismiss(item);
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    abstract void onItemDismiss(SecretEntry entry);

    abstract void onItemClick(SecretEntry entry);
}
