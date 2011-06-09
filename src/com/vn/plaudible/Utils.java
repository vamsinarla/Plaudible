package com.vn.plaudible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

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

	/**
	 * Utility function to generate a TinyURL useful for sharing links
	 * @param url
	 * @return
	 */
	public static String generateTinyUrl(String url) {
		String tinyUrl;
		try {
            HttpClient client = new DefaultHttpClient();
            String urlTemplate = "http://tinyurl.com/api-create.php?url=%s";
            String uri = String.format(urlTemplate, URLEncoder.encode(url));
            HttpGet request = new HttpGet(uri);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            try {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    // TODO: Support other encodings
                    String enc = "utf-8";
                    Reader reader = new InputStreamReader(in, enc);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    tinyUrl = bufferedReader.readLine();
                    if (tinyUrl != null) {
                        return tinyUrl;
                    } else {
                        throw new IOException("empty response");
                    }
                } else {
                    String errorTemplate = "unexpected response: %d";
                    String msg = String.format(errorTemplate, statusCode);
                    throw new IOException(msg);
                }
            } finally {
                in.close();
            }
        } catch (Exception exception) {
        	// In case we couldn't generate a short URL send the original back
        	return url;
        }
	}
}
