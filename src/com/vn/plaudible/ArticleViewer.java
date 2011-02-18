package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EncodingUtils;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ViewSwitcher;

/**
 * View a text only version of the article for fast reading.
 * @author vamsi
 *
 */
public class ArticleViewer extends Activity {

	private static final long TIMEOUT = 5000;
	
	private String source;
	private String appEngineUrl;
	
	private ArrayList<Article> articles;
	private Integer currentArticleIndex;
	
	private WebView webview1;
	private WebView webview2;
	private WebView currentWebView;
	private ViewSwitcher switcher;
	private ImageButton shareButton;
	private ImageButton nextArticleButton;
	private ImageButton previousArticleButton;
	
	private ViewVisibilityController viewVisibilityController;
	
	/** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Expand the layout
        setContentView(R.layout.article_viewer);
        
        // Get the intent and the related extras
        Intent intent = this.getIntent();
        articles = (ArrayList<Article>) intent.getSerializableExtra("articles");
        currentArticleIndex = (Integer) intent.getIntExtra("currentArticleIndex", 0);
        source = (String) intent.getStringExtra("source");
        
        // Share button stuff
        shareButton = (ImageButton) this.findViewById(R.id.articleshareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");

				// Get a tiny url
				String shortUrl = generateTinyUrl(articles.get(currentArticleIndex).getUrl());
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, articles.get(currentArticleIndex).getTitle() + " " + shortUrl);
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.article_share_subject));
				
				startActivity(Intent.createChooser(shareIntent, "Share using"));
			}
		});
        
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
        viewVisibilityController.addView(nextArticleButton);
        viewVisibilityController.addView(previousArticleButton);
        viewVisibilityController.addView(shareButton);
        
        // WebView stuff
        webview1 = (WebView) this.findViewById(R.id.webview1);
        webview1.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
		        case MotionEvent.ACTION_DOWN:
		        		viewVisibilityController.setViewVisibility(View.VISIBLE);
		        		
		        		ArticleViewerUIOverlayTimer timer = new ArticleViewerUIOverlayTimer(TIMEOUT, viewVisibilityController);
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
		        		
			        	ArticleViewerUIOverlayTimer timer = new ArticleViewerUIOverlayTimer(TIMEOUT, viewVisibilityController);
		        		timer.start();
		                break;
				}
				return false;
			}
		});
        
        // Set currentWebView
        currentWebView = webview1;
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    /**
     * Move to display next article
     */
    protected void moveToNextArticle() {
    	if (currentArticleIndex < articles.size() - 1) {
    		++currentArticleIndex;
	    	
    		swapWebView();
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
	    	
    		swapWebView();
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
        setTitle(articles.get(index).getTitle());
        
    	// Construct the AppEngine URL for the ArticleServlet
    	appEngineUrl = getString(R.string.appengine_url) + "/article2";
    	String articleUrl = articles.get(index).getUrl(); 
    	
        // Load the page from app Engine's article servlet
        String postData = URLEncoder.encode("source") + "=" + URLEncoder.encode(source) + "&";
        postData += URLEncoder.encode("type") + "=" + URLEncoder.encode("html") + "&"; // Default to using HTML in text only
        postData += URLEncoder.encode("link") + "=" + URLEncoder.encode(articleUrl);
        
        currentWebView.postUrl(appEngineUrl, EncodingUtils.getBytes(postData, "BASE64"));
    }
    
    /**
     * Class to make a list of views visible or invisible
     */
    class ViewVisibilityController {
    	
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
		public void addView(View newView) {
			views.add(newView);
		}
		
		/**
		 * Make visible or invisible
		 */
		public void setViewVisibility(int newState) {
			newState = ((newState == View.GONE) ? View.GONE : View.VISIBLE);
			for (int index = 0; index < views.size(); ++index) {
				views.get(index).setVisibility(newState);
			}
		}
    }
  
	/**
	 * Timer to make the overlay UI like share button disappear	
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
			viewVisibilityController.setViewVisibility(View.GONE);
		}
		/**
		 *  Do nothing here
		 */
		public void onTick(long millisUntilFinished) {
		}
	}
	
	/**
	 * Utility function to generate a TinyURL useful for sharing links
	 * @param url
	 * @return
	 */
	static String generateTinyUrl(String url) {
		String tinyUrl;
		try {
            HttpClient client = new DefaultHttpClient();
            String urlTemplate = "http://tinyurl.com/api-create.php?url=%s";
            String uri = String.format(urlTemplate, URLEncoder.encode(url));
            HttpGet request = new HttpGet(uri);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            try {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    // TODO: Support other encodings
                    String enc = "utf-8";
                    Reader reader = new InputStreamReader(in, enc);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    tinyUrl = bufferedReader.readLine();
                    if (tinyUrl != null) {
                        return tinyUrl;
                    } else {
                        throw new IOException("empty response");
                    }
                } else {
                    String errorTemplate = "unexpected response: %d";
                    String msg = String.format(errorTemplate, statusCode);
                    throw new IOException(msg);
                }
            } finally {
                in.close();
            }
        } catch (Exception exception) {
        	// In case we couldn't generate a short URL send the original back
        	return url;
        }
	}
}
