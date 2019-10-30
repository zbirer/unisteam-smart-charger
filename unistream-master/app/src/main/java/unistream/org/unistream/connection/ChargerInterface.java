package unistream.org.unistream.connection;

import android.util.Log;

/**
 * Created by pluderma on 05/01/2016.
 */
public class ChargerInterface{
	private static final String TAG = "ChargerInterface";


	/**
	 * send the charger command to charge from time, until time
	 * @param from the from UTC
	 * @param to to UTC
	 */
	public static void charge(long from, long to){
		if(from < to){
			Log.d(TAG, "activate the charger from " + from + " to " + to);
			ChargerManager.sendMessageToBT("start " + from);
			ChargerManager.sendMessageToBT("stop " + to);
		}
		else{
			Log.e(TAG, "unable to send charge message to charger, end time is before start time");
		}
	}
}
