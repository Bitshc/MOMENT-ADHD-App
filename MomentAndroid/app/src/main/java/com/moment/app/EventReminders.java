package com.moment.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import java.util.*;

final class EventReminders {
 static final String CHANNEL="event_reminders";
 static void channel(Context c){c.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Gentle event reminders",NotificationManager.IMPORTANCE_DEFAULT));}
 static boolean allowed(Context c){channel(c);return (Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&c.getSystemService(NotificationManager.class).areNotificationsEnabled()&&c.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
 static PendingIntent alarm(Context c,long id,int stage,long arrival){return PendingIntent.getBroadcast(c,0,new Intent(c,EventReminderReceiver.class).setAction("com.moment.app.EVENT_REMINDER").setData(Uri.parse("moment://reminder/"+id+"/"+stage)).putExtra("id",id).putExtra("stage",stage).putExtra("arrival",arrival),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void clear(Context c,long id){for(int s=0;s<ReminderPlan.MINUTES.length;s++)c.getSystemService(AlarmManager.class).cancel(alarm(c,id,s,0));c.getSystemService(NotificationManager.class).cancel("event-"+id,1);}
 static synchronized void reschedule(Context c){
  // Runs on a DB/background thread. Keep the prior ID set so deleted events are cancelled too.
  android.content.SharedPreferences prefs=c.getSharedPreferences("event_reminder_state",0);
  for(String id:prefs.getStringSet("scheduled",Collections.emptySet()))try{clear(c,Long.parseLong(id));}catch(NumberFormatException ignored){}
  HashSet<String> scheduled=new HashSet<>();long now=System.currentTimeMillis();
  try(Store store=new Store(c);Cursor rows=store.reminderEvents()){
   while(rows.moveToNext()){
    long id=rows.getLong(0);scheduled.add(String.valueOf(id));
    if(!allowed(c))continue;
    try{
     long at=ReminderPlan.arrival(rows.getString(2),rows.getString(3),ZoneId.systemDefault());
     for(int stage=0;stage<ReminderPlan.MINUTES.length;stage++)if(ReminderPlan.trigger(at,stage)>now || (ReminderPlan.latestDue(at,now)==stage && prefs.getLong("delivered-"+id+"-"+stage,0)!=at))
      c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,Math.max(now,ReminderPlan.trigger(at,stage)),alarm(c,id,stage,at));
    }catch(IllegalArgumentException|DateTimeException ignored){}
   }
  }
  prefs.edit().putStringSet("scheduled",scheduled).apply();
 }
 static synchronized void deliver(Context c,long id,int stage,long expectedArrival){
  if(stage<0||stage>=ReminderPlan.MINUTES.length||!allowed(c))return;
  try(Store store=new Store(c);Cursor row=store.reminderEvent(id)){
   if(!row.moveToFirst())return;
   long at=ReminderPlan.arrival(row.getString(2),row.getString(3),ZoneId.systemDefault()),now=System.currentTimeMillis();
   if(at!=expectedArrival||ReminderPlan.latestDue(at,now)!=stage)return;
   String key="delivered-"+id+"-"+stage;
   android.content.SharedPreferences state=c.getSharedPreferences("event_reminder_state",0);
   if(state.getLong(key,0)==at)return;
   long minutes=Math.max(1,(at-now+59999)/60000);
   String body=stage==0?"A gentle heads-up: ":stage==1?"A little time to get ready: ":"A quick check-in: ";
   body+=row.getString(1)+" starts in about "+minutes+" minutes. Take the next small step.";
   Intent open=new Intent(c,MainActivity.class).setData(Uri.parse("moment://event/"+id)).putExtra("event_day",row.getString(2));
   PendingIntent content=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("MOMENT · your event is coming up").setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(content).setAutoCancel(true);
   String address=row.getString(4);
   if(!address.isEmpty()){
    PendingIntent maps=PendingIntent.getActivity(c,0,MapsLink.intent(address,""),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    n.addAction(new Notification.Action.Builder(null,"Open Maps",maps).build());
   }
   c.getSystemService(NotificationManager.class).notify("event-"+id,1,n.build());state.edit().putLong(key,at).commit();
  }catch(IllegalArgumentException|DateTimeException ignored){}
 }
}
