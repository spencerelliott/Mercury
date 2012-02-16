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

public class BitbucketParser extends RepositoryParser {

	@Override
	public String parseRevisionID(String id) {
		//Find the position of the colon (string in the form of "changeset:X")
		int colon_position = id.indexOf(':', 0);
		
		//Extract and return the revision number
		return "Rev. " + id.substring(colon_position+1, id.length());
	}

	@Override
	public Date parseUpdateTime(String time) {
		//Bitbucket formats their date like so: "Fri, 21 May 2010 03:25:42 +0200"
		//This can be parsed by the Date object making it 10x easier to parse		
		return new Date(time);
	}

}
