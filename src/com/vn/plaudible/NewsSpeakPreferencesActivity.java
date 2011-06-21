package com.vn.plaudible;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Settings page
 * @author vamsi
 *
 */
public class NewsSpeakPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		setTitle("NewsSpeak preferences");
	}
}
