package com.vn.plaudible.tts;

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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.vn.plaudible.FeedViewerActivity;
import com.vn.plaudible.R;
import com.vn.plaudible.Utils;
import com.vn.plaudible.types.Article;
import com.vn.plaudible.types.Event;
import com.vn.plaudible.types.EventListener;
import com.vn.plaudible.types.Playlist;

/**
 * The service that speaks
 * @author vamsi
 *
 */
public class SpeechService extends Service {

	/**
	 * System services for notifications and incoming call statuses
	 */
	private NotificationManager notificationManager;
	private TelephonyManager telephonyManager;
	
	/**
	 * Needed to keep the phone from turning the service off
	 */
	private WakeLock lock;
	
	private TTSDataAdapter<Article> ttsDataAdapter;
	private TTSEngineController ttsController;
	
	private static final int NOTIFICATION_ID = 1001;
	
	private enum State { PLAYING, PAUSED, NOT_PLAYING, PAUSED_ON_CALL };
	private State state;
	
	/**
	 * Mechanism for the activities to talk to service
	 * @author vamsi
	 *
	 */
	public class SpeechBinder extends Binder {
		public SpeechService getService() {
			return SpeechService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 *  Listener class to respond to call state changes
	 * @author vamsi
	 *
	 */
	class CallListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int phoneState, String incomingNumber) {
			if (phoneState == TelephonyManager.CALL_STATE_IDLE && state == State.PAUSED_ON_CALL) {
				resumeReading();
			} else if (state == State.PLAYING) {
				pauseReading();
				state = State.PAUSED_ON_CALL;
			}
		}
	}
	
	/**
	 * Class that listens to completion of speech 
	 * @author vnarla
	 *
	 */
	class ArticlePlaybackCompletionListener implements EventListener {
		@Override
		public void actionPerformed(Event e) {
			Log.d("SpeechService", "Article playback complete");
			stopReading();
		}
	}
	
	@Override
	public void onCreate() {
		// Init state
		state = State.NOT_PLAYING;
		
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		
		// Prevent the service from sleeping by keeping the CPU awake
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Reading article");
		lock.acquire();
	}

	/**
	 *  Call status checking
	 * @return
	 */
	private boolean checkCallStatus() {
		int dataState = telephonyManager.getCallState();
		if (dataState != TelephonyManager.CALL_STATE_IDLE) {
			Toast butterToast = Toast.makeText(this, Utils.getStringFromResourceId(R.string.call_in_progress),
											   Toast.LENGTH_SHORT);
			butterToast.show();
			state = State.NOT_PLAYING;
			return false;
		}
		
		return true;
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
		
		if (state == State.PAUSED || state == State.PAUSED_ON_CALL) {
			notificationIcon = R.drawable.pause64;
			notificationTitle = Utils.getStringFromResourceId(R.string.reading_paused);
		} else {
			notificationTitle = Utils.getStringFromResourceId(R.string.reading_playing);
			notificationIcon = R.drawable.play64;
		}
		
		Notification notification = new Notification(notificationIcon, text,
													System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		Intent notificationIntent = new Intent(this, FeedViewerActivity.class);
		notificationIntent.putExtra("NewsSource", ttsDataAdapter.getCurrentItem().getSource());
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		// Set the intent in the notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this.getApplicationContext(), notificationTitle, text, contentIntent);
		
		notificationManager.notify(NOTIFICATION_ID, notification);
		
		// Keep the service in the foreground so the OS does not kill it for freeing resources
		startForeground(NOTIFICATION_ID, notification);
	}
	
	public void initializeSpeechService(TextToSpeech ttsEngine) {
		ttsDataAdapter = new TTSDataAdapter<Article>();
		ttsController = new TTSEngineController(ttsEngine, ttsDataAdapter);
		
		// Acquire the telephony service instance which is required to stop reading when a call is incoming
		telephonyManager.listen(new CallListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }
	
	public void startReadingArticle(Article article) {
		// Check if a call is on
		if (!checkCallStatus()) {
			Log.d("::readArticle", "Call is in progress, will not read article");
			return;
		}

		// Add the article as the first item in the playlist
		ttsDataAdapter.addItem(article, 0 /* First item */);
		
		// Show a notification
		showNotification(article.getTitle());
		
		// Select preferences for TTS Engine and speak "init text"
		ttsController.setTTSPreferences(this);
		ttsController.speak(article.getContentForSpeech(), null); 

		// Start reading
		ttsController.speakFromPlaylist();
		
		// Set the playback completion listener
		ttsController.setItemCompletionListner(new ArticlePlaybackCompletionListener());
		
		// Change the state
		state = State.PLAYING;
	}
	
	/**
	 *  Return the state of the service. If reading then return the article index it is reading currently
	 * @return
	 */
	public boolean isReading() {
		return (state == State.PLAYING);
	}
	
	/**
	 *  Pause the reading. There should only be this one chunk in the TTS queue
	 */
	public void pauseReading() {
		ttsController.pause();
		state = State.PAUSED;
	}

	/**
	 *  Resume reading the same chunk where we left off
	 */
	public void resumeReading() {
		ttsController.resume();
		state = State.PLAYING;
	}

	public Article getCurrentItem() {
		return ttsDataAdapter.getCurrentItem();
	}

	public Playlist getPlaylist() {
		return ttsDataAdapter.getPlaylist();
	}

	public void stopReading() {
		if (state == State.PLAYING) {
			state = State.NOT_PLAYING;
			ttsController.stop();
			notificationManager.cancelAll();
		}
	}
}
