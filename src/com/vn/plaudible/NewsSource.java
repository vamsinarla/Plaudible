package com.vn.plaudible;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

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
	public static final Integer DISPLAYINDEX_NOTSET = -1;

	/**
	 * Types
	 * @author vamsi
	 *
	 */
	public enum SourceType { INVALID, NEWSPAPER, BLOG };
	
	/**
	 * Static comparator for NewsSource
	 */
	static final Comparator<NewsSource> DISPLAYINDEX_ORDER = new Comparator<NewsSource>() {
		public int compare(NewsSource n1, NewsSource n2) {
			return n1.getDisplayIndex().compareTo(n2.getDisplayIndex());
		}
	};
	
	/**
	 * Newspaper attributes
	 */
	private String title;
	private SourceType type;
	private Locale locale;
	private boolean hasCategories;
	private Integer currentCategory;
	private boolean subscribed;
	private Integer displayIndex;
	private boolean preferred;
	private String defaultUrl;
	private ArrayList<String> categoryUrls;
	private ArrayList<String> categories;
	
	public NewsSource() {
		this.title = null;
		this.type = SourceType.INVALID;
		this.hasCategories = false;
		this.currentCategory = 0;
		this.defaultUrl = null;
		this.categoryUrls = null;
		this.categories = null;
		this.displayIndex = DISPLAYINDEX_NOTSET;
		this.preferred = false;
		this.subscribed = false;
		this.locale = Locale.getDefault();
	}
	
	public NewsSource(String title, String type, Locale locale, boolean hasCategories, ArrayList<String> categories,
			ArrayList<String> categoryURLs, String defaultUrl, boolean preferred, boolean subscribed,
			Integer displayIndex) {
		
		this.title = title;
		this.type = getType(type);
		this.locale = locale;
		this.hasCategories = hasCategories;
		this.categories = categories;
		this.categoryUrls = categoryURLs;
		this.defaultUrl = defaultUrl;
		this.preferred = preferred;
		this.subscribed = subscribed;
		this.displayIndex = displayIndex;
		
		this.currentCategory = 0;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public String getCurrentCategoryName() {
		if (isHasCategories()) {
			return categories.get(currentCategory);
		}
		return null;
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
	 * Set type from String
	 * @param string
	 */
	public void setType(String string) {
		if (string.equalsIgnoreCase("newspaper")) {
			this.setType(SourceType.NEWSPAPER);
		} else if (string.equalsIgnoreCase("blog")) {
			this.setType(SourceType.BLOG);
		} else {
			this.setType(SourceType.INVALID);
		}
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
