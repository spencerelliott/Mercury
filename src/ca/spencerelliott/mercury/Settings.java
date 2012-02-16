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

import ca.spencerelliott.mercury.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;


public class Settings extends PreferenceActivity {
	//Create the intent based on what we want to do
	private Intent intent;
	private PendingIntent p_intent;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		intent = new Intent("ca.spencerelliott.mercury.REFRESH_CHANGESETS");
		p_intent = PendingIntent.getBroadcast(this, 0, intent, 0);
		
		addPreferencesFromResource(R.xml.preferences);
		
		final CheckBoxPreference notification_check = (CheckBoxPreference)findPreference("notifications");
		final ListPreference notification_interval = (ListPreference)findPreference("notification_interval");
		final Preference version_info = (Preference)findPreference("version");
		
		//Set the summary of the version info using the version number and the extras
		version_info.setSummary(this.getString(R.string.version_info) + " " + this.getString(R.string.version_info_extra));
		
		OnPreferenceClickListener click_listener = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				//Check to see if the clicked preference was a checkbox
				if(preference instanceof CheckBoxPreference) {
					//If so, cast it as a checkbox
					CheckBoxPreference checkbox = (CheckBoxPreference)preference;
					
					//Check to see which checkbox was clicked
					if(checkbox.equals(notification_check)) {
						//If the notification checkbox was clicked, start the notification service
						if(checkbox.isChecked()) {
							String value = notification_interval.getValue();
							installService(Long.parseLong(value));
						//If unchecked, stop the notification service
						} else {
							removeService();
						}
					}
				}
				return true;
			}
		};
		
		//Use the previously defined click listener
		notification_check.setOnPreferenceClickListener(click_listener);
		
		notification_interval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				//Update the notification service in the background by removing and restarting it
				removeService();
				installService(Long.parseLong((String)newValue));
				
				//Tell the program to persist the value
				return true;
			}
		});
	}
	
	//This will start the notification service on the device
	private boolean installService(long interval) {
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		//Create a repeating alarm
		alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+interval, interval, p_intent);
		return false;
	}
	
	//This will stop the notification service on the device
	private boolean removeService() {
		//Get the alarm manager from the system
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		//Cancel any alarms that are part of this
		alarm.cancel(p_intent);
		return false;
	}
}
