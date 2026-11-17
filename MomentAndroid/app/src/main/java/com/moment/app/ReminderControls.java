package com.moment.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.*;
import java.time.*;

final class ReminderControls {
 static boolean permission(MainActivity a){
  if(EventReminders.allowed(a))return true;
  if(Build.VERSION.SDK_INT>=33 && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},88);
   Toast.makeText(a,"Allow notifications, then turn gentle reminders on.",Toast.LENGTH_LONG).show();
  }else new AlertDialog.Builder(a).setTitle("Allow gentle reminders").setMessage("MOMENT notifications are turned off in Android settings. Enable the Gentle event reminders channel, then turn reminders on here.").setNegativeButton("Later",null).setPositiveButton("Settings",(d,w)->a.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,a.getPackageName()))).show();
  return false;
 }
 static CheckBox choice(MainActivity a,LinearLayout p){
  CheckBox check=new CheckBox(a);check.setText("Gentle reminders · 1 hour, 30 min and 10 min before");p.addView(check);
  check.setOnCheckedChangeListener((v,on)->{if(on&&!permission(a))check.setChecked(false);});return check;
 }
 static void attach(MainActivity a,LinearLayout panel,long id,String day,String time,boolean enabled){
  panel.addView(MomentFeatures.text(a,"A little help keeping time",17,true));
  panel.addView(MomentFeatures.text(a,"Reminders are based on the event time, not traffic or a calculated leave time. They work on your phone without an account or server. Android may deliver them late in battery-saving modes.",13,false));
  CheckBox on=new CheckBox(a);on.setText("Gentle reminders · 1 hour, 30 min and 10 min before");on.setChecked(enabled);panel.addView(on);
  TextView status=MomentFeatures.text(a,enabled?(EventReminders.allowed(a)?"Reminders on":"Saved, but Android notifications are blocked") :"Reminders off",13,false);panel.addView(status);
  on.setOnCheckedChangeListener((v,checked)->{
   if(checked){
    try{if(ReminderPlan.arrival(day,time,ZoneId.systemDefault())<=System.currentTimeMillis())throw new Exception();}
    catch(Exception e){on.setChecked(false);Toast.makeText(a,"Set a valid future event date and time first.",Toast.LENGTH_LONG).show();return;}
    if(!permission(a)){on.setChecked(false);return;}
   }
   a.write(()->a.store.setReminders(id,checked));status.setText(checked?"Reminders on":"Reminders off");
  });
  Button preview=new Button(a);preview.setAllCaps(false);preview.setText("Try a gentle reminder now");panel.addView(preview);
  preview.setOnClickListener(v->{if(permission(a)){
   Notification n=new Notification.Builder(a,EventReminders.CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("MOMENT · reminder preview").setContentText("A gentle heads-up: your event is coming up. Take the next small step.").setAutoCancel(true).build();
   a.getSystemService(NotificationManager.class).notify("preview",1,n);Toast.makeText(a,"Preview sent to your notifications.",Toast.LENGTH_SHORT).show();
  }});
 }
}
