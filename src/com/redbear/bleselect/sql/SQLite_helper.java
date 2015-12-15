package com.redbear.bleselect.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class SQLite_helper extends SQLiteOpenHelper {
 
	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 1;	
	public static final String TABLE_USER = "user";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NAME = "date_connection";
	public static final String COLUMN_EMAIL = "date_disconnection";
	public static final String COLUMN_AGE = "rssi_conn";
	public static final String COLUMN_PHONE = "rssi_discon";
	
	private static final String TABLE_CREATE = 
			"CREATE TABLE " + TABLE_USER + " (" +
					COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COLUMN_NAME + " TEXT, " +
					COLUMN_AGE + " TEXT, " +
					COLUMN_EMAIL + " TEXT, " +
					COLUMN_PHONE + " NUMERIC " +
					")";		 			
	
	public SQLite_helper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	 	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS"+TABLE_CREATE);
	}
}
