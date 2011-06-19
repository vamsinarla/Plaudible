package com.vn.plaudible.types;

import java.io.Serializable;

import com.vn.plaudible.R;
import com.vn.plaudible.Utils;
import com.vn.plaudible.R.string;

/**
 * Class for representing Article
 * Implements Serializable to allow passing as intent extras
 * @author vamsi
 *
 */
public class Article extends Item implements Serializable {
	
	/**
	 * UID for Serializable
	 */
	private static final long serialVersionUID = 1L;

	private static final String EMPTY_CONTENT = "";
	
	private String title;
	private String description;
	private String content;
	private String url;
	private boolean isDownloaded;
	private int id;
	private NewsSource source;
	
	public Article() {
		this.title = this.description = this.content = null;
		this.source = null;
		this.isDownloaded = false;
	}
	
	public Article(String title, String description, String url, NewsSource newsSource) {
		this.title = title;
		this.description = description;
		this.url = url;
		this.source = newsSource;
		this.isDownloaded = false;		
	}
	
	public Article(String title, String description, String url) {
		this.title = title;
		this.description = description;
		this.url = url;
	}

	public String getContent() {
		return (isDownloaded() ? content : Article.EMPTY_CONTENT);
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

	public void setSource(NewsSource source) {
		this.source = source;
	}

	public NewsSource getSource() {
		return source;
	}

	public String getContentForSpeech() {
		return Utils.getStringFromResourceId(R.string.article_start_reading) + this.title;
	}

}
