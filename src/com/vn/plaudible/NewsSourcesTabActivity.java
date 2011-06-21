package com.vn.plaudible;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
	    intent = new Intent().setClass(this, NewsSourcesActivity.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("display").setIndicator(getString(R.string.subscribed_tab_title), res.getDrawable(R.drawable.subscribed_lists))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the playlist tab to manage playlist
	    intent = new Intent().setClass(this, MarkedListManagerActivity.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("playlist").setIndicator(getString(R.string.playlist_tab_title), res.getDrawable(R.drawable.bookmark_icon))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the manage tab to manage the list of subscribed newssources
	    intent = new Intent().setClass(this, ReorderNewsSourcesPageActivity.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("manage").setIndicator(getString(R.string.manage_tab_title), res.getDrawable(R.drawable.manage_newssources))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the manage tab to manage the list of subscribed newssources
	    intent = new Intent().setClass(this, NewsSpeakPreferencesActivity.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("settings").setIndicator(getString(R.string.settings_tab_title), res.getDrawable(R.drawable.settings))
             			.setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Create an Intent to launch the search tab to search
	    intent = new Intent().setClass(this, SearchPage.class);
	    // Define the tabspec for this activity
	    spec = tabHost.newTabSpec("search").setIndicator(getString(R.string.search_tab_title), res.getDrawable(R.drawable.add_newssource))
             			.setContent(intent);
	    tabHost.addTab(spec);
	}
	
	/**
	 * Create the options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_page_options_menu, menu);
	    return true;
	}
	
    /**
     * Handle the options menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        switch (item.getItemId()) {
            case R.id.newsspeak_share:
            	Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");

				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share_body));

				startActivity(Intent.createChooser(shareIntent, "Share using"));
                break;
            case R.id.newsspeak_feedback:
            	Intent myIntent = new Intent(android.content.Intent.ACTION_SEND);
				String to[] = { getString(R.string.feedback_mail) };

				myIntent.setType("plain/text");

				myIntent.putExtra(android.content.Intent.EXTRA_EMAIL, to);
				myIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));

				startActivity(myIntent);
            	break;
            case R.id.newsspeak_help:
            	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        		
        		alertDialog.setTitle(getString(R.string.newsspeak_help));
        		alertDialog.setMessage(getString(R.string.help_text));
        		
        		alertDialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
        			@Override
        			public void onClick(DialogInterface dialog, int which) {
        				
        			}
        		});
        		alertDialog.show();
            	break;
        }
        return true;
    }
}
