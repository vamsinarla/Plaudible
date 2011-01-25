package com.vn.plaudible;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Search Page 
 * @author vnarla
 *
 */
public class SearchPage extends Activity {

	protected static final long SEARCHING_TIMEOUT = 10000;

	private ProgressDialog spinningWheel;
	
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
				String searchURLString = getString(R.string.appengine_url) + "";
				
				// Show the spinning wheel and the counter, which suspends the wheel in SEARCHING_TIMEOUT milli-seconds
		        showSpinningWheel("", getString(R.string.searching_spin_text), SEARCHING_TIMEOUT);
				
				try {
					URL searchURL = new URL(searchURLString);
					
				} catch (Exception exception) {
					exception.printStackTrace();
				} finally {
					suspendSpinningWheel();
				}
			}
		});
		
		// Set the behaviour for the add custom feed button
		Button addFeedButton = (Button) findViewById(R.id.add_custom_source);
		addFeedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dialog dialog = new Dialog(SearchPage.this);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.add_custom_feed_dialog);
				dialog.setCancelable(true);
				
				Button cancelButton = (Button) dialog.findViewById(R.id.cancel_dialog);
				cancelButton.setTag(dialog);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Dialog dialog = (Dialog) v.getTag();
						dialog.dismiss();
					}
				});
				
				/* Add feed functionality
					1. Perform data validation
					2. Perform name/url checks
					3. Perform DB update */ 
				Button addFeedButton = (Button) dialog.findViewById(R.id.add_custom_feed);
				addFeedButton.setTag(dialog);
				addFeedButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Dialog dialog = (Dialog) v.getTag();
						
						// EditText views for the title and feedUrl fields
						EditText titleEditBox = (EditText) dialog.findViewById(R.id.custom_feed_title);
						Editable customFeedTitle = titleEditBox.getText();
						
						// Verify the URL
						EditText feedURLEditBox = (EditText) dialog.findViewById(R.id.custom_feed_url);
						
						try {
							URL feedURL = new URL(feedURLEditBox.getText().toString());
						} catch (MalformedURLException exception) {
							Toast burntToast = Toast.makeText(dialog.getContext(), R.string.invalid_url_message, Toast.LENGTH_SHORT);
						}	
					}
				});
				
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
	
	/**
	 * Timer class for suspending the spinner
	 * @author narla
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
			Toast burntToast = Toast.makeText(SearchPage.this, getString(R.string.search_failed), Toast.LENGTH_SHORT);
			burntToast.show();
		}
		/**
		 *  Do nothing here
		 */
		public void onTick(long millisUntilFinished) {
		}
	}
	
	/**
	 * Show the spinning wheel
	 */
   public void showSpinningWheel(String title, String text, long timeout) {
	   ProgressDialogTimer timer = new  ProgressDialogTimer(timeout, timeout);
       spinningWheel = ProgressDialog.show(SearchPage.this, title, text, true);
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
}
