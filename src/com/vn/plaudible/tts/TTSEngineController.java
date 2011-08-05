package com.vn.plaudible.tts;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

import com.vn.plaudible.R;
import com.vn.plaudible.Utils;
import com.vn.plaudible.types.Event;
import com.vn.plaudible.types.EventListener;

public class TTSEngineController {
	/**
	 * TTS stuff. HashMap used for putting in a synthesis completion listener
	 */
	private TextToSpeech ttsEngine;
	private HashMap<String, String> speechHash;
	
	private EventListener completionListener;
	private PlaybackCompletionListener sentenceCompletionListener;
	
	// Data adapter
	private TTSDataAdapter<?> ttsDataAdapter;
	
	// Our queueing policy is to read blobs by flushing old ones
	private static final int TTSEngineQueuePolicy = TextToSpeech.QUEUE_ADD;
	
	private static final String utteranceCompletionId = "Finished reading sentence";
	
	/**
	 * Class listens to playback completion of text synthesis
	 * @author vnarla
	 *
	 */
	class PlaybackCompletionListener implements OnUtteranceCompletedListener {

		@Override
		public void onUtteranceCompleted(String utteranceId) {
			if (utteranceCompletionId.equals(utteranceId)) {
				readNextChunk();
			} else {
				setPlaybackCompletionListner(null);
				completionListener.actionPerformed(new Event("Finished reading article"));
			}
		}
	}
	
	TTSEngineController(TextToSpeech engine, TTSDataAdapter<?> adapter) {
		this.ttsEngine = engine;
		this.ttsDataAdapter = adapter;
		
		// Specify the hashmap for the TTS Engine
		speechHash = new HashMap<String, String>();
		speechHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceCompletionId);
		
		sentenceCompletionListener = new PlaybackCompletionListener();
		setPlaybackCompletionListner(sentenceCompletionListener);
	}

	void speakFromPlaylist() {
		// Re-init this
		speechHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceCompletionId);
		setPlaybackCompletionListner(sentenceCompletionListener);
		ttsDataAdapter.prepareChunks();
		readCurrentChunk();
	}
	
	void readCurrentChunk() {
		String chunk = ttsDataAdapter.getCurrentChunk();
		speak(chunk, speechHash);
	}
	
	void readNextChunk() {
		String chunk = ttsDataAdapter.getNextChunk();
		speak(chunk, speechHash);
	}
	
	void stop() {
		ttsEngine.stop();
		ttsEngine.setOnUtteranceCompletedListener(null);
	}
	
	void destroy() {
		ttsEngine.shutdown();
	}

	void setPlaybackCompletionListner(PlaybackCompletionListener playbackCompletionListner) {
		ttsEngine.setOnUtteranceCompletedListener(playbackCompletionListner);
	}

	void pause() {
		stop();
	}

	void resume() {
		setPlaybackCompletionListner(sentenceCompletionListener);
		readNextChunk();
	}
	
	/**
	 *  Set the TTS preferences
	 */
	void setTTSPreferences(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String language = prefs.getString("languagePref", "UK");
		if (language.equalsIgnoreCase("UK") &&
				(ttsEngine.isLanguageAvailable(Locale.UK) != TextToSpeech.LANG_NOT_SUPPORTED) &&
				(ttsEngine.isLanguageAvailable(Locale.UK) != TextToSpeech.LANG_MISSING_DATA)) {
			ttsEngine.setLanguage(Locale.UK);
		} else {
			ttsEngine.setLanguage(Locale.US);
		}
		
		String speed = prefs.getString("speedPref", "1.0");
		ttsEngine.setSpeechRate(Float.parseFloat(speed));
	}

	/**
	 * Speak a given text
	 * @param text
	 * @param hashMap
	 */
	void speak(String text, HashMap<String, String> hashMap) {
		if (text != null) {
			ttsEngine.speak(text.trim(), TTSEngineQueuePolicy, hashMap);
		} else {
			// If no text to read we have finished reading the article
			hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, Utils.getStringFromResourceId(R.string.article_reading_finished));
			ttsEngine.speak(Utils.getStringFromResourceId(R.string.article_reading_finished), TTSEngineQueuePolicy, hashMap);
		}
	}

	/**
	 * Set the item TTS playback listener
	 * @param listener
	 */
	public void setItemCompletionListner(EventListener listener) {
		completionListener = listener;
	}
}
