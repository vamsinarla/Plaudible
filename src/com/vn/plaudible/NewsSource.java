package com.vn.plaudible;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class for representing a news source.
 * Implements serializable so it can be passed as Intent extras
 * @author vamsi
 *
 */
public class NewsSource implements Serializable {
	
	/**
	 * UID for Serializable
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Types
	 * @author vamsi
	 *
	 */
	public enum SourceType { NEWSPAPER, BLOG };
	
	/**
	 * Pre defined standard sources shared across all sources with categories
	 */
	static ArrayList<String> categories = new ArrayList<String>();
		
	static {
		categories.add("US");
		categories.add("World"); 
		categories.add("Business");
		categories.add("Politics"); 
		categories.add("Sports"); 
		categories.add("Technology"); 
		categories.add("Health"); 
		categories.add("Opinion");
	}
	
	/**
	 * Newspaper attributes
	 */
	private String title;
	private SourceType type;
	private boolean hasCategories;
	
	public NewsSource() {
		title = null;
	}
	
	public NewsSource(String title, String url, SourceType type, boolean hasCategories) {
		this.title = title;
		this.type = type;
		this.hasCategories = hasCategories;
	}
	
	/**
	 * Get Type enum
	 * @param type
	 * @return
	 */
	public static SourceType getType(String type) {
		if (type.equalsIgnoreCase("newspaper")) {
			return SourceType.NEWSPAPER;
		} else if (type.equalsIgnoreCase("blog")) {
			return SourceType.BLOG;
		}
		return SourceType.NEWSPAPER;
	}
	
	/**
	 * Get Type
	 * @return
	 */
	public SourceType getType() {
		return type;
	}

	/**
	 * Set type
	 * @param type
	 */
	public void setType(SourceType type) {
		this.type = type;
	}

	/**
	 * Get title
	 * @return
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Set title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	

	public boolean isHasCategories() {
		return hasCategories;
	}

	public void setHasCategories(boolean hasCategories) {
		this.hasCategories = hasCategories;
	}

	/**
	 * Get categories 
	 */
	public ArrayList<String> getCategories() {
		if (isHasCategories()) {
			return categories;
		}
		return null;
	}
	
}
