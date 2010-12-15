package com.vn.plaudible;

public class NewsSource {
	public enum SourceType { NEWSPAPER, BLOG };
	
	private String title;
	private String url;
	private SourceType type;
	
	public NewsSource() {
		this.title = this.url = null;
	}
	
	public NewsSource(String title, String url, SourceType type) {
		this.title = title;
		this.url = url;
		this.type = type;
	}
	
	public static SourceType getType(String type) {
		if (type.equalsIgnoreCase("newspaper")) {
			return SourceType.NEWSPAPER;
		} else if (type.equalsIgnoreCase("blog")) {
			return SourceType.BLOG;
		}
		return SourceType.NEWSPAPER;
	}
	
	public SourceType getType() {
		return type;
	}

	public void setType(SourceType type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
}
