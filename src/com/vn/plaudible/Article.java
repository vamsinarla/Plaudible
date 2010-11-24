package com.vn.plaudible;

public class Article extends Object {
	
	private String title;
	private String description;
	
	public Article(String title, String description) {
		this.title = title;
		this.description = description;
	}
	
	public Article() {
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
