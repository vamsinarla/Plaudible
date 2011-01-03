package com.vn.plaudible;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * View a text only version of the article for fast reading.
 * @author vamsi
 *
 */
public class ArticleViewer extends Activity {

	private Article article;
	private ProgressDialog spinningWheel;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Show progress dialog
        showSpinningWheel();
        
        WebView webview = new WebView(this);
        setContentView(webview);
        
        // Get the intent and the related extras
        Intent intent = this.getIntent();
        article = (Article) intent.getSerializableExtra("Article");
        
        // Set the title
        setTitle(article.getTitle());
        
        // Load the page
        webview.loadUrl(article.getUrl());
        
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
}
