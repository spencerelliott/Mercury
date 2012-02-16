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
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import ca.spencerelliott.mercury.Beans.ChangesetBean;

public class GoogleCodeAtomHandler implements AtomHandler {
	private GoogleCodeParser parser = null;
	private ArrayList<Beans.ChangesetBean> changeset_list = null;
	
	private String current_link = "";
	private String current_rev_id = "";
	private String current_title = "";
	private String current_author = "";
	private String current_content = "";
	private long current_update_time = -1;
	
	//These are more of a novelty right now, they could be used for error checking...
	private boolean read_entry = false;
	private boolean get_rev_id_title = false;
	private boolean get_author = false;
	private boolean get_update_time = false;
	private boolean get_content = false;
	
	//Stores the current character buffer for a tag
	private StringBuilder buffer = null;
	
	private String URL_SUFFIX = "/hgchanges/basic";
	private String URL_PREFIX = "/feeds/";
	
	GoogleCodeAtomHandler() {
		
		buffer = new StringBuilder();
		parser = new GoogleCodeParser();
		changeset_list = new ArrayList<Beans.ChangesetBean>();
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String info = new String(ch).substring(start, start+length);
		
		//Just keep appending characters to the current string builder
		buffer.append(info);
		
	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		String name = localName.trim();
		
		if(name.equals("entry")) {
			//Make sure we're reading an entry before ending it
			if(!read_entry)
				throw new SAXException();
			else {
				//Notify that we are not reading an entry now
				read_entry = false;
				
				//Create a new changeset bean based on the read information
				Beans.ChangesetBean new_bean = Beans.ChangesetBean.getInstance((long)0, current_rev_id, (long)0, current_link, current_title, current_author, current_update_time, current_content);
				
				//Add the bean to the list
				changeset_list.add(new_bean);
				clear();
			}
		}
		
		if(read_entry) {
			//Remove the state when the title has been read
			if(name.equals("title")) {
				//Use the parser to process the revision
				current_rev_id = parser.parseRevisionID(buffer.toString().trim());
				
				String[] split_info = buffer.toString().split(":");
				
				//Split the info up and get the commit text
				if(split_info.length > 1) 
					current_title = split_info[1];
				else
					current_title = "";
				
				get_rev_id_title = false;
			//Remove the state when the name has been read
			} else if(name.equals("name")) {
				//Just store the authors name and append if there is more than 1 name
				if(current_author.equals(""))
					current_author = buffer.toString().trim();
				else
					current_author = current_author + ", " + buffer.toString().trim();
				
				get_author = false;
			//Remove the state when the update time has been read
			} else if(name.equals("updated")) {
				//Use the parser to create a Date object with the time and then get the time in milliseconds
				current_update_time = parser.parseUpdateTime(buffer.toString().trim()).getTime();
				
				get_update_time = false;
			} else if(name.equals("content")) {
				//Store the contents of the changeset while removing the <br/> tags
				current_content = buffer.toString().trim().replace("<br/>", "");
				
				get_content = false;
			}
		}
		
		//Create a new buffer
		buffer = new StringBuilder();
		
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		String name = localName.trim();
		
		if(name.equals("entry")) {
			//Make sure we're not already reading another entry before starting a new one
			if(read_entry)
				throw new SAXException();
			else
				read_entry = true;
		}
		
		if(read_entry) {
			//Extract the link from the feed
			if(name.equals("link")) {
				if(atts.getIndex("href") >= 0) {
					current_link = atts.getValue(atts.getIndex("href"));
				}
			//Set the state to read the revision id and title
			} else if(name.equals("title")) {
				get_rev_id_title = true;
			//Set the state to read the committers name
			} else if(name.equals("name")) {
				get_author = true;
			//Set the state to read the update time
			} else if(name.equals("updated")) {
				get_update_time = true;
			} else if(name.equals("content")) {
				get_content = true;
			}
		}
		
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}
	
	public void clear() {
		current_link = "";
		current_rev_id = "";
		current_title = "";
		current_author = "";
		current_content = "";
	}
	
	//Sets the repository id on all of the changesets in this array list
	public void setAllRepositoryID(long id) {
		int bean_count = changeset_list.size();
		
		for(int i = 0; i < bean_count; i++) {
			
		}
	}
	
	//Retrieves all the changesets from this object after reading
	@Override
	public ArrayList<Beans.ChangesetBean> getAllChangesets() {
		return changeset_list;
	}

	@Override
	public void trimStartingFromRevision(String revision) {
		boolean start_trimming = false;
		
		Iterator<Beans.ChangesetBean> iter = changeset_list.iterator();
		
		while(iter.hasNext()) {
			Beans.ChangesetBean current = iter.next();
			
			//Find the revision with the id to trim from
			if(!start_trimming && (current.getRevisionID().equals(revision)))
				start_trimming = true;
			
			//If we're trimming, remove the changeset from the list
			if(start_trimming)
				iter.remove();
		}
	}

	@Override
	public String getSuffix() {
		return URL_SUFFIX;
	}
	
	@Override
	public String getPrefix() {
		return URL_PREFIX;
	}

	@Override
	public String formatURL(String url) {
		String[] formatted_url = url.split("/");
		
		String full_url = formatted_url[0] + "/" + formatted_url[1] + "/" + formatted_url[2] + getPrefix() + formatted_url[3] + "/" + formatted_url[4] + getSuffix();
		
		return full_url;
	}
}
