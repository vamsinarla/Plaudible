package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.os.AsyncTask;
import android.util.Log;

/**
 * AsyncTask which takes care of all downloading , feeds and articles.
 * @author vamsi
 *
 */
public class PlaudibleAsyncTask extends AsyncTask<PlaudibleAsyncTask.Payload, Article, PlaudibleAsyncTask.Payload> {

	public static final int FEED_DOWNLOADER_TASK = 1001;
	public static final int ARTICLE_DOWNLOADER_TASK = 1002;
	
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Post execution stuff for the AsyncTasks
	 */
	protected void onPostExecute(PlaudibleAsyncTask.Payload payload) {
		if (payload.result != null) {
			Plaudible activity = (Plaudible) payload.data[0];
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				activity.setArticles((ArrayList<Article>) payload.data[3]);
				break;
			case ARTICLE_DOWNLOADER_TASK:
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Main AsynTask work horse. Feed and Article downloading are done in this
	 */
	protected PlaudibleAsyncTask.Payload doInBackground(PlaudibleAsyncTask.Payload... params) {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		PlaudibleAsyncTask.Payload payload = params[0];
		Plaudible activity = (Plaudible) payload.data[0];
		ArrayList<Article> articles;
		String source;
		String category;
		
		InputStream responseStream;
		BufferedReader reader;
		StringBuilder builder;
		String oneLine;		
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				// Extract the URL and arraylist of articles from the payload data
				source = (String) payload.data[1];
				category = (String) payload.data[2];
				articles = (ArrayList<Article>) payload.data[3];
				
				// encode the source into UTF 8
				source = URLEncoder.encode(source, "UTF-8");
				
				// Construct the URL for calling the FeedServlet on AppEngine. Latest version of
				// app engine URL must be in strings.xml
				source = activity.getString(R.string.appengine_url) + "/feed?newspaper=" + source;
				
				// Add the category param
				if (category != null) {
					// add category
					source += "&category=" + category;
				}
				
				// Construct the FeedHandler(SAXParser) and parse the response from AppEngine
				URL feedUrl = new URL(source);
				FeedHandler feedHandler = new FeedHandler(articles);
				responseStream = feedUrl.openConnection().getInputStream();
				parser.parse(responseStream, feedHandler);
				
				// Write Success
				payload.result = new String("Success");
				break;
			case ARTICLE_DOWNLOADER_TASK:
				// Extract the index of the article and the arraylist of articles
				Integer position = (Integer) payload.data[1];
				articles = (ArrayList<Article>) payload.data[2];
				source = (String) payload.data[3];
				
				for (int index = position; /*index < articles.size()*/ index <= position; ++index) {
				// for (int index = 0; index < articles.size() /*index <= position*/; ++index) {
					
					// Download the article only if it hasn't been till yet
					if (articles.get(index).isDownloaded() == false) {
					
						// encode the source into UTF 8
						source = URLEncoder.encode(source, "UTF-8");
						
						// Construct the AppEngine URL for the ArticleServlet
						source = activity.getString(R.string.appengine_url) + "/article?source=" + source;
						source += "&link=" + articles.get(position).getUrl();
						source += "&type=text"; // We want only text for reading
						
						// Get the response from AppEngine
						URL articleUrl = new URL(source);
						responseStream = articleUrl.openConnection().getInputStream();
						reader = new BufferedReader(new InputStreamReader(responseStream));
						builder = new StringBuilder();
						
						while ((oneLine = reader.readLine()) != null) {
							builder.append(oneLine);
						}
						reader.close();
						
						// Set the params for the article and mark as downloaded
						articles.get(index).setContent(builder.toString());
						articles.get(index).setDownloaded(true);
						payload.result = new String("Downloaded");
					}
				}
				break;
			}
		} catch (MalformedURLException exception) {
			Log.e("PlaudibleAsyncTask::doInBackground", exception.getMessage());
			payload.result = null;
		}
		catch (Exception exception) {	
			Log.e("PlaudibleAsyncTask::doInBackground", exception.getMessage());
			exception.printStackTrace();
			payload.result = null;
		}
		return payload;
	}

	/**
	 * Payload pattern for efficient and simple use of AsyncTasks
	 * @author vamsi
	 *
	 */
	public static class Payload {
		public int taskType;
		public Object[] data;
		public Object result;
		
		public Payload(int taskType, Object[] data) {
			this.taskType = taskType;
			this.data = data;
		}
	}
}
