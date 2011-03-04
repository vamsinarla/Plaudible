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
		currentPlaylist.add(position, newArticle);
	}
	
	public void addArticle(Article newArticle) {
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

	public ArrayList<Article> getArticles() {
		return currentPlaylist;
	}

	public Article get(int position) {
		return currentPlaylist.get(position);
	}

	public void remove(int from) {
		currentPlaylist.remove(from);
	}

	public void add(int to, Article temp) {
		currentPlaylist.add(to, temp);
	}

	public boolean contains(Article article) {
		return currentPlaylist.contains(article);
	}
}