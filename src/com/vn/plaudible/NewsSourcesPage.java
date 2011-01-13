package com.vn.plaudible;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Screen that displays the list of all newspapers and blogs
 * @author vamsi
 *
 */
public class NewsSourcesPage extends ListActivity {
	
	private ArrayList<NewsSource> allSources;
	
	private NewsSourcesAdapter adapter;	
	private SpeechService mSpeechService;
	private EditText filterText;
	
	static class ViewHolder {
		ImageView image;
		TextView text;
		Integer position;
	}
	
	/**
	 * Called on activity creation
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        
        allSources = new ArrayList<NewsSource>();
       
        try {
        	populateSubscribedSourcesFromDB();
        } catch (SQLException exception) {
        	exception.printStackTrace();
        }
        
        // Sort the sources as per the display index
	    Collections.sort(allSources, NewsSource.DISPLAYINDEX_ORDER);
	    
        adapter = new NewsSourcesAdapter(this, R.layout.main_page_list_item, allSources);
        setListAdapter(adapter);
        
        this.getListView().setTextFilterEnabled(true);
        
        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);
        
        bindSpeechService();
    }
	
	/**
	 * Return the status of data connectivity
	 * @return boolean
	 */
	private boolean checkDataConnectivity() {
		ConnectivityManager conMan = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
		
		if(networkInfo != null && networkInfo.isConnected()){
			return true;
		}
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		filterText.setText("");
	}
	
	@Override
	protected void onDestroy() {
		unBindSpeechService();
		filterText.removeTextChangedListener(filterTextWatcher);

		super.onDestroy();
	}
	
    /**
     *  Listen for configuration changes and this is basically to prevent the 
     *	activity from being restarted. Do nothing here.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Read XML resources in order to populate list of blogs and newspapers that we support
     * @throws XmlPullParserException
     * @throws IOException
     */
	private void populateSourcesFromXML() throws XmlPullParserException, IOException {
		XmlResourceParser parser = getResources().getXml(R.xml.newssourcesdata);	
		NewsSource source = null;
		
		int eventType = parser.getEventType();
		while (eventType != XmlResourceParser.END_DOCUMENT) {
			if(eventType == XmlResourceParser.START_DOCUMENT) {
	              System.out.println("Start document");
	          } else if(eventType == XmlResourceParser.END_DOCUMENT) {
	              System.out.println("End document");
	          } else if(eventType == XmlResourceParser.START_TAG) {
	        	  if (parser.getName().equalsIgnoreCase("Item")) {
	        		  source = new NewsSource();
	        	  } else if (parser.getName().equalsIgnoreCase("Title")) {
	        		  source.setTitle(parser.nextText());
	        	  } else if (parser.getName().equalsIgnoreCase("Type")) {
	        		  source.setType(NewsSource.getType(parser.nextText()));
	        	  } else if (parser.getName().equalsIgnoreCase("Categories")) {
	        		  source.setHasCategories(parser.nextText().equalsIgnoreCase("true") ? true : false);
	        	  } 
	          } else if(eventType == XmlResourceParser.END_TAG) {
	              if (parser.getName().equalsIgnoreCase("Item")) {
	            	  allSources.add(source);
	              }
	          } else if(eventType == XmlResourceParser.TEXT) {
	              System.out.println("Text " + parser.getText());
	          }
	          eventType = parser.next();
	         }
	}
	
	/**
	 * Populate the sources from the DB
	 */
	private void populateSubscribedSourcesFromDB() throws SQLException {
		NewsSpeakDBAdapter adapter = new NewsSpeakDBAdapter(this);
		adapter.open(NewsSpeakDBAdapter.READ_WRITE);
		
		allSources = adapter.fetchAllNewsPapers(true); // Get only NewsSources we have subscribed to	
		adapter.close();		
	}
	
	/**
	 * Array adapter class for the listView
	 * @author vamsi
	 *
	 */
	private class NewsSourcesAdapter extends ArrayAdapter<NewsSource> implements View.OnClickListener, Filterable {
			
		private ArrayList<NewsSource> filteredSources;
		private Context context;
		private NewsSourcesFilter sourcesFilter;
		
		public NewsSourcesAdapter(Context context, int viewResourceId, ArrayList<NewsSource> newssources) {
			super(context, viewResourceId, newssources);
			
			this.filteredSources = newssources;
			this.context = context;
		}
		
		/**
		 * Create a custom filter
		 */
		@Override
		public Filter getFilter() {
			if (sourcesFilter == null) {
				sourcesFilter = new NewsSourcesFilter();
			}
			
			return sourcesFilter;
		}
	
		/**
		 * Get item's View
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.main_page_list_item, null);
				
				holder = new ViewHolder();
				holder.image = (ImageView) convertView.findViewById(R.id.newsPaperImages);
				holder.text = (TextView) convertView.findViewById(R.id.NewsPaperTitle);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			NewsSource source = getItem(position);
			
			holder.position = position;
			holder.text.setText(source.getTitle());
			
			// Decide on the appropriate icon
			if (source.getType() == NewsSource.SourceType.NEWSPAPER) {
				holder.image.setImageResource(R.drawable.news);
			} else if (source.getType() == NewsSource.SourceType.BLOG) {
				holder.image.setImageResource(R.drawable.blog);
			}
			
			convertView.setOnClickListener(this);
			return convertView;
		}
		
		/**
		 * Return the count of the filtered items
		 */
		public int getCount() {
			return filteredSources.size();
		}
		
		/**
		 * Get item from the filtered sources
		 */
		public NewsSource getItem(int position) {
			return filteredSources.get(position);
		}
		
		public long getItemId(int position) {
			return position;
		}

		/**
		 * The onClick method, which starts Plaudible
		 */
		@Override
		public void onClick(View view) {
			// If no data link then just don't open Plaudible activity. Best to stop it here.
			if (!checkDataConnectivity()) {
				Toast butterToast = Toast.makeText(this.getContext(), "No connection available", Toast.LENGTH_SHORT);
				butterToast.show();
				return;
			}
			
			ViewHolder holder = (ViewHolder) view.getTag();
			NewsSource source = getItem(holder.position);
			
			// Start Plaudible
			Intent listArticlesInFeed = new Intent();
			listArticlesInFeed.setClass(context, Plaudible.class);
			listArticlesInFeed.putExtra("NewsSource", source);
			
			context.startActivity(listArticlesInFeed);
		}
		
		/**
		 * Class to implement filter on the array adapter we use to 
		 * show the news sources. We need a custom implementation
		 * since Articles are 'complex'.
		 * @author vamsi
		 *
		 */
		private class NewsSourcesFilter extends Filter {

			/**
			 * Called on UI thread, to perform the filtering.
			 * @param constraint
			 * @return
			 */
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				ArrayList<NewsSource> subSources = new ArrayList<NewsSource>();
				
				// Only if constraint string is not empty
				if (constraint != null && constraint.toString().length() > 0) {
					Pattern regex = Pattern.compile((String) constraint, Pattern.CASE_INSENSITIVE);
					
					// Go through 'all' the sources and perform filtering
					for (int index = 0; index < allSources.size(); ++index) {
						NewsSource source = allSources.get(index);
						Matcher m = regex.matcher(source.getTitle());
						if (m.find()) {
							subSources.add(source);
						}
					}
					// Adding into the result set
					results.values = subSources;
					results.count = subSources.size();
				} else {
					// If no constraint then results must be same as all the sources
					synchronized (allSources){
						results.values = allSources;
						results.count = allSources.size();
					}
				}
				return results;
			}

			/**
			 * Publish the results to the array adapter
			 * @param constraint
			 * @param results
			 */
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				filteredSources = (ArrayList<NewsSource>) results.values;
				adapter.notifyDataSetChanged();
			}
		}
	}
	
	/**
	 * Listener for edit text changes to perform filtering
	 */
	private TextWatcher filterTextWatcher = new TextWatcher() {

	    public void afterTextChanged(Editable s) {
	    }

	    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	    }

	    /**
	     * Listen for character by character updates and call filtering and notify the adapter
	     */
	    public void onTextChanged(CharSequence s, int start, int before, int count) {
	        adapter.getFilter().filter(s);
	        adapter.setNotifyOnChange(true);
	    }
	};
	
	
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
		this.bindService(new Intent(NewsSourcesPage.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
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
