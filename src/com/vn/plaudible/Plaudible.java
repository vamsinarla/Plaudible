package com.vn.plaudible;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class Plaudible extends ListActivity {
	
	// ViewHolder pattern for efficient ListAdapter usage
	static class ViewHolder {
        TextView title;
        TextView description;
        ImageButton playButton;
        ImageButton browserButton;
        
        int position;
    }

	private ArrayList<Article> articles;
	private ArticleListAdapter adapter;
	private String currentNewsSource;
	private ImageButton currentPlayButtonView;
	private SpeechService mSpeechService;
	private ProgressDialog spinningWheel;
	
	private static final int TIMEOUT = 10000;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.articles_list);
 
        // Show the spinning wheel and the counter, which suspends the wheel in TIMEOUT seconds
        ProgressDialogTimer timer = new  ProgressDialogTimer(TIMEOUT, TIMEOUT);
        spinningWheel = ProgressDialog.show(Plaudible.this, "", "Loading articles ...", true);
        timer.start();
        
        // Get the intent and the related extras
        Intent intent = this.getIntent();
        currentNewsSource = intent.getStringExtra("Source");
        
        // Set the title
        this.setTitle(currentNewsSource);
        
        // Set the volume control to media. So that when user presses volume button it adjusts the media volume
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        articles = new ArrayList<Article>();
        adapter = new ArticleListAdapter(this, R.layout.articles_list_item, articles);
        
        setListAdapter(adapter);

        new PlaudibleAsyncTask().execute(
        		new PlaudibleAsyncTask.Payload(
        				PlaudibleAsyncTask.FEED_DOWNLOADER_TASK,
        				new Object[] { Plaudible.this,
        								currentNewsSource,
        								articles }));
        bindSpeechService();
    }
	
    @Override
    protected void onDestroy() {
    	unBindSpeechService();
    	super.onDestroy();
    }
    
    // Listen for configuration changes and this is basically to prevent the 
    // activity from being restarted. Do nothing here.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
   // Used by the AsyncTask to update the set of articles
   public void setArticles(ArrayList<Article> articles) {
	   this.articles = articles;   
	   this.adapter.notifyDataSetChanged();
	   
	   if (spinningWheel.isShowing()) {
		   spinningWheel.cancel();
	   }
   }
   
   private class ArticleListAdapter extends ArrayAdapter<Article> implements View.OnClickListener {
	   
	   ArrayList<Article> articles;
	   
	   public ArticleListAdapter(Context context, int textViewResourceId, ArrayList<Article> articles) {
		   super(context, textViewResourceId, articles);
		   
		   this.articles = articles;
		   this.setNotifyOnChange(true);
	   }
	   
	   @Override
	   public View getView(int position, View convertView, ViewGroup parent) {
		   ViewHolder holder;
		   
		   // First time creating the holder. Inflate the views only once with this pattern.
		   if (convertView == null) {
			   LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			   convertView = inflater.inflate(R.layout.articles_list_item, null);
			   
			   holder = new ViewHolder();
			   holder.title = (TextView) convertView.findViewById(R.id.ArticleTitle);
			   holder.description = (TextView) convertView.findViewById(R.id.ArticleDescription);
			   holder.playButton = (ImageButton) convertView.findViewById(R.id.ArticlePlay);
			   holder.browserButton = (ImageButton) convertView.findViewById(R.id.BrowserButton);
			   
			   // Decide the drawable for this button
			   Integer articleBeingRead = mSpeechService.getCurrentlyReadArticle();
			   boolean isSpeechServiceReading = mSpeechService.isReading();
			   
			   // if we return to the same news source then see if any article needs to be on play.
			   // Set the currentPlayButtonView appropriately
			   if (isSpeechServiceReading && articleBeingRead == position &&
					   currentNewsSource.equalsIgnoreCase(mSpeechService.getCurrentNewsSource())) {
				   currentPlayButtonView = holder.playButton;
				   holder.playButton.setImageResource(R.drawable.pause64);
			   }
			   convertView.setTag(holder);
		    } else {
			   holder = (ViewHolder) convertView.getTag();
		    }
		   
		   convertView.setOnClickListener(this);
		   
		   holder.title.setText(articles.get(position).getTitle());
		   holder.description.setText(articles.get(position).getDescription());

		   // Set the tag for the browser button and set its click listener
		   holder.browserButton.setTag((Integer)position);
		   holder.browserButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					
					// Start the browser with the URI of the article
					Uri uri = Uri.parse(getItem(position).getUrl());
					Intent webViewIntent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(Intent.createChooser(webViewIntent, "Open this article in"));
				}
		   });
		   
		   // Set the tag for the button as the index
		   holder.playButton.setTag((Integer)position);
		   holder.position = position;
		   
		   // Set the listener on the play button
		   holder.playButton.setOnClickListener(new View.OnClickListener() {
			   @Override
			   public void onClick(View v) {
				   Integer position = (Integer) v.getTag();
				   ImageButton view = (ImageButton) v;
				   Integer articleBeingRead = mSpeechService.getCurrentlyReadArticle();
				   boolean isSpeechServiceReading = mSpeechService.isReading();
	
				   // State checking
				   if (isSpeechServiceReading) {
					   // Case I: Speech service is speaking
					   // Case a. Current newspaper is the one being read
					   if (currentNewsSource.equalsIgnoreCase(mSpeechService.getCurrentNewsSource())) {
						   // Case 1. User clicked to pause reading the current article
						   if (articleBeingRead == position) {
							   view.setImageResource(R.drawable.play64);
							   mSpeechService.pauseReading();
							   return;
						   } else {
							   // Speech service was reading another article and user clicked a different one to read out
							   // Set the button image of the old article to play
							   if (currentPlayButtonView != null) {
								   // Could be null when we return back to the same news source 
								   currentPlayButtonView.setImageResource(R.drawable.play64);
							   }
						   }
					   }
				   } else {
					   // Case II: Speech Service is not speaking
					   // Case a. Click is for resuming or starting to read the current article.
					   // 	      Here we just want to resume reading so do not enter here if article needs to start reading.
					   if (articleBeingRead == position && articles.get(position).isDownloaded()) {
						   view.setImageResource(R.drawable.pause64);
						   mSpeechService.resumeReading();
						   return;
					   }
				   }
				   
				   if (!articles.get(position).isDownloaded()) {
					   // Set the button to pause and start the AsyncTask to download the article
					   view.setImageResource(R.drawable.pause64);
					   currentPlayButtonView = view;
					   new PlaudibleAsyncTask().execute(
							   new PlaudibleAsyncTask.Payload(
									   PlaudibleAsyncTask.ARTICLE_DOWNLOADER_TASK,
									   new Object[] { Plaudible.this,
											   			position,
											   			articles,
											   			currentNewsSource }));
				   } else {
					   view.setImageResource(R.drawable.pause64);
					   currentPlayButtonView = view;
					   sendArticleForReading(articles.get(position), currentNewsSource);
				   }
			   }
		   });
		   
		   return convertView;
	   }

	   public int getCount() {
		   return articles.size();
	   }
	   
	   public Article getItem(int position) {
		   return articles.get(position);
	   }
	   
	   public long getItemId(int position) {
		   return position;
	   }

	    // Open the browser when the user clicks on the article
		@Override
		public void onClick(View view) {
			ViewHolder holder = (ViewHolder) view.getTag();
			
			// Start the browser with the URI of the article
			Uri uri = Uri.parse(getItem(holder.position).getUrl());
			Intent webViewIntent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(Intent.createChooser(webViewIntent, "Open this article in"));
		}
   }
	
	// Send an article to the service so it can begin reading
	public void sendArticleForReading(Article article, String newsSource) {
		if (mSpeechService != null) {
			mSpeechService.readArticle(article, newsSource);
		}
	}
	
	public class ProgressDialogTimer extends CountDownTimer {
		public ProgressDialogTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		// Suspend the spinning wheel after some time.
		public void onFinish() {
		   if (spinningWheel.isShowing()) {
			   spinningWheel.cancel();
		   }
		}
		// Do nothing here
		public void onTick(long millisUntilFinished) {
			
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
		this.bindService(new Intent(Plaudible.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	void unBindSpeechService() {
		if (mSpeechService != null) {
			this.unbindService(mConnection);
		}
	}
}