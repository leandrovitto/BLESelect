package com.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SQLite_helper extends SQLiteOpenHelper {
 
	private static final String DATABASE_NAME = "storage";
	private static final int DATABASE_VERSION = 1;
	public static final String TABLE_USER = "user";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_DATE_CONNECTION = "date_connection";
	public static final String COLUMN_DATE_DISCONNECTION = "date_disconnection";
	public static final String COLUMN_RSSI_CONN = "rssi_conn";
	public static final String COLUMN_RSSI_DISCONN = "rssi_discon";
	public static final String COLUMN_DEV = "dev";
	
	private static final String TABLE_CREATE = 
			"CREATE TABLE " + TABLE_USER + " (" +
					COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COLUMN_DATE_CONNECTION + " TEXT, " +
					COLUMN_DATE_DISCONNECTION + " TEXT, " +
					COLUMN_RSSI_CONN + " TEXT, " +
					COLUMN_RSSI_DISCONN + " TEXT, " +
					COLUMN_DEV + " TEXT ) ";
	
	public SQLite_helper(Context context) {

		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		//context.deleteDatabase(DATABASE_NAME);
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
