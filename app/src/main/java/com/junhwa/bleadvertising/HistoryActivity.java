package com.junhwa.bleadvertising;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import java.util.UUID;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        final Context context = this;
        adapter = new RecyclerAdapter();
        adapter.setOnItemListener(new OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerAdapter.ItemViewHolder holder, View view, int position) {
                final UuidHistory data = adapter.getData(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete").setMessage(data.getUuid().toString());
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        UuidDbManager.getInstance(getApplicationContext()).delete("uuid=\"" + data.getUuid().toString() + "\"", null);
                        notifyToRecyclerView();
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        notifyToRecyclerView();
    }

    private void notifyToRecyclerView() {
        adapter.clearData();
        Cursor cursor = UuidDbManager.getInstance(getApplicationContext()).getUuid();
        while (cursor.moveToNext()) {
            adapter.addItem(new UuidHistory(UUID.fromString(cursor.getString(0))));
        }
        adapter.notifyDataSetChanged();
    }
}
