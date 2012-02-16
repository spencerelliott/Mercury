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

import java.util.Date;

abstract public class RepositoryParser {
	
	public RepositoryParser() { }
	
	protected static String URL_TO_FEED = "/atom-log";
	
	//Parses the HTML content inside a repository entry in the atom feed
	public String parseContents(String contents) {
		return new String("HTML content");
	}
	
	//Should parse the revision id and return a formatted id
	abstract public String parseRevisionID(String id);
	
	//Should parse the 'updated' tag in the feed and return a formatted date
	abstract public Date parseUpdateTime(String time);
}
