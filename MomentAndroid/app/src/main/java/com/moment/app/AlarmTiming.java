package com.moment.app;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
final class AlarmTiming {
 static boolean precise(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 static void schedule(Context c,long at,PendingIntent pending){AlarmManager am=c.getSystemService(AlarmManager.class);try{if(precise(c)){am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending);return;}}catch(SecurityException ignored){}am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending);}
 static void settings(MainActivity a){if(Build.VERSION.SDK_INT>=31)a.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+a.getPackageName())));}
}
