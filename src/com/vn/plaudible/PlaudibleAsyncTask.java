package com.vn.plaudible;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.NodeList;

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
				try 
				{
					int index = (Integer) payload.data[1];
					ArrayList<Article> articles = (ArrayList<Article>) payload.data[2];
					
					if (!articles.get(index).isDownloaded()) {
						HttpResponse response = (HttpResponse) payload.result;
						Page p = new Page(response.getEntity().getContent(), "UTF-8");
						Lexer l = new Lexer(p);
						Parser parser = new Parser(l);
						NodeFilter filter = new TagNameFilter("p");
						String content = new String();
						NodeList list = parser.parse(filter);
						for (int i = 0; i < list.size(); ++i) {
							content += list.elementAt(i).toPlainTextString();
						}
						articles.get(index).setContent(content);
						articles.get(index).setDownloaded(true);
					}
					activity.sendArticleForReading(articles.get(index));
					
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
						URI url = new URI(articles.get(index).getUrl());
						DefaultHttpClient client = new DefaultHttpClient();
						HttpGet request = new HttpGet();
						request.setURI(url);
						HttpResponse response = client.execute(request);
				
						payload.result = response;
		            } else {
						payload.result = new String("Already downloaded");
					}
				}
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
