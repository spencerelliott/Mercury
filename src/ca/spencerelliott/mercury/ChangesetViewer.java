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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class ChangesetViewer extends Activity {
	private final int VIEW_IN_BROWSER = Menu.FIRST;
	
	private String commit_text = "";
	private String changes = "";
	private String authors = "";
	private String link = "";
	private long update_time = -1;
	//private boolean is_https = false;
	
	@Override
	public void onCreate(Bundle bundle) {	
		super.onCreate(bundle);
		
		setContentView(R.layout.changeset_view);
		
		//Notify the user if an invalid intent was passed
		if(getIntent() == null) Toast.makeText(this, R.string.invalid_intent, 1000).show();
		
		Intent passed_intent = getIntent();
		
		//Make sure the intent that was passed was not null and extract the data from it
		if(passed_intent != null) {
			commit_text = passed_intent.getStringExtra("changeset_commit_text");
			changes = passed_intent.getStringExtra("changeset_changes");
			authors = passed_intent.getStringExtra("changeset_authors");
			link = passed_intent.getStringExtra("changeset_link");
			update_time = passed_intent.getLongExtra("changeset_updated", -1);
			//is_https = passed_intent.getBooleanExtra("is_https", false);
		}
		
		//Add all of the data to the fields on the page
		((TextView)findViewById(R.id.changeset_view_commit_text)).setText(commit_text);
		((TextView)findViewById(R.id.changeset_view_date)).setText(update_time >= 0 ? new java.util.Date(update_time).toLocaleString() : "");
		((TextView)findViewById(R.id.changeset_view_change_by)).setText(authors);
		((TextView)findViewById(R.id.changeset_view_changes)).setText(changes);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		//Add the view in browser menu button
		menu.add(0, VIEW_IN_BROWSER, 0, R.string.changesets_view_browser).setIcon(android.R.drawable.ic_menu_view);//.setEnabled(!is_https);
		return true;
		
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		
		switch(item.getItemId()) {
		//Open the changeset in the browser
		case VIEW_IN_BROWSER:
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
			startActivity(intent);
			break;
		}
		
		return true;
	}
}
