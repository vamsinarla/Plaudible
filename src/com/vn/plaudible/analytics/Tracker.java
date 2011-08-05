package com.vn.plaudible.analytics;

import android.content.Context;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.vn.plaudible.R;

public class Tracker {
	private static final int ANALYTICS_REPORTING_INTERVAL = 60;

	/**
	 * Google Analytics
	 */
	private GoogleAnalyticsTracker tracker;
	private Context context;
	
	private static Tracker _instance;
	
	private Tracker(Context _context) {
		context = _context;
		
		// Get the analytics tracker instance
	    tracker = GoogleAnalyticsTracker.getInstance();
	    
	    // Start the tracker in auto dispatch mode to update every few seconds
	    tracker.start(context.getString(R.string.analytics_id), ANALYTICS_REPORTING_INTERVAL, context);
	}
	
	public static synchronized Tracker getInstance(Context context) {
		if (_instance == null) {
			_instance = new Tracker(context);
		}
		return _instance;
	}
	
	public void trackPageView(String pageTitle) {
		tracker.trackPageView(pageTitle);
	}
	
	public void trackEvent(String event, String eventSubType, String data) {
		tracker.trackEvent(event, eventSubType, data, 0);
	}
}
