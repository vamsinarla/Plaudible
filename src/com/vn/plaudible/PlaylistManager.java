package com.vn.plaudible;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.vn.plaudible.dragdrop.DragListener;
import com.vn.plaudible.dragdrop.DragNDropListView;
import com.vn.plaudible.dragdrop.DropListener;
import com.vn.plaudible.dragdrop.RemoveListener;
import com.vn.plaudible.tts.SpeechService;
import com.vn.plaudible.types.Playlist;
import com.vn.plaudible.types.Item;

public class PlaylistManager extends ListActivity {
	
	private SpeechService mSpeechService;
	
	static class ViewHolder {
        TextView itemTitle;
        ImageButton removeButton;
    }
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set view related stuff
        setContentView(R.layout.playlist_manager);
        
        ListView listView = getListView();
	    
	    if (listView instanceof DragNDropListView) {
	    	((DragNDropListView) listView).setDropListener(mDropListener);
	    	((DragNDropListView) listView).setRemoveListener(mRemoveListener);
	    	((DragNDropListView) listView).setDragListener(mDragListener);
	    }
	    
        bindSpeechService();
	}
	
    @Override
    protected void onDestroy() {
    	unBindSpeechService();
    	super.onDestroy();
    }
    
    /**
     * Drag and Drop supporting adapter which holds the ArrayList of NewsSource
     * @author vamsi
     *
     */
    private class DragNDropAdapter extends ArrayAdapter<Item> implements RemoveListener, DropListener, View.OnClickListener {

    	private Context mContext;
    	private LayoutInflater mInflater;
    	private Playlist<Item> mPlaylist;
    	
        public DragNDropAdapter(Context context, int resource, Playlist<Item> playlist) {
        	super(context, resource, playlist.getItems());
        	mPlaylist = playlist;
        	mContext = context;
        	
        	setNotifyOnChange(true);
        }
        
        /**
         * The number of items in the list
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return mPlaylist.getSize();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficient to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Item getItem(int position) {
            return mPlaylist.get(position);
        }

        /**
         * Use the array index as a unique id.
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }
        /**
    	 * The onClick method
    	 */
        @Override
    	public void onClick(View view) {
        	Toast toast = Toast.makeText(mContext, getString(R.string.reorder_page_help), Toast.LENGTH_LONG);
    		toast.show();
    	}

        /**
         * Make a view to hold each row.
         *
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
            	mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			convertView = mInflater.inflate(R.layout.dragitem, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.itemTitle = (TextView) convertView.findViewById(R.id.itemText);
                holder.removeButton = (ImageButton) convertView.findViewById(R.id.removeItem);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
            
            // Bind the data efficiently with the holder. Set the text
            holder.itemTitle.setText(mPlaylist.get(position).getTitle());
            holder.itemTitle.setOnClickListener(this);
            
            // Set the remove functionality
            holder.removeButton.setTag((Integer) position);
            holder.removeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Integer position = (Integer) v.getTag();
					mPlaylist.remove(position);
					
					// Notify dataset change
					notifyDataSetChanged();
				}
            });
            
			return convertView;
        }
        
		@Override
		public void onDrop(int from, int to) {
			// Swap the items in the adapter
    		Item temp = mPlaylist.get(from);
    		mPlaylist.remove(from);
    		mPlaylist.add(to,temp);
		}

		@Override
		public void onRemove(int which) {
			if (which < 0 || which > mPlaylist.getSize()) return;		
			mPlaylist.remove(which);
		}

    }
    
	/**
	 * Connection to the Service. All Activities must have this.
	 */
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
	
	/**
	 * Bind to the Speech Service. Called from onCreate() on this activity
	 */
	void bindSpeechService() {
		this.bindService(new Intent(PlaylistManager.this, SpeechService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	/**
	 * Unbind from the Speech Service. Called from onDestroy() on this activity
	 */
	void unBindSpeechService() {
		if (mSpeechService != null) {
			this.unbindService(mConnection);
		}
	}

	/**
	 * Drop listener
	 */
	private DropListener mDropListener = 
		new DropListener() {
        public void onDrop(int from, int to) {
        	ListAdapter adapter = getListAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onDrop(from, to);
        		getListView().invalidateViews();
        	}
        }
    };
    
    /**
     * Item remove listener
     */
    private RemoveListener mRemoveListener =
        new RemoveListener() {
        public void onRemove(int which) {
        	ListAdapter adapter = getListAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onRemove(which);
        		getListView().invalidateViews();
        	}
        }
    };
    
    /**
     * Item drag listener. The callbacks are registered with the listview and
     * we perform any action here that needs to be done after an UI event
     * occurred.
     */
    private DragListener mDragListener =
    	new DragListener() {
    	
    	int backgroundColor = 0xFFFFA500; 
    	int defaultBackgroundColor;
    	
			public void onDrag(int x, int y, ListView listView) {
				// TODO Auto-generated method stub
			}

			public void onStartDrag(View itemView) {
				itemView.setVisibility(View.INVISIBLE);
				defaultBackgroundColor = itemView.getDrawingCacheBackgroundColor();
				itemView.setBackgroundColor(backgroundColor);
				ImageView iv = (ImageView)itemView.findViewById(R.id.dragHolder);
				if (iv != null) iv.setVisibility(View.INVISIBLE);
			}

			public void onStopDrag(View itemView) {
				itemView.setVisibility(View.VISIBLE);
				itemView.setBackgroundColor(defaultBackgroundColor);
				ImageView iv = (ImageView)itemView.findViewById(R.id.dragHolder);
				if (iv != null) iv.setVisibility(View.VISIBLE);
			}
    	
    };
}
