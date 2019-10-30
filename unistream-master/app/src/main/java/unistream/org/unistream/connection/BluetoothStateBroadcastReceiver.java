package unistream.org.unistream.connection;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by pluderma on 30/12/2015.
 */
public class BluetoothStateBroadcastReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent){
		final String action = intent.getAction();

		if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
			final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
			switch(state){
				case BluetoothAdapter.STATE_OFF:

					break;
				case BluetoothAdapter.STATE_TURNING_OFF:

					break;
				case BluetoothAdapter.STATE_ON:
					ChargerManager.connect();
					break;

				case BluetoothAdapter.STATE_TURNING_ON:

					break;
			}
		}
	}

}
