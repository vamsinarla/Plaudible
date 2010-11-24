package com.vn.plaudible;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;

public class SpeechService extends Service {

	private NotificationManager nm;
	private static final int NOTIFY_ID = R.layout.main;

	private TextToSpeech ttsEngine;
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onDestroy() {
		nm.cancel(NOTIFY_ID);
	}
	
	public IBinder getBinder() {
		return mBinder;
	}
	
	public void abc() {
	}
	
	private final PLSInterface.Stub mBinder = new PLSInterface.Stub() {
		
		@Override
		public void readArticle(String text) throws RemoteException {
			// ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null);
			Integer i = new Integer(42);
		}

		@Override
		public void stopReading() throws RemoteException {
			// ttsEngine.stop();
			Integer i = new Integer(42);
		}
	};

}
