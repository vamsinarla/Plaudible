package com.vn.plaudible;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Plaudible extends ListActivity {
	
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

	/**
	 * Google Analytics
	 */
	GoogleAnalyticsTracker tracker;
	
	private ArrayList<Article> articles;
	private ArticleListAdapter adapter;
	private NewsSource currentNewsSource;
	private SpeechService mSpeechService;
	private ProgressDialog spinningWheel;
	
	private SlidingDrawer slidingDrawer;
	
	private static final int DOWNLOADING_TIMEOUT = 10000;
	private static final int DROPDOWNBAR_TIMEOUT = 7000;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set view related stuff
        setContentView(R.layout.articles_list);
 
        // Get the analytics tracker instance
        tracker = GoogleAnalyticsTracker.getInstance();
        
        // Start the tracker in auto dispatch mode to update every 60 seconds
        tracker.start(getString(R.string.analytics_id), 60, this);
        
        // Show the spinning wheel and the counter, which suspends the wheel in TIMEOUT milli-seconds
        showSpinningWheel("", getString(R.string.loading_articles), DOWNLOADING_TIMEOUT);
        
        // Get the intent and the related extras
        Intent intent = getIntent();
        currentNewsSource = (NewsSource) intent.getSerializableExtra("NewsSource");
        
        // Track this news source being opened
        tracker.trackPageView("/news/" + currentNewsSource.getTitle());
        
        // Enable the sliding drawer if there are categories for this source
        slidingDrawer = (SlidingDrawer) findViewById(R.id.SlidingDrawer);
        
        if (currentNewsSource.isHasCategories()) {
    		// Setup sliding drawer with a listview with the categories
            setupSlidingDrawer();
    		setTitle(currentNewsSource.getTitle() + " : " + currentNewsSource.getCurrentCategoryName());
        }
        else {
        	setTitle(currentNewsSource.getTitle());
        }
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Arraylist adapter
        articles = new ArrayList<Article>();
        adapter = new ArticleListAdapter(this, R.layout.articles_list_item, articles);
        
        setListAdapter(adapter);

        new PlaudibleAsyncTask().execute(
        		new PlaudibleAsyncTask.Payload(
        				PlaudibleAsyncTask.FEED_DOWNLOADER_TASK,
        				new Object[] { Plaudible.this,
        								currentNewsSource.getTitle(),
        								(currentNewsSource.isHasCategories() ? 
        								 currentNewsSource.getCurrentCategoryName() : null), 
        								articles }));
        bindSpeechService();
    }
	
    @Override
    protected void onDestroy() {
    	unBindSpeechService();
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
    *  Used by the AsyncTask to update the set of articles
    * @param articles
    */
   public void setArticles(ArrayList<Article> articles) {
	   this.articles = articles;   
	   adapter.notifyDataSetChanged();
	   
	   // Articles were fetched so we can suspend the wheel
	   suspendSpinningWheel();
	   
	   // Bottom bar
	   displayBottomBar();
   }
   
   /**
    * Setup the sliding drawer to show categories
    */
   private void setupSlidingDrawer() {
	   	// Make it visible
		slidingDrawer.setVisibility(View.VISIBLE);
	    
	   	ListView categoriesListView = (ListView) findViewById(R.id.categoriesListView);
	   	categoriesListView.setAdapter(new CategoryListAdapter(Plaudible.this, 
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
		   
	       // Show the spinning wheel and the counter, which suspends the wheel in TIMEOUT milli-seconds
	       showSpinningWheel("", getString(R.string.loading_articles), DOWNLOADING_TIMEOUT);
	       
	       // Set the title and the category
	       currentNewsSource.setCurrentCategoryIndex(holder.position);
	       setTitle(currentNewsSource.getTitle() + " : " + currentNewsSource.getCurrentCategoryName());
	        
	       // Clear old articles
	       articles.clear();
	       
	       new PlaudibleAsyncTask().execute(
	        		new PlaudibleAsyncTask.Payload(
	        				PlaudibleAsyncTask.FEED_DOWNLOADER_TASK,
	        				new Object[] { Plaudible.this,
	        								currentNewsSource.getTitle(),
	        								(currentNewsSource.isHasCategories() ? // Check category existence
	        								 categories.get(holder.position) : null),
	        								 articles }));
	   }
   }
   
   /**
    * ArrayList adapter for Articles
    * @author vamsi
    *
    */
   private class ArticleListAdapter extends ArrayAdapter<Article> implements View.OnClickListener {
	   
	   ArrayList<Article> articles;
	   
	   public ArticleListAdapter(Context context, int textViewResourceId, ArrayList<Article> articles) {
		   super(context, textViewResourceId, articles);
		   
		   this.articles = articles;
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
		   
		   holder.title.setText(articles.get(position).getTitle());
		   holder.description.setText(articles.get(position).getDescription());
		   holder.position = position;

		   return convertView;
	   }

	   public int getCount() {
		   return articles.size();
	   }
	   
	   public Article getItem(int position) {
		   return articles.get(position);
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
						
			// Get the View of the dropDownBar
			View dropDownBar = view.findViewById(R.id.dropDownBar);
			
			// On Click we show a DropDownBar and remove it after a few seconds
			// First we remove the bottom drawable of the description textview
			// Enable visibility on the bottom bar and then
        	// Set a timer to make the drop down bar go away after a few seconds
        	DropDownBarTimer timer = new DropDownBarTimer(DROPDOWNBAR_TIMEOUT, DROPDOWNBAR_TIMEOUT, view);
        	timer.start();
        	
        	// Set the functionality of the drop down bar
        	// The browser button
        	ImageButton browserButton = (ImageButton) dropDownBar.findViewById(R.id.browserButton);
        	browserButton.setTag(holder.position);
        	browserButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					
					// Track the event of browser being opened to read
					tracker.trackEvent("article", "browser", currentNewsSource.getTitle(), 0);
			        
					Uri uri = Uri.parse(adapter.getItem(position).getUrl());
					Intent webViewIntent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(Intent.createChooser(webViewIntent, "Open this article in"));
				}
			});
        	
        	// The speak button
        	ImageButton speakButton = (ImageButton) dropDownBar.findViewById(R.id.speakButton);
        	speakButton.setTag(holder.position);
        	speakButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					if (!articles.get(position).isDownloaded()) {
						   // Spinning wheel to show while the article is being downloaded
						   showSpinningWheel("", getString(R.string.loading_articles), DOWNLOADING_TIMEOUT);
						   
						   // Start the article download
						   @SuppressWarnings("rawtypes")
						   AsyncTask task = new PlaudibleAsyncTask().execute(
								   new PlaudibleAsyncTask.Payload(
										   PlaudibleAsyncTask.ARTICLE_DOWNLOADER_TASK,
										   new Object[] { Plaudible.this,
												   			position,
												   			articles,
												   			currentNewsSource.getTitle() }));
						    
						    // Track the event of article being spoken out
							tracker.trackEvent("article", "speak", currentNewsSource.getTitle(), 0);
					        
						    // Wait on the task to get completed
							try {
								task.get();
							} catch (InterruptedException e) {
								showToast("Downloading error");
							} catch (ExecutionException e) {
								showToast("Downloading error");
							}
							// Suspend the spinning wheel
							suspendSpinningWheel();
					   	}
						// Send the article for reading
						sendArticleForReading(articles.get(position), currentNewsSource);
						
						// Display the bottom bar
						displayBottomBar();
				}
			});
        	
        	
        	// The text only button
        	ImageButton textOnlyButton = (ImageButton) dropDownBar.findViewById(R.id.textOnlyButton);
        	textOnlyButton.setTag(holder.position);
        	textOnlyButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					
					// Track the event of article being read in text only
					tracker.trackEvent("article", "textonly", currentNewsSource.getTitle(), 0);
			        
					// Construct the AppEngine URL for the ArticleServlet
					String articleUrl = getString(R.string.appengine_url) + "/article?source=" + 
													currentNewsSource.getTitle();
					articleUrl += "&link=" + articles.get(position).getUrl();
					articleUrl += "&type=html"; // We want HTML for the WebView
					
					// Start the ArticleViewer activity
					Intent listArticlesInFeed = new Intent();
					listArticlesInFeed.setClass(getContext(), ArticleViewer.class);
					listArticlesInFeed.putExtra("Article", articles.get(position));
					listArticlesInFeed.putExtra("ArticleUrl", articleUrl); // The URL which should be opened for the Article
					
					startActivity(listArticlesInFeed);
				}
			});
        	// End of onClick stuff
		}
   }
   
	/**
	 * Show the spinning wheel
	 */
   public void showSpinningWheel(String title, String text, long timeout) {
	   ProgressDialogTimer timer = new  ProgressDialogTimer(timeout, timeout);
       spinningWheel = ProgressDialog.show(Plaudible.this, title, text, true);
       timer.start();
   }
   
   /**
    * Suspend the spinning wheel
    */
   	public void suspendSpinningWheel() {
	   if (spinningWheel.isShowing()) {
		   spinningWheel.cancel();
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
	public void sendArticleForReading(Article article, NewsSource newsSource) {
		if (mSpeechService != null) {
			mSpeechService.readArticle(article, newsSource);
		}
	}
	
	/**
	 * Display the bottom bar which contains the pause/play button
	 */
	public void displayBottomBar() {
		Article article;
		
		// See if an article is with the SpeechService (paused or not) doesn't matter
		if (mSpeechService != null && (article = mSpeechService.getCurrentlyReadArticle()) != null) {
        	View bottomBar = findViewById(R.id.articlelistbottombar);
        	bottomBar.setVisibility(View.VISIBLE);
        	
        	// Set the text on the bottom bar
        	TextView bottomBarText = (TextView) findViewById(R.id.articleListBottomBarText);
        	String text = mSpeechService.getCurrentNewsSource().getTitle() + " - " + article.getTitle();
         	bottomBarText.setText(text);
        	
        	// Set the correct icon on the bottom bar
        	ImageButton bottomBarIcon = (ImageButton) findViewById(R.id.articleListBottomBarIcon);
        	if (mSpeechService.isReading()) {
        		bottomBarIcon.setImageResource(R.drawable.pause64);
            } else {
            	bottomBarIcon.setImageResource(R.drawable.play64);
        	}
        	
        	bottomBarIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ImageButton button = (ImageButton) v;
					if (mSpeechService.isReading()) {
						button.setImageResource(R.drawable.play64);
		        		mSpeechService.pauseReading();
		            } else {
		            	button.setImageResource(R.drawable.pause64);
		            	mSpeechService.resumeReading();
		        	}
				}
			});
        }
		
	}
	
	/**
	 * Timer for the progress dialog
	 * @author vamsi
	 *
	 */
	private class ProgressDialogTimer extends CountDownTimer {
		public ProgressDialogTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		/**
		 *  Suspend the spinning wheel after some time.
		 */
		public void onFinish() {
			suspendSpinningWheel();
			displayBottomBar();
		}
		/**
		 *  Do nothing here
		 */
		public void onTick(long millisUntilFinished) {
			
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
			holder.description.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			
			// Make the drop down bar visible
			dropDownBar = view.findViewById(R.id.dropDownBar);
			dropDownBar.setVisibility(View.VISIBLE);
 		}
		/**
		 *  Remove the drop down bar now
		 */
		public void onFinish() {
			// Put back the down arrow drawable for description TextView
			holder.description.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, android.R.drawable.arrow_down_float);
			
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
		this.bindService(new Intent(Plaudible.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
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