package com.vn.plaudible;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler - SAX implementation for handling the XML returned by the FeedServlet on AppEngine
 * @author vamsi
 *
 */
public class FeedHandler extends DefaultHandler {
	
	private ArrayList<Article> articles;
	private Article currentArticle;
	private StringBuilder builder;
	
	private String titleTag;
	private String descriptionTag;
	private String itemTag;
	private String linkTag;
	
	/**
	 * Ctor. Supply the articles arraylist which initially has no elements
	 * @param articles
	 */
	FeedHandler(ArrayList<Article> articles) {
		itemTag = "item";
		titleTag = "title";
		descriptionTag = "description";
		linkTag = "link";
		
		this.articles = articles;
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
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);
		
		if (this.currentArticle != null){
			if (localName.equalsIgnoreCase(titleTag)) {
				currentArticle.setTitle(builder.toString().trim());
			} else if (localName.equalsIgnoreCase(descriptionTag)) {
				currentArticle.setDescription(builder.toString().trim());
			} else if (localName.equalsIgnoreCase(linkTag)) {
				currentArticle.setUrl(builder.toString().trim());
			} else if (localName.equalsIgnoreCase(itemTag)) {
				
				// Set the id of the article which is basically its index
				currentArticle.setId(articles.size());
				
				// Add to the array list
				articles.add(currentArticle);
			}
			builder.setLength(0);	
		}
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
		if (localName.equalsIgnoreCase(itemTag)){
			this.currentArticle = new Article();
			builder.setLength(0);
		}
	}
}