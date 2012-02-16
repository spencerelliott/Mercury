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
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import android.content.Context;

public class BitbucketAtomHandler implements AtomHandler {
	private BitbucketParser parser = null;
	private ArrayList<Beans.ChangesetBean> changeset_list = null;
	
	private String current_link = "";
	private String current_rev_id = "";
	private String current_title = "";
	private String current_author = "";
	private String current_content = "";
	private long current_update_time = -1;
	
	private boolean new_entry = false;
	private boolean process_content = false;
	
	String URL_SUFFIX = "/atom";
	String URL_PREFIX = "";
	
	StringBuilder buffer = null;
	
	public BitbucketAtomHandler() {
		//Create the objects needed for parsing
		parser = new BitbucketParser();
		changeset_list = new ArrayList<Beans.ChangesetBean>();
		buffer = new StringBuilder();
	}
	
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
	public void characters(char[] ch, int start, int length) throws SAXException {
		buffer.append(ch, start, length);
	}

	@Override
	public void endDocument() throws SAXException {
		Collections.reverse(changeset_list);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		String name = localName.trim();
		
		if(name.equals("entry")) {
			//Make sure we're reading an entry before ending it
			if(!new_entry)
				throw new SAXException();
			else {
				//Notify that we are not reading an entry now
				new_entry = false;
				
				//Create a new changeset bean based on the read information
				Beans.ChangesetBean new_bean = Beans.ChangesetBean.getInstance((long)0, current_rev_id, (long)0, current_link, current_title, current_author, current_update_time, current_content);
				
				//Add the bean to the list
				changeset_list.add(new_bean);
				clear();
			}
		}
		
		if(new_entry) {
			//Save the title of the commit
			if(name.equals("title")) {
				current_title = buffer.toString().trim();
			} else if(name.equals("id")) {
				current_rev_id = parser.parseRevisionID(buffer.toString().trim());
			//Save the authors of the commit
			} else if(name.equals("name")) {
				//Just store the authors name and append if there is more than 1 name
				if(current_author.equals(""))
					current_author = buffer.toString().trim();
				else
					current_author = current_author + ", " + buffer.toString().trim();
			//Get the update time in epoch time
			} else if(name.equals("updated")) {
				//Use the parser to create a Date object with the time and then get the time in milliseconds
				current_update_time = parser.parseUpdateTime(buffer.toString().trim()).getTime();
			} else if(name.equals("content")) {
				//Tell the program we're not parsing content anymore
				process_content = false;
			//Grab the contents from the feed only if we are processing content
			} else if((name.equals("li") || name.equals("a")) && process_content) {
				current_content += buffer.toString().trim().replace("<br/>", "").replace("\t", "") + "\n";
			}
		}
		
		//Create a new buffer
		buffer = new StringBuilder();
	}

	@Override
	public void endPrefixMapping(String arg0) throws SAXException {
		

	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		

	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		

	}

	@Override
	public void setDocumentLocator(Locator locator) {
		

	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		

	}

	@Override
	public void startDocument() throws SAXException {
		

	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		//Check to see if the entry has started so we only process entry values
		if(localName.equals("entry")) {
			if(new_entry)
				throw new SAXException();
			else {
				new_entry = true;
				buffer = new StringBuilder();
			}
		}
		
		if(new_entry) {
			//Extract the link from the feed
			if(localName.equals("link")) {
				if(atts.getIndex("href") >= 0) {
					current_link = atts.getValue(atts.getIndex("href"));
				}
			//Set the state to read the content of the change
			} else if(localName.equals("content"))
				process_content = true;
			//This is a hack so the changeset information won't be read again
			else if(localName.equals("li"))
				buffer = new StringBuilder();
		}

	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		

	}
	
	public void clear() {
		//Clear all of the temporary data
		current_link = "";
		current_rev_id = "";
		current_title = "";
		current_author = "";
		current_content = "";
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
		//Return the formatted url
		return url + getSuffix();
	}

}
