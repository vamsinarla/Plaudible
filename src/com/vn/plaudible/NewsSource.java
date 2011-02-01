package com.vn.plaudible;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	private String language;
	private String country;
	private boolean hasCategories;
	private Integer currentCategory;
	private boolean subscribed;
	private Integer displayIndex;
	private boolean preferred;
	private String defaultUrl;
	private ArrayList<String> categoryUrls;
	private ArrayList<String> categories;
	
	/**
	 * Static method to construct NewsSources array from JSON
	 * The methods that will NOT be set are
	 * displayIndex, subscribed
	 * @throws JSONException 
	 */
	public static ArrayList<NewsSource> getNewsSourcesFromJSON(String objectString)
	throws JSONException {
		ArrayList<NewsSource> sources = new ArrayList<NewsSource> ();
		
		JSONObject root = new JSONObject(objectString);
		
		if (root.has("sources")) {
			JSONArray sourcesArray = new JSONArray();
			sourcesArray = root.getJSONArray("sources");
			
			for (int index = 0; index < sourcesArray.length(); ++index) {
				sources.add(getNewsSourceFromJSON(sourcesArray.getString(index)));
			}
		} else {
			throw new JSONException("JSON was incorrect");
		}
		
		return sources;
	}
	
	/**
	 * Static method to construct a NewsSource object from JSON
	 * The methods that will NOT be set are
	 * displayIndex, subscribed
	 * @throws JSONException 
	 */
	public static NewsSource getNewsSourceFromJSON(String objectString)
	throws JSONException {
		JSONObject object = new JSONObject(objectString);
		NewsSource source = new NewsSource();
		
		source.setTitle(object.getString("title"));
		source.setType(object.getString("type"));
		source.setHasCategories(object.getBoolean("hasCategories"));
		source.setDefaultUrl(object.getString("defaultUrl"));
		source.setLanguage(object.getString("language"));
		source.setCountry(object.getString("country"));
		
		if (source.isHasCategories()) {
			// Build the array list of categories
			ArrayList<String> javaList = new ArrayList<String>();
			JSONArray jsonList = object.getJSONArray("categories");
			for (int i = 0; i < jsonList.length(); ++i) {
				javaList.add(jsonList.getString(i));
			}
			source.setCategories(javaList);
			
			// Build the array list of category urls
			javaList = new ArrayList<String>();
			jsonList = object.getJSONArray("categoryUrls");
			for (int i = 0; i < jsonList.length(); ++i) {
				javaList.add(jsonList.getString(i));
			}
			source.setCategoryUrls(javaList);
		}
		
		source.setPreferred(object.getBoolean("preferred"));
		
		return source;
	}
	
	/**
	 * Default Ctor
	 */
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
		this.language = Locale.getDefault().getLanguage();
		this.country = Locale.getDefault().getCountry();
	}
	
	/**
	 * Construct it properly
	 * @param title
	 * @param type
	 * @param language
	 * @param country
	 * @param hasCategories
	 * @param categories
	 * @param categoryURLs
	 * @param defaultUrl
	 * @param preferred
	 * @param subscribed
	 * @param displayIndex
	 */
	public NewsSource(String title, String type, String language, String country, boolean hasCategories, ArrayList<String> categories,
			ArrayList<String> categoryURLs, String defaultUrl, boolean preferred, boolean subscribed,
			Integer displayIndex) {
		
		this.title = title;
		this.type = getType(type);
		this.language = language;
		this.country = country;
		this.hasCategories = hasCategories;
		this.categories = categories;
		this.categoryUrls = categoryURLs;
		this.defaultUrl = defaultUrl;
		this.preferred = preferred;
		this.subscribed = subscribed;
		this.displayIndex = displayIndex;
		
		this.currentCategory = 0;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
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
