package com.vn.plaudible;

import java.util.ArrayList;
import java.util.Collections;

import android.app.ListActivity;
import android.content.Context;
import android.database.SQLException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.vn.plaudible.dragdrop.DragListener;
import com.vn.plaudible.dragdrop.DragNDropListView;
import com.vn.plaudible.dragdrop.DropListener;
import com.vn.plaudible.dragdrop.RemoveListener;

public class ReorderNewsSourcesPage extends ListActivity {

    static class ViewHolder {
        TextView text;
    }

    private ArrayList<NewsSource> allSources;
	private boolean changesWereMade;
	private NewsSpeakDBAdapter mDBAdapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.manage_newssources);
	  
	    changesWereMade = false;
	  
	    mDBAdapter = new NewsSpeakDBAdapter(this);
	    mDBAdapter.open(NewsSpeakDBAdapter.READ_WRITE);
		
	    try {
	    	populateSubscribedSourcesFromDB();
	    } catch (SQLException exception) {
	    	exception.printStackTrace();
	    }
	    
	    // Sort the sources as per the display index
	    Collections.sort(allSources, NewsSource.DISPLAYINDEX_ORDER);
	    
	    setListAdapter(new DragNDropAdapter(this, R.layout.dragitem, allSources));
	    ListView listView = getListView();
	    
	    if (listView instanceof DragNDropListView) {
	    	((DragNDropListView) listView).setDropListener(mDropListener);
	    	((DragNDropListView) listView).setRemoveListener(mRemoveListener);
	    	((DragNDropListView) listView).setDragListener(mDragListener);
	    }
	}

	@Override
	protected void onDestroy() {
		mDBAdapter.close();
		super.onDestroy();
	}

	/**
	 * We commit the changes when the activity becomes invisible
	 */
	@Override
	protected void onPause() {
		if (changesWereMade) {
			commitChanges();
		}
		super.onPause();
	}
	
	/**
	 * Commit the DB changes
	 */
	private void commitChanges() throws SQLException {
		// TODO: Optimize updating records
		for (int index = 0; index < allSources.size(); ++index) {
			mDBAdapter.modifyNewsSourceDisplayIndex(allSources.get(index));
		}
	}
	
	/**
	 * Populate the sources from the DB
	 */
	private void populateSubscribedSourcesFromDB() throws SQLException {
		allSources = mDBAdapter.fetchAllNewsPapers(true); // Get only NewsSources we have subscribed to	
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
    
    /**
     * Drag and Drop supporting adapter which holds the ArrayList of NewsSource
     * @author vamsi
     *
     */
    private class DragNDropAdapter extends ArrayAdapter<NewsSource> implements RemoveListener, DropListener, View.OnClickListener {

    	private Context mContext;
    	private LayoutInflater mInflater;
        private ArrayList<NewsSource> mContent;
    	
        public DragNDropAdapter(Context context, int resource, ArrayList<NewsSource> objects) {
        	super(context, resource, objects);
        	mContent = objects;
        	mContext = context;
        }
        
        /**
         * The number of items in the list
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return mContent.size();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficient to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public NewsSource getItem(int position) {
            return mContent.get(position);
        }

        /**
         * Use the array index as a unique id.
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }
        /**
    	 * The onClick methd
    	 */
        @Override
    	public void onClick(View view) {
        	TextView text = (TextView) view;
        	Toast toast = Toast.makeText(mContext, text.getText(), Toast.LENGTH_SHORT);
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
                holder.text = (TextView) convertView.findViewById(R.id.itemText);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            holder.text.setText(mContent.get(position).getTitle());
            holder.text.setOnClickListener(this);

            return convertView;
        }

        /**
         * Remove an item
         */
    	public void onRemove(int which) {
    		if (which < 0 || which > mContent.size()) return;		
    		mContent.remove(which);
    	}

    	/**
    	 * Swap items
    	 */
    	public void onDrop(int from, int to) {
    		// First swap the display indices of the two items
    		Integer tempIndex = mContent.get(from).getDisplayIndex();
    		mContent.get(from).setDisplayIndex(mContent.get(to).getDisplayIndex());
    		mContent.get(to).setDisplayIndex(tempIndex);
    		
    		// Swap the items in the adapter
    		NewsSource temp = mContent.get(from);
    		mContent.remove(from);
    		mContent.add(to,temp);
    		
    		changesWereMade = true;
    	}
    }
}