package com.sql;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLIte_manager {
	SQLiteOpenHelper dbhelper;
	SQLiteDatabase dbdata;
	Context context;
	public static int BY_ID = 0;
	public static int BY_DATE_DISC = 1;
	private static String[] coloumns = { SQLite_helper.COLUMN_ID,SQLite_helper.COLUMN_DATE_CONNECTION,
			SQLite_helper.COLUMN_RSSI_DISCONN, SQLite_helper.COLUMN_DATE_DISCONNECTION,
			SQLite_helper.COLUMN_RSSI_CONN };

	public SQLIte_manager(Context con) {
		dbhelper = new SQLite_helper(con);
		context = con;
	}

	public void open_DB() {
		dbdata = dbhelper.getWritableDatabase();
  	}

	public void close_DB() {
		dbhelper.close();

	}

	public void create(user us) 
	{	ContentValues info = new ContentValues();
		info.put(SQLite_helper.COLUMN_DATE_CONNECTION, us.getDate_connection());
		info.put(SQLite_helper.COLUMN_DATE_DISCONNECTION, us.getDate_disconnection());
		info.put(SQLite_helper.COLUMN_RSSI_CONN, us.getRssi_conn());
		info.put(SQLite_helper.COLUMN_RSSI_DISCONN, us.getRssi_discon());
		dbdata.insert(SQLite_helper.TABLE_USER, null, info);
	  	}

	public List<user> getall(int i) {

		List<user> users = new ArrayList<user>();
		Cursor cur = dbdata.query(SQLite_helper.TABLE_USER, coloumns, null,
				null, null, null, null);

		database_pull(users, cur);

		return users;

	}

	public List<user> getall() {

		List<user> users = new ArrayList<user>();
		Cursor cur = dbdata.query(SQLite_helper.TABLE_USER, coloumns, null,
				null, null, null, null);

		database_pull(users, cur);

		return users;

	}

	public List<user> getsorted(int option) {

		List<user> users = new ArrayList<user>();
		Cursor cur;
		if (option == 0) {
			cur = dbdata.query(SQLite_helper.TABLE_USER, coloumns, null, null,
					null, null, SQLite_helper.COLUMN_ID + " DESC");
		} else {
			cur = dbdata.query(SQLite_helper.TABLE_USER, coloumns, null, null,
					null, null, SQLite_helper.COLUMN_DATE_DISCONNECTION + " ASC");
		}

		database_pull(users, cur);

		return users;

	}

	private void database_pull(List<user> users, Cursor cur) {
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				user temp = new user();

				temp.setId(cur.getString(cur.getColumnIndex(SQLite_helper.COLUMN_ID)));

				temp.setDate_connection(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_DATE_CONNECTION)));

				temp.setDate_disconnection(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_DATE_DISCONNECTION)));

				temp.setRssi_conn(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_RSSI_CONN)));

				temp.setRssi_discon(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_RSSI_DISCONN)));
				users.add(temp);
			}

		}
	}
	public void deleteAll() {
		dbdata.delete(SQLite_helper.TABLE_USER, null, null);

	}

	public void delete(user us) {
		dbhelper.onOpen(dbdata);
		dbdata.delete(SQLite_helper.TABLE_USER, SQLite_helper.COLUMN_DATE_CONNECTION + "="
				+ us.getDate_connection(), null);
		dbhelper.close();

	}

	public int getHighestID() {
		final String MY_QUERY = "SELECT MAX(id) FROM " + SQLite_helper.TABLE_USER;
		Cursor cur = dbdata.rawQuery(MY_QUERY, null);
		cur.moveToFirst();
		int ID = cur.getInt(0);
		cur.close();
		return ID;
	}

	public void update_RssiConnect(int id,String v){
		ContentValues con = new ContentValues();
		con.put(SQLite_helper.COLUMN_RSSI_CONN, v);
		dbdata.update(SQLite_helper.TABLE_USER, con, SQLite_helper.COLUMN_ID + "=" + id, null);
	}

	public void update_RssiDisconnect(int id,String v){
		ContentValues con = new ContentValues();
		con.put(SQLite_helper.COLUMN_RSSI_DISCONN, v);
		dbdata.update(SQLite_helper.TABLE_USER, con, SQLite_helper.COLUMN_ID + "=" + id,null);
	}
	public void update_DataDisconnect(int id,String v){
		ContentValues con = new ContentValues();
		con.put(SQLite_helper.COLUMN_DATE_DISCONNECTION, v);
		dbdata.update(SQLite_helper.TABLE_USER, con, SQLite_helper.COLUMN_ID + "=" + id,null);
	}
	public void update_DataConnect(int id,String v){
		ContentValues con = new ContentValues();
		con.put(SQLite_helper.COLUMN_DATE_CONNECTION, v);
		dbdata.update(SQLite_helper.TABLE_USER, con, SQLite_helper.COLUMN_ID + "=" + id,null);
	}
}
