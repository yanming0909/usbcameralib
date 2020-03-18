package com.serenegiant.usb.mydb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySqliteHelper extends SQLiteOpenHelper {

    public static final String TAG = "MYSQLITEHELPER";

    public static final String CREATE_Movie= "create table t_movie (" +

            "id integer primary key, path varchar(1000), currentdate varchar(20),file_name varchar(30),audio_path varchar(30))";

    public MySqliteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,

                          int version) {

        super(context, name, factory, version);

    }

    @Override

    public void onOpen(SQLiteDatabase db) {

        Log.i(TAG,"open db");

        super.onOpen(db);

    }

    @Override

    public void onCreate(SQLiteDatabase db) {

        Log.i(TAG,"create db");

        Log.i(TAG,"before excSql");

        db.execSQL(CREATE_Movie);

        Log.i(TAG,"after excSql");

    }

    @Override

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


    }

}
