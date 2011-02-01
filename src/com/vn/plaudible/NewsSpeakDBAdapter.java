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
	
	/**
	 * DB related vars
	 */
	private int numberOfNewsSources;

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "NEWSSPEAKDATABASE";
	private static final String NEWSPAPERS_TABLE_NAME = "NEWSPAPERS";
    private static final String NEWSPAPERS_TABLE_CREATE =
                "CREATE TABLE " + NEWSPAPERS_TABLE_NAME + " (" +
                "NAME " + " TEXT PRIMARY KEY, " +
                "TYPE " + " TEXT, " +
                "DEFAULTURL " + " TEXT NOT NULL, " +
                "COUNTRY " + " TEXT NOT NULL, " +
                "LANGUAGE " + " TEXT NOT NULL, " +
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
     * Upgrade the Database. THIS FUNCTION IS TO BE USED ONLY IN DEV 
     */
    public void upgrade() {
    	if (mDbHelper != null) {
    		mDbHelper.onUpgrade(mDb, 0, 1);
    	}
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
    public long createNewsSource(NewsSource source) throws SQLException {
    	ContentValues values = new ContentValues();
    	values.put("NAME", source.getTitle());
    	values.put("TYPE", source.getType().toString());
    	values.put("COUNTRY", source.getCountry());
    	values.put("LANGUAGE", source.getLanguage());
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
	    		
	    		rowId = mDb.insertOrThrow(CATEGORIES_TABLE_NAME, null, values);
			}
		}
		
		// Increment the number of sources
		++numberOfNewsSources;
    	return rowId;
    }
    
    /**
     * Fetch all newspapers, corresponding to particular selection criteria.
     * To filter out certain columns only
     * @param sources The Arraylist to populate
     * @param columns The columns to fetch. Put * for all columns and comma separated list otherwise
     * @param fetchFullObject true or false since this function is dumb enough
     * 			not to understand / infer from columns arg
     */
    public void fetchAllNewsPapers(ArrayList<NewsSource> sources, String columns, boolean fetchFullObject) {
    	// Filtering based on columns
    	Cursor cursor = mDb.rawQuery("SELECT " +  columns + " FROM " +
    								NEWSPAPERS_TABLE_NAME +
    								" WHERE SUBSCRIBED <> ? " +
    								" ORDER BY DISPLAYINDEX ASC",
    								new String[] { "0" });

    	NewsSource source;
    	// Iterate over the NewsSources and append to list.
    	if (cursor != null && cursor.moveToFirst()) {
    		while (cursor.isAfterLast() == false) {
    			// We are asking for a full object or not
    			source = getNewsSourceFromCursor(cursor, fetchFullObject);
    			sources.add(source);
    			cursor.moveToNext();
    			
    			// Complete building the NewsSource object with categories
    			if (fetchFullObject && source.isHasCategories()) {
    				// Get all the categories belonging to the newssource and in the same 
    				// order they were inserted into the DB. That is the order we want them
    				// displayed, not have them sorted lexicographically.
    				String query = "SELECT * FROM " + CATEGORIES_TABLE_NAME + " WHERE NAME = ? ORDER BY ROWID";

    				// Generate the query
    				Cursor categoryCursor = mDb.rawQuery(query, new String[] { source.getTitle() });
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
    				// Close the cursor
    				categoryCursor.close();
    			}
    		}
        }
    	
    	// Close the cursor
    	cursor.close();
    	numberOfNewsSources = sources.size();
    }
    
    /**
     * Get a NewsSource object from a Cursor
     * TODO make this function more robust
     * @param cursor
     * @return
     */
    private NewsSource getNewsSourceFromCursor(Cursor cursor, boolean isFullObject) {
    	if (!isFullObject) {
    		NewsSource source = new NewsSource();
    		
    		source.setTitle(cursor.getString(0));
    		source.setType(cursor.getString(1));
    		source.setDisplayIndex(cursor.getInt(2));
    		
    		return source;
    	} else if (cursor.getColumnCount() < 9) { // We need these many for full object
			return null;
		} else { // Ok, we have been asked for a full object and we have the columns
			NewsSource source = new NewsSource();
			
			source.setTitle(cursor.getString(0));
			source.setType(NewsSource.getType(cursor.getString(1)));
			source.setDefaultUrl(cursor.getString(2));
			source.setCountry(cursor.getString(3));
			source.setLanguage(cursor.getString(4));
			source.setHasCategories(cursor.getInt(5) != 0 ? true : false);
			source.setSubscribed(cursor.getInt(6) != 0 ? true : false);
			source.setDisplayIndex(cursor.getInt(7));
			source.setPreferred(cursor.getInt(8) != 0 ? true : false);
			
			return source;
		}
	}

	/**
     * Check if a newspaper exists in the DB and return if it does.
     * This function ALWAYS returns the complete object
     * @param name
     * @return Returns null if the newspaper did not exist, otherwise returns the COMPLETE NewsSource object for that
     */
    public NewsSource getNewsPaper(String name) {
    	Cursor cursor = mDb.query(NEWSPAPERS_TABLE_NAME, null, "NAME = ?", new String[]{name}, null, null, null);
    	if (cursor != null && cursor.getCount() != 0) {
    		cursor.moveToFirst();
        } else {
        	cursor.close();
        	return null;
        }
    	
    	NewsSource source = getNewsSourceFromCursor(cursor, true);
    	
    	// Complete building the NewsSource object with categories info.
		if (source.isHasCategories()) {
			// Get all the categories belonging to the newssource and in the same 
			// order they were inserted into the DB. That is the order we want them
			// displayed, not have them sorted lexicographically.
			String query = "SELECT * FROM " + CATEGORIES_TABLE_NAME + " WHERE NAME = ? ORDER BY ROWID";

			// Generate the query
			Cursor categoryCursor = mDb.rawQuery(query, new String[] { source.getTitle() });
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
			// Close the cursor
			categoryCursor.close();
		}
		cursor.close();
    	
    	// Get newspaper should always get full object
    	return source;
    }
    
    /**
     * Modify values of a record
     * @return Returns if an update was successful
     */
    public boolean modifyNewsSourceDisplayIndex(NewsSource source) {
    	ContentValues values = new ContentValues();
    	values.put("DISPLAYINDEX", source.getDisplayIndex());
    	mDb.update(NEWSPAPERS_TABLE_NAME, values, "NAME = ?", new String[]{source.getTitle()});
    	return true;
    }

    /**
     * Remove a particular NewsSource
     * @param newsSource
     */
	public int removeNewsSource(NewsSource newsSource) {
		int rowsAffected = mDb.delete(NEWSPAPERS_TABLE_NAME, "NAME = ?", new String[]{newsSource.getTitle()});
		if (rowsAffected != 0) {
			--numberOfNewsSources;
		}
		return rowsAffected;
	}
	
	/**
	 * Cache the numbder of subscribed sources to prevent frequent DB access
	 * @return
	 */
	public int getNumberOfNewsSources() {
		return numberOfNewsSources;
	}
}
