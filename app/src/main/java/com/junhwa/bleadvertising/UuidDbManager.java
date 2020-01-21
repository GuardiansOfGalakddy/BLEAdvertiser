package com.junhwa.bleadvertising;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UuidDbManager {
    static final String UUID_DB = "UUID.db";
    static final String UUID_TABLE = "LVL";
    static final int DB_VERSION = 1;

    Context mContext = null;

    private static UuidDbManager mDbManager = null;
    private SQLiteDatabase mDatabase = null;

    private UuidDbManager(Context context) {
        this.mContext = context;

        mDatabase = this.mContext.openOrCreateDatabase(UUID_DB, Context.MODE_PRIVATE, null);

        mDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + UUID_TABLE +
                        "(uuid  TEXT PRIMARY KEY UNIQUE)");
    }

    public static UuidDbManager getInstance(Context context) {
        if (mDbManager == null)
            mDbManager = new UuidDbManager(context);
        return mDbManager;
    }

    public long insert(ContentValues addRowValue) {
        return mDatabase.insert(UUID_TABLE, null, addRowValue);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    public Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        return mDatabase.query(UUID_TABLE, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    public int update(ContentValues updateRowValue, String whereClause, String[] whereArgs) {
        return mDatabase.update(UUID_TABLE, updateRowValue, whereClause, whereArgs);
    }

    public int delete(String whereClause, String[] whereArgs) {
        return mDatabase.delete(UUID_TABLE, whereClause, whereArgs);
    }

    public Cursor getUuid() {
        String[] columns = new String[]{"uuid"};
        Cursor cursor = mDbManager.query(columns, null, null, null, null, null);
        return cursor;
    }
}
