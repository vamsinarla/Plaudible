package com.vn.plaudible;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class NewsSpeakPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		setTitle("NewsSpeak preferences");
	}
}
