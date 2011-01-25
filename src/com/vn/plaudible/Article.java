package com.vn.plaudible;

import java.io.Serializable;

/**
 * Class for representing Article
 * Implements Serializable to allow passing as intent extras
 * @author vamsi
 *
 */
public class Article implements Serializable {
	
	/**
	 * UID for Serializable
	 */
	private static final long serialVersionUID = 1L;
	
	private String title;
	private String description;
	private String content;
	private String url;
	private boolean isDownloaded;
	private int id;
	
	public Article() {
		this.title = this.description = this.content = null;
		this.isDownloaded = false;
	}
	
	public Article(String title, String description, String url) {
		this.title = title;
		this.description = description;
		this.url = url;		
		this.isDownloaded = false;		
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isDownloaded() {
		return isDownloaded;
	}

	public void setDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
