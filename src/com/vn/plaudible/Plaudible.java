package com.vn.plaudible;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.vn.plaudible.PlaudibleAsyncTask.Payload;

public class Plaudible extends ListActivity implements TextToSpeech.OnInitListener {
	
	// ViewHolder pattern for efficient ListAdapter usage
	static class ViewHolder {
        TextView title;
        TextView description;
        ImageButton playButton;
        
        int position;
    }

	private ArrayList<Article> articles;
	private ArticleListAdapter adapter;
	private TextToSpeech ttsEngine;	
	private SpeechService mSpeechService;
	private String source;
	private String url;
		
	private static final int TTS_INSTALLED_CHECK_CODE = 1;
	
	private static final int NO_INTERNET_DIALOG = 1001;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
 
        Intent intent = this.getIntent();
        source = intent.getStringExtra("Source");
        url = intent.getStringExtra("URL");
        
        this.setTitle(source);
        
        articles = new ArrayList<Article>();
        adapter = new ArticleListAdapter(this, R.layout.list_item, articles);
        
        setListAdapter(adapter);

        new PlaudibleAsyncTask().execute(
        		new PlaudibleAsyncTask.Payload(
        				PlaudibleAsyncTask.FEED_DOWNLOADER_TASK,
        				new Object[] { Plaudible.this,
        								source,
        								articles }));
        
        bindSpeechService();
        checkAndInstallTTSEngine();
    }
	
    @Override
    protected void onDestroy() {
    	ttsEngine.stop();
    	ttsEngine.shutdown();
    	
    	unBindSpeechService();
    	super.onDestroy();
    }
    
    // Check for presence of a TTSEngine and install if not found
    protected void checkAndInstallTTSEngine() {
	    Intent checkIntent = new Intent();
	    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	    startActivityForResult(checkIntent, TTS_INSTALLED_CHECK_CODE);
    }
    
    // Called for the intent which checks if TTS was installed and starts TTS up
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_INSTALLED_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                ttsEngine = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }
    
   // Used by the AsyncTask to update the set of articles
   public void setArticles(ArrayList<Article> articles) {
	   this.articles = articles;   
	   this.adapter.notifyDataSetChanged();
   }
   
   @SuppressWarnings("rawtypes")
   private class ArticleListAdapter extends ArrayAdapter<Article> implements View.OnClickListener {
	   
	   ArrayList<Article> articles;
	   Context context;
	   
	   public ArticleListAdapter(Context context, int textViewResourceId, ArrayList<Article> articles) {
		   super(context, textViewResourceId, articles);
		   
		   this.context = context;
		   this.articles = articles;
		   
		   this.setNotifyOnChange(true);
	   }
	   
	   @Override
	   public View getView(int position, View convertView, ViewGroup parent) {
		   ViewHolder holder;
		   
		   if (convertView == null) {
			   LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			   convertView = inflater.inflate(R.layout.list_item, null);
			   
			   holder = new ViewHolder();
			   holder.title = (TextView) convertView.findViewById(R.id.ArticleTitle);
			   holder.description = (TextView) convertView.findViewById(R.id.ArticleDescription);
			   holder.playButton = (ImageButton) convertView.findViewById(R.id.ArticlePlay);
			   
			   convertView.setTag(holder);
		    } else {
			   holder = (ViewHolder) convertView.getTag();
		    }
		   
		   convertView.setOnClickListener(this);
		   
		   holder.title.setText(this.articles.get(position).getTitle());
		   holder.description.setText(this.articles.get(position).getDescription());

		   // Set the tag for the button as the index
		   holder.playButton.setTag((Integer)position);
		   holder.position = position;
		   
		   // Set the listener on the play button
		   holder.playButton.setOnClickListener(new View.OnClickListener() {
			   @Override
			   public void onClick(View v) {
				   Integer position = (Integer) v.getTag();
				   ImageButton view = (ImageButton) v;
				   view.setImageResource(R.drawable.pause64);
				   
				   if (!articles.get(position).isDownloaded()) {
					   AsyncTask<Payload, Article, Payload> task = new PlaudibleAsyncTask().execute(
							   new PlaudibleAsyncTask.Payload(
									   PlaudibleAsyncTask.ARTICLE_DOWNLOADER_TASK,
									   new Object[] { Plaudible.this,
											   			position,
											   			articles,
											   			source }));
				   } else {
					   sendArticleForReading(articles.get(position));
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
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
   }

    // OnInitListener for TTSEngine initialization
    // Check if the Service is bound and if it is then we can set the TTS Engine it should use
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
	           ttsEngine.setLanguage(Locale.US);	            
	           
	           if (mSpeechService != null) {
	        	   mSpeechService.setTTSEngine(this.ttsEngine);
	           }
		}
	}
	
	// Send an article to the service so it can begin reading
	public void sendArticleForReading(Article article) {
		if (mSpeechService != null) {
			mSpeechService.readArticle(article);
		}
	}
	
	// Check for internet access
	private boolean isInternetConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		/*if ( info == null)
			return false;
		else
			return true;*/
		return info.isConnected();
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
		this.unbindService(mConnection);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case Plaudible.NO_INTERNET_DIALOG:
				return new AlertDialog.Builder(Plaudible.this)
		    		.setIcon(android.R.drawable.alert_dark_frame)
		    		.setTitle("Plaudible")
		    		.create();
		}
		
		return null;
	}
}