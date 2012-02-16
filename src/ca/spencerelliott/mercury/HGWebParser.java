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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HGWebParser extends RepositoryParser {
	// date format for parsing HWeb times including timezone offset
	private static final SimpleDateFormat HGWEB_DATE =
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	@Override
	public String parseRevisionID(String id) {
		String[] split_rev = id.split("/");
		int i = 0;
		
		//Look for the string "rev" since the revision will be right after that
		while(!split_rev[i].equals("rev")) {
			i++;		
		}
		
		//Return the formatted revision
		return split_rev[i+1];
	}

	@Override
	public Date parseUpdateTime(String time) {
		// TODO: find better default time in case parsing fails
		//       (perhaps return null and let caller decide what to do)
		Date updateTime = new Date();
		
		try {
			
			// parses the correct local time by using the date format
			// which includes parsing time zone
			updateTime = HGWEB_DATE.parse(time);
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
		
		return updateTime;
	}

}
