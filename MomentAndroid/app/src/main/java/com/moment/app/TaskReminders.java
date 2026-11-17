package com.moment.app;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import java.util.*;

final class TaskReminders {
 static final String CHANNEL="task_chore_reminders";
 static void channel(Context c){NotificationChannel channel=new NotificationChannel(CHANNEL,"To-do and chore reminders",NotificationManager.IMPORTANCE_HIGH);channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);c.getSystemService(NotificationManager.class).createNotificationChannel(channel);}
 static boolean allowed(Context c){channel(c);return c.getSystemService(NotificationManager.class).areNotificationsEnabled()&&(android.os.Build.VERSION.SDK_INT<33||c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)&&c.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
 static PendingIntent pending(Context c,long id,String action,long target,String day){return PendingIntent.getBroadcast(c,0,new Intent(c,TaskReminderReceiver.class).setAction(action).setData(Uri.parse("moment://task/"+id+"/"+action+"/"+target)).putExtra("id",id).putExtra("target",target).putExtra("day",day),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static PendingIntent alarm(Context c,long id,long target){return PendingIntent.getBroadcast(c,0,new Intent(c,TaskReminderReceiver.class).setAction("FIRE").setData(Uri.parse("moment://task-alarm/"+id)).putExtra("id",id).putExtra("target",target),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static long target(Cursor row){long snooze=row.getLong(9);return snooze>0?snooze:TaskPlan.due(row.getString(3),row.getString(5));}
 static synchronized void reschedule(Context c){
  android.content.SharedPreferences state=c.getSharedPreferences("task_reminder_state",0);Set<String> old=new HashSet<>(state.getStringSet("ids",Collections.emptySet()));Set<String> active=new HashSet<>();
  for(String id:old)try{c.getSystemService(AlarmManager.class).cancel(alarm(c,Long.parseLong(id),0));}catch(NumberFormatException ignored){}
  try(Store db=new Store(c);Cursor rows=db.taskReminders()){while(rows.moveToNext())try{long id=rows.getLong(0),at=target(rows);active.add(String.valueOf(id));if(at!=rows.getLong(10)&&allowed(c))AlarmTiming.schedule(c,Math.max(System.currentTimeMillis(),at),alarm(c,id,at));}catch(java.time.DateTimeException|IllegalArgumentException ignored){}}
  old.removeAll(active);for(String id:old)c.getSystemService(NotificationManager.class).cancel("task-"+id,1);state.edit().putStringSet("ids",active).apply();
 }
 static synchronized void receive(Context c,Intent intent){
  if("TEST".equals(intent.getAction())){if(allowed(c)){c.getSystemService(NotificationManager.class).notify("test",1,new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("MOMENT · scheduled reminder test").setContentText("This notification was delivered by Android’s alarm scheduler.").setAutoCancel(true).build());c.getSharedPreferences("task_reminder_state",0).edit().putLong("test_delivered",System.currentTimeMillis()).apply();}return;}
  long id=intent.getLongExtra("id",-1),expected=intent.getLongExtra("target",0);String action=intent.getAction();
  try(Store db=new Store(c);Cursor rows=db.taskReminders()){
   while(rows.moveToNext())if(rows.getLong(0)==id){
    long at=target(rows);String day=rows.getString(3);if(at!=expected)return;
    if("DONE".equals(action)){db.completeOccurrence(id,day);c.getSystemService(NotificationManager.class).cancel("task-"+id,1);reschedule(c);MomentWidget.refresh(c);LockGlance.refresh(c);return;}
    if("SNOOZE".equals(action)){db.taskSnooze(id,day,System.currentTimeMillis()+10*60000L);c.getSystemService(NotificationManager.class).cancel("task-"+id,1);reschedule(c);return;}
    if(!"FIRE".equals(action)||System.currentTimeMillis()<at||rows.getLong(10)==at||!allowed(c))return;
    String title=rows.getString(1),kind=rows.getString(2);String body=("chore".equals(kind)?"A little nudge for your chore: ":"A little nudge for your to-do: ")+title+". Start small, snooze for 10 minutes, or mark it done.";
    Intent open=new Intent(c,MainActivity.class).putExtra("event_day",day).setData(Uri.parse("moment://task-open/"+id));
    Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("MOMENT · "+title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("MOMENT · a reminder is ready").build()).setAutoCancel(true)
     .addAction(new Notification.Action.Builder(null,"Done",pending(c,id,"DONE",at,day)).build()).addAction(new Notification.Action.Builder(null,"Snooze 10 min",pending(c,id,"SNOOZE",at,day)).build()).build();
    c.getSystemService(NotificationManager.class).notify("task-"+id,1,n);db.taskDelivered(id,at);return;
   }
  }
 }
 static void test(Context c){AlarmTiming.schedule(c,System.currentTimeMillis()+30000,PendingIntent.getBroadcast(c,901,new Intent(c,TaskReminderReceiver.class).setAction("TEST"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));c.getSharedPreferences("task_reminder_state",0).edit().putLong("test_scheduled",System.currentTimeMillis()).apply();}
}
