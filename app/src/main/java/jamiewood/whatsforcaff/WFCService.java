package jamiewood.whatsforcaff;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WFCService extends Service {
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		buildUpdate();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void buildUpdate(){
		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		int[] ids = awm.getAppWidgetIds(new ComponentName(this, WhatsForCaffWidgetProvider.class));
		
		if(ids.length>0){ // if there are actually some widgets to update
			SharedPreferences sp = this.getSharedPreferences(Util.SHARED_PREFS_NAME, Context.MODE_PRIVATE);

			String dateStr = Util.getMenuDateString();

			if(sp.contains(dateStr)) {
				for(int i : ids){
					int minHeight = awm.getAppWidgetOptions(i).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
					int layoutId = R.layout.whatsforcaff_widget;
					if(minHeight>100){
						layoutId = R.layout.whatsforcaff_widget_tall;
					}

					RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), layoutId);
					try {
						WhatsForCaffWidgetProvider.updateRemoteViews(getApplicationContext(), views, new JSONObject(sp.getString(dateStr, "")));
					}catch(JSONException e){
						e.printStackTrace();
					}
					awm.updateAppWidget(i, views);
				}
				System.out.println("Rendering update!");
			}else{
				new MenuGetterTask(this).execute(ids);
				System.out.println("Downloading menu!");
			}
		}
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
