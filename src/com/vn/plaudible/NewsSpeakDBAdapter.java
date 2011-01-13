package com.vn.plaudible;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NewsSpeakDBAdapter {

	/**
	 * public constants
	 */
	public static final int READ_ONLY = 1001;
	public static final int READ_WRITE = 1002;
	
	private Context mContext;
	private NewsSpeakDBHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "NEWSSPEAKDATABASE";
	private static final String NEWSPAPERS_TABLE_NAME = "NEWSPAPERS";
    private static final String NEWSPAPERS_TABLE_CREATE =
                "CREATE TABLE " + NEWSPAPERS_TABLE_NAME + " (" +
                "NAME " + " TEXT PRIMARY KEY, " +
                "TYPE " + " TEXT, " +
                "DEFAULTURL " + " TEXT NOT NULL, " +
                "HASCATEGORIES " + " BOOLEAN NOT NULL, " +
                "SUBSCRIBED " + "BOOLEAN NOT NULL, " +
                "DISPLAYINDEX " + "INTEGER, " +
                "PREFERRED " + "BOOLEAN" +
                ");";
    private static final String CATEGORIES_TABLE_NAME = "CATEGORIES";
    private static final String CATEGORIES_TABLE_CREATE = 
    			"CREATE TABLE " + CATEGORIES_TABLE_NAME + " (" +
    			"CATEGORY " + "TEXT NOT NULL, " +
    			"NAME " + "TEXT NOT NULL, " +
    			"LINK " + "TEXT NOT NULL, " +
    			"FOREIGN KEY(NAME) REFERENCES " + 
    			NEWSPAPERS_TABLE_NAME + "(NAME), " +
    			"PRIMARY KEY(NAME, CATEGORY)" +
    			");";
	
	private class NewsSpeakDBHelper extends SQLiteOpenHelper {
		public NewsSpeakDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	
		/**
		 * Create the tables here and populate them with the featured sources
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(NEWSPAPERS_TABLE_CREATE);
			db.execSQL(CATEGORIES_TABLE_CREATE);
		}
	
		/**
		 * On upgrade remove the old tables and put new ones
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + CATEGORIES_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + NEWSPAPERS_TABLE_NAME);
			onCreate(db);
		}
	}
	
	/**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public NewsSpeakDBAdapter(Context ctx) {
        mContext = ctx;
    }
    
	/**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public NewsSpeakDBAdapter open(int mode) throws SQLException {
        mDbHelper = new NewsSpeakDBHelper(mContext);
        
        if (mode == READ_WRITE) {
        	mDb = mDbHelper.getWritableDatabase();
        } else {
        	mDb = mDbHelper.getReadableDatabase();
        }
        return this;
    }
    
    /**
     * Close the connection
     */
    public void close() {
        mDbHelper.close();
    }
    
    /**
     * Create a new NewsSource record
     */
    public long createNewsSource(NewsSource source) {
    	ContentValues values = new ContentValues();
    	values.put("NAME", source.getTitle());
    	values.put("TYPE", source.getType().toString());
    	values.put("DEFAULTURL", source.getDefaultUrl());
    	values.put("HASCATEGORIES", source.isHasCategories());
    	values.put("SUBSCRIBED", true); // When you create a NewsSource we automatically subscribe to it
    	values.put("DISPLAYINDEX", source.getDisplayIndex()); // No display Index will be set the first time around
    	values.put("PREFERRED", source.isPreferred());
    	
    	long rowId;
    	ArrayList<String> categories;
    	ArrayList<String> categoryUrls;
		if ((rowId = mDb.insert(NEWSPAPERS_TABLE_NAME, null, values)) != 0 && source.isHasCategories()) {
			categories = source.getCategories();
			categoryUrls = source.getCategoryUrls();
			
			for (int ii = 0; ii < categories.size(); ++ii) {
	    		values = new ContentValues();
	    		values.put("CATEGORY", categories.get(ii));
	    		values.put("LINK", categoryUrls.get(ii));
	    		values.put("NAME", source.getTitle());
	    		
	    		rowId = mDb.insert(CATEGORIES_TABLE_NAME, null, values);
			}
		}
    	return rowId;
    }
    
    /**
     * Fetch all newspapers.
     */
    public ArrayList<NewsSource> fetchAllNewsPapers(boolean subscribedFilter) {
    	// TODO: Order by display order index and dont filter on columns since we need to
    	// use the newssource object (in entirety) as intent to the Plaudible class for example.
    	Cursor cursor = mDb.rawQuery("SELECT * FROM " +
    								NEWSPAPERS_TABLE_NAME +
    								" WHERE SUBSCRIBED <> ? " +
    								" ORDER BY DISPLAYINDEX ASC",
    								new String[] { "0" });

    	ArrayList<NewsSource> sources = new ArrayList<NewsSource>();
    	NewsSource source;
    	
    	// Iterate over the NewsSources and append to list.
    	if (cursor != null && cursor.moveToFirst()) {
    		while (cursor.isAfterLast() == false) {
    			source = getNewsSourceFromCursor(cursor);
    			sources.add(source);
    			cursor.moveToNext();
    			
    			// Complete building the NewsSource object with categories
    			// TODO: Optimize this to either be lazy or structure the Query acc.
    			if (source.isHasCategories()) {
    				// Get all the categories belonging to the newssource and in the same 
    				// order they were inserted into the DB. That is the order we want them
    				// displayed, not have them sorted lexicographically.
    				String query = "SELECT * FROM " + CATEGORIES_TABLE_NAME + " WHERE NAME = '" + 
    								source.getTitle() + "' ORDER BY ROWID";
    				Cursor categoryCursor = mDb.rawQuery(query, null);
    				ArrayList <String> names = new ArrayList<String>();
    				ArrayList <String> urls = new ArrayList<String>();
    				
    				if (categoryCursor != null && categoryCursor.moveToFirst()) {
    					while (categoryCursor.isAfterLast() == false) {
    						names.add(categoryCursor.getString(0));
    						urls.add(categoryCursor.getString(2));
    						categoryCursor.moveToNext();
    					}
    					source.setCategories(names);
    					source.setCategoryUrls(urls);
    				}
    			}
    		}
        }
    	cursor.close();
        return sources;
    }
    
    /**
     * Get a NewsSource object from a Cursor
     * @param cursor
     * @return
     */
    private NewsSource getNewsSourceFromCursor(Cursor cursor) {
		NewsSource source = new NewsSource();
		
		source.setTitle(cursor.getString(0));
		source.setType(NewsSource.getType(cursor.getString(1)));
		source.setDefaultUrl(cursor.getString(2));
		source.setHasCategories(cursor.getInt(3) != 0 ? true : false);
		source.setSubscribed(cursor.getInt(4) != 0 ? true : false);
		source.setDisplayIndex(cursor.getInt(5));
		source.setPreferred(cursor.getInt(6) != 0 ? true : false);
		
		return source;
	}

	/**
     * Fetch a newspaper
     * @param name
     * @turn
     */
    public Cursor fetchNewsPaper(String name) {
    	Cursor cursor = mDb.query(NEWSPAPERS_TABLE_NAME, new String[] {"NAME"}, null, null, null, null, "NAME ASC");
    	if (cursor != null) {
    		cursor.moveToFirst();
        }
        return cursor;
    }
    
    /**
     * Modify values of a record
     * @return Returns if an update was successful
     */
    public boolean modifyNewsSourceDisplayIndex(NewsSource source) {
    	String query = "UPDATE " + NEWSPAPERS_TABLE_NAME + " SET DISPLAYINDEX='" + source.getDisplayIndex() +
    					"' WHERE NAME='" + source.getTitle() + "'";
    	Cursor cursor = mDb.rawQuery(query, null);
    	cursor.close();
    	return true;
    }
}
