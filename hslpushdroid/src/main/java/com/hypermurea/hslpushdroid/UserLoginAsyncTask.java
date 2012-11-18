package com.hypermurea.hslpushdroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.AsyncTask;
import android.util.Log;

public class UserLoginAsyncTask extends AsyncTask<String,Void,List<String>> {

	public static final int UUID = 0;
	public static final int REGISTRATION_ID = 1;
	
	private final String TAG = "UserLoginAsyncTask";
	
	private UserSignalListener listener;
	private String serviceUrl;
	
	public UserLoginAsyncTask(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}
	
	@Override
	protected List<String> doInBackground(String... params) {
		String queryString = "/gcm?uuid=" + params[UUID] + "&regId=" + params[REGISTRATION_ID]; 

		HttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(serviceUrl + queryString);
		
		Log.d(TAG, serviceUrl+queryString);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();

		List<String> linesOfInterest = null;
		
		try {
			String responseString = httpClient.execute(request, responseHandler);
			JSONArray response = new JSONArray(responseString);
			
			linesOfInterest = new ArrayList<String>();
			for( int i = 0; i < response.length(); i ++) {
				linesOfInterest.add(response.getString(i));
			}	
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return linesOfInterest;
	}
	
	@Override
	public void onPostExecute(List<String> result) {
		if(result != null) {
			listener.signalUserLoggedIn(result);
		} else {
			listener.signalLoginFailed();
		}
	}
	
	public void setUserSignalListener(UserSignalListener listener) {
		this.listener = listener;
	}

}
