package unistream.org.unistream.connection;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.*;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by pluderma on 20/12/2015.
 */
public class ChargerManager{
	private static final String TAG = "ChargerManager";
	private static final String DEVICE = "device";

	public static final String COMMUNICATION_MESSAGE = "com.unistream.BT_MESSAGE";
	public static final String BT_MESSAGE = "btMessage";
	public static final String NOT_SUPPORTED = "device doesn't support uart";

	// constants
	public static final UUID TX_CHAR_UUID = UUID.fromString("00005501-D102-11E1-9B23-00025B00A5A5");
	public static final UUID RX_SERVICE_UUID = UUID.fromString("00005500-D102-11E1-9B23-00025B00A5A5");
	public static final UUID RX_CHAR_UUID = UUID.fromString("00005501-D102-11E1-9B23-00025B00A5A5");
	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	static class State{
		static final int DISCONNECTED = 0;
		static final int CONNECTED = 1;
		static final int INITIALIZING = 1;
	}

	static int state = State.DISCONNECTED;
	static boolean shouldTryConnect;
	static BluetoothAdapter bluetoothAdapter = null;
	static Context context = null;
	static String deviceAddress = null;
	static BluetoothGatt bluetoothGatt = null;
	static BluetoothStateBroadcastReceiver bluetoothStateBroadcastReceiver = null;
	static BluetoothDevice remoteDevice = null;

	public static boolean init(ContextWrapper context) throws BluetoothDisabledException{
		if(state == State.CONNECTED)
			return true;

		if(state == State.DISCONNECTED){
			ChargerManager.context = context;
			shouldTryConnect = true;

			BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
			bluetoothAdapter = bluetoothManager.getAdapter();
			if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
				update("Bluetooth is disabled, please ");
				throw new BluetoothDisabledException();
			}
			if(!deviceExists(context))
				return false;
			else
				connect();
		}

		return true;
	}

	private static boolean deviceExists(ContextWrapper context){
		SharedPreferences deviceSharedPreferences = context.getSharedPreferences("device", Context.MODE_PRIVATE);
		deviceAddress = deviceSharedPreferences.getString(DEVICE, null);
		if(deviceAddress == null){
			shouldTryConnect = false;
			return false;
		}
		return true;
	}

	@SuppressLint("CommitPrefEdits")
	public static void saveDevice(Context context, String deviceAddress){
		ChargerManager.deviceAddress = deviceAddress;
		SharedPreferences deviceSharedPreferences = context.getSharedPreferences(DEVICE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = deviceSharedPreferences.edit();
		editor.putString(DEVICE, deviceAddress);
		editor.commit();
	}

	static void connect(){
		if(state != State.CONNECTED){
			state = State.INITIALIZING;
			shouldTryConnect = true;
			if(bluetoothGatt != null)
				bluetoothGatt.connect();
			else{
				if(remoteDevice == null)
					remoteDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
				bluetoothGatt = remoteDevice.connectGatt(context, false, new BluetoothConnectionCallback());
			}
		}
	}

	public static void disconnect(){
		shouldTryConnect = false;
		state = State.DISCONNECTED;

		if(bluetoothStateBroadcastReceiver != null)
			ChargerManager.context.unregisterReceiver(bluetoothStateBroadcastReceiver);

		if(bluetoothGatt == null){
			return;
		}
		Log.w(TAG, "mBluetoothGatt closed");
		bluetoothGatt.close();
		bluetoothGatt = null;
	}

	/**
	 * Enable notifications from and to the device
	 */
	static void enableTXNotification(){
		if(bluetoothGatt == null){
			Log.e(TAG, "bluetoothGatt null");
			update(NOT_SUPPORTED);
			return;
		}
		else
			Log.d(TAG, "mBluetoothGatt is OK");

		Log.d(TAG, "enableTXNotification");
		BluetoothGattService RxService = bluetoothGatt.getService(RX_SERVICE_UUID);
		if(RxService == null){
			Log.d(TAG, "no RX service on the device");
			update("Connection error (RX)");
			return;
		}
		else
			Log.w(TAG, "RxService is OK");

		BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
		if(TxChar == null){
			Log.e(TAG, "Tx charateristic not found!");
			update("Tx charateristic not found!");
			return;
		}
		else
			Log.w(TAG, "Tx charateristic is OK");
		bluetoothGatt.setCharacteristicNotification(TxChar, true);
		BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		bluetoothGatt.writeDescriptor(descriptor);
	}

	public static void sendMessageToBT(String message){
		BluetoothGattService RxService = bluetoothGatt.getService(RX_SERVICE_UUID);
		if(RxService == null){
			update("Rx service not found!");
			Log.e(TAG, NOT_SUPPORTED);
			return;
		}
		BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
		if(RxChar == null){
			update("Rx charateristic not found!");
			Log.w(TAG, NOT_SUPPORTED);
			return;
		}

		try{
			RxChar.setValue(message.getBytes("UTF-8"));
		}
		catch(UnsupportedEncodingException e){
			Log.e(TAG, "message encoding not supported", e);
		}
		boolean status = bluetoothGatt.writeCharacteristic(RxChar);
		Log.d(TAG, "write TXchar - status=" + status);
	}

	static void update(String message){
		Intent intent = new Intent(COMMUNICATION_MESSAGE);
		intent.putExtra(BT_MESSAGE, message);
		context.sendBroadcast(intent);
	}
}
