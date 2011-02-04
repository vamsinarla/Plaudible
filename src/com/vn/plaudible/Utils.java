package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLConnection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Simple Utility functions
 * @author narla
 *
 */
public class Utils {
	/**
	 * Get a string from an inputStream
	 * @param inputStream
	 * @return
	 * @throws IOException 
	 */
	public static String getStringFromInputStream(InputStream inputStream) 
	throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder builder = new StringBuilder();
		String oneLine;
		
		while ((oneLine = reader.readLine()) != null) {
			builder.append(oneLine);
		}
		reader.close();
		
		return builder.toString();
	}

	/**
	 * Write POST variables to the connection
	 * @param conn
	 * @param args
	 * @throws IOException
	 */
	public static void postVars(URLConnection conn, String args) throws IOException {
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	    wr.write(args);
	    wr.flush();
	}
	
	
	/**
	 * Return the status of data connectivity
	 * @return boolean
	 */
	public static boolean checkDataConnectivity(Context context) {
		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
		
		if(networkInfo != null && networkInfo.isConnected()){
			return true;
		}
		return false;
	}
}
