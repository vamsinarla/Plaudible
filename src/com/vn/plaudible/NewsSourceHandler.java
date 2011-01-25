package com.vn.plaudible;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Can construct an ArrayList of NewsSource objects from the standard
 * XML format for NewsSource(s) we follow 
 * @author vamsi
 *
 */
class NewsSourceHandler extends DefaultHandler {
	
	private StringBuilder builder;
	private ArrayList<String> array;
	private ArrayList<NewsSource> sources;
	private NewsSource source;
	
	NewsSourceHandler(ArrayList<NewsSource> sources) {
		this.sources = sources;
	}
	
	/** 
	 * Collecting all characters. Use string builder for efficiency
	 */
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		builder.append(ch, start, length);
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		builder = new StringBuilder();
	}
	
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, name, attributes);
		if (name.equalsIgnoreCase("source")){
			source = new NewsSource();
		} else if (name.equalsIgnoreCase("categories")) {
			array = new ArrayList<String>();
		}  else if (name.equalsIgnoreCase("categoryUrls")) {
			array = new ArrayList<String>();
		}
		builder.setLength(0);
	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);
		
		if (source != null){
			if (name.equalsIgnoreCase("name")) {
				source.setTitle(builder.toString());
			} else if (name.equalsIgnoreCase("type")) {
				source.setType(builder.toString());
			} else if (name.equalsIgnoreCase("preferred")) {
				source.setPreferred(builder.toString().equalsIgnoreCase("true") ? true : false);
			} else if (name.equalsIgnoreCase("hascategories")) {
				source.setHasCategories(builder.toString().equalsIgnoreCase("true") ? true : false);
			} else if (name.equalsIgnoreCase("defaultLink")) {
				source.setDefaultUrl(builder.toString());
			} else if (name.equalsIgnoreCase("title")) {
				array.add(builder.toString());
			} else if (name.equalsIgnoreCase("link")) {
				array.add(builder.toString());
			} else if (name.equalsIgnoreCase("categoryUrls")) {
				source.setCategoryUrls(array);
			} else if (name.equalsIgnoreCase("categories")) {
				source.setCategories(array);
			} else if (name.equalsIgnoreCase("source")) {
				sources.add(source);
			}
			builder.setLength(0);	
		}
	}
}