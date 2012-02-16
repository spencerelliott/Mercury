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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class CodePlexAtomHandler implements AtomHandler {
	CodePlexParser parser = null;
	private ArrayList<Beans.ChangesetBean> changeset_list = null;
	
	private String current_link = "";
	private String current_rev_id = "";
	private String current_title = "";
	private String current_author = "";
	private String current_content = "";
	private long current_update_time = -1;
	
	private boolean new_entry = false;
	private boolean process_content = false;
	
	String URL_SUFFIX = "/Project/ProjectRss.aspx?ProjectRSSFeed=codeplex://sourcecontrol/";
	String URL_PREFIX = "";
	
	String URL_PROJ_NAME = "";
	
	StringBuilder buffer = null;
	
	public CodePlexAtomHandler() {
		parser = new CodePlexParser();
		buffer = new StringBuilder();
		changeset_list = new ArrayList<Beans.ChangesetBean>();
	}
	
	@Override
	public String formatURL(String url) {
		//Split up the URL
		String[] split_url = url.split("//");
		
		try {
			//Again, try to split up the url to get the project name
			String[] split_uri = split_url[1].split("[.]");
			
			//Attempt to get the project name
			URL_PROJ_NAME = split_uri[0];
		} catch(Exception e) {
			//If there is a failure, make the project name empty
			URL_PROJ_NAME = "";
		}
		
		return url + getSuffix();
	}

	@Override
	public ArrayList<Beans.ChangesetBean> getAllChangesets() {
		return changeset_list;
	}

	@Override
	public String getPrefix() {
		return URL_PREFIX;
	}

	@Override
	public String getSuffix() {
		//Return the suffix plus the project name appended
		return URL_SUFFIX + URL_PROJ_NAME;
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
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		//Append to the buffer
		buffer.append(new String(ch, start, length));
	}

	@Override
	public void endDocument() throws SAXException {

	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		String name = localName.trim();
		
		if(name.equals("item")) {
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
			//Save the title/description of the commit
			if(name.equals("description")) {
				current_title = buffer.toString().trim();
			//Save the link to the commit
			} else if(name.equals("link")) {
				current_link = buffer.toString().trim();
			//Create an epoch date based on the publish date
			} else if(name.equals("pubDate")) {
				current_update_time = parser.parseUpdateTime(buffer.toString().trim()).getTime();
			//Retrieve the revision id from the title tag in the feed
			} else if(name.equals("title")) {
				current_rev_id = parser.parseRevisionID(buffer.toString().trim());
			//Get the author of the commit
			} else if(name.equals("author")) {
				current_author = buffer.toString().trim();
			//Store the message for the commit
			} else if(name.equals("guid")) {
				current_content = buffer.toString().trim();
			}
		}
		
		//Create a new buffer
		buffer = new StringBuilder();
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {

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
		if(localName.equals("item")) {
			if(new_entry)
				throw new SAXException();
			else {
				new_entry = true;
				buffer = new StringBuilder();
			}
		}

	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {

	}
	
	public void clear() {
		current_link = "";
		current_rev_id = "";
		current_title = "";
		current_author = "";
		current_content = "";
	}

}
