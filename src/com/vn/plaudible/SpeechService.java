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
	
	private static final int NOTIFICATION_ID = 1001;
	private static final int silenceInterval = 1000;
	
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
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		lock.release();
		// nm.cancel(R.string.speech_service_started);
	}
	
	private final IBinder mBinder = new SpeechBinder();
	
	private void showNotification(String text) {
		Notification notification = new Notification(android.R.drawable.star_on, text,
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
	
		this.ttsEngine.speak("Text to speech is initialized.", TextToSpeech.QUEUE_ADD, null);
     }
	
	public void readArticle(Article article) {
		this.currentArticle = article;
		this.chunkIndex = 0;
		this.chunks = null;
		
		showNotification(currentArticle.getTitle());
		
		// Prevent the service from sleeping by keeping the CPU awake
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reading article");
		lock.acquire();
		
		ttsEngine.speak("Plaudible will now read " + currentArticle.getTitle(), TextToSpeech.QUEUE_FLUSH, null);
		ttsEngine.playSilence(silenceInterval, TextToSpeech.QUEUE_ADD, null);
		
		// Create chunks of the article and read each chunk after the other
		prepareChunks();
		readCurrentChunk();
	}
	
	private void prepareChunks() {
		String chunk = currentArticle.getContent();
		chunks = chunk.split("\\.");
		chunkIndex = 0;
	}

	private void readCurrentChunk() {
		ttsEngine.speak(chunks[chunkIndex], TextToSpeech.QUEUE_ADD, speechHash);
	}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		if (utteranceId.equals("Finished reading sentence")) {
			Log.d("SpeechService", "Finished reading sentence");
			if (chunkIndex == chunks.length) {
				// Finished reading the article.
				ttsEngine.speak("Plaudible finished reading the article.", TextToSpeech.QUEUE_ADD, null);
			} else {
				++chunkIndex;
				readCurrentChunk();
			}
		}
	}
}
