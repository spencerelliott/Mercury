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
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class AddRepository extends Activity {
	private EditText repo_name;
	
	private EditText username;
	private EditText password;
	private EditText key;
	
	private EditText url;
	
	private Spinner repo_types;
	private Spinner repo_auth;
	
	EncryptionHelper encrypt_helper = null;
	DatabaseHelper db_helper = null;
	
	private boolean edit_mode = false;
	private long edit_repo = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_repo);
		
		//Check to see if extras were passed to the intent. In that case, edit the repository
		if(getIntent().getExtras() != null) {
			Bundle rec_bundle = getIntent().getExtras();
			
			//Get the repository to edit
			edit_repo = rec_bundle.getLong("editRepo", -1);
			
			//If the repository id is valid, set the activity to edit mode
			if(edit_repo >= 0) {
				edit_mode = true;
				this.setTitle(R.string.add_repo_edit_title);
			}
		}
		
		encrypt_helper = EncryptionHelper.getInstance("DEADBEEF".toCharArray(), new byte[] { 'L', 'O', 'L' });
		db_helper = DatabaseHelper.getInstance(this);
		
		repo_name = (EditText)findViewById(R.id.repo_name);
		
		username = (EditText)findViewById(R.id.add_repo_username);
		password = (EditText)findViewById(R.id.add_repo_password);
		password.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD); //Make this into a password box
		password.setTransformationMethod(new PasswordTransformationMethod());
		
		key = (EditText)findViewById(R.id.add_repo_key);
		
		url = (EditText)findViewById(R.id.add_repo_url);
		
		repo_types = (Spinner)findViewById(R.id.repo_type_spinner);
		repo_auth = (Spinner)findViewById(R.id.repo_authentication_spinner);
		
		//Create a new list for storing the services the program can handle
		ArrayList<String> services = new ArrayList<String>();
		
		//Add the services supported by the program
		services.add("HGWeb Served");
		services.add("Google Code");
		services.add("Bitbucket");
		services.add("CodePlex");
		
		//Create a new adapter with the list of services
		AddRepoAdapter type_adapter = new AddRepoAdapter(this, services);
		
		//Add the adapter to the spinner that stores the list of services
		repo_types.setAdapter(type_adapter);
		
		//Create a new list to store authentication types
		ArrayList<String> authentication = new ArrayList<String>();
		
		//Add the authentication types
		authentication.add(0, "None");
		authentication.add(Mercury.AuthenticationTypes.HTTP, "HTTP");
		authentication.add(Mercury.AuthenticationTypes.TOKEN, "Token");
		//authentication.add(SSH, "SSH");
		
		//Create a new adapter based on the authentication types
		AddRepoAdapter auth_adapter = new AddRepoAdapter(this, authentication);
		
		//Add the adapter to the authentication spinner
		repo_auth.setAdapter(auth_adapter);
		
		//Create a listener that triggers whenever the authentication type is changed
		repo_auth.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapterview, View view,
					int position, long id) {
				//If anything other than "None" is selected, enable the username and password or ssh fields
				if(position > Mercury.AuthenticationTypes.NONE) {
					if(position == Mercury.AuthenticationTypes.TOKEN) {
						if(username.getVisibility() != View.GONE || password.getVisibility() != View.GONE) {
							//Hide both the username and password if they're not already gone
							username.setVisibility(View.GONE);
							password.setVisibility(View.GONE);
						}
						
						//Make the key text box visible
						key.setVisibility(View.VISIBLE);
						
						//Enable the controls on the key text box
						key.setEnabled(true);
						key.setFocusable(true);
						key.setClickable(true);
						key.setFocusableInTouchMode(true);
						
					} else {
						//Make the username and password visible
						username.setVisibility(View.VISIBLE);
						password.setVisibility(View.VISIBLE);
						
						key.setVisibility(View.GONE);
						key.setText("");
						
						//Re-enable the username and password text boxes
						username.setEnabled(true);
						username.setCursorVisible(true);
						username.setClickable(true);
						username.setFocusable(true);
						username.setFocusableInTouchMode(true);
						
						password.setEnabled(true);
						password.setCursorVisible(true);
						password.setClickable(true);
						password.setFocusable(true);
						password.setFocusableInTouchMode(true);
					}
				//Otherwise, disable all fields from user interaction
				} else {
					key.setEnabled(false);
					key.setFocusable(false);
					key.setClickable(false);
					
					username.setEnabled(false);
					username.setFocusable(false);
					username.setClickable(false);
					
					password.setEnabled(false);
					password.setClickable(false);
					password.setFocusable(false);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				//Will never be called on a spinner
			}
			
		});
		
		Button save_button = (Button)findViewById(R.id.add_repo_save);
		Button discard = (Button)findViewById(R.id.add_repo_discard);
		
		discard.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				killActivity(Activity.RESULT_CANCELED);
			}	
		});
		
		save_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//Check the user input
				if(checkInput()) {
					//Attempt to save the repository
					if(saveRepository())
						//If successful go back to the list
						killActivity(Activity.RESULT_OK);
				}
			}
			
		});
		
		if(edit_mode) {			
			//Get the information for this repository
			Beans.RepositoryBean repo_info = db_helper.getRepository(edit_repo, null);
			
			//Set the information in the activity
			repo_name.setText(repo_info.getTitle());
			url.setText(repo_info.getUrl());
			
			username.setText(repo_info.getUsername());
			password.setText(repo_info.getPassword());
			key.setText(repo_info.getSSHKey());
			
			repo_types.setSelection(repo_info.getType());
			repo_auth.setSelection(repo_info.getAuthentication());
		}
		
	}
	
	//Kills the activity
	private void killActivity(int result) {
		this.setResult(result);
		this.finish();
	}
	
	//Checks to make sure all of the input on the screen is valid
	private boolean checkInput() {
		//Make sure they have given the repository a name
		if(repo_name.getText().length() == 0) {
			Toast.makeText(this, R.string.add_repo_missing_repo_name, 1000).show();
			return false;
		}
		
		//Check to see if the URL is missing
		if(url.getText().length() == 0) { 
			Toast.makeText(this, R.string.add_repo_missing_url, 1000).show();
			return false;
		}
		
		//Check to see if the user has entered a valid URL using a regular expression
		if(!Pattern.matches("((https?):((//)|(\\\\\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", url.getText().toString())) {
			Toast.makeText(this, R.string.add_repo_invalid_url, 1000).show();
			return false;
		}
		
		//Check to see if the user has selected HTTP or HTTPS as the authentication method
		if(repo_auth.getSelectedItemPosition() == Mercury.AuthenticationTypes.HTTP) { //|| repo_auth.getSelectedItemPosition() == Mercury.AuthenticationTypes.HTTPS) {
			//Make sure a username has been supplied
			if(username.getText().length() == 0) {
				Toast.makeText(this, R.string.add_repo_missing_user_pass, 1000).show();
				return false;
			}
		}
		
		//Check to see if the user selected SSH validation
		if(repo_auth.getSelectedItemPosition() == Mercury.AuthenticationTypes.TOKEN) {
			//Make sure they entered their SSH key
			if(key.getText().length() == 0) {
				Toast.makeText(this, R.string.add_repo_missing_ssh, 1000).show();
				return false;
			}
		}
		
		return true;
	}
	
	//Saves the repository to the database
	private boolean saveRepository() {
		String encrypted_username = new String(encrypt_helper.encrypt(username.getText().toString()));
		String encrypted_password = new String(encrypt_helper.encrypt(password.getText().toString()));
		
		Beans.RepositoryBean cur_repo = Beans.RepositoryBean.getInstance(edit_repo, repo_name.getText().toString(), url.getText().toString(), repo_types.getSelectedItemPosition(), repo_auth.getSelectedItemPosition(), 
				encrypted_username, encrypted_password, new String(encrypt_helper.encrypt(key.getText().toString())), 
				encrypt_helper);
		
		//Make sure edit mode is not on
		if(!edit_mode) {
			//Attempt to add the repository to the application
			if(!db_helper.insert(cur_repo)) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(R.string.add_repo_adding_error_title);
				alert.setMessage("Could not add repository to the list");
				alert.setPositiveButton(android.R.string.ok, null);
				alert.show();
				return false;
			}
			
			Toast.makeText(this, R.string.add_repo_added_repo, 1000).show();
		} else {
			//Attempt to update the repository in the database
			if(!db_helper.update(cur_repo)) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(R.string.add_repo_adding_error_title);
				alert.setMessage("Could not update repository");
				alert.setPositiveButton(android.R.string.ok, null);
				alert.show();
				return false;
			}
			
			Toast.makeText(this, R.string.add_repo_updated_repo, 1000).show();
		}
		
		return true;
	}
	
	public class AddRepoAdapter extends BaseAdapter {
		
		//Globals to store the list and context
		List<String> servicesList = null;
		Context context = null;
		
		//Just store the list and context on creation
		public AddRepoAdapter(Context context, List<String> strings) {
			this.context = context;
			this.servicesList = strings;
		}
		
		//Return the amount of objects in the adapter
		@Override
		public int getCount() {
			return servicesList.size();
		}

		@Override
		public Object getItem(int position) {
			return servicesList.get(position);
		}

		//Get the id for an item in the list
		@Override
		public long getItemId(int position) {
			return position;
		}

		//Returns a new view which will display inside the spinner depending on the item selected
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Only create a new view if needed
			if(convertView == null)
				convertView = new AddRepoView(this.context, (String)getItem(position), null);
			else
				((AddRepoView) convertView).setViewText((CharSequence)getItem(position));
			
			return convertView;
		}
		
		//Returns a new view based on the objects inside the adapter (this will show when the spinner is clicked)
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			//Only create a new view if needed
			if(convertView == null)
				convertView = new AddRepoDropDownView(this.context, (String)getItem(position), null);
			else
				((AddRepoDropDownView) convertView).setViewText((CharSequence)getItem(position));
			
			return convertView;
		}
		
		//Will be displayed in the layout as the spinner text
		private class AddRepoView extends LinearLayout {
			
			//Drawable icon = null;
			TextView service = null;
			
			AddRepoView(Context context, String service, Drawable icon) {
				super(context);
				
				//Creates new parameters for the objects to be shown
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.setMargins(3, 3, 3, 0);
				
				//Creates a new text view object and sets the text as the passed service in an 18 point black font
				this.service = new TextView(context);
				this.service.setText(service);
				this.service.setTextSize(18f);
				this.service.setTextColor(Color.BLACK);
				this.addView(this.service, params);
			}
			
			//Sets the text that will be shown on screen
			public void setViewText(CharSequence text) {
				service.setText(text);
			}
		}
		
		//Will be displayed in the pop-up when the spinner is clicked
		private class AddRepoDropDownView extends LinearLayout {
			
			//Drawable icon = null;
			TextView service = null;
			
			AddRepoDropDownView(Context context, String service, Drawable icon) {
				super(context);
				
				//Creates new parameters for the pop-up
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.setMargins(5,5,5,0);
				
				//Sets the text of the objects inside the pop-up to the service with a 24 point black font with margins
				this.service = new TextView(context);
				this.service.setText(service);
				this.service.setTextSize(24f);
				this.service.setPadding(15,20,20,15);
				this.service.setTextColor(Color.BLACK);
				this.addView(this.service, params);
			}
			
			//Sets the text that will be shown on screen
			public void setViewText(CharSequence text) {
				service.setText(text);
			}
		}
		
	}
}
