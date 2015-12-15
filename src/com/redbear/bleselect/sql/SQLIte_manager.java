package com.redbear.bleselect.sql;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class SQLIte_manager {
	SQLiteOpenHelper dbhelper;
	SQLiteDatabase dbdata;
	Context context;
	public static int BY_NAME = 0;
	public static int BY_AGE = 1;
	private static String[] coloumns = { SQLite_helper.COLUMN_NAME,
			SQLite_helper.COLUMN_AGE, SQLite_helper.COLUMN_EMAIL,
			SQLite_helper.COLUMN_PHONE };

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
		info.put(SQLite_helper.COLUMN_NAME, us.getDate_connection());
		info.put(SQLite_helper.COLUMN_EMAIL, us.getDate_disconnection());
		info.put(SQLite_helper.COLUMN_PHONE, us.getRssi_conn());
		info.put(SQLite_helper.COLUMN_AGE, us.getRssi_discon());
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
					null, null, SQLite_helper.COLUMN_NAME + " ASC");
		} else {
			cur = dbdata.query(SQLite_helper.TABLE_USER, coloumns, null, null,
					null, null, SQLite_helper.COLUMN_AGE + " ASC");
		}

		database_pull(users, cur);

		return users;

	}

	private void database_pull(List<user> users, Cursor cur) {
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				user temp = new user();

				temp.setDate_connection(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_NAME)));

				temp.setDate_disconnection(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_EMAIL)));

				temp.setRssi_conn(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_AGE)));

				temp.setRssi_discon(cur.getString(cur
						.getColumnIndex(SQLite_helper.COLUMN_PHONE)));
				users.add(temp);
			}

		}
	}
	public void deleteAll() {
		dbdata.delete(SQLite_helper.TABLE_USER, null, null);

	}

	public void delete(user us) {
		dbhelper.onOpen(dbdata);
		dbdata.delete(SQLite_helper.TABLE_USER, SQLite_helper.COLUMN_NAME + "="
				+ us.getDate_connection(), null);
		dbhelper.close();
	}
}
