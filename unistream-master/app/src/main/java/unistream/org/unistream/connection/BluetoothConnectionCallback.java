package unistream.org.unistream.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * Created by pluderma on 21/12/2015.
 */
public class BluetoothConnectionCallback extends BluetoothGattCallback{
	private static final String TAG = "ConnectionCallBack";

	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
		if(newState == BluetoothProfile.STATE_CONNECTED){
			ChargerManager.state = ChargerManager.State.CONNECTED;
			ChargerManager.update("Charger Connected");
			ChargerManager.bluetoothGatt.discoverServices();
		}
		else if(newState == BluetoothProfile.STATE_DISCONNECTED){
			ChargerManager.state = ChargerManager.State.DISCONNECTED;
			ChargerManager.update("Charger disconnected");
			if(ChargerManager.shouldTryConnect)
				ChargerManager.connect();
		}
		else{
			String message = String.format("received unknown new state [%s] status [%s]", newState, status);
			Log.e(TAG, message);
			ChargerManager.update(message);
		}
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
		try{
			if(ChargerManager.TX_CHAR_UUID.equals(characteristic.getUuid())){
				String messageFromDevice = new String(characteristic.getValue(), "UTF-8");
				Log.d(TAG, "message from the device: " + messageFromDevice);

//				if(messageFromDevice.equals("RF_Ready")){
//					ChargerManager.state = ChargerManager.State.CONNECTED;
//					ChargerManager.update("connected");
//				}
			}
		}
		catch(UnsupportedEncodingException e){
			Log.e(TAG, "Error getting message back from the device", e);
		}
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status){
		if(status == BluetoothGatt.GATT_SUCCESS){
			// sometimes the discovery notification comes after the connection had been formed, in that case no point in notifying on discovery
			if(ChargerManager.state != ChargerManager.State.CONNECTED)
				ChargerManager.update("Charger found");
			ChargerManager.enableTXNotification();
		}
		else{
			ChargerManager.update("No charger found");
		}
	}
}
