package com.redbear.bleselect.sql;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
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
		setContentView(R.layout.sql_list);

		manager = new SQLIte_manager(this);
		manager.open_DB();
		ls = (ListView) findViewById(R.id.listView_sql);
		users = new ArrayList<user>();
		users = manager.getall();
		setlist();
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

	}

	private void setlist() {
		List_adapter adapter = new List_adapter(this, R.layout.list_items,
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
		if (id == R.id.byname) {
			ls.setAdapter(null);
			users = manager.getsorted(manager.BY_NAME);
			setlist();
			Toast.makeText(this, "Sorted by name", Toast.LENGTH_LONG).show();

		} else if (id == R.id.byage) {
			ls.setAdapter(null);
			users = manager.getsorted(manager.BY_AGE);
			setlist();

			Toast.makeText(this, "Sorted by Age", Toast.LENGTH_LONG).show();
		}
		return super.onOptionsItemSelected(item);
	}
}
