package ca.spencerelliott.mercury;

/************************************************************************
 * This file is part of Mercury.
 *
 * Mercury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mercury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Mercury.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Spencer Elliott
 * @author spencer@spencerelliott.ca
 ************************************************************************/

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AlarmReceiver extends BroadcastReceiver {
	public final static int ALARM = 0;
	public final static int ON_BOOT = 1;
	
	@Override
	public void onReceive(Context context, Intent intent) {		
		//Receive the broadcast intent and start the background service
		if(intent.getAction().equals(Mercury.REFRESH_INTENT)){
			//Get the service intent
			Intent new_intent = new Intent(context, ChangesetService.class);
			
			//Tell the service it was triggered from an alarm
			new_intent.putExtra("ca.spencerelliott.mercury.call", AlarmReceiver.ALARM);
			
			//Start the service
	    	context.startService(new_intent);
		} else if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			//Grab the default shared preferences
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			
			//Get whether or not the user would like to receive notifications
			boolean do_notifications = preferences.getBoolean("notifications", false);
			
			//If the user has opted to receive notifications
			if(do_notifications) {
				//Create the service intent
				Intent new_intent = new Intent(context, ChangesetService.class);
				
				//Add a notification for the service so it knows this was on boot
				new_intent.putExtra("ca.spencerelliott.mercury.call", AlarmReceiver.ON_BOOT);
			
				//Start the service
	    		context.startService(new_intent);
			}
		}
	}

}
