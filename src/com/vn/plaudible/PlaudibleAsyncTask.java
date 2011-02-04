package com.vn.plaudible;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.app.ProgressDialog;
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
	public static final int FEATURED_SOURCES_DOWNLOAD_TASK = 1003;
	
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Post execution stuff for the AsyncTasks
	 */
	protected void onPostExecute(PlaudibleAsyncTask.Payload payload) {
		if (payload.result != null) {
			
			switch (payload.taskType) {
				case FEED_DOWNLOADER_TASK: {
					Plaudible activity = (Plaudible) payload.data[0];
					activity.setArticles((ArrayList<Article>) payload.data[2]);
					break;
				}
				case ARTICLE_DOWNLOADER_TASK:
					break;
				case FEATURED_SOURCES_DOWNLOAD_TASK: {
					ProgressDialog spinningWheel = (ProgressDialog) payload.data[1]; 
					if (spinningWheel.isShowing()) {
						spinningWheel.cancel();
					}
					break;
				}
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
		ArrayList<Article> articles;
		NewsSource source;
		InputStream responseStream;
		String link;
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			switch (payload.taskType) {
				case FEED_DOWNLOADER_TASK: {
					Plaudible activity = (Plaudible) payload.data[0];
					
					// Extract the URL and arraylist of articles from the payload data
					source = (NewsSource) payload.data[1];
					articles = (ArrayList<Article>) payload.data[2];
					
					// Construct the URL for calling the FeedServlet on AppEngine. Latest version of
					// app engine URL must be in strings.xml
					link = activity.getString(R.string.appengine_url) + "feed2";
					
					// POST args
					String args = URLEncoder.encode("feedLink", "UTF-8") + "=" + 
									URLEncoder.encode(source.getCurrentURL(), "UTF-8");
					
					// Construct the FeedHandler(SAXParser) and parse the response from AppEngine
					URL feedUrl = new URL(link);
					FeedHandler feedHandler = new FeedHandler(articles);
					URLConnection conn = feedUrl.openConnection();
					
					// Write the POST vars
					Utils.postVars(conn, args);
					
					// Get the response and parse it
					responseStream = conn.getInputStream();
					parser.parse(responseStream, feedHandler);
					
					// Write Success
					payload.result = new String("Success");
					break;
				}
				case ARTICLE_DOWNLOADER_TASK: {
					Plaudible activity = (Plaudible) payload.data[0];
					
					// Extract the index of the article and the arraylist of articles
					Integer position = (Integer) payload.data[1];
					articles = (ArrayList<Article>) payload.data[2];
					source = (NewsSource) payload.data[3];
					
					for (int index = position; /*index < articles.size()*/ index <= position; ++index) {
						
						// Download the article only if it hasn't been till yet
						if (articles.get(index).isDownloaded() == false) {
						
							// Construct the AppEngine URL for the ArticleServlet
							link = activity.getString(R.string.appengine_url) + "/article?source="; 
							link += source.getTitle();
							link += "&link=" + articles.get(position).getUrl();
							link += "&type=text"; // We want only text for reading
							
							// Get the response from AppEngine
							URL articleUrl = new URL(link);
							responseStream = articleUrl.openConnection().getInputStream();
							
							// Set the params for the article and mark as downloaded
							articles.get(index).setContent(Utils.getStringFromInputStream(responseStream));
							articles.get(index).setDownloaded(true);
							payload.result = new String("Downloaded");
						}
					}
					break;
				}
				case FEATURED_SOURCES_DOWNLOAD_TASK: {
					HomePage activity = (HomePage) payload.data[0];
					
					// Perform the heavy task of downloading and bulk insertion into DB 
					activity.populateSourcesIntoDB();
					payload.result = new String("Downloaded");
					break;
				}
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
