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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.SAXException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Xml;

public class ChangesetService extends Service {
	//Constants for the index of the different repositories
	private final static int REPO_HGSERVE = 0;
	private final static int REPO_GOOGLECODE = 1;
	private final static int REPO_BITBUCKET = 2;
	private final static int REPO_CODEPLEX = 3;
	
	//Constants for the index of the different authentication methods in the spinner
	private final static int NONE = 0;
	private final static int HTTP = 1;
	private final static int HTTPS = 2;
	private final static int SSH = 3;
	
	private int notification_colour = 0;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {		
	    int broadcast_call = intent.getIntExtra("ca.spencerelliott.mercury.call", -1);
	    
	    //If this was on boot we want to set up the alarm to set up repeating notifications
	    if(broadcast_call == AlarmReceiver.ON_BOOT) {
	    	//Get the alarm service
	    	AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    	
			//Generate the interval between notifications, default to fifteen minutes
			long interval = Long.parseLong(prefs.getString("notification_interval", "900000"));
			
			//Create the intents to launch the service again
			Intent new_intent = new Intent("ca.spencerelliott.mercury.REFRESH_CHANGESETS");
			PendingIntent p_intent = PendingIntent.getBroadcast(this, 0, new_intent, 0);
			
			final Calendar c = Calendar.getInstance();
			
			//Create a repeating alarm
			alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, c.getTimeInMillis()+interval, interval, p_intent);
			
			//Stop the service since we're waiting for the interval
			stopSelf();
	    }
	    
	    //Create a new feed processor
		FeedProcessor processor = new FeedProcessor();
		
		//Gather all of the URLs from the databases here
		
		//Let the processor handle all of the URLs and notify the user of any new changesets
		processor.execute();
	    
	}
	
	//Do not allow binding to this service since it should not be running constantly
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	//This will handle processing all of the feeds for each repository
	private class FeedProcessor extends AsyncTask<URL, Void, Void> {
		@Override
		protected Void doInBackground(URL... params) {
			boolean new_changeset = false;
			String repo_changes = "";
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			
			String pref_colour = prefs.getString("led_colour", "-16711681");

			try {
				notification_colour = Integer.parseInt(pref_colour);
			} catch(Exception e) {
				notification_colour = Color.CYAN;
			}
			
			ArrayList<Beans.RepositoryBean> repo_list = null;
			DatabaseHelper db_helper = DatabaseHelper.getInstance(getBaseContext());
			
			repo_list = db_helper.getAllRepositories(null);
			
			for(Beans.RepositoryBean repo_type : repo_list) {
				AtomHandler feed_handler = null;
				
				//Detect the type of repository and create a parser based on that
				switch(repo_type.getType()) {
				case REPO_HGSERVE:
					feed_handler = new HGWebAtomHandler();
					break;
				case REPO_GOOGLECODE:
					feed_handler = new GoogleCodeAtomHandler();
					break;
				case REPO_BITBUCKET:
					feed_handler = new BitbucketAtomHandler();
					break;
				case REPO_CODEPLEX:
					break;
				}
				
				HttpResponse response = null;
				
				try {
					//Create a new request for the feed based on the url
					HttpClient httpClient = new DefaultHttpClient();
					HttpContext localContext = new BasicHttpContext();
					HttpGet httpGet = new HttpGet((repo_type.getUrl().endsWith("/") || repo_type.getUrl().endsWith("\\") ? feed_handler.formatURL(repo_type.getUrl().substring(0, repo_type.getUrl().length()-1)) : feed_handler.formatURL(repo_type.getUrl())));
					
					//Check to see if the user enabled HTTP authentication
					if(repo_type.getAuthentication() == HTTP) {
						//Get their username and password
						byte[] decrypted_info = (repo_type.getUsername() + ":" + repo_type.getPassword()).getBytes();
						
						//Add the header to the http request
						httpGet.addHeader("Authorization", "Basic " + Base64.encodeBytes(decrypted_info));
					}
					
					//Attempt to load the page from the internet
					response = httpClient.execute(httpGet, localContext);
				} catch (ClientProtocolException e2) {
					e2.printStackTrace();
				} catch (IOException e2) {
					e2.printStackTrace();
				} catch (NullPointerException e3) {
					
				} catch(Exception e) {
					
				}
				
				BufferedReader reader = null;
				
				//Create a new reader based on the information retrieved
				if(response != null) {
					try {
						reader = new BufferedReader(
						    new InputStreamReader(
						      response.getEntity().getContent()
						    )
						  );
					} catch (IllegalStateException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					continue;
				}
				
				//Make sure both the feed handler and info loaded from the web are not null
				if(reader != null && feed_handler != null) {
					try {
						Xml.parse(reader, feed_handler);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (SAXException e) {
						e.printStackTrace();
					}
				} else {
					continue;
				}
				
				//Retreive all the beans from the handler
				ArrayList<Beans.ChangesetBean> beans = feed_handler.getAllChangesets();
				
				//Test the first bean to the last revision entered in the database
				if(!beans.isEmpty()) {
					if(!db_helper.getLastRev(repo_type.getId()).equals(beans.get(0).getRevisionID())) {
						new_changeset = true;
						
						//Create the string of repositories for the notification
						if(repo_changes.length() == 0)
							repo_changes = repo_type.getTitle();
						else
							repo_changes += ", " + repo_type.getTitle();
						
						//Update the last revision
						db_helper.updateLastRev(repo_type.getId(), beans.get(0).getRevisionID());
					}
				}
			}
			
			//Show a notification if there were any new changesets
			if(new_changeset) {
				NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				
				final Calendar c = Calendar.getInstance();
				
				Notification notification = new Notification(R.drawable.notification_icon, "New changeset available", c.getTimeInMillis());
				notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
				
				//Use the default options for notifications
				notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
				notification.ledARGB = notification_colour;
				notification.ledOffMS = 2000;
				notification.ledOnMS = 200;
				
				Context context = getApplicationContext();
				CharSequence contentTitle = "New changesets available";
				CharSequence contentText = repo_changes;
				Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("repo://ca.spencerelliott.mercury/browser"));
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

				notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
				
				nm.notify(1, notification);
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void results) {
			//Once we're done processing the data and notifying the user, stop the service
			stopSelf();
		}
		
	}

}
