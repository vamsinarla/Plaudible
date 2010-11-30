package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class PlaudibleAsyncTask extends AsyncTask<PlaudibleAsyncTask.Payload, Article, PlaudibleAsyncTask.Payload> {

	public static final int FEED_DOWNLOADER_TASK = 1001;
	public static final int ARTICLE_DOWNLOADER_TASK = 1002;
	
	@Override
	protected void onPostExecute(PlaudibleAsyncTask.Payload payload) {
		if (payload.result != null) {
			Plaudible activity = (Plaudible) payload.data[0];
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				activity.setArticles((ArrayList<Article>) payload.data[2]);
				break;
			case ARTICLE_DOWNLOADER_TASK:
				SAXParserFactory factory = SAXParserFactory.newInstance();
				try {
					SAXParser parser = factory.newSAXParser();
					
					String content = new String();
					ArticleParser articleHandler = new ArticleParser(content);
					HttpResponse response = (HttpResponse) payload.result;
					parser.parse(response.getEntity().getContent(), articleHandler);
				} catch (Exception exception) {
					Log.e("onPostExecute", exception.getMessage());
				}
				break;
			}
		}
	}
	
	@Override
	protected PlaudibleAsyncTask.Payload doInBackground(PlaudibleAsyncTask.Payload... params) {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		PlaudibleAsyncTask.Payload payload = params[0];
		ArrayList<Article> articles;
		
		try {
			SAXParser parser = factory.newSAXParser();
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				// Extract the URL and arraylist of articles from the payload data
				URL feedUrl = (URL) payload.data[1];
				articles = (ArrayList<Article>) payload.data[2];
				
				FeedHandler feedHandler = new FeedHandler(articles);
				parser.parse(feedUrl.openConnection().getInputStream(), feedHandler);
				
				payload.result = new String("Success");
				break;
			case ARTICLE_DOWNLOADER_TASK:
				// Extract the index of the article and the arraylist of articles
				Integer position = (Integer) payload.data[1];
				articles = (ArrayList<Article>) payload.data[2];
				
				for (int index = position; /*index < articles.size()*/ index <= position; ++index) {
					// Download the article only if it hasn't been till yet
					if (articles.get(index).isDownloaded() == false) {
						String content = new String();
						URI url = new URI(articles.get(index).getUrl());
						HttpClient client = new DefaultHttpClient();
						HttpGet request = new HttpGet();
						
						request.setURI(url);
						HttpResponse response = client.execute(request);
						
						payload.result = response;
		                // parser.parse(response.getEntity().getContent(), articleHandler);
						articles.get(index).setContent(content);
						articles.get(index).setDownloaded(true);
					}
				}
				// payload.result = new String("Success");
				break;
				
			}
		} catch (Exception exception) {	
				Log.e("PlaudibleAsyncTask::doInBackground", exception.getMessage());
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
