package jamiewood.whatsforcaff;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Util {

    public static String VERSION = "0.62";
    public static String SHARED_PREFS_NAME = "menustore";

    public static String getMenuDateString(){
        // Get today's date
        Calendar cal = Calendar.getInstance();
        // Advance to next sunday's date
        cal.add(Calendar.DAY_OF_WEEK, 7 - (cal.get(Calendar.DAY_OF_WEEK) - 1));

        SimpleDateFormat sdf = new SimpleDateFormat("'menu'_dd_MM_yyyy");

        return sdf.format(cal.getTime());
    }

}
