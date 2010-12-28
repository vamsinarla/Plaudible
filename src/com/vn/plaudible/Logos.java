package com.vn.plaudible;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Logos extends ListActivity {
	
	private ArrayList<NewsSource> sources;
	private LogosAdapter adapter;	
	private SpeechService mSpeechService;
	
	static class ViewHolder {
		ImageView image;
		TextView text;
		Integer position;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        
        sources = new ArrayList<NewsSource>();
        
        try {
			populateSourcesFromXML();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        adapter = new LogosAdapter(this, R.layout.main_page_list_item, sources);
        setListAdapter(adapter);
        
        bindSpeechService();
    }
	
	// Show a toast and return data status
	private boolean checkDataConnectivity() {
		ConnectivityManager conMan = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
		
		if(networkInfo != null && networkInfo.isConnected()){
			return true;
		}
		return false;
	}
	
	@Override
	protected void onDestroy() {
		unBindSpeechService();

		super.onDestroy();
	}
	
    // Listen for configuration changes and this is bascially to prevent the 
    // activity from being restarted. Do nothing here.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

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
	        	  } 
	          } else if(eventType == XmlResourceParser.END_TAG) {
	              if (parser.getName().equalsIgnoreCase("Item")) {
	            	  sources.add(source);
	              }
	          } else if(eventType == XmlResourceParser.TEXT) {
	              System.out.println("Text " + parser.getText());
	          }
	          eventType = parser.next();
	         }
	}
	
	private class LogosAdapter extends ArrayAdapter<NewsSource> implements View.OnClickListener {
			
		private ArrayList<NewsSource> sources;
		private Context context;
		
		public LogosAdapter(Context context, int viewResourceId, ArrayList<NewsSource> icons) {
			super(context, viewResourceId, icons);
			
			this.sources = icons;
			this.context = context;
		}
	
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
			if (source.getType() == NewsSource.SourceType.NEWSPAPER) {
				holder.image.setImageResource(R.drawable.news);
			} else if (source.getType() == NewsSource.SourceType.BLOG) {
				holder.image.setImageResource(R.drawable.blog);
			}
			
			convertView.setOnClickListener(this);
			return convertView;
		}
		
		public int getCount() {
			return sources.size();
		}
		
		public NewsSource getItem(int position) {
			return sources.get(position);
		}
		
		public long getItemId(int position) {
			return position;
		}

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
			
			Intent listArticlesInFeed = new Intent();
			listArticlesInFeed.setClass(context, Plaudible.class);
			listArticlesInFeed.putExtra("Source", source.getTitle());
			
			context.startActivity(listArticlesInFeed);
		}
	}
	
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
	
	void bindSpeechService() {
		this.bindService(new Intent(Logos.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	void unBindSpeechService() {
		if (mSpeechService != null) {
			this.unbindService(mConnection);
		}
	}
	
}
