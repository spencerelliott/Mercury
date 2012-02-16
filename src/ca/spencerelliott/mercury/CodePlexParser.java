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

public class CodePlexParser extends RepositoryParser {

	@Override
	public String parseRevisionID(String id) {
		//Split the revision id from the comma
		String[] split_title = id.split(",");
		
		try {
			//Get the revision id
			String rev_id = split_title[1].trim();
			
			//Remove the # from the start of the id
			rev_id = rev_id.replace('#', ' ').trim();
			
			//Return it
			return "Rev. " + rev_id;
		} catch(Exception e) {
			//If an error occurs, return a blank revision
			return "";
		}
	}

	@Override
	public Date parseUpdateTime(String time) {
		return new Date(time);
	}

}
