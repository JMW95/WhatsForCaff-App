package jamiewood.whatsforcaff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MenuGetterTask extends AsyncTask<Object, Void, JSONObject>{

	private int[] wids;
	private Context ctx;
	
	public MenuGetterTask(Context context){
		this.ctx = context;
	}
	
	@Override
	protected JSONObject doInBackground(Object... params) {
		
		wids = (int[])params[0];
		
		StringBuilder stringBuilder = new StringBuilder();
		HttpClient httpClient = new DefaultHttpClient();
		
		long timestamp = new Date().getTime()/1000L;
		
		try{
			
			// set up post request to the whatsforcaff api
			HttpPost httpPost = new HttpPost("https://hypernerd.co.uk/caff/api");
			List<NameValuePair> postData = new ArrayList<NameValuePair>();
			postData.add(new BasicNameValuePair("timestamp",Long.toString(timestamp)));
			postData.add(new BasicNameValuePair("source","Widget " + WFCService.VERSION));
			
			// load device_id from SharedPreferences
			SharedPreferences sp = ctx.getSharedPreferences("menustore", Context.MODE_PRIVATE);
			if(sp.contains("uuid")){
				System.out.println("Loaded UUID: " + sp.getString("uuid", ""));
				postData.add(new BasicNameValuePair("uuid", sp.getString("uuid", "")));
			}else{
				System.out.println("No UUID stored, making request without it.");
			}
			
			httpPost.setEntity(new UrlEncodedFormEntity(postData));
			
			HttpResponse response = httpClient.execute(httpPost);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if(statusCode == 200){
				HttpEntity entity = response.getEntity();
				InputStream inputStream = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				while((line=reader.readLine())!=null){
					stringBuilder.append(line);
				}
				inputStream.close();
			}else{
				System.out.println("Bad response code from server: " + statusCode);
			}
			
			try {
				JSONObject json = new JSONObject(stringBuilder.toString());
				
				return json;
				
			} catch (JSONException e) {
				e.printStackTrace();
				
				System.out.println(stringBuilder.toString());
				
				JSONObject json = new JSONObject();
				try { json.put("error", "Server didn't send correct data!"); } catch (JSONException e1) {e1.printStackTrace();}
				return json;
			}
			
		}catch(IOException e){
			e.printStackTrace();
			JSONObject json = new JSONObject();
			try { json.put("error", "Error connecting to server!"); } catch (JSONException e1) {e1.printStackTrace();}
			return json;
		}
	}
	
	@Override
	public void onPostExecute(JSONObject result){
		
		// store downloaded menu and device_id in SharedPreferences
		SharedPreferences sp = ctx.getSharedPreferences("menustore", Context.MODE_PRIVATE);
		Editor ed = sp.edit();
		
		if(result.has("mobileapp")){
			try{
				JSONObject appInfo = result.getJSONObject("mobileapp");
				if(appInfo.has("uuid")){
					ed.putString("uuid", appInfo.getString("uuid"));
					System.out.println("Storing server-provided UUID: " + appInfo.getString("uuid"));
				}
			}catch(JSONException e){ e.printStackTrace(); }
		}
		
		ed.putString("menu", result.toString());
		ed.commit();
		
		if(result.has("error")){
			try {
				Toast.makeText(ctx, "ERROR: " + result.getString("error"), Toast.LENGTH_LONG).show();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
		for(int i : wids){
			int minHeight = awm.getAppWidgetOptions(i).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
			int layoutId = R.layout.whatsforcaff_widget;
			if(minHeight>100){
				layoutId = R.layout.whatsforcaff_widget_tall;
			}
			
			RemoteViews views = new RemoteViews(ctx.getPackageName(), layoutId);
			WhatsForCaffWidgetProvider.updateRemoteViews(ctx, views, result);
			awm.updateAppWidget(i, views);
		}
	}

}
