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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import ca.spencerelliott.mercury.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class RepoBrowser extends Activity {
	
	public static final int ADD_REPO = Menu.FIRST;
	public static final int SETTINGS = Menu.FIRST + 1;
	
	public static final int DELETE_REPO = Menu.FIRST + 2;
	public static final int EDIT_REPO = Menu.FIRST + 3;
	
	private static final String REPO_TITLE = "title";
	private static final String REPO_URL = "url";
	
	private ListView repo_list = null;
	
	//Stores the repository data
	private ArrayList<Map<String,?>> repos = null;
	private ArrayList<Beans.RepositoryBean> repo_data = null;
	
	//Stores the last item that was touched during a long hold
	private int last_item_touched = -1;
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			SimpleAdapter adapter = new SimpleAdapter(RepoBrowser.this, repos, R.layout.repo_list, 
	        		new String[] { REPO_TITLE, REPO_URL }, new int[] { R.id.repo_list_title, R.id.repo_list_url });
	        
	        repo_list.setAdapter(adapter);
		}
	};
	
	//Creates an item for the repository browser screen
	private Map<String,?> createListItem(String title, String url) {
		Map<String,String> newMap = new HashMap<String,String>();
		newMap.put(REPO_TITLE, title);
		newMap.put(REPO_URL, url);
		return newMap;
	}
	
	private DatabaseHelper db_helper = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	TextView empty;
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Cancel any notifications previously setup
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(1);
        
        //Create an array list of values to store the repository information
        repos = new ArrayList<Map<String,?>>();
        repo_data = new ArrayList<Beans.RepositoryBean>();
        
        // *** Test data ***
        //repos.add(createListItem("Test", "http://www.test.com"));
        
        empty = (TextView)findViewById(R.id.EmptyRepoText);
        
        //Retrieve the list view on the repo browser screen
        repo_list = (ListView)findViewById(R.id.repo_list);
        
        //Load the contents of the list
        loadList();
        
        //Set the empty view of the list
        repo_list.setEmptyView(empty);
        
        //Rudimentary on item click listener for now
        repo_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterview, View view, int position,
					long id) {
				//Create the new intent to launch the changeset activity
				//Intent intent = new Intent(RepoBrowser.this, Changesets.class);
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("repo://ca.spencerelliott.mercury/changesets/" + repo_data.get(position).getId()));
				
				//Start the activity
				startActivity(intent);
			}
        	
        });
        
        //Register the list for a context menu
        registerForContextMenu(repo_list);
        
        /*

        //*** Encryption tests ***
        byte[] salt = new byte[] { 0, 1, 2, 3};
        EncryptionHelper encrypt_helper = EncryptionHelper.getInstance("Test".toCharArray(), salt);
        byte[] encrypted = encrypt_helper.encrypt("Hello, World! This is an encryption test!");
        
        EncryptionHelper decrypt_helper = EncryptionHelper.getInstance("Test".toCharArray(), salt);
        String decrypted = decrypt_helper.decrypt(encrypted);
        
        //Toast.makeText(this, decrypted, 1000).show();
        //************************
        
        //*** Parser tests ***
        GoogleCodeParser p = new GoogleCodeParser();
        String revision = p.parseRevisionID("Revision c96f9ec3f1: Test");
        Date date = p.parseUpdateTime("2010-05-14T20:42:37Z");
        Toast.makeText(this, date.toString(), 1000).show();
        
        BitbucketParser b = new BitbucketParser();
        Date b_date = b.parseUpdateTime("Fri, 21 May 2010 04:31:28 +0200");
        Toast.makeText(this, b_date.toString(), 1000).show();
        //*******************

        */
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	//Add the options to the menu
    	menu.add(0, RepoBrowser.ADD_REPO, 0, "Add").setIcon(android.R.drawable.ic_menu_add);
    	menu.add(0, RepoBrowser.SETTINGS, 1, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
    	
    	return true;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menu_info) {
    	super.onCreateContextMenu(menu, view, menu_info);
    	
    	//Get the context menu info from the adapter
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menu_info;
		last_item_touched = info.position;
		
		//Add the options to the context menu
		menu.add(0, RepoBrowser.EDIT_REPO, 0, "Edit");
		menu.add(0, RepoBrowser.DELETE_REPO, 1, "Delete");
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch(item.getItemId()) {
    	case ADD_REPO:
    		//Shows the activity to add a repository to the list
    		Intent add_intent = new Intent(RepoBrowser.this, AddRepository.class);
    		startActivityForResult(add_intent, 0);
    		break;
    	case SETTINGS:
    		//Show the settings activity
    		Intent settings_intent = new Intent(RepoBrowser.this, Settings.class);
    		startActivity(settings_intent);
    		break;
    	case DELETE_REPO:
    		//Create a dialog to ask the user if they are sure they want to delete it
    		AlertDialog.Builder alert = new AlertDialog.Builder(this);
    		
    		//Add the title and message to the dialog
    		alert.setTitle(R.string.delete_repo_title);
    		alert.setMessage(R.string.delete_repo_message);
    		
    		//Create the listener that will respond to the button presses
    		OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Delete the entry if the user selects OK
					if(which == AlertDialog.BUTTON_POSITIVE) {
						deleteLastSelected();
					} else if(which == AlertDialog.BUTTON_NEGATIVE) {
						
					}
				}			
    		};
    		
    		//Set the buttons of the dialog using the on click listener created above
    		alert.setPositiveButton(android.R.string.ok, listener);
    		alert.setNegativeButton(android.R.string.cancel, listener);
    		
    		//Show the dialog
    		alert.show();
    		break;
    	case EDIT_REPO:
    		//Create the intent
    		Intent edit_intent = new Intent(RepoBrowser.this, AddRepository.class);
    		
    		Bundle edit_bundle = new Bundle();
    		edit_bundle.putLong("editRepo", repo_data.get(last_item_touched).getId());
    		
    		//Add that we want to edit a repository, not add
    		edit_intent.putExtras(edit_bundle);
    		
    		startActivityForResult(edit_intent, 0);    		
    		break;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    protected void onActivityResult(int request, int result, Intent intent) {
    	//Check to see which activity returned a value
    	switch(request) {
    	case 0:
    		//If the result was ok, reload the list
    		if(result == Activity.RESULT_OK) {
    			loadList();
    		}
    		break;
    	}
    }
    
    private void loadList() {
    	db_helper = DatabaseHelper.getInstance(this);
    	
    	//Retrieve all of the repositories
    	repo_data = db_helper.getAllRepositories(null);
    	
    	//Clear out the current state of the repositories list
    	repos.clear();
    	
    	//Loop through each item in the repository data list
    	for(Beans.RepositoryBean b : repo_data) {
    		//Add a displayable version of the repository data
    		repos.add(createListItem(b.getTitle(), b.getUrl()));
    	}
        
    	db_helper.cleanup();
    	db_helper = null;
    	
        //Set the adapter to a simple adapter created from the list generated from the SQLite database
        handler.sendEmptyMessage(0);
    }
    
    private boolean deleteLastSelected() {
    	if(last_item_touched == -1) return false;
    	
    	db_helper = DatabaseHelper.getInstance(this);
    	
    	//ProgressDialog dialog = ProgressDialog.show(this, R.string.delete_repo_dialog_title, R.string.delete_repo_dialog_message, false);
    	ProgressDialog dialog = new ProgressDialog(this);
    	
    	//Set the title of the dialog
    	dialog.setTitle(R.string.delete_repo_dialog_title);
    	
    	//This had to be hard coded in. For some reason you can't specify a resource id
    	dialog.setMessage("Please wait...");
    	
    	//Show the dialog
    	dialog.show();
    	
    	//Delete the repository selected from the list
    	db_helper.deleteRepository(repo_data.get(last_item_touched).getId());
    	
    	//Remove the repository from the displayed list
    	repo_data.remove(last_item_touched);
    	repos.remove(last_item_touched);
    	
    	//Reload all of the data
    	handler.sendEmptyMessage(0);
    	
    	//Reset the last touched item
    	last_item_touched = -1;
    	
    	//Dismiss the dialog
    	dialog.dismiss();
    	
    	db_helper.cleanup();
    	db_helper = null;
    	
    	//Notify the user the repository was deleted
    	Toast.makeText(this, R.string.delete_repo_confirmation, 1000).show();
    	return true;
    }
}