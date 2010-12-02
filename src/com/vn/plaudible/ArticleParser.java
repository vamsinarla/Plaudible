package com.vn.plaudible;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ArticleParser extends DefaultHandler {
	
	private String content;
	private StringBuilder builder;
	private String contentTag;
	private boolean startContent;
	
	ArticleParser(String content) {
		this.content = content;
		this.contentTag = "div";
		this.startContent = false;
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		if (startContent) {
			builder.append(ch, start, length);
		}
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		// builder = new StringBuilder();
	}
	
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, name, attributes);
		/*if (localName.equalsIgnoreCase("p")) {
			startContent = true;
			builder.setLength(0);
		}*/
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);
		/*if (localName.equalsIgnoreCase("p")) {
			content = content + builder.toString().trim();
			startContent = false;
		}*/
	}
	
	@Override
	public void endDocument()
			throws SAXException {
		super.endDocument();
	}
	
}
