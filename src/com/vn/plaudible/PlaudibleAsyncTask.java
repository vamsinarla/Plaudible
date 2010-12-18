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

import org.xml.sax.InputSource;

import android.os.AsyncTask;
import android.util.Log;

public class PlaudibleAsyncTask extends AsyncTask<PlaudibleAsyncTask.Payload, Article, PlaudibleAsyncTask.Payload> {

	public static final int FEED_DOWNLOADER_TASK = 1001;
	public static final int ARTICLE_DOWNLOADER_TASK = 1002;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onPostExecute(PlaudibleAsyncTask.Payload payload) {
		if (payload.result != null) {
			Plaudible activity = (Plaudible) payload.data[0];
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				activity.setArticles((ArrayList<Article>) payload.data[2]);
				break;
			case ARTICLE_DOWNLOADER_TASK:
				Integer index = (Integer) payload.data[1];
				ArrayList<Article> articles = (ArrayList<Article>) payload.data[2];
				String source = (String) payload.data[3];
				
				activity.sendArticleForReading(articles.get(index), source);
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected PlaudibleAsyncTask.Payload doInBackground(PlaudibleAsyncTask.Payload... params) {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		PlaudibleAsyncTask.Payload payload = params[0];
		ArrayList<Article> articles;
		String source;
		
		InputStream responseStream;
		BufferedReader reader;
		StringBuilder builder;
		String oneLine;		
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				// Extract the URL and arraylist of articles from the payload data
				articles = (ArrayList<Article>) payload.data[2];
				source = (String) payload.data[1];
				
				source = URLEncoder.encode(source, "UTF-8");
				source = "http://evecal.appspot.com/feed?newspaper=" + source;
				
				URL feedUrl = new URL(source);
				FeedHandler feedHandler = new FeedHandler(articles);
				responseStream = feedUrl.openConnection().getInputStream();
				InputSource s = new InputSource();
				s.setByteStream(responseStream);
				s.setEncoding("ISO-8859-1");
				parser.parse(s, feedHandler);
				
				payload.result = new String("Success");
				break;
			case ARTICLE_DOWNLOADER_TASK:
				// Extract the index of the article and the arraylist of articles
				Integer position = (Integer) payload.data[1];
				articles = (ArrayList<Article>) payload.data[2];
				source = (String) payload.data[3];
				
				for (int index = position; /*index < articles.size()*/ index <= position; ++index) {
					// Download the article only if it hasn't been till yet
					if (articles.get(index).isDownloaded() == false) {
						source = "http://evecal.appspot.com/article?source=" + URLEncoder.encode(source, "UTF-8");
						source += "&link=" + articles.get(position).getUrl();
						
						URL articleUrl = new URL(source);
						responseStream = articleUrl.openConnection().getInputStream();
						reader = new BufferedReader(new InputStreamReader(responseStream));
						builder = new StringBuilder();
						
						while ((oneLine = reader.readLine()) != null) {
							builder.append(oneLine);
						}
						reader.close();
						
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
