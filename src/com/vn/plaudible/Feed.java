package com.vn.plaudible;

import java.io.Serializable;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class represents a feed that is returned from the 
 * backend. It can be built from a JSON response from the 
 * server and holds a list of articles and feed information
 * 
 * @author vnarla
 *
 */
public class Feed implements Serializable {

	/**
	 * Default id
	 */
	private static final long serialVersionUID = 1L;
	
	private ArrayList<Article> items;
	private String title;
	private String language;
	private String description;
	private String url;
	
	Feed() {
		items = new ArrayList<Article>();
	}
	
	public static Feed getFeedFromJSON(String objectString) throws JSONException {
		Feed feed = new Feed();
		JSONObject feedJSON = new JSONObject(objectString);
		
		// Feed params
		feed.title = feedJSON.getString("title");
		feed.language = feedJSON.getString("language");
		feed.description = feedJSON.getString("description");
		
		JSONArray itemsJSON = feedJSON.getJSONArray("entries");
		JSONObject itemJSON;
		Article item;
		
		for (int index = 0; index < itemsJSON.length(); index++) {
			itemJSON = itemsJSON.getJSONObject(index);
			item = new Article(itemJSON.getString("title"),
								itemJSON.getString("description"),
								itemJSON.getString("link"));
			feed.addItem(item);
		}
		return feed;
	}
	
	void clear() {
		items.clear();
	}

	public void addItem(Article item) {
		items.add(item);
	}
	
	public Article getItem(Integer index) {
		return items.get(index);
	}
	
	public Integer getCount() {
		return items.size();
	}

	public ArrayList<Article> getItems() {
		return items;
	}

	public String getTitle() {
		return title;
	}

	public String getLanguage() {
		return language;
	}

	public String getDescription() {
		return description;
	}

	int size() {
		return items.size();
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
