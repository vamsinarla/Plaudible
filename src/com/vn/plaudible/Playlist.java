package com.vn.plaudible;

import java.util.ArrayList;

/**
 * Class which represents a playlist of items to read
 * 
 * @author narla
 *
 */
public class Playlist<Item> {

	private ArrayList<Item> currentPlaylist;
	private Integer currentItemIndex;
	
	public Playlist() {
		currentItemIndex = 0;
		currentPlaylist = new ArrayList<Item>();
	}
	
	public void addItem(Item newItem, Integer position) {
		currentPlaylist.add(position, newItem);
	}
	
	public void addItem(Item newItem) {
		currentPlaylist.add(newItem);
	}
	
	public void removeItem(Item removeItem) {
		currentPlaylist.remove(removeItem);
	}
	
	public void shuffleItem(Integer oldPosition, Integer newPosition) {
		Item swap = currentPlaylist.get(oldPosition);
		currentPlaylist.remove(oldPosition);
		currentPlaylist.add(newPosition, swap);
	}
	
	public void clearPlaylist() {
		currentPlaylist.clear();
	}
	
	public Item getCurrentItem() {
		return currentPlaylist.get(currentItemIndex);
	}
	
	public Integer moveToNext() {
		return (currentItemIndex < getSize() - 1) ?
					++currentItemIndex : currentItemIndex;
	}
	
	public Integer moveToPrevious() {
		return (currentItemIndex > 0) ?
					--currentItemIndex : currentItemIndex;
	}
	
	public Integer jumpTo(Integer newPosition) {
		if (newPosition > 0 && newPosition < getSize()) {
			currentItemIndex = newPosition;
		}
		return currentItemIndex;
	}
	
	public Integer getSize() {
		return currentPlaylist.size();
	}
	
	public boolean isEmpty() {
		return getSize() == 0;
	}

	public ArrayList<Item> getItems() {
		return currentPlaylist;
	}

	public Item get(int position) {
		return currentPlaylist.get(position);
	}

	public void remove(int from) {
		currentPlaylist.remove(from);
	}

	public void add(int to, Item temp) {
		currentPlaylist.add(to, temp);
	}

	public boolean contains(Item item) {
		return currentPlaylist.contains(item);
	}
}