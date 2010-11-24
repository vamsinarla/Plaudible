package com.vn.plaudible;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class SpeechService extends Service {

	private NotificationManager nm;
	
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
}
