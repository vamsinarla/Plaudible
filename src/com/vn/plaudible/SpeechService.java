package com.vn.plaudible;

import java.util.HashMap;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * The service that speaks
 * @author vamsi
 *
 */
public class SpeechService extends Service implements OnUtteranceCompletedListener {

	/**
	 * System services for notifications and incoming call statuses
	 */
	private NotificationManager notificationManager;
	private TelephonyManager telephonyManager;
	
	/**
	 * TTS stuff. HashMap used for putting in a synthesis completion listener
	 */
	private TextToSpeech ttsEngine;
	private HashMap<String, String> speechHash;
	
	/**
	 * Needed to keep the phone from turning the service off
	 */
	private WakeLock lock;
	
	/**
	 * Playlist of articles
	 */
	private ArticlePlaylist playlist; 
	private Article currentArticle;
	private String[] chunks;
	private Integer chunkIndex;
	private boolean pausedReading;
	
	private static final int NOTIFICATION_ID = 1001;
	private static final int silenceInterval = 1000;
	
	public static final int NOT_SPEAKING = -1;
	
	/**
	 * Mechanism for the activities to talk to service
	 * @author vamsi
	 *
	 */
	public class SpeechBinder extends Binder {
		SpeechService getService() {
			return SpeechService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onCreate() {
		
		// Init state
		pausedReading = true;
		currentArticle = null;
		chunkIndex = 0;
		chunks = null;
		playlist = new ArticlePlaylist();
		
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		
		// Specify the hashmap for the TTS Engine
		speechHash = new HashMap();
		speechHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Finished reading sentence");
		
		// Prevent the service from sleeping by keeping the CPU awake
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reading article");
		lock.acquire();
		
		// Acquire the telephony service instance which is required to stop reading when a call is incoming
		telephonyManager.listen(new CallListener(), PhoneStateListener.LISTEN_CALL_STATE);
	}

	/**
	 *  Call status checking
	 * @return
	 */
	private boolean checkCallStatus() {
		int dataState = telephonyManager.getCallState();
		
		if (dataState != TelephonyManager.CALL_STATE_IDLE) {
			Toast butterToast = Toast.makeText(this, "Cannot start reading when the call is on", Toast.LENGTH_SHORT);
			butterToast.show();
			return false;
		}
		
		return true;
	}
	
	/**
	 *  Listener class to respond to call state changes
	 * @author vamsi
	 *
	 */
	private class CallListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			// No articles to read right now means we dont care about state changes
			if (currentArticle == null) {
				return;
			}
			
			// We got a notification about state change. If state is not idle we should pause reading
			if (state != TelephonyManager.CALL_STATE_IDLE) {
				pauseReading();
			} else {
				// Call is over now, so start reading
				resumeReading();
			}
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		lock.release();
		notificationManager.cancel(NOTIFICATION_ID);
	}
	
	private final IBinder mBinder = new SpeechBinder();
	
	/**
	 * Notifications
	 * @param text
	 */
	private void showNotification(String text) {
		// Cancel any previous notifications
		notificationManager.cancel(NOTIFICATION_ID);
		
		int notificationIcon;
		String notificationTitle;
		
		if (pausedReading) {
			notificationIcon = R.drawable.pause64;
			notificationTitle = "NewsSpeak is paused";
		} else {
			notificationTitle = "NewsSpeak is reading";
			notificationIcon = R.drawable.play64;
		}
		
		Notification notification = new Notification(notificationIcon, text,
													System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		
		Intent notificationIntent = new Intent(this, Plaudible.class);
		notificationIntent.putExtra("NewsSource", currentArticle.getNewsSource());
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		// Set the intent in the notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this.getApplicationContext(), notificationTitle, text, contentIntent);
		
		notificationManager.notify(NOTIFICATION_ID, notification);
		
		// Keep the service in the foreground so the OS does not kill it for freeing resources
		startForeground(NOTIFICATION_ID, notification);
	}
	
	public void setTTSEngine(TextToSpeech ttsEngine) {
		this.ttsEngine = ttsEngine;
		this.ttsEngine.setOnUtteranceCompletedListener(this);
    }
	
	public void readArticle(Article article) {
		
		// Check if a call is on
		if (!checkCallStatus()) {
			return;
		}
		
		// Select preferences for TTS Engine
		setTTSPreferences();
		
		// Clean up state
		currentArticle = article;
		chunkIndex = 0;
		chunks = null;
		pausedReading = false;
		
		// Add the article as the first item in the playlist
		playlist.addArticle(article, 0 /* First item */);
		
		// Show a notification
		showNotification(currentArticle.getTitle());
		
		ttsEngine.speak("NewsSpeak will now read " + currentArticle.getTitle(), TextToSpeech.QUEUE_FLUSH, null);
		ttsEngine.playSilence(silenceInterval, TextToSpeech.QUEUE_ADD, null);
		
		// Create chunks of the article and read each chunk after the other
		prepareChunks();
		readCurrentChunk();
	}
	
	/**
	 *  Set the TTS preferences
	 */
	private void setTTSPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
	 *  Return the state of the service. If reading then return the article index it is reading currently
	 * @return
	 */
	public boolean isReading() {
		return !pausedReading;
	}
	
	/**
	 *  Return the index of the currently read article
	 * @return
	 */
	public Article getCurrentlyReadArticle() {
		return currentArticle;
	}
	
	/**
	 * Return the playlist
	 */
	public ArticlePlaylist getArticlePlaylist() {
		return playlist;
	}
	
	/**
	 *  Return the currently read news source
	 * @return
	 */
	public NewsSource getCurrentNewsSource() {
		if (currentArticle != null) {
			return currentArticle.getNewsSource();
		}
		return null;
	}
	
	/**
	 *  Split the article's content into sentences
	 */
	private void prepareChunks() {
		String chunk = currentArticle.getContent();
		if (chunk == null || chunk.equalsIgnoreCase("")) {
			chunk = new String("Sorry, This article could not be read.");
		}
		chunks = chunk.split("\\.");
		chunkIndex = 0;
	}

	/**
	 *  Read the current chunk
	 */
	private void readCurrentChunk() {
		if (chunkIndex < chunks.length && !pausedReading) {
			// Add the current chunk to the queue. The queue must have only 1 chunk at a time
			// Trim the string here to remove extraneous spaces.
			ttsEngine.speak(chunks[chunkIndex].trim(), TextToSpeech.QUEUE_ADD, speechHash);
		} else {
			// Finished reading the article.
			ttsEngine.speak("NewsSpeak finished reading the article.", TextToSpeech.QUEUE_ADD, speechHash);
			Log.d("SpeechService::readCurrentChunk", "Article is finished. No more chunks to read.");
		}
	}
	
	/**
	 *  Read the next chunk
	 */
	private void readNextChunk() {
		pausedReading = false;
		++chunkIndex;
		readCurrentChunk();
	}
	
	/**
	 *  Pause the reading. There should only be this one chunk in the TTS queue
	 */
	public void pauseReading() {
		pausedReading = true;
		showNotification(currentArticle.getTitle());
		ttsEngine.stop();
	}

	/**
	 *  Resume reading the same chunk where we left off
	 */
	public void resumeReading() {
		pausedReading = false;
		showNotification(currentArticle.getTitle());
		readCurrentChunk();
	}
	
	@Override
	public void onUtteranceCompleted(String utteranceId) {
		if (utteranceId.equals("Finished reading sentence")) {
			if (chunkIndex == chunks.length) {
				// Reset all the necessary state variables
				pausedReading = true;
				chunkIndex = 0;
				chunks = null;

				// Move to the next item in the playlist
				playlist.removeArticle(currentArticle);
				currentArticle = null;
				
				if (!playlist.isEmpty()) {
					playlist.moveToNext();
					
					currentArticle = playlist.getCurrentArticle();
					readArticle(currentArticle);
				} else {
					// Cancel the notification
					notificationManager.cancel(NOTIFICATION_ID);
				}
				
			} else if (!pausedReading){
				// Read the next chunk
				readNextChunk();
			}
		}
	}
}
