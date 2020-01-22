package com.junhwa.bleadvertising;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ItemViewHolder> implements OnItemClickListener {
    private ArrayList<UuidHistory> listData = new ArrayList<>();
    private OnItemClickListener listener;

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new ItemViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.onBind(listData.get(position));
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public void addItem(UuidHistory data) {
        listData.add(data);
        this.notifyDataSetChanged();
    }

    static public class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView textView1;

        ItemViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            textView1 = itemView.findViewById(R.id.textView2);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (listener != null)
                        listener.onItemClick(ItemViewHolder.this, view, position);
                }
            });
        }

        void onBind(UuidHistory data) {
            textView1.setText(data.getUuid().toString());
        }
    }

    public UuidHistory getData(int position) {
        return listData.get(position);
    }

    public void setOnItemListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onItemClick(ItemViewHolder holder, View view, int position) {
        if (listener != null)
            listener.onItemClick(holder, view, position);
    }

    public void clearData() {
        this.listData.clear();
        this.notifyDataSetChanged();
    }
}
