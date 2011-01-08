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
                "HASCATEGORIES " + " BOOLEAN NOT NULL" +
                "DISPLAYED " + "BOOLEAN NOT NULL" +
                "SUBSCRIBED " + "BOOLEAN NOT NULL" +
                "DISPLAYINDEX " + "INTEGER" +
                "PREFERRED " + "BOOLEAN" +
                ");";
    private static final String CATEGORIES_TABLE_NAME = "CATEGORIES";
    private static final String CATEGORIES_TABLE_CREATE = 
    			"CREATE TABLE " + CATEGORIES_TABLE_NAME + " (" +
    			"CATEGORY " + "TEXT NOT NULL, " +
    			"NEWSPAPER " + "TEXT NOT NULL, " +
    			"LINK " + "TEXT NOT NULL, " +
    			"FOREIGNKEY(NEWSPAPER) REFERENCES NEWSPAPERS(NAME), " +
    			"PRIMARY KEY(NEWSPAPER, CATEGORY)" +
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
     * Open the notes database. If it cannot be opened, try to create a new
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
    	values.put("DEFAULTURL", source.getTitle());
    	values.put("TYPE", source.getType().toString());
    	values.put("HASCATEGORIES", source.isHasCategories());
    	values.put("DISPLAYED", source.isDisplayed());
    	values.put("SUBSCRIBED", source.isSubscribed());
    	values.put("DISPLAYINDEX", source.getDisplayIndex());
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
	    		values.put("NEWSPAPER", source.getTitle());
	    		
	    		rowId = mDb.insert(CATEGORIES_TABLE_NAME, null, values);
			}
		}
    	return rowId;
    }
    
    /**
     * Fetch all newspapers.
     */
    public Cursor fetchAllNewsPapers() {
    	Cursor cursor = mDb.query(NEWSPAPERS_TABLE_NAME, new String[] {"NAME"}, null, null, null, null, null);
    	if (cursor != null) {
    		cursor.moveToFirst();
        }
        return cursor;
    }
    
    /**
     * Fetch a newspaper
     * @param name
     * @return
     */
    public Cursor fetchNewsPaper(String name) {
    	
    }
}
