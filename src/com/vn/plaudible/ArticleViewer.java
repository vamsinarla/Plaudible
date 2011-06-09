package com.vn.plaudible;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.util.EncodingUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * View a text only version of the article for fast reading.
 * @author vamsi
 *
 */
public class ArticleViewer extends Activity {

	// Timeout in ms for removing the overlay UI
	private static final long VISIBILITY_TIMEOUT = 3000;
	
	private NewsSource currentNewsSource;
	private ArrayList<Article> articles;
	private Integer currentArticleIndex;
	
	private TextView title;
	private WebView webview1;
	private WebView webview2;
	private WebView currentWebView;
	private ViewSwitcher switcher;
	
	private ImageButton nextArticleButton;
	private ImageButton previousArticleButton;
	
	private ViewVisibilityController viewVisibilityController;
	
	/**
	 * Google Analytics
	 */
	GoogleAnalyticsTracker tracker;

	protected SpeechService mSpeechService;
	
	/** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove the Title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Expand the layout
        setContentView(R.layout.article_viewer);
        
        // Get the analytics tracker instance
        tracker = GoogleAnalyticsTracker.getInstance();
        
        // Start the tracker in auto dispatch mode to update every 60 seconds
        tracker.start(getString(R.string.analytics_id), 60, this);
        
        // Get the intent and the related extras
        Intent intent = this.getIntent();
        articles = (ArrayList<Article>) intent.getSerializableExtra("articles");
        currentArticleIndex = (Integer) intent.getIntExtra("currentArticleIndex", 0);
        currentNewsSource = (NewsSource) intent.getSerializableExtra("source");
        
        // Next article button stuff
        nextArticleButton = (ImageButton) findViewById(R.id.nextArticleButton);
        nextArticleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArticleViewer.this.moveToNextArticle();
			}
		});
        
        // Next article button stuff
        previousArticleButton = (ImageButton) findViewById(R.id.previousArticleButton);
        previousArticleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArticleViewer.this.moveToPreviousArticle();
			}
		});
        
        // View Switching for reading next & previous articles
        switcher = (ViewSwitcher) this.findViewById(R.id.articleSwitcher);
        
        // Add all overlay views to the visibility controller
        // These views need to be shown in a timed fashion
        viewVisibilityController = new ViewVisibilityController();
        viewVisibilityController.registerView(nextArticleButton);
        viewVisibilityController.registerView(previousArticleButton);
        // viewVisibilityController.registerView(shareButton);
        
        // WebView stuff
        // We are creating two WebViews here to switch between two articles.
        webview1 = (WebView) this.findViewById(R.id.webview1);
        webview1.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
		        case MotionEvent.ACTION_DOWN:
		        		viewVisibilityController.setViewVisibility(View.VISIBLE);
		        		openOptionsMenu();
		        		
		        		ArticleViewerUIOverlayTimer timer = new ArticleViewerUIOverlayTimer(VISIBILITY_TIMEOUT, viewVisibilityController);
		        		timer.start();
		                break;
				}
				return false;
			}
		});
        webview2 = (WebView) this.findViewById(R.id.webview2);
        webview2.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
		        case MotionEvent.ACTION_DOWN:
			        	viewVisibilityController.setViewVisibility(View.VISIBLE);
		        		openOptionsMenu();
		        		
			        	ArticleViewerUIOverlayTimer timer = new ArticleViewerUIOverlayTimer(VISIBILITY_TIMEOUT, viewVisibilityController);
		        		timer.start();
		                break;
				}
				return false;
			}
		});
        
        // Set currentWebView
        currentWebView = webview1;
        
        // Bind speech service
        bindSpeechService();
        
        // Get the title textview
        title = (TextView) this.findViewById(R.id.articleViewerTitle);
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    /**
     * Move to display next article
     */
    protected void moveToNextArticle() {
    	if (currentArticleIndex < articles.size() - 1) {
    		++currentArticleIndex;
    		
	    	// Swap webviews 
    		swapWebView();
    		
    		// Play switching animation. Move the new article
    		// in from the right to the left
    		switcher.setInAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.push_left_in));
    		switcher.setOutAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.push_left_out));
    		displayArticle(currentArticleIndex);
    		switcher.showPrevious();
    	}
	}

	/**
     * Move to display previous article
     */
	protected void moveToPreviousArticle() {
		if (currentArticleIndex > 0) {
    		--currentArticleIndex;
	    	
    		// Swap webviews
    		swapWebView();
    		
    		// Play switching animation. Move the new article
    		// in from the left to the right
    		switcher.setInAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.push_right_in));
    		switcher.setOutAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.push_right_out));
    		displayArticle(currentArticleIndex);
    		switcher.showNext();
		}
	}
	
	/**
	 * Swap the webviews after clearing the older content
	 */
    private void swapWebView() {
    	if (currentWebView == webview1) {
    		webview2.clearView();
    		currentWebView = webview2;
    	} else {
    		webview1.clearView();
    		currentWebView = webview1;
    	}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.article_viewer_menu, menu);
        return true;
    }
    
    /**
     * Handle the options menu
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Article currentArticle = articles.get(currentArticleIndex);
    	
        switch (item.getItemId()) {
            case R.id.article_share:
            	Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");

				// Get a tiny url
				String shortUrl = Utils.generateTinyUrl(currentArticle.getUrl());
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
												getString(R.string.article_share_subject) + 
												currentArticle.getTitle());
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shortUrl);
				
				startActivity(Intent.createChooser(shareIntent, "Share using"));
                break;
            case R.id.article_speak:
				try {
					getArticleContent();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	mSpeechService.readArticle(currentArticle);
                break;
            case R.id.article_webpage:
            	// Track the event of browser being opened to read
				tracker.trackEvent("article", "browser", currentNewsSource.getTitle(), 0);
		        
				Uri uri = Uri.parse(currentArticle.getUrl());
				Intent webViewIntent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(Intent.createChooser(webViewIntent, "Open this article in"));
            	break;
        }
        return true;
    }
    
	/**
     * Show the article here
     */
    @Override
    public void onResume() {
    	super.onResume();
    	
    	displayArticle(currentArticleIndex);
    }
    
    /**
     * Display's the current article after fetching from appEngine
     * @param index
     */
    private void displayArticle(Integer index) {
    	// Set the title
    	title.setText(articles.get(index).getTitle());
        
    	// Construct the AppEngine URL for the ArticleServlet
    	String appEngineUrl = getString(R.string.appengine_url) + "/article2";
    	String articleUrl = articles.get(index).getUrl(); 
    	
        // Load the page from app Engine's article servlet
        String postData = URLEncoder.encode("currentNewsSource") + "=" + URLEncoder.encode(currentNewsSource.getTitle()) + "&";
        postData += URLEncoder.encode("type") + "=" + URLEncoder.encode("html") + "&"; // Default to using HTML in text only
        postData += URLEncoder.encode("link") + "=" + URLEncoder.encode(articleUrl);
        
        currentWebView.postUrl(appEngineUrl, EncodingUtils.getBytes(postData, "BASE64"));
        
        // Collect analytics
        // Track the event of article being read in text only mode
		tracker.trackEvent("article", "textonly", currentNewsSource.getTitle(), 0);
    }
    
    /**
     * Get the content of the article in the desired format
     * @param format
     * @throws IOException 
     */
    private void getArticleContent() throws IOException {
    	Article currentArticle = articles.get(currentArticleIndex);
    	
    	// Construct the AppEngine URL for the ArticleServlet
		String link = getString(R.string.appengine_url) + "article2";
		
		String postArgs = URLEncoder.encode("source", "UTF-8") + "=" + URLEncoder.encode(currentNewsSource.getTitle(), "UTF-8") + "&";
		postArgs += URLEncoder.encode("link", "UTF-8") + "=" + URLEncoder.encode(currentArticle.getUrl(), "UTF-8") + "&";
		postArgs += URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("text", "UTF-8");
		
		// Get the response from AppEngine
		URL articleUrl = new URL(link);
		URLConnection conn = articleUrl.openConnection();
		Utils.postVars(conn, postArgs);
		
		// Set the params for the article and mark as downloaded
		currentArticle.setContent(Utils.getStringFromInputStream(conn.getInputStream()));
		currentArticle.setDownloaded(true);
    }
    
    /**
     * Class to make a list of views visible or invisible
     */
    class ViewVisibilityController {
    	// Hold all the views registered with the controller
    	private ArrayList<View> views;
		
    	/**
    	 * Ctor
    	 */
    	public ViewVisibilityController() {
    		views = new ArrayList<View>();
    	}
    	
    	/**
		 * Add views to the array list
		 * @param newView
		 */
		public void registerView(View newView) {
			views.add(newView);
		}
		
		/**
		 * Remove or unregister views from the controller
		 */
		public void unRegisterView(View view) {
			views.remove(view);
		}
		
		/**
		 * Make visible or invisible all UI widgets registered in the controller
		 */
		public void setViewVisibility(int newState) {
			newState = ((newState == View.GONE) ? View.GONE : View.VISIBLE);
			for (int index = 0; index < views.size(); ++index) {
				views.get(index).setVisibility(newState);
			}
		}
    }
  
	/**
	 * Timer to make the overlay UI disappear	
	 * @author vamsi
	 *
	 */
	class ArticleViewerUIOverlayTimer extends CountDownTimer {
		
		private ViewVisibilityController viewVisibilityController;
		
		/**
		 * The view of the share button
		 * @param viewVisibilityController 
		 */
		public ArticleViewerUIOverlayTimer(long millisInFuture, ViewVisibilityController viewVisibilityController) {
			super(millisInFuture, millisInFuture);
			this.viewVisibilityController = viewVisibilityController;
		}
		
		/**
		 *  Remove the overlay UI
		 */
		public void onFinish() {
			closeOptionsMenu();
			viewVisibilityController.setViewVisibility(View.GONE);
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
		this.bindService(new Intent(ArticleViewer.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
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
