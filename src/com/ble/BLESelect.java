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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
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
	//private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();
	public static List<BluetoothDevice> mDevice = new ArrayList<BluetoothDevice>();
	;

	private BluetoothGattCharacteristic mWriteCharacteristic, mReadCharacteristic;
	private ExpandableListView mGattServicesList;

	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
	String ConnessioneBLEState_message;

	Button lastDeviceBtn = null;
	Button scanAllBtn = null;
	Button logBtn = null;
	TextView uuidTv = null;
	TextView lastUuid = null;
	TextView parametro1 = null;
	TextView data_ora = null;
	TextView textStatus= null;
	int valori_rssi;
	String rssi_avg;
	int k_conta_rssi=1;
	private static final int REQUEST_ENABLE_BT = 1;
	static final long SCAN_PERIOD = 2000;
	public static final int REQUEST_CODE = 30;
	private TextView mConnectionState;
	private TextView mDataField;
	private String mDeviceAddress;
	private String mDeviceName;
	private boolean flag = true;
	private boolean first_run=true;
	private boolean connState = false;
	private static final int MSG_HUMIDITY = 101;
	private boolean ConnessioneBLEState=false;
	String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	String fname = "flash.txt";
	SQLIte_manager manager;
	SQLite_helper hlep;
	String date;
	private Timer timer;
	private TimerTask timerTask;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
									   IBinder service) {
			mBluetoothLeService = ((BLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.e(TAG, "onServiceDisconnected");
			mBluetoothLeService = null;

		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (BLService.ACTION_GATT_CONNECTED.equals(action)) {
				flag = true;
				connState = true;
				//writeToFile(mDeviceName + " ( " + mDeviceAddress + " )");
				lastUuid.setText(mDeviceName + " ( " + mDeviceAddress + " )");
				lastDeviceBtn.setVisibility(View.GONE);
				logBtn.setVisibility(View.GONE);
				uuidTv.setText("AVG RSSI...");
				scanAllBtn.setText("Disconnect");
				//*************************
				//SQL INSERT
				manager=new SQLIte_manager(BLESelect.this);
				manager.open_DB();
				//manager.deleteAll();
				user u=new user();
				date = (DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()).toString());
				u.setDate_connection(date);
				manager.create(u);
				generateTone(300, 250).play();
				Toast.makeText(getApplicationContext(), "Saved:Connected @ "+date,
						Toast.LENGTH_LONG).show();
				startReadRssi();
			} else if (BLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				flag = false;
				connState = false;
				first_run=true;


				parametro1.setText("");
				manager=new SQLIte_manager(BLESelect.this);
				manager.open_DB();
				manager.update_DataDisconnect(manager.getHighestID(), date);
				manager.update_RssiDisconnect(manager.getHighestID(), rssi_avg);
				generateTone(360, 250).play();
				Toast.makeText(getApplicationContext(), "Saved:Disconnected @ " + date,
						Toast.LENGTH_LONG).show();
				//scanLeDevice();
				ConnessioneBLEState=mBluetoothLeService.connect(mDeviceAddress);
			} else if (BLService.ACTION_GATT_RSSI.equals(action)) {
				//EFFETTUO UNA MEDIA DI 10 VALORI e LI VISUALIZZO
				//RSSI Ha oscillazioni molto forti causa onde radio

					valori_rssi+=Integer.parseInt(
							intent.getStringExtra(BLService.EXTRA_DATA));
					k_conta_rssi++;
					if(k_conta_rssi==20){
						//Log.i(TAG, "|||" + valori_rssi / 20);
						rssi_avg=String.valueOf(valori_rssi / 20);
						displayData(rssi_avg);
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
				if(mNotifyCharacteristic!=null)
					displayCharacteristic_STM32(mNotifyCharacteristic);
			}

		}
	};


	private void writeToFile(String flash) {
		File sdfile = new File(path, fname);
		try {
			FileOutputStream out = new FileOutputStream(sdfile);
			out.write(flash.getBytes());
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	private String readConnDevice() {
		String filepath = path + "/" + fname;
		String line = null;

		File file = new File(filepath);
		try {
			FileInputStream f = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(f, "GB2312");
			BufferedReader dr = new BufferedReader(isr);
			line = dr.readLine();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return line;
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
			parametro1.setText("Accelerometro:\t" + data);
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

	Handler handler=new Handler();

	final Runnable updateTask=new Runnable() {
		@Override
		public void run() {
			date = (DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()).toString());
			data_ora.setText(date);
			ConnessioneBLEState_message=mBluetoothLeService.BLE_STATUS_CONNECTION_STRING;
			textStatus.setText(mBluetoothLeService.BLE_STATUS_CONNECTION_STRING);
			handler.postDelayed(this,1000);
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

		date = (DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()).toString());
		data_ora.setText(date);

		handler.postDelayed(updateTask, 1000);
		//********************

		String connDeviceInfo = readConnDevice();
		if (connDeviceInfo == null) {
			lastUuid.setText("");
		} else {
			mDeviceName = connDeviceInfo.split("\\( ")[0].trim();
			String str = connDeviceInfo.split("\\( ")[1];
			mDeviceAddress = str.substring(0, str.length() - 2);
			lastUuid.setText(connDeviceInfo);
		}

		lastDeviceBtn = (Button) findViewById(R.id.ConnLastDevice);

		lastDeviceBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mDevice.clear();
				String connDeviceInfo = readConnDevice();
				if (connDeviceInfo == null) {
					Toast toast = Toast.makeText(BLESelect.this,
							"No Last connect device!", Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();

					return;
				}

				String str = connDeviceInfo.split("\\( ")[1];
				final String mDeviceAddress = str.substring(0, str.length() - 2);

				scanLeDevice();
				ConnessioneBLEState=mBluetoothLeService.connect(mDeviceAddress);
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

			}
		});

		scanAllBtn = (Button) findViewById(R.id.ScanAll);
		scanAllBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (ConnessioneBLEState == false) {
					scanLeDevice();

					try {
						Thread.sleep(BLESelect.SCAN_PERIOD);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					Intent intent = new Intent(getApplicationContext(),
							Device.class);
					startActivityForResult(intent, REQUEST_CODE);
				} else {
					mBluetoothLeService.disconnect();
					ConnessioneBLEState=false;
					mBluetoothLeService.close();
					scanAllBtn.setText("Scan All");
					parametro1.setText("");
					uuidTv.setText(R.string.no_connected);
					lastDeviceBtn.setVisibility(View.VISIBLE);
					logBtn.setVisibility(View.VISIBLE);
					gattServiceIntent = new Intent(BLESelect.this, BLService.class);
					bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

				}
			}
		});

		logBtn = (Button) findViewById(R.id.Log);
		logBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(),Display_items.class);
				startActivity(i);

				}
			});


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

	@Override
	protected void onResume() {
		super.onResume();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

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
	protected void onStop() {
		super.onStop();

		flag = false;

		unregisterReceiver(mGattUpdateReceiver);
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
		}

		super.onActivityResult(requestCode, resultCode, data);
	}


	void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);
				//textStatus.setText("Status:scanleDevice");
				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
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
					mBluetoothLeService.setCharacteristicNotification(
							mNotifyCharacteristic, false);
					mNotifyCharacteristic = null;
				}
				mBluetoothLeService.readCharacteristic(characteristic);

			}
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
				mNotifyCharacteristic = characteristic;
				BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
						UUID.fromString(BLGattAttributes.STM32_ACCELEROMETER_PARAMETER), 1);

				characteristic.addDescriptor(descriptor);

				mBluetoothLeService.setCharacteristicNotification(
						characteristic, true);
			}
			//return true;
			for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
				Log.e(TAG, "BluetoothGattDescriptor: " + descriptor.getUuid().toString());
			}
		}
	}



	private void displayCharacteristic_STM32(final BluetoothGattCharacteristic characteristic) {


		String msg;
		byte[] ADCValue3 = characteristic.getValue();
		String adc3Hex = ADCValue3.toString()
				.replace("[", "")   
				.replace("]", "");

//      Log.e("ADC3", "ADC CH3 characteristicvalue from TEST is " + adc3Hex);
//      Log.i("ADC3", "ADC Last 6CH3 characteristicvalue from TEST is " + adc3Hex.substring(adc3Hex.length() - 6));  //Prints last 6 of this string

		// Get UUID
		String ch3 = (String.valueOf(characteristic.getUuid()));
		String ch3UUID = ch3.substring(0, Math.min(ch3.length(), 8));
//      Log.d("ADC3", "ADC FIRST 6CH3 characteristicvalue from TEST is " + ch3.substring(0, Math.min(ch3.length(), 8)));  //Print first 6 of this string


		String adc3hex6 = adc3Hex.substring(adc3Hex.length() - 6);

		StringBuilder sb = new StringBuilder();
		for (byte b : ADCValue3) {
			if (sb.length() > 0) {
				//sb.append(':');
			}
		sb.append(String.format("%02x", b)); }
		StringBuilder sb1 = new StringBuilder();
		sb1.append(sb.substring(6, 8));
		sb1.append(sb.substring(4, 6));
		String test=sb1.toString();
		Log.w("ADC3", "StringBuilder------ " + sb1);
		short acc_x = (short) Integer.parseInt(test,16);
		StringBuilder sb2 = new StringBuilder();
		sb2.append(sb.substring(10, 12));
		sb2.append(sb.substring(8, 10));
		test=sb2.toString();
		short acc_y = (short) Integer.parseInt(test,16);
		StringBuilder sb3 = new StringBuilder();
		sb3.append(sb.substring(14));
		sb3.append(sb.substring(12, 14));
		test=sb3.toString();
		short acc_z = (short) Integer.parseInt(test,16);

		Log.w("ADC3", "StringBuilder " + sb + "****** " + acc_x + " | " + acc_y + " | " + acc_z + " | ");
		msg = "|X:"+ acc_x + " |Y: " + acc_y + " |Z: " + acc_z + " | ";
		displayData_STM32(msg);




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