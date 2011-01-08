package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;

/**
 * View a text only version of the article for fast reading.
 * @author vamsi
 *
 */
public class ArticleViewer extends Activity {

	private static final long TIMEOUT = 5000;
	
	private Article article;
	private String articleUrl;
	private ProgressDialog spinningWheel;
	private ImageButton shareButton;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Expand the layout
        setContentView(R.layout.article_viewer);
        
        // Share button stuff
        shareButton = (ImageButton) this.findViewById(R.id.articleshareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");

				// Get a tiny url
				String shortUrl = generateTinyUrl(article.getUrl());
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, article.getTitle() + " " + shortUrl);
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.article_share_subject));
				
				startActivity(Intent.createChooser(shareIntent, "Share using"));
			}
		});
        
        // WebView stuff
        WebView webview = (WebView) this.findViewById(R.id.webview);
        webview.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
		        case MotionEvent.ACTION_DOWN:
		        		shareButton.setVisibility(View.VISIBLE);
		        		ArticleViewerUITimer timer = new ArticleViewerUITimer(TIMEOUT, TIMEOUT, 
		        												shareButton);
		        		timer.start();
		                break;
				}
				return false;
			}
		});
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Show progress dialog
        showSpinningWheel();
        
        // Get the intent and the related extras
        Intent intent = this.getIntent();
        article = (Article) intent.getSerializableExtra("Article");
        articleUrl = (String) intent.getStringExtra("ArticleUrl");
        
        // Set the title
        setTitle(article.getTitle());
        
        // Load the page from app Engine's article servlet
        webview.loadUrl(articleUrl);
        
        // Suspend progress dialog
        suspendSpinningWheel();
    }
    
    /**
	 * Show the spinning wheel
	 */
   public void showSpinningWheel() {
	   spinningWheel = ProgressDialog.show(ArticleViewer.this, "", "Loading article ...", true);
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
	 * Timer to make the overlay UI like share button disappear	
	 * @author vamsi
	 *
	 */
	class ArticleViewerUITimer extends CountDownTimer {
		/**
		 * The view of the share button
		 */
		private View view;
		
		public ArticleViewerUITimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		
		public ArticleViewerUITimer(long millisInFuture, long countDownInterval, View v) {
			super(millisInFuture, countDownInterval);
			view = v;
		}
		/**
		 *  Remove the overlay UI
		 */
		public void onFinish() {
			view.setVisibility(View.GONE);
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
