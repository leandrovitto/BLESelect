package com.redbear.bleselect.sql;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.redbear.bleselect.R;

public class List_adapter extends ArrayAdapter<user> {

	private Context context;
	private List<user> listdata;

	public List_adapter(Context con, int resource, List<user> objects) {
		super(con, resource, objects);

		context = con;
		listdata = objects; // Initializing list data

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.list_items, parent, false);
		user temp = listdata.get(position);
		TextView name = (TextView) view.findViewById(R.id.name);
		TextView age = (TextView) view.findViewById(R.id.Age);
		TextView email = (TextView) view.findViewById(R.id.email);
		TextView phone = (TextView) view.findViewById(R.id.Phone);
		name.setText("User name :" + temp.getName());
		age.setText("Age : "+temp.getAge());
		email.setText("Email : "+temp.getEmail());
		phone.setText("Phone : "+temp.getPhone());
		return view;
	}
}
