package com.vn.plaudible;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

public class SpeechService extends Service {

	private NotificationManager nm;
	private TextToSpeech ttsEngine;
	
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
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		showNotification();
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
	
	private void showNotification() {
		
	}
	
	public void setTTSEngine(TextToSpeech ttsEngine) {
		this.ttsEngine = ttsEngine;
		
		this.ttsEngine.speak("Text to speech is initialized.",
				TextToSpeech.QUEUE_ADD, null);
        
	}
	
	public void readArticle(String content) {
		this.ttsEngine.speak(content, TextToSpeech.QUEUE_FLUSH, null);
	}
}
