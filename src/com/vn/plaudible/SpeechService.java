package com.vn.plaudible;

import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;

public class SpeechService extends Service implements OnUtteranceCompletedListener {

	private NotificationManager notificationManager;
	private TextToSpeech ttsEngine;
	private HashMap<String, String> speechHash;

	private static final int NOTIFICATION_ID = 1001;

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
		speechHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Finished reading article");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		// nm.cancel(R.string.speech_service_started);
	}
	
	private final IBinder mBinder = new SpeechBinder();
	
	private void showNotification(String text) {
		Notification notification = new Notification(android.R.drawable.star_on, text,
													System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Plaudible.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(this.getApplicationContext(), 
										"Plaudible", text, contentIntent);
		
		notificationManager.notify(NOTIFICATION_ID, notification);
		startForeground(NOTIFICATION_ID, notification);
	}
	
	public void setTTSEngine(TextToSpeech ttsEngine) {
		this.ttsEngine = ttsEngine;
		
		this.ttsEngine.speak("Text to speech is initialized.",
				TextToSpeech.QUEUE_ADD, null);
     
		this.ttsEngine.setOnUtteranceCompletedListener(this);
		this.ttsEngine.setSpeechRate((float) 1.5);
	}
	
	public void readArticle(Article article) {
		showNotification(article.getTitle());
		this.ttsEngine.speak(article.getContent(), TextToSpeech.QUEUE_FLUSH, speechHash);
	}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		if (utteranceId == "Finished reading article") {
			Log.d("SpeechService", "Finished reading article");
		}
	}
}
