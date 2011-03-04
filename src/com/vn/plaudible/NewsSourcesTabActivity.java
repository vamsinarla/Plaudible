package com.vn.plaudible;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;


/**
 * Activity that takes care of drawing the Tabs to host 
 * the various other activities related to viewing, editing
 * and searching for NewsSources
 * @author vamsi
 *
 */
public class NewsSourcesTabActivity extends TabActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.newssources_tabwidget);
	    
	    Resources res = getResources();      // Resource object to get Drawables
	    TabHost tabHost = getTabHost(); 	 // The activity TabHost
	    TabHost.TabSpec spec;  				 // Resusable TabSpec for each tab
	    Intent intent;

	    // Create an Intent to launch the default tab to show the list of subscribed newssources
	    intent = new Intent().setClass(this, NewsSourcesPage.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("display").setIndicator(getString(R.string.subscribed_tab_title), res.getDrawable(R.drawable.subscribed_lists))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the playlist tab to manage playlist
	    intent = new Intent().setClass(this, PlaylistManager.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("playlist").setIndicator(getString(R.string.playlist_tab_title), res.getDrawable(R.drawable.playlist))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the manage tab to manage the list of subscribed newssources
	    intent = new Intent().setClass(this, ReorderNewsSourcesPage.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("manage").setIndicator(getString(R.string.manage_tab_title), res.getDrawable(R.drawable.settings))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the search tab to search
	    intent = new Intent().setClass(this, SearchPage.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("search").setIndicator(getString(R.string.search_tab_title), res.getDrawable(R.drawable.search))
             			.setContent(intent);
	    tabHost.addTab(spec);
	 }
}
