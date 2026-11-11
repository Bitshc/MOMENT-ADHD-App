package com.moment.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import java.time.*;
import java.util.*;

/** Opt-in daily due reminder, scheduled with Android's inexact alarm delivery. */
public final class DailyReminder extends android.content.BroadcastReceiver {
 static final String ACTION="com.moment.app.DAILY_REMINDER";
 static final String CHANNEL="moment_due_daily";
 static final int ID=3110;
 static boolean enabled(Context c){return c.getSharedPreferences("moment_settings",0).getBoolean("daily_reminders",false);}
 static void setEnabled(Context c,boolean on){
  c.getSharedPreferences("moment_settings",0).edit().putBoolean("daily_reminders",on).apply();
  schedule(c);if(!on)c.getSystemService(NotificationManager.class).cancel(ID);
 }
 static PendingIntent alarm(Context c){
  return PendingIntent.getBroadcast(c,3110,new Intent(c,DailyReminder.class).setAction(ACTION),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 static void schedule(Context c){
  AlarmManager alarm=c.getSystemService(AlarmManager.class);
  alarm.cancel(alarm(c));
  if(!enabled(c))return;
  LocalDateTime target=LocalDate.now().atTime(9,0);
  if(!target.isAfter(LocalDateTime.now()))target=target.plusDays(1);
  long trigger=target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,alarm(c));
 }
 @Override public void onReceive(Context context,Intent intent){
  final Context app=context.getApplicationContext();final PendingResult pending=goAsync();
  new Thread(()->{try{
   if(ACTION.equals(intent.getAction())&&enabled(app))publish(app);
   schedule(app);
  }finally{pending.finish();}},"moment-daily-reminders").start();
 }
 static void publish(Context c){
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  if(!nm.areNotificationsEnabled())return;
  NotificationChannel channel=new NotificationChannel(CHANNEL,"Daily to-dos and chores",NotificationManager.IMPORTANCE_DEFAULT);
  channel.setDescription("One gentle morning reminder for unfinished to-dos and chores");
  channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
  nm.createNotificationChannel(channel);
  int chores=0,todos=0;String first=null;
  String today=LocalDate.now().toString();
  try(Store store=new Store(c);Cursor rows=store.all()){
   while(rows.moveToNext()){
    if(rows.getInt(4)!=0||rows.getString(3).compareTo(today)>0)continue;
    String kind=rows.getString(2);
    if("task".equals(kind)){todos++;if(first==null)first=rows.getString(1);}
    if("chore".equals(kind)){chores++;if(first==null)first=rows.getString(1);}
   }
  }
  if(todos+chores==0){nm.cancel(ID);return;}
  Intent open=new Intent(c,MainActivity.class);
  PendingIntent tap=PendingIntent.getActivity(c,3110,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  String summary=todos+" to-dos · "+chores+" chores";
  Notification.Builder n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder)
   .setContentTitle("MOMENT · your gentle morning reminder")
   .setContentText(summary+" · Start with one small thing")
   .setStyle(new Notification.BigTextStyle().bigText(summary+" due or overdue. First step: "+first))
   .setContentIntent(tap).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE)
   .setPublicVersion(new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder)
      .setContentTitle("MOMENT · reminders ready").build());
  nm.notify(ID,n.build());
 }
}
