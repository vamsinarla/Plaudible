package com.vn.plaudible;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.os.AsyncTask;
import android.util.Log;

public class PlaudibleAsyncTask extends AsyncTask<PlaudibleAsyncTask.Payload, Article, PlaudibleAsyncTask.Payload> {

	public static final int FEED_DOWNLOADER_TASK = 1001;
	public static final int ARTICLE_DOWNLOADER_TASK = 1002;
	
	@Override
	protected void onPostExecute(PlaudibleAsyncTask.Payload payload) {
		if (payload.result != null) {
			
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				Plaudible activity = (Plaudible) payload.data[0];
				activity.setArticles((ArrayList<Article>) payload.data[2]);
				break;
			}
		}
	}
	
	@Override
	protected PlaudibleAsyncTask.Payload doInBackground(PlaudibleAsyncTask.Payload... params) {
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		PlaudibleAsyncTask.Payload payload = params[0];
		
		try {
			switch (payload.taskType) {
			case FEED_DOWNLOADER_TASK:
				SAXParser parser = factory.newSAXParser();
				FeedHandler feedHandler = new FeedHandler();
				
				// Extract the URL and array list from the payload data
				URL feedUrl = (URL) payload.data[1];
				ArrayList<Article> articles = (ArrayList<Article>) payload.data[2];
				
				parser.parse(feedUrl.openConnection().getInputStream(), feedHandler);
				
				articles = feedHandler.getArticles();
				payload.data[2] = articles;
				payload.result = new String("Success");
				break;
			case ARTICLE_DOWNLOADER_TASK:
				// Extract the URL from the payload data
				URL articleUrl = (URL) payload.data[1];
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
