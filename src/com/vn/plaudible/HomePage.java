package com.vn.plaudible;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import com.vn.plaudible.tts.SpeechService;

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
				
				/* Create a NewsSource that Plaudible can use
				 * Construct URL for google news on the following search Term 
				 */
				String googleNewsUrl = "http://news.google.com/news?pz=1&cf=all";
				googleNewsUrl += "&ned=" + Locale.getDefault().getCountry();
				// googleNewsUrl += "&hl=" + Locale.getDefault().getLanguage();
				googleNewsUrl += "&hl=en";
				googleNewsUrl += "&q=" + searchTerm;
				googleNewsUrl += "&cf=all&output=rss"; 
					
				NewsSource newsSource = 
					new NewsSource(getString(R.string.topic_search_title) + " " + searchTerm,
									"blog",
									Locale.getDefault().getLanguage(),
									Locale.getDefault().getCountry(),
									false,
									null,
									null,
									googleNewsUrl,
									false,
									false,
									0);
				
				// Now create and dispatch an intent to PLaudible to read this NewsSource
				Intent quickNewsIntent = new Intent();
				quickNewsIntent.setClass(HomePage.this, FeedViewerActivity.class);
				quickNewsIntent.putExtra("NewsSource", newsSource);
				
				startActivity(quickNewsIntent);
						
			}
		});
		
        bindSpeechService();
        checkAndInstallTTSEngine();
    }
    
    /**
     * Populate NewsSources in the DB on first time usage.
     * The URL to fetch the JSON results are computed elsewhere
     * and then we get all the sources to begin with. Then we enter them
     * into the DB, but before that we must check the preferred sources
     * and give them the special treatment
     */
    protected void populateSourcesIntoDB() {
    	NewsSpeakDBAdapter dbAdapter = new NewsSpeakDBAdapter(this);
    	dbAdapter.open(NewsSpeakDBAdapter.READ_WRITE);
    	
    	// Clean up the DB with any artifacts
    	dbAdapter.upgrade();
    	
    	// Make a call to AppEngine and get the featured sources
    	String link = getURLForFeaturedSources();
    	
    	try {
        	URL feedUrl = new URL(link);

        	// Parse the response stream from AppEngine
	    	ArrayList<NewsSource> sources = new ArrayList<NewsSource>();
	    	InputStream responseStream = feedUrl.openConnection().getInputStream();
			
			// Construct the Array of sources from the JSON String
			sources = NewsSource.getNewsSourcesFromJSON(Utils.getStringFromInputStream(responseStream));
			
			// Insert the NewsSources into the localDB
			for (int index = 0; index < sources.size(); ++index) {
				// Set the display index
				sources.get(index).setDisplayIndex(index);
				
				// Here we treat preferred sources differently
				if (sources.get(index).isPreferred()) {
					NewsSource.createPreferred(sources.get(index));
				}
				
				// Insert into DB
				dbAdapter.createNewsSource(sources.get(index));
			}
			
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		dbAdapter.close();
    	}
    }

    /**
     * We must construct the URL for the featured sources which are
     * used first time the app are called
     * @return
     */
	private String getURLForFeaturedSources() {
		String country = Locale.getDefault().getCountry();
		String language = Locale.getDefault().getLanguage();
		
		String url = getString(R.string.appengine_url) + "/featuredSources?";
		url += "country=" + country;
		url += "&language=" + language;
		
		return url;
	}

	/**
     * Find out if this is the first run of NewsSpeak
     * @return
     */
    protected boolean isFirstRun() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean result = prefs.getBoolean("FirstRun", true);
    	
     	return result;
	}
    
    /**
     * Set the first run on the successful completion of the first run
     * @return
     */
    protected boolean setFirstRun() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putBoolean("FirstRun", false);
		editor.commit();
	
		return true;
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
