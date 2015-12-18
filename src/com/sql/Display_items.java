package com.sql;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.redbear.bleselect.R;

public class Display_items extends Activity {
	SQLIte_manager manager;
	ListView ls;
	List<user> users;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_items);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle("Log Database");
		manager = new SQLIte_manager(this);
		manager.open_DB();
		ls = (ListView) findViewById(R.id.listView_sql);
		users = new ArrayList<user>();
		users = manager.getall();
		setlist();

		/*
		ls.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				ls.setAdapter(null);
				user us = (user) arg0.getAdapter().getItem(arg2);
				manager.delete(us);
				users = manager.getsorted(manager.BY_NAME);
				setlist();

				Toast.makeText(Display_items.this, "deleted user"+us.getName(), Toast.LENGTH_LONG).show();
				return true;
			}
		});
*/
	}

	private void setlist() {
		List_adapter adapter = new List_adapter(this, R.layout.display_items_list,
				users);
		ls.setAdapter(adapter);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.display_menu, menu);
		return true;
	}

	public void toastnotify(String state) {
		Toast.makeText(this, "Enter Field" + state, Toast.LENGTH_SHORT).show();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.back) {
			onBackPressed();
		}else if (id == R.id.by_id) {
			ls.setAdapter(null);
			users = manager.getsorted(manager.BY_ID);
			setlist();
			Toast.makeText(this, "Sorted by ID", Toast.LENGTH_LONG).show();

		} else if (id == R.id.delete_all) {
			ls.setAdapter(null);
			manager.deleteAll();
			users = manager.getall();
			setlist();

			Toast.makeText(this, "Delete ALL", Toast.LENGTH_LONG).show();
		}else if (id == android.R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

}
