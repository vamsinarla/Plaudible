package com.vn.plaudible;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.vn.plaudible.analytics.Tracker;
import com.vn.plaudible.tts.SpeechService;
import com.vn.plaudible.types.Article;
import com.vn.plaudible.types.Feed;
import com.vn.plaudible.types.NewsSource;

public class FeedViewerActivity extends ListActivity {
	
	/**
	 *  ViewHolder pattern for efficient ListAdapter usage
	 * @author vamsi
	 *
	 */
	static class ViewHolder {
        TextView title;
        TextView description;
        
        int position;
    }

	private FeedListAdapter adapter;
	private NewsSource currentNewsSource;
	private SpeechService mSpeechService;
	private SharedPreferences preferences;
	
	private SlidingDrawer slidingDrawer;
	private Tracker tracker;
	
	
	// Intents and extra strings
	public static final String INTENT_NEWSSOURCE = "newssource";
	
	private static final int DROPDOWNBAR_TIMEOUT = 7000;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set view related stuff
        setContentView(R.layout.articles_list);
        
        // Get the intent and the related extras
        Intent intent = getIntent();
        currentNewsSource = (NewsSource) intent.getSerializableExtra(INTENT_NEWSSOURCE);
        
        // Get a analytics tracking instance
        tracker = Tracker.getInstance(this);
        
        // Track this news source being opened
        tracker.trackPageView("/news/" + currentNewsSource.getTitle());
        
        // Enable the sliding drawer if there are categories for this source
        initializeSlidingDrawer();
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Arraylist adapter
        adapter = new FeedListAdapter(this, R.layout.articles_list_item, new Feed());
        setListAdapter(adapter);

        // Load feed will automatically load the correct feed as per the current newssource
        loadFeed();
        
        bindSpeechService();
        
        // Load application preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }
	
    
	
    /**
     * Set the current theme based on the preferences
     */
    protected void onResume() {
    	super.onResume();
    	
    	// Restore current theme
    	Utils.setCurrentTheme(this);
    	
    	// Suspend the spinning wheel
		Utils.suspendSpinningWheel();
		
    	displayBottomBar();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
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
     * Function that loads a feed as per the current URl of the current newssource
     */
    private void loadFeed() {
    	// Set the URL for the feed to load
    	adapter.feed.setUrl(currentNewsSource.getCurrentURL());
    	
    	new PlaudibleAsyncTask().execute(
        		new PlaudibleAsyncTask.Payload(
        				PlaudibleAsyncTask.FEED_DOWNLOADER_TASK,
        				new Object[] { FeedViewerActivity.this,
        								adapter.feed }));
    }
    
   /**
    *  Used by the AsyncTask to update the set of articles
    * @param articles
    */
   public void setFeed(Feed feed) {
	   adapter.feed = feed; 
	   adapter.notifyDataSetChanged();
	   
	   // Articles were fetched so we can suspend the wheel
	   Utils.suspendSpinningWheel();
	   
	   // Bottom bar
	   displayBottomBar();
   }
   
   /**
    * Initialize the sliding drawer if required
    */
   private void initializeSlidingDrawer() {
	   String title;
	   // Enable the sliding drawer if there are categories for this source
	   slidingDrawer = (SlidingDrawer) findViewById(R.id.SlidingDrawer);
	   
	   if (currentNewsSource.isHasCategories()) {
			// Setup sliding drawer with a listview with the categories
		   setupSlidingDrawer();
		   
		   title = currentNewsSource.getTitle() + " : " + currentNewsSource.getCurrentCategoryName();
	   }
	   else {
		   title = currentNewsSource.getTitle();
	   }
	   setTitle(title);
   }
   
   /**
    * Setup the sliding drawer to show categories
    */
   private void setupSlidingDrawer() {
	   	// Make it visible
		slidingDrawer.setVisibility(View.VISIBLE);
	    
	   	ListView categoriesListView = (ListView) findViewById(R.id.categoriesListView);
	   	categoriesListView.setAdapter(new CategoryListAdapter(FeedViewerActivity.this, 
	   											R.layout.categories_list_item, 
												currentNewsSource.getCategories()));
   }
   
   /**
    * ArrayList adapter for categories shown in sliding drawer
    * @author vamsi
    *
    */
   private class CategoryListAdapter extends ArrayAdapter<String> implements View.OnClickListener {

	   private ArrayList<String> categories;
	   public CategoryListAdapter(Context context, int textViewResourceId, ArrayList<String> objects) {
		   super(context, textViewResourceId, objects);
		   categories = objects;
	   }
	   
	   /**
	    * Item's View
	    */
	   @Override
	   public View getView(int position, View convertView, ViewGroup parent) {
		   ViewHolder holder;

		   // First time creating the holder. Inflate the views only once with this pattern.
		   if (convertView == null) {
			   LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			   convertView = inflater.inflate(R.layout.categories_list_item, null);

			   holder = new ViewHolder();
			   holder.title = (TextView) convertView.findViewById(R.id.categoryTitle);
			   convertView.setTag(holder);
		   } else {
			   holder = (ViewHolder) convertView.getTag();
		   }
		   convertView.setOnClickListener(this);
		   
		   holder.title.setText(categories.get(position));
		   holder.position = position;
		   
		   return convertView;
	   }
	   
	   /**
	    * When the user clicks on the category reload the feed by setting up a new
	    * AsyncTask and then refresh the Article list
	    */
	   @Override
	   public void onClick(View v) {
		   ViewHolder holder = (ViewHolder) v.getTag();
		   
		   // Close the drawer
		   slidingDrawer.animateClose();
		   
	       // Set the title and the category
	       currentNewsSource.setCurrentCategoryIndex(holder.position);
	       setTitle(currentNewsSource.getTitle() + " : " + currentNewsSource.getCurrentCategoryName());
	        
	       // Clear old articles
	       adapter.feed.clear();
	       
	       // Load feed will automatically load the correct feed as per the current newssource
	       loadFeed();
	   }
   }
   
   /**
    * ArrayList adapter for Articles
    * @author vamsi
    *
    */
   private class FeedListAdapter extends ArrayAdapter<Article> implements View.OnClickListener {
	   
	   private Feed feed;
	   private Context context;
	   
	   public FeedListAdapter(Context context, int textViewResourceId, Feed feed) {
		   super(context, textViewResourceId, feed.getItems());
		   
		   this.feed = feed;
		   this.context = context;
		   setNotifyOnChange(true);
	   }
	   
	   /**
	    * Item's View
	    */
	   @Override
	   public View getView(int position, View convertView, ViewGroup parent) {
		   ViewHolder holder;
		   
		   // First time creating the holder. Inflate the views only once with this pattern.
		   if (convertView == null) {
			   LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			   convertView = inflater.inflate(R.layout.articles_list_item, null);
			   
			   holder = new ViewHolder();
			   holder.title = (TextView) convertView.findViewById(R.id.ArticleTitle);
			   holder.description = (TextView) convertView.findViewById(R.id.ArticleDescription);
			   
			   convertView.setTag(holder);
		    } else {
			   holder = (ViewHolder) convertView.getTag();
		    }
		   
		   convertView.setOnClickListener(this);
		   
		   holder.title.setText(feed.getItem(position).getTitle());
		   
		   if (Utils.isGoogleNewsFeed(feed.getUrl())) {
			   holder.description.setText(Html.fromHtml(feed.getItem(position).getDescription()));
		   } else {
			   holder.description.setText((feed.getItem(position).getDescription()));
		   }
		   holder.position = position;

		   return convertView;
	   }

	   public int getCount() {
		   return feed.size();
	   }
	   
	   public Article getItem(int position) {
		   return feed.getItem(position);
	   }
	   
	   public long getItemId(int position) {
		   return position;
	   }

	    /**
	     *  Open the browser when the user clicks on the article
	     */
		@Override
		public void onClick(View view) {
			ViewHolder holder = (ViewHolder) view.getTag();
			final Context application = context;
						
			// Get the View of the dropDownBar
			View dropDownBar = view.findViewById(R.id.dropDownBar);
			
			// On Click we show a DropDownBar and remove it after a few seconds
			// First we remove the bottom drawable of the description textview
			// Enable visibility on the bottom bar and then
        	// Set a timer to make the drop down bar go away after a few seconds
        	DropDownBarTimer timer = new DropDownBarTimer(DROPDOWNBAR_TIMEOUT, DROPDOWNBAR_TIMEOUT, view);
        	timer.start();
        	
        	// Set the functionality of the drop down bar
        	// The share button
        	Button shareButton = (Button) dropDownBar.findViewById(R.id.shareButton);
        	shareButton.setTag(holder.position);
        	shareButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Track the event of article being spoken out
					tracker.trackEvent("article", "share", currentNewsSource.getTitle());
					
					Integer position = (Integer) v.getTag();
					
					Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
					shareIntent.setType("text/plain");
					shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getBaseContext().getString(R.string.article_share_subject) +
																" - " +
																feed.getItem(position).getTitle());
					shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, Utils.generateTinyUrl(feed.getItem(position).getUrl()));
					startActivity(Intent.createChooser(shareIntent, application.getString(R.string.article_share_dialog_title)));
				}	
        	});
        
        	// The browser button
        	Button browserButton = (Button) dropDownBar.findViewById(R.id.browserButton);
        	browserButton.setTag(holder.position);
        	browserButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					
					// Track the event of browser being opened to read
					tracker.trackEvent("article", "browser", currentNewsSource.getTitle());
			        
					Uri uri = Uri.parse(adapter.getItem(position).getUrl());
					Intent webViewIntent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(Intent.createChooser(webViewIntent, "Open this article in"));
				}
			});
        	
        	// The speak button
        	Button speakButton = (Button) dropDownBar.findViewById(R.id.speakButton);
        	speakButton.setTag(holder.position);
        	speakButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					if (!feed.getItem(position).isDownloaded()) {
						   // Spinning wheel to show while the article is being downloaded
						   Utils.showSpinningWheel(FeedViewerActivity.this, "", getString(R.string.loading_articles));
						   
						   // Start the article download
						   @SuppressWarnings("rawtypes")
						   AsyncTask task = new PlaudibleAsyncTask().execute(
								   new PlaudibleAsyncTask.Payload(
										   PlaudibleAsyncTask.ARTICLE_DOWNLOADER_TASK,
										   new Object[] { FeedViewerActivity.this,
												   			position,
												   			feed,
												   			currentNewsSource }));
						    
						    // Track the event of article being spoken out
							tracker.trackEvent("article", "speak", currentNewsSource.getTitle());
					        
						    // Wait on the task to get completed
							try {
								task.get();
							} catch (InterruptedException e) {
								showToast("Downloading error");
							} catch (ExecutionException e) {
								showToast("Downloading error");
							}
							// Suspend the spinning wheel
							Utils.suspendSpinningWheel();
					   	}
						// Send the article for reading
						sendArticleForReading(feed.getItem(position));
						
						// Display the bottom bar
						displayBottomBar();
				}
			});
        	
        	
        	// The text only button
        	Button textOnlyButton = (Button) dropDownBar.findViewById(R.id.textOnlyButton);
        	textOnlyButton.setTag(holder.position);
        	textOnlyButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					
					// Track the event of article being read in text only
					tracker.trackEvent("article", "textonly", currentNewsSource.getTitle());
			        
					// Start a loading dialog
					Utils.showSpinningWheel(FeedViewerActivity.this, "", getString(R.string.no_articles_to_show));
					
					// Start the ArticleViewer activity
					Intent articleViewerLaunchIntent = new Intent();
					articleViewerLaunchIntent.setClass(getContext(), ArticleViewerActivity.class);
					articleViewerLaunchIntent.putExtra(ArticleViewerActivity.INTENT_CURRENT_SOURCE, currentNewsSource); // The source of this article
					articleViewerLaunchIntent.putExtra(ArticleViewerActivity.INTENT_FEED, feed);
					articleViewerLaunchIntent.putExtra(ArticleViewerActivity.INTENT_CURRENT_ARTILCE_INDEX, position);
					
					startActivity(articleViewerLaunchIntent);
				}
			});
        	
        	Button playlistButton = (Button) dropDownBar.findViewById(R.id.playlistButton);
        	playlistButton.setTag(holder.position);
        	playlistButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(application, "Bookmarking is coming soon in future updates", Toast.LENGTH_LONG).show();
				}
			});
        	
        	// End of onClick stuff
		}
   }
   
   	/**
    * Show the toast for error messages
    * @param toastText
    */
   	public void showToast(String toastText) {
	   Toast burntToast = Toast.makeText(getBaseContext(), toastText, Toast.LENGTH_SHORT);
	   burntToast.show();
   	}
	
	/**
	 *  Send an article to the service so it can begin reading
	 * @param article
	 * @param newsSource
	 */
	public void sendArticleForReading(Article article) {
		if (mSpeechService != null) {
			mSpeechService.startReadingArticle(article);
		}
	}
	
	/**
	 * Display the bottom bar which contains the pause/play button
	 */
	public void displayBottomBar() {
		
		// See if an article is with the SpeechService (paused or not) doesn't matter
		if (mSpeechService != null && mSpeechService.isReading()) {
        	View bottomBar = findViewById(R.id.articlelistbottombar);
        	bottomBar.setVisibility(View.VISIBLE);
        	
        	// Set the text on the bottom bar
        	TextView bottomBarText = (TextView) findViewById(R.id.articleListBottomBarText);
        	String text = null;
        	try {
        		text = mSpeechService.getCurrentItem().getSource().getTitle() + " - " + mSpeechService.getCurrentItem().getTitle();
        	} catch (Exception e) {
        		text = "";
        	} finally {
        		bottomBarText.setText(text);
        	}
        	
        	// Set the correct play/pause icon on the bottom bar
        	Button pauseButton = (Button) findViewById(R.id.articleListBottomBarPauseButton);
        	if (mSpeechService.isReading()) {
        		pauseButton.setText("Pause");
            } else {
            	pauseButton.setText("Play");
        	}
        	
        	final View speechControllerBar = bottomBar; 
        	pauseButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Button button = (Button) v;
					if (mSpeechService.isReading()) {
						mSpeechService.pauseReading();
		        		button.setText("Play");
		            } else {
		            	mSpeechService.resumeReading();
		            	button.setText("Pause");
		        	}
				}
			});
        	
        	Button stopButton = (Button) findViewById(R.id.articleListBottomBarStopButton);
        	stopButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mSpeechService.isReading()) {
						mSpeechService.stopReading();
						View bottomBar = findViewById(R.id.articlelistbottombar);
						bottomBar.setVisibility(View.GONE);
					}
				}
			});
        }
		
	}
	
	/**
	 * Timer for the drop down bar shown on the item view
	 * @author vamsi
	 *
	 */
	public class DropDownBarTimer extends CountDownTimer {
		/**
		 * The view of the drop down bar, the article item
		 */
		private View view;
		private ViewHolder holder;
		private View dropDownBar;
		
		public DropDownBarTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		
		public DropDownBarTimer(long millisInFuture, long countDownInterval, View v) {
			super(millisInFuture, countDownInterval);

			// The view of the article item on which click was received 
			view = v;
			holder = (ViewHolder) v.getTag();
			
			// Remove the bottom drawable of the TextView for description
			holder.description.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.up_arrow, 0);
			
			// Make the drop down bar visible
			dropDownBar = view.findViewById(R.id.dropDownBar);
			dropDownBar.setVisibility(View.VISIBLE);
 		}
		
		/**
		 *  Remove the drop down bar now
		 */
		public void onFinish() {
			// Put back the down arrow drawable for description TextView
			holder.description.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.down_arrow, 0);
			
			// Remove the dropDownBar
			dropDownBar.setVisibility(View.GONE);
		}
		
		/**
		 *  Do nothing here
		 */
		public void onTick(long millisUntilFinished) {
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
		this.bindService(new Intent(FeedViewerActivity.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
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