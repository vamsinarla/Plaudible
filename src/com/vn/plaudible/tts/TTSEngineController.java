package com.vn.plaudible.tts;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

public class TTSEngineController {
	/**
	 * TTS stuff. HashMap used for putting in a synthesis completion listener
	 */
	private TextToSpeech ttsEngine;
	private HashMap<String, String> speechHash;
	
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
			speakFromPlaylist();
		}
	}
	
	TTSEngineController(TextToSpeech engine, TTSDataAdapter<?> adapter) {
		this.ttsEngine = engine;
		this.ttsDataAdapter = adapter;
		
		// Specify the hashmap for the TTS Engine
		speechHash = new HashMap<String, String>();
		speechHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceCompletionId);
	}
	
	void speakFromPlaylist() {
		ttsDataAdapter.prepareChunks();
		String chunk = ttsDataAdapter.getNextChunk();
		ttsEngine.speak(chunk.trim(), TTSEngineQueuePolicy, speechHash);
	}
	
	void stop() {
		ttsEngine.stop();
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
		speakFromPlaylist();
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

	void speak(String text) {
		ttsEngine.speak(text, TTSEngineQueuePolicy, null);
	}
}
