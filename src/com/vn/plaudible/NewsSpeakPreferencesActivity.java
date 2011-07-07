package com.vn.plaudible;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

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
		
		PreferenceScreen screen = getPreferenceScreen();
		Preference autoUpdatePreference = screen.findPreference(getString(R.string.auto_update_pref_key));
		
		autoUpdatePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(final Preference preference) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=com.vn.plaudible"));
					startActivity(intent);
					return true;
				}
			});
	}
}
