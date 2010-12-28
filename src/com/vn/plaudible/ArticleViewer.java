package com.vn.plaudible;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * View a text only version of the article for fast reading.
 * @author vamsi
 *
 */
public class ArticleViewer extends Activity {

	private String articleText;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.article_viewer);
        
        WebView webview = new WebView(this);
        setContentView(webview);
        
        // Get the intent and the related extras
        Intent intent = this.getIntent();
        articleText = intent.getStringExtra("content");
        
        /*TextView articleTextView = (TextView) this.findViewById(R.id.ArticleText);
        articleTextView.setText(articleText);*/
        
        webview.loadData(articleText, "text/html", "utf-8");
    }
}
