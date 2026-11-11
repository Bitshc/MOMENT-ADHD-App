package com.moment.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import java.time.LocalDate;
import java.util.*;

/** Opt-in, privacy-aware glance notifications. No background service or lock bypass. */
public final class LockGlance {
 static final String CHANNEL="moment_glance_v1";
 static final int ID=2250;
 static final String DONE="com.moment.app.LOCK_CHORE_DONE";
 static boolean permitted(Context c){
  if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  return c.getSystemService(NotificationManager.class).areNotificationsEnabled();
 }
 static void refresh(Context context){
  Context c=context.getApplicationContext();
  NotificationManager manager=c.getSystemService(NotificationManager.class);
  if(!LockCapture.enabled(c)||!permitted(c)){manager.cancel(ID);return;}
  NotificationChannel ch=new NotificationChannel(CHANNEL,"MOMENT lock-screen glance",NotificationManager.IMPORTANCE_LOW);
  ch.setDescription("Optional tasks and chore shortcut; hide text in lock-screen settings if you prefer.");
  ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
  manager.createNotificationChannel(ch);
  ArrayList<String> lines=new ArrayList<>();
  long choreId=-1;String choreTitle=null;
  LocalDate today=LocalDate.now();
  String now=today.toString(), horizon=today.plusDays(3).toString();
  try(Store store=new Store(c);Cursor rows=store.all()){
   while(rows.moveToNext()){
    String kind=rows.getString(2),day=rows.getString(3),name=rows.getString(1);
    if(("task".equals(kind)||"chore".equals(kind))&&rows.getInt(4)==0&&day.compareTo(horizon)<=0){
     if("chore".equals(kind)&&day.compareTo(now)<=0&&choreId<0){choreId=rows.getLong(0);choreTitle=name;}
     if(lines.size()<3)lines.add(("chore".equals(kind)?"🧹 ":"☐ ")+(day.compareTo(now)>0?"Soon · ":"")+name);
    }else if("event".equals(kind)&&day.compareTo(now)>=0&&day.compareTo(horizon)<=0&&lines.size()<3){
     lines.add("📅 "+day+" · "+name);
    }else if("birthday".equals(kind)&&lines.size()<3){
     try{
      LocalDate birth=LocalDate.parse(day),next=MainActivity.birthdayThisYear(LocalDate.parse(day),today.getYear());
      if(next.isBefore(today))next=MainActivity.birthdayThisYear(birth,today.getYear()+1);
      if(!next.isAfter(today.plusDays(3)))lines.add("🎂 "+next+" · "+name);
     }catch(Exception ignored){}
    }
   }
  }
  final String title=lines.isEmpty()?"You're caught up for now":lines.size()+" reminders";
  Notification.Builder n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_today)
   .setContentTitle("MOMENT · "+title)
   .setContentText(lines.isEmpty()?"Use quick capture whenever a thought appears":lines.get(0))
   .setStyle(new Notification.InboxStyle().addLine(lines.size()>0?lines.get(0):"No pending tasks").addLine(lines.size()>1?lines.get(1):"").addLine(lines.size()>2?lines.get(2):""))
   .setVisibility(Notification.VISIBILITY_PRIVATE)
   .setPublicVersion(new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_today)
     .setContentTitle("MOMENT · daily glance").setContentText("Your plans are ready").build())
   .setCategory(Notification.CATEGORY_REMINDER).setOngoing(true).setOnlyAlertOnce(true);
  Intent open=new Intent(c,MainActivity.class);
  n.setContentIntent(PendingIntent.getActivity(c,2250,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  if(choreId>=0){
   Intent action=new Intent(c,LockChoreReceiver.class).setAction(DONE).putExtra("id",choreId);
   PendingIntent done=PendingIntent.getBroadcast(c,(int)(choreId%Integer.MAX_VALUE),action,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   n.addAction(new Notification.Action.Builder(android.R.drawable.checkbox_on_background,
      "✓ Done: "+(choreTitle.length()>24?choreTitle.substring(0,24)+"…":choreTitle),done).build());
  }
  manager.notify(ID,n.build());
 }
}
