package com.sql;

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

		TextView id= (TextView) view.findViewById(R.id.identifier);
		TextView name = (TextView) view.findViewById(R.id.Data_Connessione);
		TextView age = (TextView) view.findViewById(R.id.Rssi_Conn);
		TextView email = (TextView) view.findViewById(R.id.Data_Disconnessione);
		TextView phone = (TextView) view.findViewById(R.id.Rssi_Disc);
		TextView dev=(TextView) view.findViewById(R.id.Dev);

		id.setText("ID:"+ temp.getId());
		name.setText("CONNESSO DATA:" + temp.getDate_connection());
		age.setText("RSSI CONN: "+temp.getRssi_conn());
		email.setText("DISCONNESSO DATA: "+temp.getDate_disconnection());
		phone.setText("RSSI DISCONN: "+temp.getRssi_discon());
		dev.setText("DEV:"+temp.getDev());
		return view;
	}
}
