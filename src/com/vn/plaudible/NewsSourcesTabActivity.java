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
	    spec = tabHost.newTabSpec("display").setIndicator(getString(R.string.subscribed_tab_title), res.getDrawable(R.drawable.textonly))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the default tab to show the list of subscribed newssources
	    intent = new Intent().setClass(this, ReorderNewsSourcesPage.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("manage").setIndicator(getString(R.string.manage_tab_title), res.getDrawable(android.R.drawable.ic_menu_manage))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	 }
}
