package com.moment.app;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;

final class ReminderCenter {
 static void show(MainActivity a){
  LinearLayout p=MomentFeatures.vertical(a);boolean allowed=TaskReminders.allowed(a);
  p.addView(MomentFeatures.text(a,allowed?"Notifications are enabled":"Notifications need attention",18,true));
  p.addView(MomentFeatures.text(a,AlarmTiming.precise(a)?"Precise alarm access is enabled. Android notification and battery settings can still affect delivery.":"Android is using flexible timing. Allow Alarms & reminders for more precise delivery.",13,false));
  Button permission=new Button(a);permission.setAllCaps(false);permission.setText("Notification permission & settings");permission.setOnClickListener(v->{if(!ReminderControls.permission(a))return;a.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,a.getPackageName()));});p.addView(permission);
  Button timing=new Button(a);timing.setText("Allow precise reminder timing");timing.setAllCaps(false);timing.setOnClickListener(v->AlarmTiming.settings(a));p.addView(timing);
  timing.setEnabled(!AlarmTiming.precise(a));
  CheckBox daily=new CheckBox(a);daily.setText("Also send one daily summary");daily.setChecked(DailyReminder.enabled(a));daily.setOnCheckedChangeListener((v,on)->{if(on&&!ReminderControls.permission(a)){daily.setChecked(false);return;}DailyReminder.setEnabled(a,on);});p.addView(daily);
  Button dailyTime=new Button(a);dailyTime.setAllCaps(false);dailyTime.setText("Daily summary time · "+String.format(java.util.Locale.US,"%02d:%02d",DailyReminder.hour(a),DailyReminder.minute(a)));dailyTime.setOnClickListener(v->a.showAlarmSettings());p.addView(dailyTime);
  TextView status=MomentFeatures.text(a,"",13,false);p.addView(status);Button test=new Button(a);test.setText("Test a scheduled reminder in 30 seconds");test.setAllCaps(false);test.setOnClickListener(v->{if(!TaskReminders.allowed(a)){ReminderControls.permission(a);return;}TaskReminders.test(a);status.setText("Test scheduled. Leave the app and check for a notification. Flexible timing can be later than 30 seconds.");});p.addView(test);
  long delivered=a.getSharedPreferences("task_reminder_state",0).getLong("test_delivered",0);if(delivered>0)status.setText("Last scheduled test delivered: "+DateFormat.getDateTimeInstance().format(new Date(delivered)));
  LinearLayout list=MomentFeatures.vertical(a);list.addView(MomentFeatures.text(a,"Your timed chores & to-dos",17,true));p.addView(list);
  ScrollView sc=new ScrollView(a);sc.addView(p);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Reminders that help you act").setView(sc).setNegativeButton("Close",null).setPositiveButton("Add a reminder",(d,w)->TaskEditor.createReminder(a)).create();dialog.show();
  a.db.execute(()->{java.util.ArrayList<String[]> rows=new java.util.ArrayList<>();try(Cursor c=a.store.taskReminders()){while(c.moveToNext())rows.add(new String[]{c.getString(0),c.getString(1),c.getString(3),c.getString(5),c.getString(7),c.getString(9)});}a.runOnUiThread(()->{if(a.isFinishing()||!dialog.isShowing())return;if(rows.isEmpty())list.addView(MomentFeatures.text(a,"No individual reminders yet. Add a to-do or chore, give it a time, and turn on Remind me.",14,false));for(String[] row:rows){Button b=new Button(a);b.setAllCaps(false);b.setText(row[1]+"\n"+row[2]+" · "+row[3]+("none".equals(row[4])?"":" · "+row[4])+(Long.parseLong(row[5])>0?" · snoozed":""));b.setOnClickListener(v->{dialog.dismiss();TaskEditor.edit(a,Long.parseLong(row[0]));});list.addView(b);}});});
 }
}
