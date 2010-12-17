package com.vn.plaudible;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;

public class SpeechService extends Service implements OnUtteranceCompletedListener {

	private NotificationManager notificationManager;
	private TextToSpeech ttsEngine;
	private HashMap<String, String> speechHash;
	private WakeLock lock;
	private Article currentArticle;
	private String[] chunks;
	private Integer chunkIndex;
	private boolean pausedReading;
	
	private static final int NOTIFICATION_ID = 1001;
	private static final int silenceInterval = 1000;
	
	public static final int NOT_SPEAKING = -1;
	
	public class SpeechBinder extends Binder {
		SpeechService getService() {
			return SpeechService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		speechHash = new HashMap();
		speechHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Finished reading sentence");
		
		// Prevent the service from sleeping by keeping the CPU awake
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reading article");
		lock.acquire();
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
	
	private void showNotification(String text) {
		// Cancel any previous notifications
		notificationManager.cancel(NOTIFICATION_ID);
		int notificationIcon;
		
		if (pausedReading) {
			notificationIcon = R.drawable.pause64;
		} else {
			notificationIcon = R.drawable.play64;
		}
		
		Notification notification = new Notification(notificationIcon, text,
													System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		
		Intent notificationIntent = new Intent(this, Plaudible.class);
		notificationIntent.putExtra("Source", "Notification");
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this.getApplicationContext(), 
										"Plaudible", text, contentIntent);
		
		notificationManager.notify(NOTIFICATION_ID, notification);
		startForeground(NOTIFICATION_ID, notification);
	}
	
	public void setTTSEngine(TextToSpeech ttsEngine) {
		this.ttsEngine = ttsEngine;
	
		this.ttsEngine.setOnUtteranceCompletedListener(this);
		this.ttsEngine.setSpeechRate((float) 1);
     }
	
	public void readArticle(Article article) {
		this.currentArticle = article;
		this.chunkIndex = 0;
		this.chunks = null;
		
		showNotification(currentArticle.getTitle());
		
		ttsEngine.speak("Plaudible will now read " + currentArticle.getTitle(), TextToSpeech.QUEUE_FLUSH, null);
		ttsEngine.playSilence(silenceInterval, TextToSpeech.QUEUE_ADD, null);
		
		pausedReading = false;
		
		// Create chunks of the article and read each chunk after the other
		prepareChunks();
		readCurrentChunk();
	}
	
	// Return the state of the service. If reading then return the article index it is reading currently
	public boolean isReading() {
		return !pausedReading;
	}
	
	public Integer getCurrentlyReadArticle() {
		if (currentArticle != null) {
			return currentArticle.getId();
		} else {
			return NOT_SPEAKING;
		}
	}
	
	// Split the article's content into sentences
	private void prepareChunks() {
		String chunk = currentArticle.getContent();
		chunks = chunk.split("\\.");
		chunkIndex = 0;
	}

	// Read the current chunk
	private void readCurrentChunk() {
		if (chunkIndex < chunks.length && !pausedReading) {
			// Add the current chunk to the queue. The queue must have only 1 chunk at a time
			// Trim the string here to remove extraneous spaces.
			ttsEngine.speak(chunks[chunkIndex].trim(), TextToSpeech.QUEUE_ADD, speechHash);
		} else {
			Log.e("SpeechService::readCurrentChunk", "Article is finished. No more chunks to read.");
		}
	}
	
	// Read the next chunk
	private void readNextChunk() {
		pausedReading = false;
		++chunkIndex;
		readCurrentChunk();
	}
	
	// Pause the reading. There should only be this one chunk in the TTS queue
	public void pauseReading() {
		pausedReading = true;
		showNotification(currentArticle.getTitle());
		ttsEngine.stop();
	}

	// Resume reading the same chunk where we left off
	public void resumeReading() {
		pausedReading = false;
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
				currentArticle = null;
				
				// Finished reading the article.
				ttsEngine.speak("Plaudible finished reading the article.", TextToSpeech.QUEUE_ADD, null);
			} else if (!pausedReading){
				// Read the next chunk
				readNextChunk();
			}
		}
	}
	
}
