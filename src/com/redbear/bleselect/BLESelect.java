package com.redbear.bleselect;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.lang.Thread;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.redbear.bleselect.util.NumberConversion;

import java.lang.Byte;



public class BLESelect extends Activity {
	private final static String TAG = BLESelect.class.getSimpleName();

	private RBLService mBluetoothLeService;
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

	Button lastDeviceBtn = null;
	Button scanAllBtn = null;
	TextView uuidTv = null;
	TextView lastUuid = null;
	TextView parametro1 = null;

	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 3000;
	public static final int REQUEST_CODE = 30;
	private TextView mConnectionState;
	private TextView mDataField;
	private String mDeviceAddress;
	private String mDeviceName;
	private boolean flag = true;
	private boolean connState = false;
	private static final int MSG_HUMIDITY = 101;

	String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	String fname = "flash.txt";

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
									   IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_CONNECTED.equals(action)) {
				flag = true;
				connState = true;

				Toast.makeText(getApplicationContext(), "Connected",
						Toast.LENGTH_SHORT).show();
				writeToFile(mDeviceName + " ( " + mDeviceAddress + " )");
				lastUuid.setText(mDeviceName + " ( " + mDeviceAddress + " )");
				lastDeviceBtn.setVisibility(View.GONE);
				scanAllBtn.setText("Disconnect");

				startReadRssi();
			} else if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				flag = false;
				connState = false;

				Toast.makeText(getApplicationContext(), "Disconnected",
						Toast.LENGTH_SHORT).show();
				scanAllBtn.setText("Scan All");

				lastDeviceBtn.setVisibility(View.VISIBLE);
			} else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
				displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				startReadCharacter(mNotifyCharacteristic);
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayCharacteristic(mNotifyCharacteristic);
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
			uuidTv.setText("RSSI:\t" + data);
		}
	}

	private void displayData_STM32(String data) {
		if (data != null) {
			parametro1.setText("Parametri BLE:\t" + data);
		}
	}

	private void startReadRssi() {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readRssi();

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

	private void startReadCharacter(final BluetoothGattCharacteristic cha) {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readCharacteristic(cha);

					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			;
		}.start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

		uuidTv = (TextView) findViewById(R.id.uuid);
		lastUuid = (TextView) findViewById(R.id.lastDevice);
		parametro1 = (TextView) findViewById(R.id.param1);

		// Sets up UI references.
		/*
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
		mGattServicesList.setOnChildClickListener(servicesListClickListner);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);*/
		//getActionBar().setTitle(mDeviceName);
		//getActionBar().setDisplayHomeAsUpEnabled(true);

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

				Timer mNewTimer = new Timer();
				mNewTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						for (BluetoothDevice device : mDevice)
							if ((device.getAddress().equals(mDeviceAddress))) {
								mBluetoothLeService.connect(mDeviceAddress);

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
				}, SCAN_PERIOD);
			}
		});

		scanAllBtn = (Button) findViewById(R.id.ScanAll);
		scanAllBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (connState == false) {
					scanLeDevice();

					try {
						Thread.sleep(SCAN_PERIOD);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}


					Intent intent = new Intent(getApplicationContext(),
							Device.class);
					startActivityForResult(intent, REQUEST_CODE);
				} else {
					mBluetoothLeService.disconnect();
					mBluetoothLeService.close();
					scanAllBtn.setText("Scan All");
					parametro1.setText("");
					uuidTv.setText(R.string.no_connected);
					lastDeviceBtn.setVisibility(View.VISIBLE);
				}
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

		Intent gattServiceIntent = new Intent(BLESelect.this, RBLService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_GATT_RSSI);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

		return intentFilter;
	}

	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);

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
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		} else if (requestCode == REQUEST_CODE
				&& resultCode == Device.RESULT_CODE) {
			mDeviceAddress = data.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
			mDeviceName = data.getStringExtra(Device.EXTRA_DEVICE_NAME);
			mBluetoothLeService.connect(mDeviceAddress);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}


	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.     displayGattServices(mBluetoothLeService.getSupportedGattServices());
	private void displaySTM32GattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		String uuid = null;
		//caratteristiche di questo servizio
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

		ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();


		for (BluetoothGattService gattService : gattServices) {

			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
			uuid = gattService.getUuid().toString();


			if (uuid.equals(RBLGattAttributes.SERVICE_STM32)) {


				final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(0);
				final int charaProp = characteristic.getProperties();
				//mHandler.sendMessage(Message.obtain(null, 50, characteristic));

				mWriteCharacteristic = characteristic;

				BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString
						("0000a001-0000-1000-8000-00805f9b34fb"),
						BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
				mWriteCharacteristic.addDescriptor(descriptor);

				mBluetoothLeService.readCharacteristic(mWriteCharacteristic);


				//byte tttt[]=characteristic.getDescriptors().get(0).getValue();
				mBluetoothLeService.readCharacteristic(characteristic);

				Log.i(TAG, "PERMISSION:" + mWriteCharacteristic.getPermissions());

				//displayCharacteristic(characteristic);


				if (characteristic.getValue() != null) {
					int test = 15;
					Log.i(TAG, "PE:" + test);
				}
				//for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

				//mBluetoothLeService.readCharacteristic(gattCharacteristic);

				//charas.add()


				//charas.add(gattCharacteristic);
				//HashMap<String, String> currentCharaData = new HashMap<String, String>();
				//uuid = gattCharacteristic.getUuid().toString();

				//currentCharaData.put(LIST_NAME, RBLGattAttributes.lookup(uuid, unknownCharaString));
				//currentCharaData.put(LIST_UUID, uuid);

/*
					if(uuid.equals(RBLGattAttributes.STM32_ACCELEROMETER_PARAMETER)){
						if (mBluetoothLeService.readCharacteristic(gattCharacteristic)){
							byte bn[]=gattCharacteristic.getValue();
							Byte b1;
							int[] intArray = new int[10];
							for (int k = 0; k < 10; k++) {
								b1 = new Byte(bn[k]);
								intArray[k] = b1.intValue();
								Log.i(TAG, "-" + intArray[k]);
							}
						}
					}*/
				//gattCharacteristicGroupData.add(currentCharaData);
			}

			//Log.i(TAG, "CHARETECR" + gattCharacteristicGroupData.get(0).toString());
				/*Byte b1;
				int[] intArray = new int[10];
				//ByteArrayOutputStream bOutput=new ByteArrayOutputStream(10);
				for (int k = 0; k < 10; k++) {
					//bOutput.write(charas.get(0).getValue()[k]);
					b1 = new Byte(bn[k]);
					intArray[k] = b1.intValue();
					Log.i(TAG, "-" + intArray[k]);
				}
*/
				/*byte b [] = bOutput.toByteArray();

				System.out.println("Print the content");

				for(int x= 0 ; x < b.length; x++) {
					//printing the characters
					b1 = new Byte(b[x]);
					i1 = b1.intValue();
					//String str1 = "int value of Byte " + b1 + " is " + i1;

					//int i = bmn.intValue();
					//System.out.print((char) b[x] + "   ");
					Log.i(TAG, "-" + Integer.toString(i1) );
					//Log.i(TAG, "--------direct-------|||>" + (char)charas.get(0).getValue()[x]);
				}
				System.out.println("   ");

				//byte bn[]= charas.get(0).getValue();*/

			//}
		}
	}


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
					LIST_NAME, RBLGattAttributes.lookup(uuid, unknownServiceString));
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
						LIST_NAME, RBLGattAttributes.lookup(uuid, unknownCharaString));
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
						UUID.fromString(RBLGattAttributes.STM32_ACCELEROMETER_PARAMETER), 1);

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

	// If a given GATT characteristic is selected, check for supported features.  This sample
	// demonstrates 'Read' and 'Notify' features.  See
	// http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
	// list of supported characteristic features.
	private final ExpandableListView.OnChildClickListener servicesListClickListner =
			new ExpandableListView.OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
											int childPosition, long id) {
					if (mGattCharacteristics != null) {
						final BluetoothGattCharacteristic characteristic =
								mGattCharacteristics.get(groupPosition).get(childPosition);
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
							mBluetoothLeService.setCharacteristicNotification(
									characteristic, true);
						}
						return true;
					}
					return false;
				}
			};

	private void displayCharacteristic(final BluetoothGattCharacteristic characteristic) {
		String msg = null;
		final byte[] data = characteristic.getValue();
		int[] intArray = new int[data.length];
		final StringBuilder stringBuilder = new StringBuilder(data.length);
		for (int k = 0; k < data.length; k++) {
			Byte b1 = new Byte(data[k]);
			intArray[k] = b1.intValue();
			//Log.i(TAG, "-" + intArray[k]);
			stringBuilder.append(Integer.toString(intArray[k]) + " | ");
		}

		String convertedtoHex=byteArrayToHex(data);
		Log.i(TAG,"HEX:"+ convertedtoHex);
		int test=Integer.decode(convertedtoHex.substring(1,4));
		Log.i(TAG, Integer.toString(test));
		msg = "ddd";
		displayData_STM32(msg);

	}

	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for(byte b: a)
			sb.append(String.format("%02x", b & 0xff));
		return sb.toString();
	}


}