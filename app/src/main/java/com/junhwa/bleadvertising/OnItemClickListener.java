package com.junhwa.bleadvertising;

import android.view.View;

public interface OnItemClickListener {
    void onItemClick(RecyclerAdapter.ItemViewHolder holder, View view, int position);
}
