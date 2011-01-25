package com.vn.plaudible;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

/**
 * Search Page 
 * @author vnarla
 *
 */
public class SearchPage extends Activity {

	// Adapter holding the search results
	private SearchResultsAdapter mResultsAdapter;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.search_page);
		
		ArrayList<NewsSource> sources = new ArrayList<NewsSource>();
		mResultsAdapter = new SearchResultsAdapter(this, R.layout.search_page, sources);
		
		// Get the listview in this page
		ListView list = (ListView) findViewById(R.id.results_list);
		list.setAdapter(mResultsAdapter);
		
		// Set the behaviour for the search button
		ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				
			}
		});
		
		// Set the behaviour for the add custom feed button
		Button addFeedButton = (Button) findViewById(R.id.add_custom_source);
		addFeedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dialog dialog = new Dialog(SearchPage.this);
				dialog.setContentView(R.layout.add_custom_feed_dialog);
				dialog.setCancelable(true);
				
				
				dialog.show();
			}
		});
		
		// Set the behaviour for the suggest button
		Button suggestButton = (Button) findViewById(R.id.suggest_feed);
		suggestButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent myIntent = new Intent(android.content.Intent.ACTION_SEND);
				String to[] = { "vamsi.narla+suggest@gmail.com" };

				myIntent.setType("plain/text");

				myIntent.putExtra(android.content.Intent.EXTRA_EMAIL, to);
				myIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
									SearchPage.this.getString(R.string.feedback_subject));
				
				startActivity(myIntent);
			}
		});
		
	}
	
	/**
	 * Class that handles the adapter for the search results
	 * @author vnarla
	 *
	 */
	private class SearchResultsAdapter extends ArrayAdapter<NewsSource> {

		private ArrayList<NewsSource> sources;
		
		public SearchResultsAdapter(Context context, int resource, ArrayList<NewsSource> objects) {
			super(context, resource, objects);
			sources = objects;
		}
		
		
	}
}
