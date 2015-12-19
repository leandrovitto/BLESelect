/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ble;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.redbear.bleselect.R;


/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BLService extends Service {
	private final static String TAG = BLService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;

	NotificationCompat.Builder mBuilder;
	PendingIntent resultPendingIntent;
	int mNotificationId;
	NotificationManager mNotifyMgr;
/*
	SQLIte_manager manager;
	SQLite_helper hlep;
	String date;*/

	public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
	public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
	public final static String ACTION_GATT_WRTING="ACTION_GATT_WRTING";
	public final static String EXTRA_DATA = "EXTRA_DATA";

	public String BLE_STATUS_CONNECTION_STRING="";
	public String BLE_STATUS_WRITING_STRING="";
	public String add="";
	public final static String EXTRA_RSSI="EXTRA_RSSI";

	public final static UUID UUID_STM32_ACCELEROMETER_PARAMETER =
			UUID.fromString(BLGattAttributes.STM32_ACCELEROMETER_PARAMETER);

	public final static UUID UUID_STM32_WRITE_TO_DEVICE =
			UUID.fromString(BLGattAttributes.STM32_WRITE_TO_DEVICE);

	public final static UUID UUID_SERVICE_STM32 =
			UUID.fromString(BLGattAttributes.SERVICE_STM32);

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String intentAction;
			Log.i(TAG, "STATUS:" + status);
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				BLE_STATUS_CONNECTION_STRING="Connected to GATT server STM32.";

				mBuilder.setContentText(BLE_STATUS_CONNECTION_STRING);
				mNotifyMgr.notify(mNotificationId, mBuilder.build());

				// Attempts to discover services after successful connection.
				Log.i(TAG, "Attempting to start service discovery:"
						+ mBluetoothGatt.discoverServices());
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				Log.i(TAG, "Disconnected from GATT server.");
				BLE_STATUS_CONNECTION_STRING="Disconnected from GATT server STM32.";

				broadcastUpdate(intentAction);
			}

			if (status== BluetoothGatt.GATT_FAILURE){
				BLE_STATUS_CONNECTION_STRING="GATT Failure STM32.";
			}

		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_RSSI, rssi);
			} else {
				Log.w(TAG, "onReadRemoteRssi received: " + status);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				//BLE_STATUS_CONNECTION_STRING="ACTION_GATT_SERVICES_DISCOVERED";
				Log.w(TAG, "onServicesDiscovered received: " + status);
			} else {
				Log.w(TAG, "onServicesDiscovered not received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
										  BluetoothGattCharacteristic characteristic, int status) {
			BLE_STATUS_WRITING_STRING="Write:" + add +" State:"+ status;
			Log.w("ADC3",BLE_STATUS_WRITING_STRING);
			if (status == BluetoothGatt.GATT_SUCCESS) {

				broadcastUpdate(ACTION_GATT_WRTING);
			}
		}



	};

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action, int rssi) {
		final Intent intent = new Intent(action);
        //BLE_STATUS_CONNECTION_STRING=String.valueOf(rssi);
		//System.out.println(String.valueOf(rssi));
		intent.putExtra(EXTRA_RSSI, String.valueOf(rssi));
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,final BluetoothGattCharacteristic characteristic) {

		final Intent intent = new Intent(action);

		if (UUID_STM32_ACCELEROMETER_PARAMETER.equals(characteristic.getUuid())) {
			//é una elaborazione dei dati inviati dalla scheda ST32 che invia i dati come un flusso di byte ad otetti
			//quindi converto in esadecimale e scambio i primi due ottetti per ottrenerli in ordine
			//0000-0000-0000-0000|acc_x_bit_meno_significativi[8byte]-acc_x_bit_significativi[8byte]|acc_y_meno..-acc_y..|acc_z_meno...-acc_
			// in hex ogni 4 byte ho un valore
			//0-1-2-3|4-5-6-7|8-9-10-11|12-13-14-15
			//per acc_x devo avere i blocchi 6-7-4-5 affiancati
			//per acc_y =10-11-8-9
			//per acc_z=14-15-12-13
			//acc_x=sb1.append(sb.substring(6, 8)) concatenato con sb1.append(sb.substring(4, 6));
			//magari esiste un metodo meno contorto per recuperarli,io purtroppo non l'ho trovato :-(
			byte[] stream_byte_received = characteristic.getValue();

			StringBuilder sb = new StringBuilder();
			for (byte b : stream_byte_received) {
				sb.append(String.format("%02x", b));
			}

			StringBuilder sb1 = new StringBuilder();
			sb1.append(sb.substring(6, 8));sb1.append(sb.substring(4, 6));
			String test=sb1.toString();
			short acc_x = (short) Integer.parseInt(test,16);

			sb1 = new StringBuilder();
			sb1.append(sb.substring(10, 12));sb1.append(sb.substring(8, 10));
			test=sb1.toString();
			short acc_y = (short) Integer.parseInt(test,16);

			sb1 = new StringBuilder();
			sb1.append(sb.substring(14,16));sb1.append(sb.substring(12, 14));
			test=sb1.toString();
			short acc_z = (short) Integer.parseInt(test,16);

			sb1 = new StringBuilder();
			sb1.append(sb.substring(18,20));sb1.append(sb.substring(16, 18));
			test=sb1.toString();
			short temperatura = (short) Integer.parseInt(test,16);

			sb1 = new StringBuilder();
			sb1.append(sb.substring(22,24));sb1.append(sb.substring(20, 22));
			test=sb1.toString();
			short Humidity = (short) Integer.parseInt(test,16);

			//EVENTUALE SENSORE DA CATTURARE ESEMPIO TEMPERATURA,AFFIANCO NEL VETTORE GATT DEL DEVICE ST32
			//16-17-18-19
			//sbX.append(sb.substring(18));sbX.append(sb.substring(16, 18));

			Log.w("ADC3", "StringBuilder " + sb + "************************** " + acc_x + " | " + acc_y + " | " + acc_z + " | ");
			String accelerometer = "|X:"+ acc_x + " |Y: " + acc_y + " |Z: " + acc_z + " | ";
			String extra_data="Temp:"+temperatura+"°\nHumidity:"+Humidity+"%\nAccelerometer:\t" + accelerometer;
			intent.putExtra(EXTRA_DATA, extra_data);

		}else{
			final byte[] data = characteristic.getValue();
			if (data != null && data.length > 0) {
				final StringBuilder stringBuilder = new StringBuilder(data.length);
				for(byte byteChar : data)
					stringBuilder.append(String.format("%02X", byteChar));
				intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
			}
		}
		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		BLService getService() {
			return BLService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle("BLE Stm32")
						.setContentText("").setAutoCancel(true);
		mNotificationId = 001;
		Intent resultIntent = new Intent(this, BLESelect.class);

		//NOTIFICATION INTENTE
		resultPendingIntent =
				PendingIntent.getActivity(
						this,
						0,
						resultIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		mBuilder.setContentIntent(resultPendingIntent);
		mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG,
					"BluetoothAdapter not initialized or unspecified address.");
			BLE_STATUS_CONNECTION_STRING="BluetoothAdapter not initialized or unspecified address.";

			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			Log.d(TAG,
					"Trying to use an existing mBluetoothGatt for connection.");
			BLE_STATUS_CONNECTION_STRING="Trying to use an existing mBluetoothGatt for connection.";

			return mBluetoothGatt.connect();

		}

		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			BLE_STATUS_CONNECTION_STRING="Device not found.  Unable to connect.";

			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.

		mBluetoothGatt = device.connectGatt(this, true, mGattCallback);

		Log.d(TAG, "Trying to create a new connection.");
		BLE_STATUS_CONNECTION_STRING="Trying to create a new connection.";

		mBluetoothDeviceAddress = address;

		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
		BLE_STATUS_CONNECTION_STRING="Disconnect...";
		mBuilder.setContentText(BLE_STATUS_CONNECTION_STRING);
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized Characteristic");
			return;
		}

		mBluetoothGatt.readCharacteristic(characteristic);
	}


	public void writeCharacteristic(BluetoothGattCharacteristic characteristic,String value) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized Characteristic");
			return;
		}
		if (UUID_STM32_WRITE_TO_DEVICE.equals(characteristic.getUuid())) {
			add=value;
			byte[] dataByte = value.getBytes();
			characteristic.setValue(dataByte);
			characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
			mBluetoothGatt.writeCharacteristic(characteristic);
		}
	}

	public void readRssi() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.readRemoteRssi();
	}

	/*public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.writeCharacteristic(characteristic);
	}*/

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (UUID_STM32_ACCELEROMETER_PARAMETER.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
					UUID.fromString(BLGattAttributes.STM32_ACCELEROMETER_PARAMETER));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}

		if (UUID_STM32_WRITE_TO_DEVICE.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
					UUID.fromString(BLGattAttributes.STM32_WRITE_TO_DEVICE));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}



	}

	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null) return null;

		return mBluetoothGatt.getServices();
	}
}
