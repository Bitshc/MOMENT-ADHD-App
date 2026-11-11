package com.moment.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;

/** Opt-in private lock screen capture via notification direct-reply. */
public final class LockCapture {
 static final String CHANNEL="moment_capture",KEY="capture_text",ACTION="com.moment.app.CAPTURE";
 static final int ID=2208;
 static boolean enabled(Context c){return c.getSharedPreferences("moment_settings",0).getBoolean("lock_capture",false);}
 static void toggle(Context c,boolean enabled){c.getSharedPreferences("moment_settings",0).edit().putBoolean("lock_capture",enabled).apply();publish(c);}
 static void publish(Context c){
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  if(!enabled(c)){nm.cancel(ID);return;}
  NotificationChannel channel=new NotificationChannel(CHANNEL,"Quick capture shortcut",NotificationManager.IMPORTANCE_DEFAULT);
  channel.setDescription("Optional lock screen shortcuts for saving tasks and events");
  channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
  nm.createNotificationChannel(channel);
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  Intent open=new Intent(c,MainActivity.class);
  PendingIntent app=PendingIntent.getActivity(c,2208,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit)
    .setContentTitle("MOMENT · quick capture")
    .setContentText("Add a to-do or event without losing your thought")
    .setPublicVersion(new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit)
      .setContentTitle("MOMENT · quick capture").setContentText("Quick add").build())
    .setVisibility(Notification.VISIBILITY_PUBLIC).setContentIntent(app).setOngoing(true)
    .setCategory(Notification.CATEGORY_REMINDER);
  b.addAction(action(c,"task","＋ To-do",101));
  b.addAction(action(c,"event","＋ Event",102));
  b.addAction(action(c,"chore","＋ Chore",103));
  nm.notify(ID,b.build());
 }
 static Notification.Action action(Context c,String kind,String name,int code){
  Intent intent=new Intent(c,LockQuickAddActivity.class).putExtra("kind",kind)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent launch=PendingIntent.getActivity(c,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Action.Builder(android.R.drawable.ic_menu_add,name,launch).build();
 }
}
