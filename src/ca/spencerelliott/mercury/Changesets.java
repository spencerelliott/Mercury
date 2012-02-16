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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Contacts.People;
import android.database.Cursor;

public class Changesets extends Activity {
	//Views for the activity
	private ListView changesets_listview;
	private ArrayList<Map<String,String>> contacts_list;
	private ArrayList<Map<String,?>> changesets_list;
	private ArrayList<Beans.ChangesetBean>changesets_data; 
	private volatile Thread load_thread;
	
	private int current_bean_count = 0;
	
	//Handler messages
	private final int SUCCESSFUL = 0;
	private final int CANCELLED = 1;
	private final int SETUP_COUNT = 2;
	private final int UPDATE_PROGRESS = 3;
	
	private boolean is_search_window = false;
	
	//Handles the changing of the adapter in the list
	private Handler list_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {			
			switch(msg.what) {
			case SUCCESSFUL:
				//Put here just in case
				break;
			case CANCELLED:
				//The dialog was cancelled
				changesets_list.clear();
				break;
			case SETUP_COUNT:
				//Setup progress bar
				current_bean_count = msg.getData().getInt("max");
				break;
			case UPDATE_PROGRESS:
				//Update progress bar
				if(current_bean_count != 0)
					getWindow().setFeatureInt(Window.FEATURE_PROGRESS, msg.getData().getInt("progress")*(9999/current_bean_count));
				else
					getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
				break;
			}
			
			if(msg.what == SUCCESSFUL || msg.what == CANCELLED) {
				//Set the data of the new list to show up in the list view
				changesets_listview.setAdapter(new SimpleAdapter(Changesets.this, changesets_list, R.layout.changeset_item, new String[] { COMMIT, FORMATTED_INFO }, new int[] { R.id.changesets_commit, R.id.changesets_info }));
				setProgressBarVisibility(false);
			}
		}
	};
	
	//Group for context menu
	private final int CONTACT_GROUP = 1;
	
	//Labels for the context menu
	private final int EMAIL_PERSON = Menu.FIRST;
	private final int VIEW_BROWSER = Menu.FIRST + 1;
	private final int LINK_COMMITTER = Menu.FIRST + 2;
	private final int MESSAGE_COMMITTER = Menu.FIRST + 3;
	private final int UNLINK_COMMITTER = Menu.FIRST + 4;
	
	private final int REFRESH = Menu.FIRST + 3;
	private final int SEARCH = Menu.FIRST + 4;
	
	//Labels for the hash map
	private final String COMMIT = "commit";
	private final String FORMATTED_INFO = "info";
	
	//Globals needed to keep track of a couple things
	private int last_item_touched = -1;
	private long repo_id = -1;
	
	//Creates a new item for the list view based on the commit text and info
	private Map<String,?> createChangeset(String commit_text, String info) {
		Map<String,String> changeset_data = new HashMap<String,String>();
		
		//Put the strings in to the appropriate key in the hash map
		changeset_data.put(COMMIT, commit_text);
		changeset_data.put(FORMATTED_INFO, info);
		
		return changeset_data;
	}
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.changesets);
		
		//Cancel any notifications previously setup
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(1);
		
		//Create a new array list for the changesets
		changesets_list = new ArrayList<Map<String,?>>();
		changesets_data = new ArrayList<Beans.ChangesetBean>();
		
		//Get the list view to store the changesets
		changesets_listview = (ListView)findViewById(R.id.changesets_list);
		
		TextView empty_text = (TextView)findViewById(R.id.changesets_empty_text);
		
		//Set the empty view
		changesets_listview.setEmptyView(empty_text);
		
		//Use a simple adapter to display the changesets based on the array list made earlier
		changesets_listview.setAdapter(new SimpleAdapter(this, changesets_list, R.layout.changeset_item, new String[] { COMMIT, FORMATTED_INFO }, new int[] { R.id.changesets_commit, R.id.changesets_info }));
		
		//Set the on click listener
		changesets_listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
				Intent intent = new Intent(Changesets.this, ChangesetViewer.class);
				
				//Pass the changeset information to the changeset viewer
				Bundle params = new Bundle();
				params.putString("changeset_commit_text", changesets_data.get(position).getTitle());
				params.putString("changeset_changes", changesets_data.get(position).getContent());
				params.putLong("changeset_updated", changesets_data.get(position).getUpdated());
				params.putString("changeset_authors", changesets_data.get(position).getAuthor());
				params.putString("changeset_link", changesets_data.get(position).getLink());
				//params.putBoolean("is_https", is_https);
				
				intent.putExtras(params);
				
				startActivity(intent);
			}
			
		});
		
		//Register the list view for opening the context menu
		registerForContextMenu(changesets_listview);
		
		//Get the intent passed by the program
		if(getIntent() != null) {
			//Check to see if this is a search window
			if(Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
				//Change the title of the activity and the empty text of the list so it looks like a search window
				this.setTitle(R.string.search_results_label);
				empty_text.setText(R.string.search_results_empty);
				
				//Retrieve the query the user entered
				String query = getIntent().getStringExtra(SearchManager.QUERY);
				
				//Convert the query to lower case
				query = query.toLowerCase();
				
				//Retrieve the bundle data
				Bundle retrieved_data = getIntent().getBundleExtra(SearchManager.APP_DATA);
				
				//If the bundle was passed, grab the changeset data
				if(retrieved_data != null) {
					changesets_data = retrieved_data.getParcelableArrayList("ca.spencerelliott.mercury.SEARCH_DATA");
				}
				
				//If we're missing changeset data, stop here
				if(changesets_data == null) return;
				
				//Create a new array list to store the changesets that were a match
				ArrayList<Beans.ChangesetBean> search_beans = new ArrayList<Beans.ChangesetBean>();
				
				//Loop through each changeset
				for(Beans.ChangesetBean b : changesets_data) {
					//Check to see if any changesets match
					if(b.getTitle().toLowerCase().contains(query)) {
						//Get the title and date of the commit
						String commit_text = b.getTitle();
						Date commit_date = new Date(b.getUpdated());
						
						//Add a new changeset to display in the list view
						changesets_list.add(createChangeset((commit_text.length() > 30 ? commit_text.substring(0, 30) + "..." : commit_text), b.getRevisionID() + " - " + commit_date.toLocaleString()));
						
						//Add this bean to the list of found search beans
						search_beans.add(b);
					}
				}
				
				//Switch the changeset data over to the changeset data that was a match
				changesets_data = search_beans;
				
				//Update the list in the activity
				list_handler.sendEmptyMessage(SUCCESSFUL);
				
				//Notify the activity that it is a search window
				is_search_window = true;
				
				//Stop loading here
				return;
			}
			
			//Get the data from the intent
			Uri data = getIntent().getData();
			
			if(data != null) {
				//Extract the path in the intent
				String path_string = data.getEncodedPath();
				
				//Split it by the forward slashes
				String[] split_path = path_string.split("/");
			
				//Make sure a valid path was passed
				if(split_path.length == 3) {
					//Get the repository id from the intent
					repo_id = Long.parseLong(split_path[2].toString());
				} else {
					//Notify the user if there was a problem
					Toast.makeText(this, R.string.invalid_intent, 1000).show();
				}
			}
		}
		
		//Retrieve the changesets
		refreshChangesets();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		int i = 0;
		
		//Get the context menu info from the adapter
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		
		//Find which item has been selected
		last_item_touched = info.position;
		
		//Create the menu
		//menu.add(0, EMAIL_PERSON, ++i, R.string.changesets_link_contact);
		menu.add(0, VIEW_BROWSER, ++i, R.string.changesets_view_browser);//.setEnabled(!is_https);
		
		SubMenu link_menu = menu.addSubMenu(0, LINK_COMMITTER, ++i, R.string.changesets_link_contact);
		
		if(contacts_list == null) {
			//Create the contacts list
			contacts_list = new ArrayList<Map<String,String>>();
			
			//Store which columns are needed from the contact
			String[] columns = { People._ID, People.NAME };
			
			//Get the Uri to the contacts content provider
			Uri contacts = People.CONTENT_URI;
			
			//Run the query to get all of the contacts on the device
			Cursor all_contacts = managedQuery(contacts, columns, null, null, People.NAME + " ASC");
			
			//Store the column number of the name and id from the content provider
			int id_column = all_contacts.getColumnIndex(People._ID);
			int name_column = all_contacts.getColumnIndex(People.NAME);
			
			//If there are columns
			if(all_contacts.moveToFirst()) {				
				//Loop through each contact and add them to the list
				do {
					//Create a new contact map
					Map<String, String> new_contact = new HashMap<String, String>();
					
					//Add the id and name to the map
					new_contact.put("id", all_contacts.getString(id_column));
					new_contact.put("name", all_contacts.getString(name_column));
					
					//Add the new contact to the list
					contacts_list.add(new_contact);
				} while(all_contacts.moveToNext());
			}
		}
		
		//Set count to -1 since the loop pre-increments the variable so the first
		//used value of count will be 0
		int count = -1;
		
		//Add all the contacts to the sub-menu
		for(Map<String,String> c : contacts_list) {
			link_menu.add(CONTACT_GROUP, Integer.parseInt(c.get("id")), ++count, c.get("name"));
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) { 
		super.onContextItemSelected(item);
		
		//Check to see if this selection was a contact selected from the sub menu
		if(item.getGroupId() == CONTACT_GROUP) {
			//Setup committer linking here
			
			return false;
		}
		
		//Called if the user clicked on the option to email the committer
		if(item.getItemId() == EMAIL_PERSON) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:test@test.com?subject=Regarding revision..."));
			startActivity(intent);
		}
		
		//Opens up the changeset on the native web page
		if(item.getItemId() == VIEW_BROWSER) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(changesets_data.get(last_item_touched).getLink()));
			
			if(intent != null)
				startActivity(intent);
		}
		
		//Open the window to link the committer with a contact in the device
		if(item.getItemId() == LINK_COMMITTER) {
			
		}
		
		//Message the contact linked to the committer
		if(item.getItemId() == MESSAGE_COMMITTER) {
			
		}
		
		//Unlink the contact from the committer
		if(item.getItemId() == UNLINK_COMMITTER) {
			
		}
		
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		//For some reason android.R.drawable.ic_menu_refresh is missing so I needed to grab the 
		//actual file from the SDK and include it in the project
		if(!is_search_window) {
			menu.add(0, REFRESH, 0, R.string.refresh).setIcon(R.drawable.ic_menu_refresh);
			menu.add(0, SEARCH, 1, R.string.changesets_search_option).setIcon(android.R.drawable.ic_menu_search);
		}
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		
		switch(item.getItemId()) {
		//Refresh the changeset list if the user selected "Refresh"
		case REFRESH:
			refreshChangesets();
			break;
		case SEARCH:
			onSearchRequested();
			break;
		}
		
		return true;
	}
	
	@Override
	public void onConfigurationChanged(Configuration config) {
		//Just switch the layout without respawning the activity
		super.onConfigurationChanged(config);	
	}
	
	@Override
	public boolean onSearchRequested() {
		//If it's already a search window, block the search from happening again
		if(is_search_window) return false;
		
		//Create a new bundle to store the changeset data
		Bundle app_data = new Bundle();
		
		//Pack up the array list data in to a parcel
		app_data.putParcelableArrayList("ca.spencerelliott.mercury.SEARCH_DATA", changesets_data);
		
		//Start the search
		startSearch(null, false, app_data, false);
		return true;
	}
	
	public void refreshChangesets() {
		//Set up the window to show the progress dialog in the title bar
		this.getWindow().setFeatureInt(Window.PROGRESS_VISIBILITY_ON, 1);
		this.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 0);
		this.setProgressBarVisibility(true);
		
		//Create a new thread to process all the data in the background
		startThread();
	}
	
	private synchronized void startThread() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		//Create the thread that will process the incoming feed
		load_thread = new Thread() {
			@Override
			public void run() {
				changesets_list.clear();
				
				DatabaseHelper db_helper = DatabaseHelper.getInstance(getApplicationContext());
				EncryptionHelper encrypt_helper = EncryptionHelper.getInstance("DEADBEEF".toCharArray(), new byte[] { 'L', 'O', 'L' });
				
				//Get the repository information from the local database
				Beans.RepositoryBean repo_type = db_helper.getRepository(repo_id, encrypt_helper);
				AtomHandler feed_handler = null;
				
				//Detect the type of repository and create a parser based on that
				switch(repo_type.getType()) {
				case Mercury.RepositoryTypes.HGSERVE:
					feed_handler = new HGWebAtomHandler();
					break;
				case Mercury.RepositoryTypes.GOOGLECODE:
					feed_handler = new GoogleCodeAtomHandler();
					break;
				case Mercury.RepositoryTypes.BITBUCKET:
					feed_handler = new BitbucketAtomHandler();
					break;
				case Mercury.RepositoryTypes.CODEPLEX:
					feed_handler = new CodePlexAtomHandler();
					break;
				}
				
				HttpURLConnection conn = null;
				boolean connected = false;
				
				try {
					// XXX We need to use our own factory to make all ssl certs work
					HttpsURLConnection.setDefaultSSLSocketFactory(NaiveSSLSocketFactory.getSocketFactory());

					String repo_url_string = (repo_type.getUrl().endsWith("/") || repo_type.getUrl().endsWith("\\") ? feed_handler.formatURL(repo_type.getUrl().substring(0, repo_type.getUrl().length()-1)) : feed_handler.formatURL(repo_type.getUrl()));

					switch(repo_type.getType()) {
					case Mercury.RepositoryTypes.BITBUCKET:
						//Only add the token if the user requested it
						if(repo_type.getAuthentication() == Mercury.AuthenticationTypes.TOKEN)
							repo_url_string = repo_url_string + "?token=" + repo_type.getSSHKey();
						break;
					}
					
					URL repo_url = new URL(repo_url_string);
					conn = (HttpURLConnection)repo_url.openConnection();
                    
					//Check to see if the user enabled HTTP authentication
					if(repo_type.getAuthentication() == Mercury.AuthenticationTypes.HTTP) {
						//Get their username and password
						byte[] decrypted_info = (repo_type.getUsername() + ":" + repo_type.getPassword()).getBytes();
						
						//Add the header to the http request
						conn.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(decrypted_info));
					}
					conn.connect();
					connected = true;
				} catch (ClientProtocolException e2) {
					AlertDialog.Builder alert = new AlertDialog.Builder(getBaseContext());
					alert.setMessage("There was a problem with the HTTP protocol");
					alert.setPositiveButton(android.R.string.ok, null);
					alert.show();
					
					//Do not allow the app to continue with loading
					connected = false;
				} catch (IOException e2) {
					AlertDialog.Builder alert = new AlertDialog.Builder(getBaseContext());
					alert.setMessage("Server did not respond with a valid HTTP response");
					alert.setPositiveButton(android.R.string.ok, null);
					alert.show();
					
					//Do not allow the app to continue with loading
					connected = false;
				} catch (NullPointerException e3) {
					
				} catch(Exception e) {
					
				}
				
				BufferedReader reader = null;
				
				//Create a new reader based on the information retrieved
				if(connected) {
					try {
						reader = new BufferedReader(
						    new InputStreamReader(conn.getInputStream()
						    )
						  );
					} catch (IllegalStateException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					list_handler.sendEmptyMessage(CANCELLED);
					return;
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
					list_handler.sendEmptyMessage(CANCELLED);
					return;
				}
				
				//Stored beans in the devices database
				ArrayList<Beans.ChangesetBean> stored_beans = null;
				
				if(prefs.getBoolean("caching", false)) {
					long last_insert = db_helper.getHighestID(DatabaseHelper.DB_TABLE_CHANGESETS, repo_id);
					
					if(last_insert >= 0) {
						//Get all of the stored changesets
						stored_beans = db_helper.getAllChangesets(repo_id, null);
						
						String rev_id = "";
						
						//Try to find the revision id of the bean that has the id of the last inserted value
						for(Beans.ChangesetBean b : stored_beans) {
							if(b.getID() == last_insert) {
								rev_id = b.getRevisionID();
								break;
							}
						}
						
						//Trim the list starting from this revision
						feed_handler.trimStartingFromRevision(rev_id);
					}
				}
				
				//Create a new bundle for the progress
				Bundle progress_bundle = new Bundle();
				
				//Retreive all the beans from the handler
				ArrayList<Beans.ChangesetBean> beans = feed_handler.getAllChangesets();
				int bean_count = beans.size();
				
				//Store the amount of changesets
				progress_bundle.putInt("max", bean_count);
				
				//Create a new message and store the bundle and what type of message it is
				Message msg = new Message();
				msg.setData(progress_bundle);
				msg.what = SETUP_COUNT;
				list_handler.sendMessage(msg);
				
				//Add each of the beans to the list
				for(int i = 0; i < bean_count; i++) {
					String commit_text = beans.get(i).getTitle();
					Date commit_date = new Date(beans.get(i).getUpdated());
					changesets_list.add(createChangeset((commit_text.length() > 30 ? commit_text.substring(0, 30) + "..." : commit_text), beans.get(i).getRevisionID() + " - " + commit_date.toLocaleString()));
					
					//Store the current progress of the changeset loading
					progress_bundle.putInt("progress", i);
					
					//Reuse the old message and send an update progress message
					msg = new Message();
					msg.setData(progress_bundle);
					msg.what = UPDATE_PROGRESS;
					list_handler.sendMessage(msg);
				}
				
				//Get the current count of changesets and the shared preferences
				long changeset_count = db_helper.getChangesetCount(repo_id);
				
				if(prefs.getBoolean("caching", false)) {
					//Get all of the stored beans from the device if not already done
					if(stored_beans == null)
						stored_beans = db_helper.getAllChangesets(repo_id, null);
					
					//Add all the changesets from the device
					for(Beans.ChangesetBean b : stored_beans) {
						changesets_list.add(createChangeset((b.getTitle().length() > 30 ? (b.getTitle().substring(0, 30)) + "..." : b.getTitle()), b.getRevisionID() + " - " + new Date(b.getUpdated()).toLocaleString()));
					}
					
					//Reverse the list so the oldest changesets are stored first
					Collections.reverse(beans);
					
					//Iterate through each bean and add it to the device's database
					for(Beans.ChangesetBean b : beans) {
						db_helper.insert(b, repo_id);
					}
					
					//Get the amount of changesets allowed to be stored on the device
					int max_changes = Integer.parseInt(prefs.getString("max_changesets", "-1"));
					
					//Delete the oldest changesets if too many have been stored
					if(changeset_count > max_changes) {						
						db_helper.deleteNumChangesets(repo_id, (changeset_count-max_changes));
					}
				} else if(changeset_count > 0) {
					//Since the user does not have caching enabled, delete the changesets
					db_helper.deleteAllChangesets(repo_id);
				}
				
				//Update the tables to the newest revision
				if(!beans.isEmpty())
					db_helper.updateLastRev(repo_id, beans.get(0).getRevisionID());
				
				//Add all of the data to the changeset list
				changesets_data.addAll(beans);
				
				if(prefs.getBoolean("caching", false))
					changesets_data.addAll(stored_beans);
				
				//Clean up the sql connection
				db_helper.cleanup();
				db_helper = null;
				
				//Notify the handler that the loading of the list was successful
				list_handler.sendEmptyMessage(SUCCESSFUL);
			}
		};
		
		//Start the thread
		load_thread.start();
	}
	
	private synchronized void stopThread() {
		//Make sure the load thread exists before attempting to stop it
		if(load_thread != null) {
			//Save the old thread
			Thread old_thread = load_thread;
			
			//Get rid of the old thread
			load_thread = null;
			
			//Interrupt the old thread
			old_thread.interrupt();
		}
	}
}
