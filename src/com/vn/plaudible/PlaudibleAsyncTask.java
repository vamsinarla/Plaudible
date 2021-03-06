package com.vn.plaudible;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.vn.plaudible.types.Article;
import com.vn.plaudible.types.Feed;
import com.vn.plaudible.types.NewsSource;

/**
 * AsyncTask which takes care of all downloading , feeds and articles.
 * @author vamsi
 *
 */
public class PlaudibleAsyncTask extends AsyncTask<PlaudibleAsyncTask.Payload, Article, PlaudibleAsyncTask.Payload> {

	public static final int FEED_DOWNLOADER_TASK = 1001;
	public static final int ARTICLE_DOWNLOADER_TASK = 1002;
	public static final int FEATURED_SOURCES_DOWNLOAD_TASK = 1003;
	
	private static final String CONTENT_SERVLET = "/article/content";
	
	@Override
	/**
	 * Post execution stuff for the AsyncTasks
	 */
	protected void onPostExecute(PlaudibleAsyncTask.Payload payload) {
		if (payload.result != null) {
			
			switch (payload.taskType) {
				case FEED_DOWNLOADER_TASK: {
					FeedViewerActivity activity = (FeedViewerActivity) payload.data[0];
					Feed feed = (Feed) payload.data[1];
					
					activity.setFeed(feed);
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
			
			payload.data = null;
			payload = null;
		}
	}
	
	@Override
	/**
	 * Main AsynTask work horse. Feed and Article downloading are done in this
	 */
	protected PlaudibleAsyncTask.Payload doInBackground(PlaudibleAsyncTask.Payload... params) {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		PlaudibleAsyncTask.Payload payload = params[0];
		Feed feed;
		NewsSource source;
		InputStream responseStream;
		String link;
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			switch (payload.taskType) {
				case FEED_DOWNLOADER_TASK: {
					FeedViewerActivity activity = (FeedViewerActivity) payload.data[0];
					
					// Extract the URL and arraylist of articles from the payload data
					feed = (Feed) payload.data[1];
					
					String args = "";
					// HACK: Workaround for stupid Google blocking requests from AppEngine
					// If news.google is not found in the URL
					if (!Utils.isGoogleNewsFeed(feed.getUrl())) {
							
						// Construct the URL for calling the FeedServlet on AppEngine. Latest version of
						// app engine URL must be in strings.xml
						link = activity.getString(R.string.appengine_url2) + "feed";
						
						// POST args
						args = URLEncoder.encode("url", "UTF-8") + "=" + 
										URLEncoder.encode(feed.getUrl(), "UTF-8");
					} else {
						link = feed.getUrl();
					}
					// Construct the FeedHandler(SAXParser) and parse the response from AppEngine
					URL feedUrl = new URL(link);
					FeedHandler feedHandler = new FeedHandler(feed);
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
					FeedViewerActivity activity = (FeedViewerActivity) payload.data[0];
					
					// Extract the index of the article and the arraylist of articles
					Integer position = (Integer) payload.data[1];
					feed = (Feed) payload.data[2];
					source = (NewsSource) payload.data[3];
					
					Article article = feed.getItem(position);
					
					for (int index = position; /*index < articles.size()*/ index <= position; ++index) {
						
						// Download the article only if it hasn't been till yet
						if (article.isDownloaded() == false) {
						
							// Construct the AppEngine URL for the ArticleServlet
							link = Utils.getStringFromResourceId(R.string.appengine_url2) + CONTENT_SERVLET;
							
							String postArgs = URLEncoder.encode("url", "UTF-8") + "=" + URLEncoder.encode(article.getUrl(), "UTF-8") + "&";
							postArgs += URLEncoder.encode("response", "UTF-8") + "=" + URLEncoder.encode("text", "UTF-8") + "&";
							postArgs += URLEncoder.encode("format", "UTF-8") + "=" + URLEncoder.encode("json", "UTF-8");
							
							// Get the response from AppEngine
							URL articleUrl = new URL(link);
							URLConnection conn = articleUrl.openConnection();
							Utils.postVars(conn, postArgs);
							
							// Set the params for the article and mark as downloaded
							article.setContent(Utils.getStringFromInputStream(conn.getInputStream()));
							article.setDownloaded(true);
							article.setSource(source);
							
							payload.result = new String("Downloaded");
						}
					}
					break;
				}
				case FEATURED_SOURCES_DOWNLOAD_TASK: {
					NewsSourcesActivity activity = (NewsSourcesActivity) payload.data[0];
					
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
