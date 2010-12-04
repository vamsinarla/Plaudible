package com.vn.plaudible;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class Logos extends ListActivity {
	
	private ArrayList<Drawable> drawables;
	private LogosAdapter adapter;
	
	static class ViewHolder {
		ImageView image;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        
        Resources res = this.getResources();
        drawables = new ArrayList<Drawable>();
        adapter = new LogosAdapter(this, R.layout.main_page_list_item, drawables);
        
        TypedArray icons = res.obtainTypedArray(R.array.icons);
        for (int i = 0; i < icons.length(); ++i) {
        	drawables.add(icons.getDrawable(i));
        }
        
        setListAdapter(adapter);
	} 
	
	private class LogosAdapter extends ArrayAdapter<Drawable> {
			
		private ArrayList<Drawable> icons;
		private Context context;
		
		public LogosAdapter(Context context, int textViewResourceId, ArrayList<Drawable> icons) {
			super(context, textViewResourceId, icons);
			
			this.icons = icons;
			this.context = context;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.main_page_list_item, null);
				
				holder = new ViewHolder();
				holder.image = (ImageView) convertView.findViewById(R.id.newsPaperImages);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.image.setImageDrawable(icons.get(position));
			
			return convertView;
		}
		
		public int getCount() {
			return icons.size();
		}
		
		public Drawable getItem(int position) {
			return icons.get(position);
		}
		
		public long getItemId(int position) {
			return position;
		}
	}
}
