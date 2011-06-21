package com.vn.plaudible;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.vn.plaudible.db.NewsSpeakDBAdapter;
import com.vn.plaudible.tts.SpeechService;
import com.vn.plaudible.types.NewsSource;

/**
 * The Starting screen of NewsSpeak
 * @author vamsi
 *
 */
public class HomePage extends Activity implements TextToSpeech.OnInitListener {
	
	private TextToSpeech ttsEngine;
	private SpeechService mSpeechService;
	
	private static final int TTS_INSTALLED_CHECK_CODE = 1;
	
	/**
	 *  Called when the activity is first created.  
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);
        
        // Share button stuff
        ImageButton shareButton = (ImageButton) this.findViewById(R.id.shareButton);
        shareButton.setImageResource(android.R.drawable.ic_menu_share);
        shareButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");

				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getBaseContext().getString(R.string.share_subject));
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getBaseContext().getString(R.string.share_body));
				
				startActivity(Intent.createChooser(shareIntent, "Share using"));
			}
        });
        
        // Submit button stuff
        ImageButton submitButton = (ImageButton) this.findViewById(R.id.feedbackButton);
        submitButton.setImageResource(android.R.drawable.ic_menu_send);
        submitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent myIntent = new Intent(android.content.Intent.ACTION_SEND);
				String to[] = { HomePage.this.getString(R.string.feedback_mail) };

				myIntent.setType("plain/text");

				myIntent.putExtra(android.content.Intent.EXTRA_EMAIL, to);
				myIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getBaseContext().getString(R.string.feedback_subject));
				
				startActivity(myIntent);
			}
        });
        
        // Get me the news stuff
        ImageButton newsButton = (ImageButton) this.findViewById(R.id.getNewsButton);
        newsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// If no data link then just don't open further activity. Best to stop it here.
				if (!Utils.checkDataConnectivity(HomePage.this)) {
					Toast butterToast = Toast.makeText(HomePage.this, R.string.connection_unavailable, Toast.LENGTH_SHORT);
					butterToast.show();
					return;
				}
				
				Intent getNewsIntent = new Intent();
				getNewsIntent.setClass(HomePage.this, NewsSourcesTabActivity.class);
				
				startActivity(getNewsIntent);
			}
        });
        
        // Settings stuff
        ImageButton settingsButton = (ImageButton) this.findViewById(R.id.settingsButton);
        settingsButton.setImageResource(android.R.drawable.ic_menu_preferences);
        settingsButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Intent settingsActivity = new Intent(HomePage.this, NewsSpeakPreferences.class);
        		startActivity(settingsActivity);
        	}
        });
        
        // Help stuff
        ImageButton helpButton = (ImageButton) this.findViewById(R.id.helpButton);
        helpButton.setImageResource(android.R.drawable.ic_menu_help);
        helpButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		AlertDialog alertDialog = new AlertDialog.Builder(HomePage.this).create();
        		
        		alertDialog.setTitle(HomePage.this.getString(R.string.help_title));
        		alertDialog.setMessage(getApplicationContext().getString(R.string.help));
        		
        		alertDialog.setButton(HomePage.this.getString(R.string.ok), new DialogInterface.OnClickListener() {
        			@Override
        			public void onClick(DialogInterface dialog, int which) {
        				
        			}
        		});
        		alertDialog.show();
        	}
        });
        
        // ShutDown button
        ImageButton offButton = (ImageButton) this.findViewById(R.id.shutDownButton);
        offButton.setImageResource(android.R.drawable.ic_lock_power_off);
        offButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Finish the activity
				finish();
			}
		});
        
        // Prepare the DB if this is the first run.
		// Place a call to AppEngine to get the list of preferred NewsSources for this user
		if (isFirstRun()) {
			// Start a spinning wheel to show we are busy
			ProgressDialog spinningWheel = ProgressDialog.show(HomePage.this, 
											"Please wait",
											"We are preparing your news list",
											true);
			spinningWheel.show();
			
			new PlaudibleAsyncTask().execute(
								new PlaudibleAsyncTask.Payload(
			        				PlaudibleAsyncTask.FEATURED_SOURCES_DOWNLOAD_TASK,
			        				new Object[] { HomePage.this,
			        								spinningWheel }));

			// We can now know that we successfully completed the first run, so set it
			setFirstRun();
		}
		
		// Quick Search box functionality
		ImageButton quickSearchButton = (ImageButton) this.findViewById(R.id.quick_search_button);
		quickSearchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText searchEditBox = (EditText) HomePage.this.findViewById(R.id.quick_search_box);
				String searchTerm = searchEditBox.getEditableText().toString();
				
				if (searchTerm == null || searchTerm.trim().equalsIgnoreCase("")) {
					Toast butterToast = Toast.makeText(HomePage.this, R.string.invalid_title_message, Toast.LENGTH_SHORT);
					butterToast.show();
					return;
				}
				
				NewsSource newsSource = Utils.generateGoogleNewsSource(searchTerm);
				
				// Now create and dispatch an intent to PLaudible to read this NewsSource
				Intent quickNewsIntent = new Intent();
				quickNewsIntent.setClass(HomePage.this, FeedViewerActivity.class);
				quickNewsIntent.putExtra(FeedViewerActivity.INTENT_NEWSSOURCE, newsSource);
				
				startActivity(quickNewsIntent);
						
			}
		});
		
        bindSpeechService();
        checkAndInstallTTSEngine();
        
        // Set the context for Utils
        Utils.setContext(this);
    }
    
    /**
     *  Listen for configuration changes and this is basically to prevent the 
     *  activity from being restarted. Do nothing here.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    /**
     * Make the behaviour of the back button same as the Home Button
     */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	        moveTaskToBack(true);
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
   
    @Override
    protected void onDestroy() {
		ttsEngine.stop();
    	ttsEngine.shutdown();
    	unBindSpeechService();
    	
    	super.onDestroy();
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
		this.bindService(new Intent(HomePage.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
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
