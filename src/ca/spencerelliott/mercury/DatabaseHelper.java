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
import java.util.Collections;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import ca.spencerelliott.mercury.EncryptionHelper;

public class DatabaseHelper {
		//Log tag
		private final static String LOG_TAG = "DatabaseHelper (Mercury)";
		
		//Database name and version
		public final static String DB_NAME = "mercury";
		public final static int DB_VERSION = 2;
		
		//Tables used in the application
		public final static String DB_TABLE_REPOS = "repositories";
		private final static String DB_TABLE_REPOS_ARGS = "(_id INTEGER PRIMARY KEY," +
				" name TEXT UNIQUE NOT NULL, url TEXT NOT NULL, type INTEGER NOT NULL, " +
				"authentication INTEGER NOT NULL, username TEXT, " +
				"password TEXT, ssh_key TEXT)";
		private final static String[] DB_TABLE_REPOS_COLS = 
			{"_id", "name", "url", "type", "authentication", "username", "password", "ssh_key"};
		
		public final static String DB_TABLE_CHANGESETS = "changesets";
		private final static String DB_TABLE_CHANGESETS_ARGS = "(_id INTEGER PRIMARY KEY," +
				" rev_id TEXT NOT NULL, repo_id INTEGER NOT NULL, link TEXT NOT NULL, " +
				"title TEXT NOT NULL, author TEXT, updated INTEGER NOT NULL, content TEXT, encryption INTEGER)";
		private final static String[] DB_TABLE_CHANGESETS_COLS = 
			{"_id", "rev_id", "repo_id", "link", "title", "author", "updated", "content", "encryption"};
		
		public final static String DB_TABLE_REV_IDS = "revision_ids";
		private final static String DB_TABLE_REV_IDS_ARGS = "(_id INTEGER PRIMARY KEY," +
				" repo_id INTEGER NOT NULL, last_rev_id TEXT NOT NULL)";
		private final static String[] DB_TABLE_REV_IDS_COLS =
			{"_id", "repo_id", "last_rev_id"};
		
		//Database open helper
		private final DatabaseHelper.DatabaseOpenHelper dbOpenHelper;
		
		//Actual database handler
		private SQLiteDatabase db = null;
		
		DatabaseHelper(Context context) {
			dbOpenHelper = new DatabaseOpenHelper(context, DatabaseHelper.DB_NAME, DatabaseHelper.DB_VERSION);
			establishDatabase();
		}
		
		//Returns an instance of a database helper to the caller
		public static DatabaseHelper getInstance(Context context) {
			return new DatabaseHelper(context);
		}
		
		//Establishes a connection to the database
		private void establishDatabase() {
			if(this.db == null) {
				db = dbOpenHelper.getWritableDatabase();
			}
		}
		
		//Closes the connection to the database
		public void cleanup() {
			if(this.db != null) {
				this.db.close();
				this.db = null;
			}
		}
		
		//Inserts a new repository bean into the database
		public boolean insert(Beans.RepositoryBean repository) {
			ContentValues values = new ContentValues();
			
			//Store all the repository values
			values.put("name", repository.getTitle());
			values.put("url", repository.getUrl());
			values.put("type", repository.getType());
			values.put("authentication", repository.getAuthentication());
			values.put("username", repository.getEncryptedUsername());
			values.put("password", repository.getEncryptedPassword());
			values.put("ssh_key", repository.getEncryptedSSHKey());
			
			//Insert the values in to the database
			long new_id = this.db.insert(DatabaseHelper.DB_TABLE_REPOS, null, values);
			
			if(new_id >= 0) {
				values = new ContentValues();
				
				//Get the id of the inserted repository information
				values.put("repo_id", new_id);
				values.put("last_rev_id", "0");
				
				this.db.insert(DatabaseHelper.DB_TABLE_REV_IDS, null, values);
			}
			
			//Make sure a vlid insert was performed
			return validateInsert(new_id, DatabaseHelper.DB_TABLE_REPOS);
		}
		
		public boolean update(Beans.RepositoryBean repository) {
			ContentValues values = new ContentValues();
			
			//Store all the new repository values
			values.put("name", repository.getTitle());
			values.put("url", repository.getUrl());
			values.put("type", repository.getType());
			values.put("authentication", repository.getAuthentication());
			values.put("username", repository.getEncryptedUsername());
			values.put("password", repository.getEncryptedPassword());
			values.put("ssh_key", repository.getEncryptedSSHKey());;
			
			//Execute the command to update the repository
			long upd_id = this.db.update(DatabaseHelper.DB_TABLE_REPOS, values, "_id = " + repository.getId(), null);
			
			return validateInsert(upd_id, DatabaseHelper.DB_TABLE_REPOS);
		}
		
		//Inserts a new changeset bean into the database
		public boolean insert(Beans.ChangesetBean changeset, long repo_id) {
			ContentValues values = new ContentValues();
			
			//Store all the changeset values
			values.put("rev_id", changeset.getRevisionID());
			values.put("repo_id", repo_id);
			values.put("link", changeset.getLink());
			values.put("title", changeset.getTitle());
			values.put("author", changeset.getAuthor());
			values.put("updated", changeset.getUpdated());
			values.put("content", changeset.getContent());

			//Insert the values in to the database
			long new_id = this.db.insert(DatabaseHelper.DB_TABLE_CHANGESETS, null, values);
			
			//Make sure a valid insert was performed
			return validateInsert(new_id, DatabaseHelper.DB_TABLE_CHANGESETS);
		}
		
		public boolean updateLastRev(long repo_id, String new_rev) {
			ContentValues values = new ContentValues();
			
			//Update the last revision id
			values.put("last_rev_id", new_rev);
			
			//Execute the command to update the field
			long upd_id = this.db.update(DatabaseHelper.DB_TABLE_REV_IDS, values, "repo_id = " + repo_id, null);
			
			//Return that all went well
			return validateInsert(upd_id, DatabaseHelper.DB_TABLE_REV_IDS);
		}
		
		public String getLastRev(long repo_id) {
			Cursor c = null;
			String rev_id = null;
			
			//Query the database for the last revision
			c = this.db.query(DatabaseHelper.DB_TABLE_REV_IDS, DatabaseHelper.DB_TABLE_REV_IDS_COLS, "repo_id = " + repo_id, null, null, null, null);
			
			if(c != null) {
				c.moveToFirst();
				
				//Get the revision id
				rev_id = c.getString(2);
			
				//Close the cursor
				c.close();
			}
			
			//Return the revision id
			return rev_id;
		}
		
		public long getHighestID(String tableName, long repo_id) {
			SQLiteStatement d = this.db.compileStatement("SELECT max(_id) FROM " + tableName + " WHERE repo_id = " + repo_id);
			
			long max_id = -1;
			
			//Get the max id of the table
			try {
				max_id = d.simpleQueryForLong();
			} catch(SQLiteDoneException e) {
				if(Mercury.DEBUG_MODE)
					Log.i("Mercury", e.toString());
			}
			
			return max_id;
		}
		
		public long getLowestID(String tableName, long repo_id) {
			SQLiteStatement d = this.db.compileStatement("SELECT min(_id) FROM " + tableName + " WHERE repo_id = " + repo_id);
			
			long min_id = -1;
			
			//Get the min id of the table
			try {
				min_id = d.simpleQueryForLong();
			} catch(SQLiteDoneException e) {
				if(Mercury.DEBUG_MODE)
					Log.i("Mercury", e.toString());
			}
			
			return min_id;
		}
		
		public long getIDofLastInsert(String tableName) {
			SQLiteStatement d = this.db.compileStatement("SELECT last_insert_rowid() FROM " + tableName);
			
			long last_id = -1;
			
			try {
				last_id = d.simpleQueryForLong();
			} catch(SQLiteDoneException e) {
				Log.e("Mercury", e.toString());
			}
			
			return last_id;
		}
		
		public void deleteNumChangesets(long repo_id, long amount) {
			Cursor c = null;
			
			//Get all the changesets from the device
			try {
				c = this.db.query(DatabaseHelper.DB_TABLE_CHANGESETS, new String[] {"_id", "repo_id"}, "repo_id = " + repo_id, null, null, null, null);
			} catch (SQLException e) {
				Log.e(DatabaseHelper.LOG_TAG, "Error while retrieving changesets for repository: " + repo_id);
			}
			
			//Make sure there is a cursor to loop through
			if(c != null && c.moveToFirst()) {
				boolean cursor_has_next = true;
				
				//Remove the requested amount from the database
				for(int i = 0; i < (amount-1) && cursor_has_next; i++) {
					deleteChangeset(c.getLong(0));
					cursor_has_next = c.moveToNext();
				}
			}
		}
		
		public long getChangesetCount(long repo_id) {
			SQLiteStatement d = this.db.compileStatement("SELECT COUNT(*) FROM " + DatabaseHelper.DB_TABLE_CHANGESETS + " WHERE repo_id=" + repo_id);
			return d.simpleQueryForLong();
		}
		
		public Beans.RepositoryBean getRepository(long id, EncryptionHelper helper) {
			Beans.RepositoryBean ret_bean = null;
			Cursor c = null;
			
			EncryptionHelper bean_helper = null;
			
			//Check to see if an encryption helper was passed in
			if(helper == null) {
				bean_helper = EncryptionHelper.getInstance("DEADBEEF".toCharArray(), new byte[] {'L', 'O', 'L' });
			} else
				bean_helper = helper;
			
			try {
				c = this.db.query(DatabaseHelper.DB_TABLE_REPOS, DatabaseHelper.DB_TABLE_REPOS_COLS, "_id = " + id, null, null, null, null);
				
				if(c != null) {
					c.moveToFirst();
					
					//Create a new bean based on the information in the database
					ret_bean = Beans.RepositoryBean.getInstance(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getString(5), c.getString(6), c.getString(7), bean_helper);
						
				}
			} catch(SQLException e) {
				Log.e(DatabaseHelper.LOG_TAG, "Error while retrieving repository: " + id);
			} finally {
				//Make sure the cursor is closed
				if(c != null && !c.isClosed())
					c.close();
			}
			
			return ret_bean;
		}
		
		public ArrayList<Beans.RepositoryBean> getAllRepositories(EncryptionHelper helper) {
			ArrayList<Beans.RepositoryBean> ret_list = new ArrayList<Beans.RepositoryBean>();
			Cursor c = null;
			
			EncryptionHelper bean_helper = null;
			
			//Check to see if an encryption helper was passed in
			if(helper == null) {
				bean_helper = EncryptionHelper.getInstance("DEADBEEF".toCharArray(), new byte[] {'L', 'O', 'L' });
			} else
				bean_helper = helper;
			
			try {
				c = this.db.query(DatabaseHelper.DB_TABLE_REPOS, DatabaseHelper.DB_TABLE_REPOS_COLS, null, null, null, null, null);
				
				if(c != null) {
					int num_rows = c.getCount();
					
					c.moveToFirst();
					
					for(int i = 0; i < num_rows; i++) {
						//Create a new bean based on the information in the database
						Beans.RepositoryBean bean = Beans.RepositoryBean.getInstance(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getString(5), c.getString(6), c.getString(7), bean_helper);
						
						//Add it to the list
						ret_list.add(bean);
						
						//Move to the next bean
						c.moveToNext();
					}
				}
			} catch(SQLException e) {
				Log.e(DatabaseHelper.LOG_TAG, "Error while retrieving repositories");
			} finally {
				//Make sure the cursor is closed
				if(c != null && !c.isClosed())
					c.close();
			}
			
			return ret_list;
		}
		
		public ArrayList<Beans.ChangesetBean> getAllChangesets(long repo_id, EncryptionHelper helper) {
			ArrayList<Beans.ChangesetBean> ret_list = new ArrayList<Beans.ChangesetBean>();
			Cursor c = null;
			
			EncryptionHelper bean_helper = null;
			
			//Check to see if an encryption helper was passed in
			if(helper == null) {
				bean_helper = EncryptionHelper.getInstance("DEADBEEF".toCharArray(), new byte[] {'L', 'O', 'L' });
			} else
				bean_helper = helper;
			
			try {
				c = this.db.query(DatabaseHelper.DB_TABLE_CHANGESETS, DatabaseHelper.DB_TABLE_CHANGESETS_COLS, "repo_id = " + repo_id, null, null, null, null);
				
				if(c != null) {
					int num_rows = c.getCount();
					
					c.moveToFirst();
					
					for(int i = 0; i < num_rows; i++) {
						//Create a new bean based on the information in the database
						Beans.ChangesetBean bean = Beans.ChangesetBean.getInstance(c.getLong(0), c.getString(1), c.getLong(2), c.getString(3), c.getString(4), c.getString(5), c.getLong(6), c.getString(7));
						
						//Add it to the list
						ret_list.add(bean);
						
						//Move to the next bean
						c.moveToNext();
					}
				}
			} catch(SQLException e) {
				Log.e(DatabaseHelper.LOG_TAG, "Error while retrieving changesets");
			} finally {
				//Make sure the cursor is closed
				if(c != null && !c.isClosed())
					c.close();
			}
			
			Collections.reverse(ret_list);
			return ret_list;
		}
		
		//Deletes a repository from the table
		public boolean deleteRepository(long id) {
			int check = db.delete(DatabaseHelper.DB_TABLE_REPOS, "_id = " + id, null);
			db.delete(DatabaseHelper.DB_TABLE_REV_IDS, "repo_id = " + id, null);
			
			//Removes changesets from the device if there are any
			deleteAllChangesets(id);
			
			return validateDelete(check, DatabaseHelper.DB_TABLE_REPOS);
		}
		
		//Deletes a single changeset from the table
		public boolean deleteChangeset(long id) {
			int check = db.delete(DatabaseHelper.DB_TABLE_CHANGESETS, "_id = " + id, null);
			return validateDelete(check, DatabaseHelper.DB_TABLE_CHANGESETS);
		}
		
		//Deletes all changesets related to a specific repository
		public boolean deleteAllChangesets(long repo_id) {
			int check = -1;
			
			try {
				check = db.delete(DatabaseHelper.DB_TABLE_CHANGESETS, "repo_id = " + repo_id, null);
			} catch(SQLiteException e) {
				Log.e(DatabaseHelper.LOG_TAG, "Could not delete changesets: " + e.getMessage());
			}
			return validateDelete(check, DatabaseHelper.DB_TABLE_CHANGESETS);
		}
		
		//Returns whether a valid insert was performed or not
		private boolean validateInsert(long id, String table) {
			if(id == -1) { 
				Log.e(DatabaseHelper.LOG_TAG, "Could not insert values into table \"" + table + "\""); 
				return false; 
			} else {
				if(Mercury.DEBUG_MODE)
					Log.i(DatabaseHelper.LOG_TAG, "Inserted values into table \"" + table + "\"");
				return true;
			}
		}
		
		//Validates the deletion of rows from the database
		private boolean validateDelete(int id, String table) {
			if(id <= 0) {
				Log.e(DatabaseHelper.LOG_TAG, "Did not delete anything from \"" + table + "\"");
				return false;
			} else {
				if(Mercury.DEBUG_MODE)
					Log.i(DatabaseHelper.LOG_TAG, "Deleted row(s) from \"" + table + "\"");
				return true;
			}
		}
		
		//Implementation of open helper
		private class DatabaseOpenHelper extends SQLiteOpenHelper {

			public DatabaseOpenHelper(Context context, String name, int version) {
				super(context, name, null, version);
			}
	
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					//Attempt to create both tables needed by the application
					createTable(db, DatabaseHelper.DB_TABLE_REPOS, DatabaseHelper.DB_TABLE_REPOS_ARGS);
					createTable(db, DatabaseHelper.DB_TABLE_CHANGESETS, DatabaseHelper.DB_TABLE_CHANGESETS_ARGS);
					createTable(db, DatabaseHelper.DB_TABLE_REV_IDS, DatabaseHelper.DB_TABLE_REV_IDS_ARGS);
				} catch(SQLException e) {
					Log.e(DatabaseHelper.LOG_TAG, e.getMessage());
				}
			}
	
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				//TODO: Look into gathering regression data and re-applying it to new tables
				
				//Drop all tables from the database
				//db.execSQL("DROP TABLE IF EXISTS " + DatabaseHelper.DB_TABLE_REPOS);
				//db.execSQL("DROP TABLE IF EXISTS " + DatabaseHelper.DB_TABLE_CHANGESETS);
				//db.execSQL("DROP TABLE IF EXISTS " + DatabaseHelper.DB_TABLE_REV_IDS);
				
				//Recreate all the tables again
				this.onCreate(db);
			}
			
			//Creates a table in the desired database using the table name and passed arguments
			private void createTable(SQLiteDatabase db, String table_name, String args) throws SQLException {
				db.execSQL("CREATE TABLE " + table_name + args);
			}
		}
	}