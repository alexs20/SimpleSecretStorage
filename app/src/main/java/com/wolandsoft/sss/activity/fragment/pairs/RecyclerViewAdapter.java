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
package com.wolandsoft.sss.activity.fragment.pairs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.entity.SecretEntryAttribute;
import com.wolandsoft.sss.service.core.CoreServiceProxy;
import com.wolandsoft.sss.util.KeySharedPreferences;
import com.wolandsoft.sss.util.LogEx;
import com.wolandsoft.sss.util.PairedDevices;

import java.util.Collections;

/**s
 * Adapter for {@link RecyclerView} component.
 */
abstract class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private final PairedDevices mDevices;

    RecyclerViewAdapter(PairedDevices devices) {
        mDevices = devices;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_pairs_include_card, parent, false);
        return new RecyclerViewHolder(card);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        holder.itemView.setLongClickable(true);
        final RecyclerViewHolder viewHolder = holder;
        final PairedDevices.PairedDevice device = mDevices.get(position);
        holder.mTxtTitle.setText(device.host);
    }

    @Override
    public int getItemCount() {
        LogEx.d("getItemCount()");
        return mDevices.size();
    }

    void deleteItem(int idx) {
        mDevices.remove(idx);
        notifyItemRemoved(idx);
    }

    void updateModel() {
        LogEx.d("updateModel()");

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
                onItemDelete(viewHolder.getAdapterPosition());
            }

        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    abstract void onItemDelete(int index);

}
