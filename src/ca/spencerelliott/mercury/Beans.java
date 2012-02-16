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

import android.os.Parcel;
import android.os.Parcelable;

public class Beans {
	public static class RepositoryBean implements Parcelable {
		public static final String LOG_TAG = "Beans.RepositoryBean (Mercury)";
		
		private long id;			//The id of the bean in the database
		private String repo_title;	//User's title for the repository
		private String url;			//The URL to the repository online
		private int type;			//The specified type of repository (found in Mercury.RepositoryTypes)
		private int authentication;	//Authentication method (found in Mercury.AuthenticationTypes)
		private String username;	//Specified username to access the database (optional)
		private String password;	//Specified password for the username (optional)
		private String ssh_key;		//SSH key/Token key for the repository (optional)
		
		private EncryptionHelper encrypt_helper = null;
		
		private RepositoryBean(long id, String title, String url, int type, int authentication, String username, String password, EncryptionHelper helper) {
			this.id = id;
			this.repo_title = title;
			this.url = url;
			this.type = type;
			this.authentication = authentication;
			this.username = username;
			this.password = password;
			this.encrypt_helper = helper;
		}
		
		private RepositoryBean(long id, String title, String url, int type, int authentication, String ssh_key, EncryptionHelper helper) {
			this.id = id;
			this.repo_title = title;
			this.url = url;
			this.type = type;
			this.authentication = authentication;
			this.ssh_key = ssh_key;
			this.encrypt_helper = helper;
		}
		
		private RepositoryBean(long id, String title, String url, int type, int authentication, String username, String password, String ssh_key, EncryptionHelper helper) {
			this.id = id;
			this.repo_title = title;
			this.url = url;
			this.type = type;
			this.authentication = authentication;
			this.username = username;
			this.password = password;
			this.ssh_key = ssh_key;
			this.encrypt_helper = helper;
		}
		
		private RepositoryBean(Parcel in) {
			this.id = in.readLong();
			this.repo_title = in.readString();
			this.url = in.readString();
			this.type = in.readInt();
			this.authentication = in.readInt();
			this.username = in.readString();
			this.password = in.readString();
			this.ssh_key = in.readString();
			this.encrypt_helper = EncryptionHelper.getInstance("DEADBEEF".toCharArray(), new byte[]{'L', 'O', 'L'});
		}
		
		//These next three functions are used to create new instances of the repository bean
		public static RepositoryBean getInstance(long id, String title, String url, int type, int authentication, String ssh_key, EncryptionHelper helper) {
			return new RepositoryBean(id, title, url, type, authentication, ssh_key, helper);
		}
		
		public static RepositoryBean getInstance(long id, String title, String url, int type, int authentication, String username, String password, EncryptionHelper helper) {
			return new RepositoryBean(id, title, url, type, authentication, username, password, helper);
		}
		
		public static RepositoryBean getInstance(long id, String title, String url, int type, int authentication, String username, String password, String ssh_key, EncryptionHelper helper) {
			return new RepositoryBean(id, title, url, type, authentication, username, password, ssh_key, helper);
		}
		
		//Sets the encryption helper for this object
		public void setEncryptionHelper(EncryptionHelper helper) {
			encrypt_helper = helper;
		}
		
		//Returns the ID of the data in the database
		public long getId() {
			return id;
		}
		
		//Gets the repository title
		public String getTitle() {
			return this.repo_title;
		}
		
		//Gets the authentication type (http, https, ssh)
		public int getAuthentication() {
			return authentication;
		}
		
		//Returns the decrypted username
		public String getUsername() {
			//Make sure the username is not empty before decrypting
			if(username == null) return null;
			
			//Decrypt the username
			String decrypted_username = encrypt_helper.decrypt(username.getBytes());
			
			//Make sure something was returned
			if(decrypted_username == null) return null;
			
			//Return the username
			return decrypted_username;
		}
		
		//Returns the still encrypted username
		public String getEncryptedUsername() {
			return username;
		}
		
		//Returns the decrypted password
		public String getPassword() {
			//Make sure there is a password to decrypt
			if(password == null) return null;
			
			//Decrypt the password
			String decrypted_password = encrypt_helper.decrypt(password.getBytes());
			
			//Make sure something was returned
			if(decrypted_password == null) return null;
			
			//Return the decrypted password
			return decrypted_password;
		}
		
		//Returns the still encrypted password
		public String getEncryptedPassword() {
			return password;
		}
		
		//Returns the decrypted ssh key
		public String getSSHKey() {
			//Make sure there is a key to decrypt
			if(ssh_key == null) return null;
			
			//Decrypt the ssh key
			String decrypted_key = encrypt_helper.decrypt(ssh_key.getBytes());
			
			//Make sure something was returned
			if(decrypted_key == null) return null;
			
			//Return the decrypted ssh key
			return decrypted_key;
		}
		
		//Returns the still encrypted key
		public String getEncryptedSSHKey() {
			return ssh_key;
		}
		
		//Returns the URL to the repository
		public String getUrl() {
			return url;
		}
		
		//Returns the type of repository this bean is
		public int getType() {
			return type;
		}

		//Had to be implemented but is not used
		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			//Write all of the data the to parcel
			parcel.writeLong(this.id);
			parcel.writeString(this.repo_title);
			parcel.writeString(this.url);
			parcel.writeInt(this.type);
			parcel.writeInt(this.authentication);
			parcel.writeString(this.username);
			parcel.writeString(this.password);
			parcel.writeString(this.ssh_key);
		}
		
		//Create the creator to expand data from a parcel
		public static final Parcelable.Creator<RepositoryBean> CREATOR =
			new Parcelable.Creator<RepositoryBean>() {
				//Run the constructor for the parcel'd data
				public RepositoryBean createFromParcel(Parcel in) {
					return new RepositoryBean(in);
				}
				
				//Creates a new array
				public RepositoryBean[] newArray(int size) {
					return new RepositoryBean[size];
				}
			};
	}
	
	public static class ChangesetBean implements Comparable<ChangesetBean>,Parcelable {
		private long id;		//The id of the changeset in the database
		private String rev_id;	//The revision id in the repository
		private long repo_id;	//The repository id found in the database
		private String link;	//URL to the changeset on the web
		private String title;	//Commit text of the changeset
		private String author;	//User that pushed and committed the changeset
		private long updated;	//The time the changeset was created
		private String content;	//The content of what changed in this changeset
		
		private ChangesetBean(long id, String rev, long repo_id, String link, String title, String author, long updated, String content) { 
			this.id = id;
			this.rev_id = rev;
			this.repo_id = repo_id;
			this.link = link;
			this.title = title;
			this.author = author;
			this.updated = updated;
			this.content = content;
		}
		
		private ChangesetBean(Parcel in) {
			this.id = in.readLong();
			this.rev_id = in.readString();
			this.repo_id = in.readLong();
			this.link = in.readString();
			this.title = in.readString();
			this.author = in.readString();
			this.updated = in.readLong();
			this.content = in.readString();
		}
		
		public static ChangesetBean getInstance(long id, String rev, long repo_id, String link, String title, String author, long updated, String content) {
			return new ChangesetBean(id, rev, repo_id, link, title, author, updated, content);
		}
		
		//Return the id of the changeset in the local database
		public long getID() {
			return id;
		}
		
		//Returns the revision
		public String getRevisionID() {
			return rev_id;
		}
		
		//Returns the repository's local id
		public long getRepoID() {
			return repo_id;
		}
		
		//Returns the link to the changeset
		public String getLink() {
			return link;
		}
		
		//Returns the commit text
		public String getTitle() {
			return title;
		}
		
		//Returns the author
		public String getAuthor() {
			return author;
		}
		
		//Returns the update time 
		public long getUpdated() {
			return updated;
		}
		
		//Returns the update time of the changeset
		public String getContent() {
			return content;
		}

		//Compares this bean to another changeset bean
		@Override
		public int compareTo(ChangesetBean another) {
			return (rev_id.compareTo(another.getRevisionID()));
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			//Save all of the data to the parcel
			dest.writeLong(this.id);
			dest.writeString(this.rev_id);
			dest.writeLong(this.repo_id);
			dest.writeString(this.link);
			dest.writeString(this.title);
			dest.writeString(this.author);
			dest.writeLong(this.updated);
			dest.writeString(this.content);
		}
		
		public static final Parcelable.Creator<ChangesetBean> CREATOR = 
			new Parcelable.Creator<ChangesetBean>() {
				public ChangesetBean createFromParcel(Parcel in) {
					return new ChangesetBean(in);
				}
				
				public ChangesetBean[] newArray(int size) {
					return new ChangesetBean[size];
				}
			};
	}
}
