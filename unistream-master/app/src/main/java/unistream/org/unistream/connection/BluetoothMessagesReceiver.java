package unistream.org.unistream.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by pluderma on 22/12/2015.
 */
public class BluetoothMessagesReceiver extends BroadcastReceiver{
	private TextView output;

	public BluetoothMessagesReceiver(TextView output){
		this.output = output;
	}

	@Override
	public void onReceive(Context context, Intent intent){
		String message = intent.getExtras().getString(ChargerManager.BT_MESSAGE);
		if(message != null && !message.isEmpty())
			output.setText(message);
	}
}
