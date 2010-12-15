package com.vn.plaudible;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Logos extends ListActivity {
	
	private ArrayList<NewsSource> sources;
	private LogosAdapter adapter;
	
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
	        	  } else if (parser.getName().equalsIgnoreCase("URL")) {
	        		  source.setUrl(parser.nextText());
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
		
		int i;
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
			ViewHolder holder = (ViewHolder) view.getTag();
			NewsSource source = getItem(holder.position);
			
			Intent listArticlesInFeed = new Intent();
			listArticlesInFeed.setClass(context, Plaudible.class);
			
			listArticlesInFeed.putExtra("Source", source.getTitle());
			listArticlesInFeed.putExtra("URL", source.getUrl());
			
			context.startActivity(listArticlesInFeed);
		}
	}
}
