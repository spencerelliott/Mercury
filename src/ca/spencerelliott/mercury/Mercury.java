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

import android.app.Application;

public class Mercury extends Application {
	public final static String REFRESH_INTENT = "ca.spencerelliott.mercury.REFRESH_CHANGESETS";
	public final static boolean DEBUG_MODE = false;
	
	//Constants for shared preferences
	public final static class Preferences {
		public final static String NOTIFICATIONS_ENABLED = "notifications";
		public final static String NOTIFICATION_INTERVAL = "notification_interval";
		public final static String CACHING_ENABLED = "caching";
		public final static String MAX_CACHE = "max_changesets";
		public final static String ENCRYPTION_ENABLED = "encryption";
	}
	
	//Constants for types of repositories
	public final static class RepositoryTypes {
		public final static int HGSERVE = 0;
		public final static int GOOGLECODE = 1;
		public final static int BITBUCKET = 2;
		public final static int CODEPLEX = 3;
	}
	
	public final static class AuthenticationTypes {
		public final static int NONE = 0;
		public final static int HTTP = 1;
		public final static int TOKEN = 2;
	}
}
