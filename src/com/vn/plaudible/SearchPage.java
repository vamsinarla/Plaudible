package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.vn.plaudible.NewsSource.SourceType;

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
	
	static class ViewHolder {
		TextView title;
		Button addButton; 
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.search_page);
		
		ArrayList<String> results = new ArrayList<String>();
		mResultsAdapter = new SearchResultsAdapter(this, R.layout.search_page, results);
		
		// Get the listview in this page
		ListView list = (ListView) findViewById(R.id.results_list);
		list.setAdapter(mResultsAdapter);
		
		// Set the behaviour for the search button
		ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				JSONObject searchResults = null;
				
				EditText searchEditText = (EditText) SearchPage.this.findViewById(R.id.search_box);
				String searchTerm = searchEditText.getEditableText().toString();
				
				String searchURLString = getString(R.string.appengine_url) + "search?searchTerm=";
				searchURLString += searchTerm;
				
				// Show the spinning wheel and the counter, which suspends the wheel in SEARCHING_TIMEOUT milli-seconds
		        showSpinningWheel("", getString(R.string.searching_spin_text), SEARCHING_TIMEOUT);
				
				try {
					URL searchURL = new URL(searchURLString);
					InputStream responseStream = searchURL.openConnection().getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
					StringBuilder builder = new StringBuilder();
					String oneLine;
					
					while ((oneLine = reader.readLine()) != null) {
						builder.append(oneLine);
					}
					reader.close();
					
					searchResults = new JSONObject(builder.toString());
					
				} catch (Exception exception) {
					exception.printStackTrace();
				} 
				
				try {
					boolean success = searchResults.getBoolean("success");
					int resultsSize = searchResults.getInt("resultsSize");
					JSONArray items = searchResults.getJSONArray("results");
					
					if (!success) {
						throw new Exception("Failed to Search");
					}
					
					for (int index = 0; index < resultsSize; ++index) {
						mResultsAdapter.add(items.getString(index));
					}
				} catch (Exception exception) {
					Toast.makeText(SearchPage.this, R.string.unknown_error_message, Toast.LENGTH_SHORT)
					.show();
					return;
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
						
						// Edittext cannot be empty
						if (titleEditBox.getText().toString().contentEquals("")) {
							Toast.makeText(dialog.getContext(), R.string.invalid_title_message, Toast.LENGTH_SHORT)
							.show();
							return;
						}
						
						// Verify the URL
						EditText feedURLEditBox = (EditText) dialog.findViewById(R.id.custom_feed_url);
						try {
							URL feedURL = new URL(feedURLEditBox.getText().toString());
						} catch (MalformedURLException exception) {
							Toast.makeText(dialog.getContext(), R.string.invalid_url_message, Toast.LENGTH_SHORT)
								.show();
							return;
						}
						
						// Add to DB
						NewsSpeakDBAdapter mDbAdapter = new NewsSpeakDBAdapter(SearchPage.this);
						mDbAdapter.open(NewsSpeakDBAdapter.READ_WRITE);
						
						// Newspaper with that name already exists
						if (mDbAdapter.getNewsPaper(titleEditBox.getText().toString()) != null) {
							Toast.makeText(dialog.getContext(), R.string.invalid_title_message, Toast.LENGTH_SHORT)
							.show();
							return;
						}
						// Create a newsSource and then add it to the DB
						NewsSource customSource = new NewsSource(titleEditBox.getText().toString(), 
																	SourceType.BLOG.toString(),
																	Locale.getDefault(),
																	false,
																	null,
																	null,
																	feedURLEditBox.getText().toString(),
																	false,
																	true,
																	mDbAdapter.getNumberOfNewsSources()+ 1 // Should be last (?)
																	); 
						// Attempt to add the source to the DB
						try {
							mDbAdapter.createNewsSource(customSource);
						} catch (SQLException exception) {
							Toast.makeText(dialog.getContext(), R.string.unknown_error_message, Toast.LENGTH_SHORT)
							.show();
							return;
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
				String to[] = { SearchPage.this.getString(R.string.feed_suggestion_email) };

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
	private class SearchResultsAdapter extends ArrayAdapter<String> {

		private ArrayList<String> results;
		
		public SearchResultsAdapter(Context context, int resource, ArrayList<String> objects) {
			super(context, resource, objects);
			results = objects;
			setNotifyOnChange(true);
		}
		
		/**
		 * Return the count of the items
		 */
		public int getCount() {
			return results.size();
		}
		
		/**
		 * Get item from the sources
		 */
		public String getItem(int position) {
			return results.get(position);
		}
		
		/**
		 * Get item id
		 */
		public long getItemId(int position) {
			return position;
		}
		
		/**
		 * Get item's View
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.search_result_item, null);
				
			}
			return convertView;
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
			Toast.makeText(SearchPage.this, getString(R.string.search_failed), Toast.LENGTH_SHORT)
				.show();
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
	   spinningWheel = ProgressDialog.show(SearchPage.this, title, text, true);
       new  ProgressDialogTimer(timeout, timeout)
       		.start();
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
