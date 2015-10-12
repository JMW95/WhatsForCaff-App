package jamiewood.whatsforcaff;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

public class MenuDialog extends FragmentActivity{

	MenuPagerAdapter menuPagerAdapter;
	ViewPager viewPager;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);	
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_menu_dialog);
		
		((Button)this.findViewById(R.id.btn_dialog_dismiss)).setOnClickListener(new OnClickListener(){
			@Override public void onClick(View v){
				MenuDialog.this.finish();
			}
		});
		
		SharedPreferences sp = getSharedPreferences("menustore", Context.MODE_PRIVATE);
		
		String jsonstr = sp.getString("menu", "");
		// show update link if there is an update
		try {
			JSONObject json = new JSONObject(jsonstr);
			final JSONObject appInfo = json.getJSONObject("mobileapp");
			final String updateUrl = appInfo.getString("link");
			if(!appInfo.getString("latest_version").equals(WFCService.VERSION)){
				View tv = this.findViewById(R.id.txt_update_link);
				
				DisplayMetrics metrics = this.getResources().getDisplayMetrics();
				int dpInPx32 = (int)(32 * (metrics.densityDpi / 160f));
				this.findViewById(R.id.menu_pager).getLayoutParams().height -= dpInPx32; // reduce the height of the menu_pager by 32dp
				
				tv.setVisibility(View.VISIBLE); // show the update link at the top of the dialog
				tv.setOnClickListener(new OnClickListener(){
					@Override public void onClick(View v){
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)); // open the update url when clicked
						startActivity(browserIntent);
					}
				});
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		menuPagerAdapter = new MenuPagerAdapter(getSupportFragmentManager(), jsonstr);
		
		viewPager = (ViewPager) findViewById(R.id.menu_pager);
		viewPager.setAdapter(menuPagerAdapter);
	}
	
	public class MenuPagerAdapter extends FragmentStatePagerAdapter {
		
		public JSONObject[] menus;
		public int numDays = 0;
		
		public MenuPagerAdapter(FragmentManager fm, String json){
			super(fm);
			
			try {
				JSONObject week = new JSONObject(json);
				
				String[] days = new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
				
				Calendar cal = GregorianCalendar.getInstance();
				int today = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // rotate Monday to be 0, Tuesday 1 etc
				
				numDays = 7 - today;
				
				menus = new JSONObject[days.length - today];
				for(int i=0; i<menus.length; i++){
					menus[i] = week.getJSONObject(days[today + i]);
					menus[i].put("day", days[today + i]);
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public Fragment getItem(int i){
			Fragment fragment = new MenuFragment();
			Bundle args = new Bundle();
			
			// fill in the json menu for this fragment here
			args.putString(MenuFragment.ARG_MENU, menus[i].toString());
			fragment.setArguments(args);
			
			return fragment;
		}
		
		@Override
		public int getCount(){
			return numDays;
		}
		
	}
	
	public class MenuFragment extends Fragment {
		public static final String ARG_MENU = "menu";
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
			View rootView = inflater.inflate(R.layout.fragment_menu_object, container, false);
			Bundle args = getArguments();
			
			try {
				JSONObject menu = new JSONObject(args.getString(ARG_MENU));
				JSONObject lunch = menu.getJSONObject("lunch");
				JSONObject dinner = menu.getJSONObject("dinner");
				
				// LUNCH
				String lunchTitleStr = menu.getString("day") + ": ";
				if(lunch.getString("type").equals("lunch")){
					lunchTitleStr += "Lunch";
					if(lunch.optString("brain", "").trim().length()>0){ // check if there is a brain food
						((TextView)rootView.findViewById(R.id.lbl_lunch_brainfood)).setText(lunch.getString("brain"));
					}else{
						rootView.findViewById(R.id.tbl_row_lunch_brainfood).setVisibility(View.GONE);
					}
					if(lunch.optString("soup", "").trim().length()>0){ // check if there is a soup
						((TextView)rootView.findViewById(R.id.lbl_lunch_soup)).setText(lunch.getString("soup"));
					}else{
						rootView.findViewById(R.id.tbl_row_lunch_soup).setVisibility(View.GONE);
					}
					((TextView)rootView.findViewById(R.id.lbl_lunch_main)).setText(lunch.getString("main"));
					((TextView)rootView.findViewById(R.id.lbl_lunch_vege)).setText(lunch.getString("vege"));
					if(lunch.optString("deli", "").trim().length()>0){ // check if there is a sandwich
						((TextView)rootView.findViewById(R.id.lbl_lunch_deli)).setText(lunch.getString("deli"));
					}else{
						rootView.findViewById(R.id.tbl_row_lunch_deli).setVisibility(View.GONE);
					}
					((TextView)rootView.findViewById(R.id.lbl_lunch_veg)).setText(lunch.getString("veg"));
					if(lunch.optString("dess", "").trim().length()>0){ // check if there is a dessert
						((TextView)rootView.findViewById(R.id.lbl_lunch_dess)).setText(lunch.getString("dess"));
					}else{
						rootView.findViewById(R.id.tbl_row_lunch_dessert).setVisibility(View.GONE);
					}
				}else if(lunch.getString("type").equals("brunch")){
					lunchTitleStr += "Brunch";
					rootView.findViewById(R.id.tbl_lunch).setVisibility(View.GONE);
					TableLayout tl = (TableLayout)rootView.findViewById(R.id.tbl_brunch);
					tl.setVisibility(View.VISIBLE);
					JSONArray items = lunch.getJSONArray("items");
					for(int i=0; i<items.length(); i++){
						tl.addView(makeBrunchRow(items.getString(i)));
					}
				}else if(lunch.getString("type").equals("none")){
					lunchTitleStr += "No Lunch";
					rootView.findViewById(R.id.tbl_lunch).setVisibility(View.GONE);
				}
				((TextView)rootView.findViewById(R.id.lbl_lunch_header)).setText(lunchTitleStr);
				
				// DINNER
				String dinnerTitleStr = menu.getString("day") + ": ";
				if(dinner.optString("type", "dinner").equals("dinner")){
					dinnerTitleStr += "Dinner";
					
					if(dinner.optString("soup", "").trim().length()>0){ // check if there is a soup
						((TextView)rootView.findViewById(R.id.lbl_dinner_soup)).setText(dinner.getString("soup"));
					}else{
						rootView.findViewById(R.id.tbl_row_dinner_soup).setVisibility(View.GONE);
					}
					((TextView)rootView.findViewById(R.id.lbl_dinner_main1)).setText(dinner.getString("main1"));
					if(dinner.optString("main2", "").trim().length()>0){ // check if there is a main2
						((TextView)rootView.findViewById(R.id.lbl_dinner_main2)).setText(dinner.getString("main2"));
					}else{
						rootView.findViewById(R.id.tbl_row_dinner_main_2).setVisibility(View.GONE);
					}
					((TextView)rootView.findViewById(R.id.lbl_dinner_vege)).setText(dinner.getString("vege"));
					((TextView)rootView.findViewById(R.id.lbl_dinner_veg)) .setText(dinner.getString("veg"));
					((TextView)rootView.findViewById(R.id.lbl_dinner_dess)).setText(dinner.getString("dess"));
				}else if(dinner.optString("type", "dinner").equals("none")){
					dinnerTitleStr += "No Dinner";
					rootView.findViewById(R.id.tbl_dinner).setVisibility(View.GONE);
				}
				((TextView)rootView.findViewById(R.id.lbl_dinner_header)).setText(dinnerTitleStr);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return rootView;
		}
	}
	
	public View makeBrunchRow(String item){
		DisplayMetrics metrics = this.getResources().getDisplayMetrics();
		int dpInPx5 = (int)(5 * (metrics.densityDpi / 160f));
		int dpInPx4 = (int)(4 * (metrics.densityDpi / 160f));
		
		TableRow tr = new TableRow(this);
		tr.setPadding(dpInPx5, dpInPx5, dpInPx5, dpInPx5);
		
		TextView tv = new TextView(this);
		LayoutParams tvp = new LayoutParams();
		tvp.setMargins(dpInPx4, 0, dpInPx4, 0);
		tv.setLayoutParams(tvp);
		tv.setTextAppearance(this, android.R.style.TextAppearance_Medium);
		tv.setText(item);
		
		tr.addView(tv);
		
		return tr;
	}
	
}
