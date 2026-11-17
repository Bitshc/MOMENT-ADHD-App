package com.moment.app;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.*;
import android.service.notification.StatusBarNotification;
import java.io.*;
import java.time.*;

/** Dependency-free device smoke test, run only on a disposable CI emulator. */
public class SmokeInstrumentation extends Instrumentation {
 MainActivity activity;
 public void onCreate(Bundle args){super.onCreate(args);start();}
 interface Condition {boolean ready() throws Exception;}
 void await(Condition condition,int seconds) throws Exception {long end=System.currentTimeMillis()+seconds*1000L;while(System.currentTimeMillis()<end){if(condition.ready())return;Thread.sleep(150);}throw new AssertionError("Timed out waiting for device state");}
 void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 Notification notification(String tag){for(StatusBarNotification n:getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())if(tag.equals(n.getTag()))return n.getNotification();return null;}
 long id(Store store,String title){try(Cursor c=store.all()){while(c.moveToNext())if(title.equals(c.getString(1)))return c.getLong(0);}throw new AssertionError("Missing item "+title);}
 String day(Store store,long id){try(Cursor c=store.all()){while(c.moveToNext())if(c.getLong(0)==id)return c.getString(3);}throw new AssertionError("Missing task");}
 boolean done(Store store,long id){try(Cursor c=store.all()){while(c.moveToNext())if(c.getLong(0)==id)return c.getInt(4)==1;}return false;}
 public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();Intent launch=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);activity=(MainActivity)startActivitySync(launch);waitForIdleSync();
  check(TaskReminders.allowed(c),"Task notifications must be granted on test emulator");check(AlarmTiming.precise(c),"Exact alarm access must be granted on test emulator");
  String today=LocalDate.now().toString(),past=LocalTime.now().withSecond(0).withNano(0).toString();
  try(Store store=new Store(c)){
   store.saveTask(-1,"QA Laundry","chore",today,past,true,"none",30);long id=id(store,"QA Laundry");
   TaskReminders.reschedule(c);await(()->notification("task-"+id)!=null,20);
   Notification first=notification("task-"+id);check(first.actions!=null&&first.actions.length==2,"Done and Snooze actions must exist");first.actions[1].actionIntent.send();
   await(()->Long.parseLong(store.taskDetails(id)[4])>System.currentTimeMillis()+8*60000,10);
   check(!done(store,id),"Snooze must not complete task");
   store.taskSnooze(id,today,System.currentTimeMillis()+1500);TaskReminders.reschedule(c);await(()->notification("task-"+id)!=null,20);
   notification("task-"+id).actions[0].actionIntent.send();await(()->done(store,id),10);
   TaskReminders.reschedule(c);check(notification("task-"+id)==null,"Completed task notification must clear");
   store.saveTask(-1,"QA Daily routine","task",today,past,true,"daily",15);long recurring=id(store,"QA Daily routine");TaskReminders.reschedule(c);await(()->notification("task-"+recurring)!=null,20);
   PendingIntent complete=notification("task-"+recurring).actions[0].actionIntent;complete.send();String tomorrow=LocalDate.now().plusDays(1).toString();await(()->tomorrow.equals(day(store,recurring)),10);complete.send();Thread.sleep(300);check(tomorrow.equals(day(store,recurring)),"Duplicate Done must not skip another occurrence");check(!done(store,recurring),"Next routine occurrence must remain unfinished");
   store.saveCalendarEvent(-1,"QA Work block",today,"10:00","",0,false,60,"Work / study");long event=id(store,"QA Work block");
   CalendarItem item=new CalendarItem(event,"QA Work block","event",today,false,"10:00",60,"Work / study","",0,"");
   runOnMainSync(()->CalendarDrag.move(activity,item,LocalDate.now().plusDays(1),"11:15"));await(()->tomorrow.equals(day(store,event))&&"11:15".equals(store.eventDetails(event)[0]),10);
   store.saveCalendarEvent(-1,"QA Rest break",today,"12:00","",0,false,45,"Rest");
   runOnMainSync(()->{activity.getPreferences(0).edit().putInt("calendar_view",1).apply();activity.selectedDay=today;activity.calendarMonth=YearMonth.from(LocalDate.now());activity.show();});
   waitForIdleSync();await(()->activity.root.findViewWithTag("week_calendar_ready")!=null,10);Thread.sleep(800);waitForIdleSync();
   Bitmap shot=getUiAutomation().takeScreenshot();check(shot!=null,"Screenshot must be captured");try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"qa-week.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}shot.recycle();
  }
  result.putString("stream","MOMENT_SMOKE_OK: scheduled alarm, Done, Snooze, repeat idempotency, moved event and week screen verified\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable error){StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));result.putString("stream","MOMENT_SMOKE_FAILED\n"+trace);finish(Activity.RESULT_CANCELED,result);}}
}
