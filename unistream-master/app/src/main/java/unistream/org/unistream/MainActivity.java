package unistream.org.unistream;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import unistream.org.unistream.connection.BluetoothDisabledException;
import unistream.org.unistream.connection.BluetoothMessagesReceiver;
import unistream.org.unistream.connection.ChargerInterface;
import unistream.org.unistream.connection.ChargerManager;
import unistream.org.unistream.connection.DeviceListActivity;

import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
	private static final int SELECT_DEVICE = 10;
	public static final String SETTINGS_HOUR = "hour";
	public static final String SETTINGS_MINUTE = "min";
	public static final String SETTINGS_NAME = "device";
	public static final String TAG = "unistream";
	private Button timePickerButton;
	private TextView connectionStatus;
	private SharedPreferences deviceSharedPreferences;
	private BluetoothMessagesReceiver bluetoothMessagesReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		connectionStatus = (TextView)findViewById(R.id.connectionStatus);
		registerForBluetoothMessages();
		deviceSharedPreferences = getApplicationContext().getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
	}

	@Override
	protected void onResume(){
		super.onResume();
		try{
			if(ChargerManager.init(this))
				setChargingEndTimeOnView();
			else // failed to connect to the device
				startActivityForResult(new Intent(MainActivity.this, DeviceListActivity.class), SELECT_DEVICE);
		}
		catch(BluetoothDisabledException e){
			showError("Bluetooth is disabled, please enable it and get back to the application");
		}

	}

	private void showError(String message){
		new AlertDialog.Builder(this)
				.setMessage(message)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which){
						finish();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}

	@Override
	protected void onPause(){
		ChargerManager.disconnect();
		if(bluetoothMessagesReceiver != null){
			try{
				unregisterReceiver(bluetoothMessagesReceiver);
			}
			catch(IllegalArgumentException e){
				Log.w(TAG, "the receiver wasn't registered");
			}
		}
		super.onPause();
	}

	private void registerForBluetoothMessages(){
		if(bluetoothMessagesReceiver == null){
			bluetoothMessagesReceiver = new BluetoothMessagesReceiver(connectionStatus);
			IntentFilter filter = new IntentFilter();
			filter.addAction(ChargerManager.COMMUNICATION_MESSAGE);
			registerReceiver(bluetoothMessagesReceiver, filter);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		switch(requestCode){
			case SELECT_DEVICE:
				try{
					ChargerManager.init(this);
				}
				catch(BluetoothDisabledException e){
					showError("Bluetooth is disabled, please enable it and get back to the application");
				}
				break;
			default:
				break;
		}
	}

	private void saveSettings(String key, int value) {
		SharedPreferences.Editor editor = deviceSharedPreferences.edit();

		editor.putInt(key, value);
		editor.commit();
		Log.i(TAG, "saveSettings: store "+key+": "+value);
	}

	private int getSettings(String key, int defaultVal) {
		int value = deviceSharedPreferences.getInt(key, defaultVal);
		Log.i(TAG, "getSettings: read "+key+": "+value);
		return value;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if(id == R.id.action_bt_testing){
			startActivity(new Intent(MainActivity.this, DeviceListActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setChargingEndTimeOnView() {
		Log.i(TAG, "setChargingEndTimeOnView");

		timePickerButton = (Button) findViewById(R.id.timePickerDisplay);

		Calendar now = Calendar.getInstance();
		int hour = getSettings(SETTINGS_HOUR, now.get(Calendar.HOUR));
		int minute = getSettings(SETTINGS_MINUTE, now.get(Calendar.MINUTE));

		updateChargeTime (hour, minute);

		Log.i(TAG, "setChargingEndTimeOnView: set it to "+hour+":"+minute);
	}


	private void updateChargeTime (int hour, int minute) {
		String timeLabel = hour + ":" + (minute < 10? "0" : "") + minute;

		timePickerButton.setText(timeLabel);

		Calendar now = Calendar.getInstance();
		int nowHour = now.get(Calendar.HOUR);
		int nowMinute = now.get(Calendar.MINUTE);


		int minutesToTarget = (hour - nowHour) * 60 + (minute - nowMinute);
		if (minutesToTarget < 0) {
			minutesToTarget = minutesToTarget + (24 * 60);
		}

		saveSettings(SETTINGS_HOUR, hour);
		saveSettings(SETTINGS_MINUTE, minute);


		long secondsTarget = minutesToTarget * 60;
		long secondsStart = secondsTarget - (120 * 60); // 120 minutes to charge

		String message;
		if (secondsStart < 0) {
			secondsStart = 0;
			message = "Start charging now";
		} else {
			message = "Charge start in " + secondsStart/60 + " minutes";
		}


		ChargerInterface.charge(secondsStart, secondsTarget);
		Log.i(TAG, "setChargeEndTime_oldTimePicker: time: " + message);


		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	public void on(View view){
		ChargerManager.sendMessageToBT("on");
	}

	public void off(View view){
		ChargerManager.sendMessageToBT("off");
	}

	public void selectCharger(View view){
		startActivity(new Intent(MainActivity.this, DeviceListActivity.class));
	}

	// timepicker - https://github.com/wdullaer/MaterialDateTimePicker
    public void showTimePicker(View view) {
        Calendar now = Calendar.getInstance();
		int hour = getSettings(SETTINGS_HOUR, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
		int minute = getSettings(SETTINGS_MINUTE, Calendar.getInstance().get(Calendar.MINUTE));

        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
				MainActivity.this,
				hour,
				minute,
				0,
				true // is24HourMode
		);

        timePickerDialog.show(getFragmentManager(), "Timepickerdialog");
    }


    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date = "You picked the following date: "+dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        //dateTextView.setText(date);
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        String time = "You picked the following time: "+hourOfDay+"h"+minute;

		updateChargeTime (hourOfDay, minute);
        //timeTextView.setText(time);
    }
}
