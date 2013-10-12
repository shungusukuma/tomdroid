/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * 
 * This file is part of Tomdroid.
 * 
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid;

import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.TimeFormatException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tomdroid.xml.NoteContentBuilder;
import org.tomdroid.xml.XmlUtils;

import java.io.Serializable;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Note implements Serializable {

	// Static references to fields (used in Bundles, ContentResolvers, etc.)
	public static final String ID = "_id";
	public static final String GUID = "guid";
	public static final String TITLE = "title";
	public static final String MODIFIED_DATE = "modified_date";
	public static final String MODIFIED_METADATA_DATE = "modified_metadata_date";
	public static final String CREATED_DATE = "created_date";
	public static final String URL = "url";
	public static final String FILE = "file";
	public static final String TAGS = "tags";
	public static final String NOTE_CONTENT = "content";
	public static final String NOTE_CONTENT_PLAIN = "content_plain";
	
	public static final String CURSOR_POSITION = "cursor_position";
	public static final String SELECTION_BOUND_POSITION = "selection_bound_position";
	public static final String WINDOW_WIDTH = "window_width";
	public static final String WINDOW_HEIGHT = "window_height";
	public static final String WINDOW_X = "window_x";
	public static final String WINDOW_Y = "window_y";
	public static final String OPEN_ON_STARTUP = "open_on_startup";
	public static final String PINNED = "pinned";
	
	// Notes constants
	public static final int NOTE_HIGHLIGHT_COLOR = 0x99FFFF00; // lowered alpha to show cursor
	public static final String NOTE_MONOSPACE_TYPEFACE = "monospace";
	public static final float NOTE_SIZE_SMALL_FACTOR = 0.8f;
	public static final float NOTE_SIZE_LARGE_FACTOR = 1.5f;
	public static final float NOTE_SIZE_HUGE_FACTOR = 1.8f;
	
	// Members
	private SpannableStringBuilder noteContent;
	private String xmlContent;
	private String url;
	private String fileName;
	private String title;
	private String tags = "";
	private String lastChangeDate;
	private String createDate;
	private String lastMetadataChangeDate;
	private int dbId;

	// Unused members (for SD Card)
	
	public int cursorPos = 0;
	public int selectionBoundPos = 0;
	public int height = 0;
	public int width = 0;
	public int X = -1;
	public int Y = -1;
	public Boolean openOnStartup = false;
	public Boolean pinned = false;
	
	// TODO before guid were of the UUID object type, now they are simple strings 
	// but at some point we probably need to validate their uniqueness (per note collection or universe-wide?) 
	private String guid;
	
	// this is to tell the sync service to update the last date after pushing this note
	public boolean lastSync = false;
	
	// Date converter pattern (remove extra sub milliseconds from datetime string)
	// ex: will strip 3020 in 2010-01-23T12:07:38.7743020-05:00
	private static final Pattern dateCleaner = Pattern.compile(
			"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})" +	// matches: 2010-01-23T12:07:38.774
			".+" + 														// matches what we are getting rid of
			"([-\\+]\\d{2}:\\d{2})");									// matches timezone (-xx:xx or +xx:xx)

	
	// Date converter to Tomboy Time-Format (add extra milliseconds to datetime string and add colon to time format)
	// ex: generated tomddroid_time: 2000-02-01T01:00:00.0000000+01:00
	public String toTomboyFormat(Time time) throws TimeFormatException {
		String timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ";
		SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.US);
		String tomdroid_time = sdf.format(new Date(time.toMillis(false)));
		tomdroid_time = tomdroid_time.substring(0,tomdroid_time.length()-2) + ":" + tomdroid_time.substring(tomdroid_time.length()-2);
		return tomdroid_time;
	}
	
	public Note() {
		tags = new String();
	}
	
	public void fromJSON (JSONObject json) {
		
		// These methods return an empty string if the key is not found
		setTitle(XmlUtils.unescape(json.optString("title")));
		setGuid(json.optString("guid"));
		setLastChangeDate(json.optString("last-change-date"));
		setLastMetadataChangeDate(json.optString("last-metadata-change-date"));
		setCreateDate(json.optString("create-date"));
		String newXMLContent = json.optString("note-content");
		setXmlContent(newXMLContent);
		JSONArray jtags = json.optJSONArray("tags");
		String tag;
		tags = new String();
		if (jtags != null) {
			for (int i = 0; i < jtags.length(); i++ ) {
				tag = jtags.optString(i);
				tags += tag + ",";
			}
		}
		
		this.width = json.optInt("width");
		this.height = json.optInt("height");
		this.X = json.optInt("x");
		this.Y = json.optInt("y");
		this.cursorPos = json.optInt("cursor-position");
		this.openOnStartup = json.optBoolean("open-on-startup");
		this.pinned = json.optBoolean("pinned");
		this.selectionBoundPos = json.optInt("selection-bound-position");
	}

	public String getTags() {
		return tags;
	}
	
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	public void addTag(String tag) {
		if(tags.length() > 0)
			this.tags = this.tags+","+tag;
		else
			this.tags = tag;
	}
	
	public void removeTag(String tag) {
		
		String[] taga = TextUtils.split(this.tags, ",");
		String newTags = "";
		for(String atag : taga){
			if(!atag.equals(tag))
				newTags += atag;
		}
		this.tags = newTags;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Time getLastChangeDate() {
		Time time = new Time();
		if (lastChangeDate == null) {
			lastChangeDate = new Time().format3339(false);
		}
		time.parse3339(lastChangeDate);
		return time;
	}
	
	public Time getLastMetadataChangeDate() {
		Time time = new Time();
		if (lastMetadataChangeDate == null) {
			lastMetadataChangeDate = new Time().format3339(false);
		}
		time.parse3339(lastMetadataChangeDate);
		return time;
	}
	
	public Time getCreateDate() {
		Time time = new Time();
		if (createDate == null) {
			createDate = new Time().format3339(false);
		}
		time.parse3339(createDate);
		return time;
	}
	
	// sets change date to now
	public void setLastChangeDate() {
		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		setLastChangeDate(time);
	}
	
	public void setLastChangeDate(Time lastChangeDateTime) {
		this.lastChangeDate = lastChangeDateTime.format3339(false);
	}
	
	public void setLastChangeDate(String lastChangeDateStr) throws TimeFormatException {
		
		// regexp out the sub-milliseconds from tomboy's datetime format
		// Normal RFC 3339 format: 			2008-10-13T16:00:00.000-07:00
		// Tomboy's (C# library) format: 	2010-01-23T12:07:38.7743020-05:00
		Matcher m = dateCleaner.matcher(lastChangeDateStr);
		if (m.find()) {
			//TLog.d(TAG, "I had to clean out extra sub-milliseconds from the date");
			lastChangeDateStr = m.group(1)+m.group(2);
			//TLog.v(TAG, "new date: {0}", lastChangeDateStr);
		}
		
		this.lastChangeDate = lastChangeDateStr;
	}
	
	// sets metadata change date to now
	public void setLastMetadataChangeDate() {
		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		setLastMetadataChangeDate(time);
	}
	
	public void setLastMetadataChangeDate(Time lastMetadataChangeDateTime) {
		this.lastMetadataChangeDate = lastMetadataChangeDateTime.format3339(false);
	}
	
	public void setLastMetadataChangeDate(String lastMetadataChangeDateStr) throws TimeFormatException {
		
		// regexp out the sub-milliseconds from tomboy's datetime format
		// Normal RFC 3339 format: 			2008-10-13T16:00:00.000-07:00
		// Tomboy's (C# library) format: 	2010-01-23T12:07:38.7743020-05:00
		Matcher m = dateCleaner.matcher(lastMetadataChangeDateStr);
		if (m.find()) {
			//TLog.d(TAG, "I had to clean out extra sub-milliseconds from the date");
			lastMetadataChangeDateStr = m.group(1)+m.group(2);
			//TLog.v(TAG, "new date: {0}", lastChangeDateStr);
		}
		
		this.lastMetadataChangeDate = lastMetadataChangeDateStr;
	}
	
	// sets create date to now
	public void setCreateDate() {
		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		setCreateDate(time);
	}
	
	public void setCreateDate(Time createDate) {
		this.createDate = createDate.format3339(false);
	}

	public void setCreateDate(String createDateStr) throws TimeFormatException {
		
		// regexp out the sub-milliseconds from tomboy's datetime format
		// Normal RFC 3339 format: 			2008-10-13T16:00:00.000-07:00
		// Tomboy's (C# library) format: 	2010-01-23T12:07:38.7743020-05:00
		Matcher m = dateCleaner.matcher(createDateStr);
		if (m.find()) {
			//TLog.d(TAG, "I had to clean out extra sub-milliseconds from the date");
			createDateStr = m.group(1)+m.group(2);
			//TLog.v(TAG, "new date: {0}", lastChangeDateStr);
		}
		
		this.createDate = createDateStr;
	}
	
	public int getDbId() {
		return dbId;
	}

	public void setDbId(int id) {
		this.dbId = id;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public void setGuid(String guid) {
		this.guid = guid;
	}

	// TODO: should this handler passed around evolve into an observer pattern?
	public SpannableStringBuilder getNoteContent(Handler handler) {
		
		// TODO not sure this is the right place to do this
		noteContent = new NoteContentBuilder().setCaller(handler).setInputSource(xmlContent).setTitle(this.getTitle()).build();
		return noteContent;
	}
	
	public String getXmlContent() {
		return xmlContent;
	}
	
	public void setXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
	}

	@Override
	public String toString() {

		return new String("Note: "+ getTitle() + " (" + getLastChangeDate() + ")");
	}
	
	// gets full xml to be exported as .note file
	public String getXmlFileString() {
		
		String tagString = "";

		if(tags.length()>0) {
			String[] tagsA = tags.split(",");
			tagString = "\n\t<tags>";
			for(String atag : tagsA) {
				tagString += "\n\t\t<tag>"+atag+"</tag>"; 
			}
			tagString += "\n\t</tags>"; 
		}

		String fileString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<note version=\"0.3\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">\n\t<title>"
				+getTitle().replace("&", "&amp;")+"</title>\n\t<text xml:space=\"preserve\"><note-content version=\"0.1\">"
				+getTitle().replace("&", "&amp;")+"\n\n" // added for compatibility
				+getXmlContent()+"</note-content></text>\n\t<last-change-date>"
				+toTomboyFormat(getLastChangeDate())+"</last-change-date>\n\t<last-metadata-change-date>"
				+toTomboyFormat(getLastMetadataChangeDate())+"</last-metadata-change-date>\n\t<create-date>"
				+toTomboyFormat(getCreateDate())+"</create-date>\n\t<cursor-position>"
				+cursorPos+"</cursor-position>\n\t<selection-bound-position>"
				+selectionBoundPos+"</selection-bound-position>\n\t<width>"
				+width+"</width>\n\t<height>"
				+height+"</height>\n\t<x>"
				+X+"</x>\n\t<y>"
				+Y+"</y>"
				+tagString+"\n\t<pinned>"
				+pinned+"</pinned>\n\t<open-on-startup>"
				+openOnStartup.toString()+"</open-on-startup>\n</note>\n";
		return fileString;
	}

}
