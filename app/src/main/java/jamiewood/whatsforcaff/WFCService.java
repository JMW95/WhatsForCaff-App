package jamiewood.whatsforcaff;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;

public class WFCService extends Service {

	public static String VERSION = "0.62";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		buildUpdate();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void buildUpdate(){
		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		int[] ids = awm.getAppWidgetIds(new ComponentName(this, WhatsForCaffWidgetProvider.class));
		
		if(ids.length>0){ // if there are actually some widgets to update
			new MenuGetterTask(this).execute(ids);
			System.out.println("Building update!");
		}
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
