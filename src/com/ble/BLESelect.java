package com.ble;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.Thread;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.redbear.bleselect.R;
import com.sql.Display_items;
import com.sql.SQLIte_manager;
import com.sql.SQLite_helper;
import com.sql.user;


public class BLESelect extends Activity {
	private final static String TAG = BLESelect.class.getSimpleName();

	Intent gattServiceIntent;
	private BLService mBluetoothLeService;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	public static List<BluetoothDevice> mDevice = new ArrayList<BluetoothDevice>();
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	Menu menu_bleselect;
	MenuItem menu_ble_item;
	TextView uuidTv = null;
	TextView lastUuid = null;
	TextView parametro1 = null;
	TextView data_ora = null;
	TextView textStatus= null;
	//RSSI
	int valori_rssi;
	String rssi_avg;
	int k_conta_rssi=1;

	private static final int REQUEST_ENABLE_BT = 1;
	static final long SCAN_PERIOD = 2000;
	public static final int REQUEST_CODE = 30;
	private String mDeviceAddress;
	private String mDeviceName;

	private boolean flag = true;
	private boolean first_run=true;
	boolean mScanning = false;
	private boolean ConnessioneBLEState=false;

	//String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	//String fname = "flash.txt";
	SQLIte_manager manager;
	String date;
	private Timer timer;
	private TimerTask timerTask;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName,IBinder service) {
			mBluetoothLeService = ((BLService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.e(TAG, "onServiceDisconnected");
		}
	};

	private void empty_View_disconnect(){
		uuidTv.setText("");
		parametro1.setText("");
		first_run=true;//per il calcolo dell'rssi
		flag = false;//connessione spenta
	}

	private void toast_notify(String data){
		Toast.makeText(getApplicationContext(), data,Toast.LENGTH_LONG).show();
	}

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (BLService.ACTION_GATT_CONNECTED.equals(action)) {
				flag = true;
				lastUuid.setText(mDeviceName + " ( " + mDeviceAddress + " )");
						//lastDeviceBtn.setVisibility(View.GONE);
				uuidTv.setText("AVG RSSI...");
						//scanAllBtn.setText("Disconnect");
				//*************************
				//-----------------------------------------SQL INSERT
				manager=new SQLIte_manager(BLESelect.this);
				manager.open_DB();
				user u=new user();
				u.setDate_connection(date);
				u.setDev(mDeviceName + " ( " + mDeviceAddress + " )");
				manager.create(u);
				generateTone(300, 250).play();
				//-----------------------------------------
				toast_notify("Saved:Connected @ " + date);
				startReadRssi();
			} else if (BLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				empty_View_disconnect();
				manager=new SQLIte_manager(BLESelect.this);
				manager.open_DB();
				manager.update_DataDisconnect(manager.getHighestID(), date);
				manager.update_RssiDisconnect(manager.getHighestID(), rssi_avg);
				generateTone(360, 250).play();
				toast_notify("Saved:Disconnected @ " + date);
			} else if (BLService.ACTION_GATT_RSSI.equals(action)) {
				//EFFETTUO UNA MEDIA DI 20 VALORI e LI VISUALIZZO
				//RSSI Ha oscillazioni molto forti causa onde radio
					valori_rssi+=Integer.parseInt(intent.getStringExtra(BLService.EXTRA_RSSI));
					k_conta_rssi++;

				if(k_conta_rssi==20){
						rssi_avg=String.valueOf(valori_rssi / 20);
						displayData(rssi_avg);//visualizzo RSSI
						if(first_run){
							first_run=false;
							manager=new SQLIte_manager(BLESelect.this);
							manager.open_DB();
							manager.update_RssiConnect(manager.getHighestID(), rssi_avg);

						}
						k_conta_rssi=1;
						valori_rssi=0;
					}

			} else if (BLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				Log.e(TAG, "ACTION_GATT_SERVICES_DISCOVERED" + mNotifyCharacteristic);
				if(mNotifyCharacteristic!=null)
					startReadCharacter(mNotifyCharacteristic);
			} else if (BLService.ACTION_DATA_AVAILABLE.equals(action)) {
				Log.e(TAG, "ACTION_DATA_AVAILABLE");
				displayData_STM32(intent.getStringExtra(BLService.EXTRA_DATA));
			}

		}
	};


	@SuppressWarnings("resource")
	private boolean readConnDevice() {
		manager=new SQLIte_manager(BLESelect.this);
		manager.open_DB();
		String connDeviceInfo=null;
		lastUuid.setText("");
		if(manager.getHighestID()>0){
			connDeviceInfo=manager.get_Device(manager.getHighestID());
			mDeviceName = connDeviceInfo.split("\\( ")[0].trim();
			String str = connDeviceInfo.split("\\( ")[1];
			mDeviceAddress = str.substring(0, str.length() - 2);
			lastUuid.setText(connDeviceInfo);
			return true;
		}
		return false;
	}


	private void displayData(String data) {
		if (data != null) {
			int dBm=Integer.parseInt(data);
			Log.d(TAG, "RSSI:" + dBm);
			uuidTv.setText("RSSI:\t" + dBm + " dBm");
		}
	}

	private void displayData_STM32(String data) {
		if (data != null) {
			parametro1.setText(data);
		}
	}

	private void startReadRssi() {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readRssi();

					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			;
		}.start();
	}


	private void startReadCharacter(final BluetoothGattCharacteristic cha) {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readCharacteristic(cha);
					Log.e(TAG, "startReadCharacter:" + cha.getValue());
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			;
		}.start();
	}






	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BLService.ACTION_GATT_RSSI);
		intentFilter.addAction(BLService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}


	@Override
	protected void onResume() {
		super.onResume();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

	}
	@Override
	protected void onStop() {
		super.onStop();
		//flag = false;
		// unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mServiceConnection != null)
			unbindService(mServiceConnection);

		System.exit(0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//l'activity result è l'operazione inversa dell'invio di dati tramite INTENT ad un altra activity
		//con questa recupero le informazioni elaborate dalla seconda acitvity
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		} else if (requestCode == REQUEST_CODE
				&& resultCode == Device.RESULT_CODE) {
			mDeviceAddress = data.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
			mDeviceName = data.getStringExtra(Device.EXTRA_DEVICE_NAME);
			ConnessioneBLEState=mBluetoothLeService.connect(mDeviceAddress);
			menu_bleselect.findItem(R.id.connect_memory).setTitle("Disconnect");

		}

		super.onActivityResult(requestCode, resultCode, data);
	}


	void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {

				mScanning = true;
				mBluetoothAdapter.startLeScan(mLeScanCallback);
				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mScanning = false;
			}
		}.start();
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
							 byte[] scanRecord) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (device != null) {
						if (mDevice.indexOf(device) == -1)
							mDevice.add(device);
					}
				}
			});
		}
	};

	//displayGattServices:ricava i servizi e le caratteristiche del GATT SERVER ma ne elabora solo la 0' del 2' servizio
	//la scheda stm32 per come l'ho programmata emette due servizi GATT uno read lo 0 dove scrive i dati dei sensori
	//e uno wrire il 1 dove si aspetta dati che l'applicazione potrebbe mandare a sua volta verso la scheda STM32
	//0 e 1 servizio sono per i GAP SERVER contengono solo informazioni del device

	private void displayGattServices(List<BluetoothGattService> gattServices) {

		if (gattServices == null) return;
		String uuid = null;
		String unknownServiceString = getResources().getString(R.string.unknown_service);
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
				= new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			Log.e(TAG, "BluetoothGattService: " + gattService.getUuid().toString());
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(
					LIST_NAME, BLGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
					new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics =
					gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas =
					new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				Log.e(TAG, "BluetoothCharacteristic: " + gattCharacteristic.getUuid().toString());
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(
						LIST_NAME, BLGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);
				gattCharacteristicGroupData.add(currentCharaData);
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);

		}

		/*SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
				this,
				gattServiceData,
				android.R.layout.simple_expandable_list_item_2,
				new String[] {LIST_NAME, LIST_UUID},
				new int[] { android.R.id.text1, android.R.id.text2 },
				gattCharacteristicData,
				android.R.layout.simple_expandable_list_item_2,
				new String[] {LIST_NAME, LIST_UUID},
				new int[] { android.R.id.text1, android.R.id.text2}
		);*/
		//mGattServicesList.setAdapter(gattServiceAdapter);
		if (mGattCharacteristics != null) {
			final BluetoothGattCharacteristic characteristic =
					mGattCharacteristics.get(2).get(0
					);
			final int charaProp = characteristic.getProperties();
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
				// If there is an active notification on a characteristic, clear
				// it first so it doesn't update the data field on the user interface.
				if (mNotifyCharacteristic != null) {
					//mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
					//mNotifyCharacteristic = null;
				}
				mBluetoothLeService.readCharacteristic(characteristic);

			}
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
				mNotifyCharacteristic = characteristic;
				BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
						UUID.fromString(BLGattAttributes.STM32_ACCELEROMETER_PARAMETER), 1);

				characteristic.addDescriptor(descriptor);

				mBluetoothLeService.setCharacteristicNotification(characteristic, true);
			}
			//return true;
			for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
				Log.e(TAG, "BluetoothGattDescriptor: " + descriptor.getUuid().toString());
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_log) {
			Intent i = new Intent(getApplicationContext(),Display_items.class);
			startActivity(i);
		}else if(id==R.id.scanning){
				//scanAllBtn.setActionView(R.layout.actionbar_indeterminate_progress);
			    menu_bleselect.findItem(R.id.scanning).setActionView(R.layout.actionbar_indeterminate_progress);
				scanLeDevice();
			    handler_scanning.postDelayed(updateTask_scanning, 100);
				/*try {

					Thread.sleep(BLESelect.SCAN_PERIOD);
					menu_bleselect.findItem(R.id.scanning).setActionView(R.layout.actionbar_indeterminate_progress);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/

		}else if(id==R.id.connect_memory){
			 mDevice.clear();
			if (ConnessioneBLEState == false) {
				if (!readConnDevice()) {
					textStatus.setText("No Memory Device");
				} else {
					scanLeDevice();
					ConnessioneBLEState = mBluetoothLeService.connect(mDeviceAddress);
					menu_bleselect.findItem(R.id.connect_memory).setTitle("Disconnect");
				}
			}else{
				mBluetoothLeService.disconnect();
				ConnessioneBLEState=false;
				mBluetoothLeService.close();
				parametro1.setText("");
				uuidTv.setText("");
				manager=new SQLIte_manager(BLESelect.this);
				manager.open_DB();
				manager.update_DataDisconnect(manager.getHighestID(), date);
				manager.update_RssiDisconnect(manager.getHighestID(), rssi_avg);
				menu_bleselect.findItem(R.id.connect_memory).setTitle(R.string.connect_Memory);
				generateTone(330, 250).play();

			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.bleselect, menu);
		menu_bleselect = menu;
		return true;
	}

	Handler handler=new Handler();
	final Runnable updateTask=new Runnable() {
		@Override
		public void run() {
			date = (DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()).toString());
			data_ora.setText(date);
			textStatus.setText("STATE: " + mBluetoothLeService.BLE_STATUS_CONNECTION_STRING);
			handler.postDelayed(this,1000);
		}
	};

	Handler handler_scanning=new Handler();
	final Runnable updateTask_scanning=new Runnable() {
		@Override
		public void run() {
			if(mScanning){
				  handler.postDelayed(this,100);
			}else{
				menu_bleselect.findItem(R.id.scanning).setActionView(null);
				Intent intent = new Intent(getApplicationContext(),Device.class);
				startActivityForResult(intent, REQUEST_CODE);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
		uuidTv = (TextView) findViewById(R.id.uuid);
		lastUuid = (TextView) findViewById(R.id.lastDevice);
		parametro1 = (TextView) findViewById(R.id.param1);
		data_ora=(TextView)findViewById(R.id.textDataOra);
		textStatus=(TextView)findViewById(R.id.textStatus);
		handler.postDelayed(updateTask, 1000);
		//********************
		readConnDevice();
		/*
				Timer mNewTimer = new Timer();
				mNewTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						for (BluetoothDevice device : mDevice)
							if ((device.getAddress().equals(mDeviceAddress))) {
								ConnessioneBLEState=mBluetoothLeService.connect(mDeviceAddress);

								return;
							}

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast toast = Toast.makeText(BLESelect.this,
										"No Last connect device!",
										Toast.LENGTH_SHORT);
								toast.setGravity(Gravity.CENTER, 0, 0);
								toast.show();
							}
						});

					}
				}, SCAN_PERIOD);*/

		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		gattServiceIntent = new Intent(BLESelect.this, BLService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private AudioTrack generateTone(double freqHz, int durationMs)
	{
		int count = (int)(44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
		short[] samples = new short[count];
		for(int i = 0; i < count; i += 2){
			short sample = (short)(Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF);
			samples[i + 0] = sample;
			samples[i + 1] = sample;
		}
		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
		track.write(samples, 0, count);
		return track;
	}

}