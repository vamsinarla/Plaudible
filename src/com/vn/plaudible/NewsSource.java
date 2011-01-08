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
	public static final Integer NO_CATEGORIES = -1;

	/**
	 * Types
	 * @author vamsi
	 *
	 */
	public enum SourceType { INVALID, NEWSPAPER, BLOG };
	
	/**
	 * Newspaper attributes
	 */
	private String title;
	private SourceType type;
	private boolean hasCategories;
	private Integer currentCategory;
	private boolean displayed;
	private boolean subscribed;
	private Integer displayIndex;
	private boolean preferred;
	private String defaultUrl;
	private ArrayList<String> categoryUrls;
	private ArrayList<String> categories;
	
	public NewsSource() {
	}
	
	public NewsSource(String title, String url, SourceType type, boolean hasCategories) {
		this.title = title;
		this.type = type;
		this.hasCategories = hasCategories;
		this.currentCategory = 0;
	}
	
	public String getCurrentCategoryName() {
		if (isHasCategories()) {
			return categories.get(currentCategory);
		}
		return "";
	}
	
	public Integer getCurrentCategoryIndex() {
		if (isHasCategories()) {
			return currentCategory;
		}
		return NO_CATEGORIES;
	}

	public void setCurrentCategoryIndex(Integer currentCategory) {
		this.currentCategory = currentCategory;
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
	
	public Integer getCurrentCategory() {
		return currentCategory;
	}

	public void setCurrentCategory(Integer currentCategory) {
		this.currentCategory = currentCategory;
	}

	public boolean isDisplayed() {
		return displayed;
	}

	public void setDisplayed(boolean displayed) {
		this.displayed = displayed;
	}

	public Integer getDisplayIndex() {
		return displayIndex;
	}

	public void setDisplayIndex(Integer displayIndex) {
		this.displayIndex = displayIndex;
	}

	public boolean isPreferred() {
		return preferred;
	}

	public void setPreferred(boolean preferred) {
		this.preferred = preferred;
	}

	public String getDefaultUrl() {
		return defaultUrl;
	}

	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}

	public ArrayList<String> getCategoryUrls() {
		return categoryUrls;
	}

	public void setCategoryUrls(ArrayList<String> categoryUrls) {
		this.categoryUrls = categoryUrls;
	}

	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}

	public SourceType getType() {
		return type;
	}

	public boolean isSubscribed() {
		return subscribed;
	}
	
	public void setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
	}
}
