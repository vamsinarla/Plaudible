package com.vn.plaudible;

import java.util.ArrayList;

/**
 * Class which represents a playlist of articles to read
 * 
 * @author narla
 *
 */
public class ArticlePlaylist {

	private ArrayList<Article> currentPlaylist;
	private Integer currentArticleIndex;
	
	public ArticlePlaylist() {
		currentArticleIndex = 0;
		currentPlaylist = new ArrayList<Article>();
	}
	
	public void addArticle(Article newArticle, Integer position) {
		if (position != null) {
			currentPlaylist.add(position, newArticle);
		}
		currentPlaylist.add(newArticle);
	}
	
	public void removeArticle(Article removeArticle) {
		currentPlaylist.remove(removeArticle);
	}
	
	public void shuffleArticle(Integer oldPosition, Integer newPosition) {
		Article swap = currentPlaylist.get(oldPosition);
		currentPlaylist.remove(oldPosition);
		currentPlaylist.add(newPosition, swap);
	}
	
	public void clearPlaylist() {
		currentPlaylist.clear();
	}
	
	public Article getCurrentArticle() {
		return currentPlaylist.get(currentArticleIndex);
	}
	
	public Integer moveToNext() {
		return (currentArticleIndex < getSize() - 1) ?
					++currentArticleIndex : currentArticleIndex;
	}
	
	public Integer moveToPrevious() {
		return (currentArticleIndex > 0) ?
					--currentArticleIndex : currentArticleIndex;
	}
	
	public Integer jumpTo(Integer newPosition) {
		if (newPosition > 0 && newPosition < getSize()) {
			currentArticleIndex = newPosition;
		}
		return currentArticleIndex;
	}
	
	public Integer getSize() {
		return currentPlaylist.size();
	}
	
	public boolean isEmpty() {
		return getSize() == 0;
	}
}