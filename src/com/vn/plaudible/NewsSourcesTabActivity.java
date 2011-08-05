package com.vn.plaudible;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;

import com.vn.plaudible.tts.SpeechService;


/**
 * Activity that takes care of drawing the Tabs to host 
 * the various other activities related to viewing, editing
 * and searching for NewsSources
 * @author vamsi
 *
 */
public class NewsSourcesTabActivity extends TabActivity implements TextToSpeech.OnInitListener {

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
	    
	    checkAndInstallTTSEngine();
	    bindSpeechService();
	}
	
    /**
     * This is to make sure that the BACK button does
     * not cause the activity to be destroyed and hence
     * the speech service to be destroyed too.
     * 
     */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK)
	    {
	        moveTaskToBack(true);
	        return true; // return
	    }
	    return false;
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

				startActivity(Intent.createChooser(shareIntent, "Share NewsSpeak on"));
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
    
    private TextToSpeech ttsEngine;
	private static final int TTS_INSTALLED_CHECK_CODE = 1;
	private SpeechService mSpeechService;
	
    /**
     *  Called for the intent which checks if TTS was installed and starts TTS up
     */
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_INSTALLED_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                ttsEngine = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }
    

    /**
     *  Check for presence of a TTSEngine and install if not found
     */
    protected void checkAndInstallTTSEngine() {
	    Intent checkIntent = new Intent();
	    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	    startActivityForResult(checkIntent, TTS_INSTALLED_CHECK_CODE);
    }
    
    
    /**
     *  OnInitListener for TTSEngine initialization
     *  Check if the Service is bound and if it is then we can set the TTS Engine it should use
     */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
	           ttsEngine.setLanguage(Locale.US);	            
	           
	           if (mSpeechService != null) {
	        	   mSpeechService.initializeSpeechService(this.ttsEngine);
	           }
		}
	}
	
	/**
	 * Connection to the Service. All Activities must have this.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSpeechService = ((SpeechService.SpeechBinder)service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSpeechService = null;
		}	
	};
	
	/**
	 * Bind to the Speech Service. Called from onCreate() on this activity
	 */
	void bindSpeechService() {
		this.bindService(new Intent(NewsSourcesTabActivity.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	/**
	 * Unbind from the Speech Service. Called from onDestroy() on this activity
	 */
	void unBindSpeechService() {
		if (mSpeechService != null) {
			this.unbindService(mConnection);
		}
	}
}
