package com.vn.plaudible;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
		HttpURLConnection urlConnection = null;
		String link;
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			switch (payload.taskType) {
				case FEED_DOWNLOADER_TASK: {
					FeedViewerActivity activity = (FeedViewerActivity) payload.data[0];
					
					// Extract the URL and arraylist of articles from the payload data
					feed = (Feed) payload.data[1];
					
					// Construct the URL for calling the FeedServlet on AppEngine. Latest version of
					// app engine URL must be in strings.xml
					link = activity.getString(R.string.appengine_url2) + "feed";
					
					// POST args
					String args = URLEncoder.encode("url", "UTF-8") + "=" + 
									URLEncoder.encode(feed.getUrl(), "UTF-8");
					
					// Construct the FeedHandler(SAXParser) and parse the response from AppEngine
					URL feedUrl = new URL(link);
					FeedHandler feedHandler = new FeedHandler(feed);
					urlConnection = (HttpURLConnection) feedUrl.openConnection();

					// Write the POST vars
					Utils.postVars(urlConnection, args);

					// Get the response and parse it
					responseStream = new BufferedInputStream(urlConnection.getInputStream());
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
							link = activity.getString(R.string.appengine_url) + "article2";
							
							String postArgs = URLEncoder.encode("source", "UTF-8") + "=" + URLEncoder.encode(source.getTitle(), "UTF-8") + "&";
							postArgs += URLEncoder.encode("link", "UTF-8") + "=" + URLEncoder.encode(article.getUrl(), "UTF-8") + "&";
							postArgs += URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("text", "UTF-8");
							
							// Get the response from AppEngine
							URL articleUrl = new URL(link);
							urlConnection = (HttpURLConnection) articleUrl.openConnection();
							Utils.postVars(urlConnection, postArgs);
							
							// Set the params for the article and mark as downloaded
							article.setContent(Utils.getStringFromInputStream(urlConnection.getInputStream()));
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
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
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
