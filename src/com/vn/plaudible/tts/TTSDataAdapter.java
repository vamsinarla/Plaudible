package com.vn.plaudible.tts;

import android.util.Log;

import com.vn.plaudible.R;
import com.vn.plaudible.Utils;
import com.vn.plaudible.types.Article;
import com.vn.plaudible.types.Playlist;

public class TTSDataAdapter<Item> {
	/**
	 * Playlist of articles
	 */
	private Playlist<Item> playlist;
	private Item currentItem;
	
	private String[] chunks;
	private Integer chunkIndex;
	
	TTSDataAdapter() {
		// Init state
		currentItem = null;
		playlist = null;
		chunkIndex = 0;
		chunks = null;
	}
	
	/**
	 *  Return the index of the currently read item
	 * @return
	 */
	Item getCurrentItem() {
		return currentItem;
	}
	
	/**
	 * Return the playlist
	 */
	Playlist<Item> getPlaylist() {
		return playlist;
	}
	
	/**
	 * Add items to playlist
	 */
	void addItem(Item newItem, Integer position) {
		playlist.addItem(newItem, position);
	}
	
	/**
	 *  Split the article's content into sentences
	 */
	void prepareChunks() {
		String chunk = ((Article) currentItem).getContent();
		if (chunk == null || chunk.equalsIgnoreCase("")) {
			chunk = new String("Sorry, This article could not be read.");
		}
		chunks = chunk.split("\\.");
		chunkIndex = 0;
	}
	
	/**
	 *  Get the next chunk
	 */
	String getNextChunk() {
		if (chunkIndex < chunks.length - 1) {
			return chunks[++chunkIndex];
		} else {
			// Finished reading the article.
			Log.d("::getNextChunk", "Article is finished. No more chunks to read.");
			moveToNextItem();
			return Utils.getStringFromResourceId(R.string.article_reading_finished);
		}
	}

	private void moveToNextItem() {
		if (chunkIndex == chunks.length) {
			// Reset all the necessary state variables
			chunkIndex = 0;
			chunks = null;

			// Move to the next item in the playlist
			playlist.removeItem(currentItem);
			currentItem = null;
			
			if (!playlist.isEmpty()) {
				playlist.moveToNext();
				
				currentItem = playlist.getCurrentItem();
				prepareChunks();
			}
		}
	}

	String getCurrentChunk() {
		return chunks[chunkIndex];
	}
}
