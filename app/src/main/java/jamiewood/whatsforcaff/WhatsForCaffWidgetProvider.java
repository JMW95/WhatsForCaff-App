package jamiewood.whatsforcaff;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

public class WhatsForCaffWidgetProvider extends AppWidgetProvider {

	private PendingIntent service = null;
	
	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions){
		appWidgetManager.updateAppWidget(appWidgetId, getRemoteViews(context, newOptions));
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		
		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if(service == null){
			Intent i = new Intent(context, WFCService.class);
			service = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		}
		
		m.setRepeating(AlarmManager.RTC, 0, AlarmManager.INTERVAL_HOUR, service);
	}
	
	@Override
	public void onDisabled(Context context){
		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		m.cancel(service);
	}
	
	public static RemoteViews getRemoteViews(Context context, Bundle extras){
		int minHeight = extras.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		
		int layoutId = R.layout.whatsforcaff_widget;
		
		if(minHeight>100){
			layoutId = R.layout.whatsforcaff_widget_tall;
		}
		
		RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
		try {
			if(extras.containsKey("menu")){
				updateRemoteViews(context, views, new JSONObject(extras.getString("menu")));
			}else{
				views.setTextViewText(R.id.txtTitle, "What's for Caff?");
				views.setTextViewText(R.id.txtItem, "Fetching menu...");
				
				// get cached menu from shared prefs
				SharedPreferences sp = context.getSharedPreferences("menustore", Context.MODE_PRIVATE);
				if(sp.contains("menu")){
					updateRemoteViews(context, views, new JSONObject(sp.getString("menu","")));
				}
				
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return views;
	}
	
	public static void updateRemoteViews(Context context, RemoteViews views, JSONObject menu){
			if(!menu.has("error")){
				// CHECK IF AN UPDATE IS AVAILABLE
				boolean updateReady = false;
				try{
					JSONObject appInfo = menu.getJSONObject("mobileapp");
					updateReady = !appInfo.getString("latest_version").equals(WFCService.VERSION);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				String[] days = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
				
				Calendar cal = Calendar.getInstance();
				String today = days[cal.get(Calendar.DAY_OF_WEEK)-1];
				
				JSONObject menutoday;
				String lunchstr = "";
				String dinnerstr = "";
				try{
					menutoday = menu.getJSONObject(today);
					
					// Lunch
					try{
						JSONObject lunch = menutoday.getJSONObject("lunch");
						if(lunch.getString("type").equals("none")){
							lunchstr = "No lunch today.";
						}else if(lunch.getString("type").equals("brunch")){
							lunchstr = "Brunch";
						}else{
							lunchstr = lunch.getString("main") + "\n" + lunch.getString("veg");
						}
					} catch (JSONException e) {
						e.printStackTrace();
						lunchstr = "Error parsing lunch menu!";
					}
					
					// Dinner
					try{
						JSONObject dinner = menutoday.getJSONObject("dinner");
						if(dinner.getString("type").equals("none")){
							dinnerstr = "No dinner today.";
						}else{
							dinnerstr = dinner.getString("main1") + 
								(dinner.optString("main2", "").trim().length()>0 ? " or\n" + dinner.getString("main2") : "") + "\n" +
								dinner.getString("veg");
						}
					} catch (JSONException e) {
						e.printStackTrace();
						dinnerstr = "Error parsing dinner menu!";
					}
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				Intent i = new Intent(context, MenuDialog.class);
				PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
				
				views.setOnClickPendingIntent(R.id.whole_widget, pi);
				if(updateReady){
					views.setViewVisibility(R.id.ll_update_prompt, View.VISIBLE);
				}
				
				switch(views.getLayoutId()){
				case R.layout.whatsforcaff_widget_tall:
					
					views.setTextViewText(R.id.txtTitle, "Lunch: " + today);
					views.setTextViewText(R.id.txtItem, lunchstr);
					views.setTextViewText(R.id.txtTitle2, "Dinner: " + today);
					views.setTextViewText(R.id.txtItem2, dinnerstr);
					break;
					
				case R.layout.whatsforcaff_widget:
					
					if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 14){ // after lunch so show dinner
						views.setTextViewText(R.id.txtTitle, "Dinner: " + today);
						views.setTextViewText(R.id.txtItem, dinnerstr);
					}else{
						views.setTextViewText(R.id.txtTitle, "Lunch: " + today);
						views.setTextViewText(R.id.txtItem, lunchstr);
					}
					
					break;
				}
			}else{
				
				Intent i = new Intent(context, WFCService.class);
				PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
				views.setOnClickPendingIntent(R.id.whole_widget, pi);
				
				// There was an error, so show some error text instead of the menu 
				switch(views.getLayoutId()){
				case R.layout.whatsforcaff_widget_tall:
					views.setTextViewText(R.id.txtTitle, "An error occurred");
					try {
						views.setTextViewText(R.id.txtItem, menu.getString("error"));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					views.setTextViewText(R.id.txtTitle2, "");
					views.setTextViewText(R.id.txtItem2, "");
					break;
				case R.layout.whatsforcaff_widget:
					views.setTextViewText(R.id.txtTitle, "An error occurred");
					try{
						views.setTextViewText(R.id.txtItem, menu.getString("error"));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					break;
				}
			}
			
		
	}
	
}
