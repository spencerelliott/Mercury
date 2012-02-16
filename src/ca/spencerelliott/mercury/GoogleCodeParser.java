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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GoogleCodeParser extends RepositoryParser {

	protected static String URL_TO_FEED = "/atom-log";
	
	@Override
	public String parseRevisionID(String id) {
		//Find the first instance of a : and the first space in the string (id in form of "Revision XXXXXXX: Commit text")
		int colon_position = id.indexOf(':', 0), space_position = id.indexOf(' ', 0);
		
		//Extract the revision number
		String edited_id = id.substring(space_position+1, colon_position);
		
		//Return the edited id
		return "Rev. " + edited_id;
	}

	@Override
	public Date parseUpdateTime(String time) {
		//Gets a new instance of a calendar object used to store the date
		//TODO: Detect the locale from the device
		Calendar new_date = Calendar.getInstance(TimeZone.getDefault(), Locale.CANADA);
		
		//Date formatted as: YYYY-MM-DDTHH:MM:SSZ --> Y - Year, M - Month, D - Day, H - Hour, M - Minutes, S - Seconds
		
		//Splits the date into separate strings
		String[] split_date = time.split("-");
		
		//Grab the year and month from the separated strings
		new_date.set(Calendar.YEAR, Integer.parseInt(split_date[0].trim().toString()));
		new_date.set(Calendar.MONTH, Integer.parseInt(split_date[1].trim().toString()));
		
		//Remove the Z at the end of the date
		split_date[2] = split_date[2].substring(0, split_date[2].length()-1);
		
		//Split the strings further
		split_date = split_date[2].split("T");
		
		//Set the day
		new_date.set(Calendar.DATE, Integer.parseInt(split_date[0].trim().toString()));
		
		//Split the time up
		split_date = split_date[1].split(":");
		
		//Get the hour, minute and second from the remaining strings
		new_date.set(Calendar.HOUR_OF_DAY, Integer.parseInt(split_date[0].trim().toString()));
		new_date.set(Calendar.MINUTE, Integer.parseInt(split_date[1].trim().toString()));
		new_date.set(Calendar.SECOND, Integer.parseInt(split_date[2].trim().toString()));
		
		//Return the date object
		return new_date.getTime();
	}

}
