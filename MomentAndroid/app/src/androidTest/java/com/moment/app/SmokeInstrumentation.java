package com.moment.app;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.widget.TextView;
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
 View titleView(View view,String title){if(view instanceof TextView&&((TextView)view).getText().toString().startsWith(title))return view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View found=titleView(((ViewGroup)view).getChildAt(n),title);if(found!=null)return found;}return null;}
 void pointer(long down,int action,float x,float y){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"Touch event injection failed");event.recycle();}
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
   store.saveCalendarEvent(-1,"QA Drag practice",today,"09:00","",0,false,60,"Personal");long dragged=id(store,"QA Drag practice");
   runOnMainSync(()->{activity.getPreferences(0).edit().putInt("calendar_view",1).apply();activity.selectedDay=today;activity.calendarMonth=YearMonth.from(LocalDate.now());activity.show();});
   waitForIdleSync();await(()->activity.root.findViewWithTag("week_calendar_ready")!=null,10);Thread.sleep(1000);waitForIdleSync();
   LocalDate destination=TaskPlan.sunday(LocalDate.now()).plusDays((LocalDate.now().getDayOfWeek().getValue()%7+1)%7);float[] coords=new float[4];
   runOnMainSync(()->{View source=titleView(activity.root,"QA Drag practice"),target=activity.root.findViewWithTag("week-day-"+destination);check(source!=null&&target!=null,"Drag source and destination must be visible");int[] start=new int[2],end=new int[2];source.getLocationOnScreen(start);target.getLocationOnScreen(end);coords[0]=start[0]+source.getWidth()/2f;coords[1]=start[1]+source.getHeight()/2f;coords[2]=end[0]+target.getWidth()/2f;coords[3]=end[1]+activity.dp(600)+source.getHeight()/2f;});
   long down=SystemClock.uptimeMillis();pointer(down,MotionEvent.ACTION_DOWN,coords[0],coords[1]);Thread.sleep(850);for(int n=1;n<=12;n++){pointer(down,MotionEvent.ACTION_MOVE,coords[0]+(coords[2]-coords[0])*n/12f,coords[1]+(coords[3]-coords[1])*n/12f);Thread.sleep(45);}pointer(down,MotionEvent.ACTION_UP,coords[2],coords[3]);
   await(()->destination.toString().equals(day(store,dragged))&&"10:00".equals(store.eventDetails(dragged)[0]),10);
   Thread.sleep(4000);waitForIdleSync();
   Bitmap shot=getUiAutomation().takeScreenshot();check(shot!=null,"Screenshot must be captured");try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"qa-week.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}shot.recycle();
  }
  result.putString("stream","MOMENT_SMOKE_OK: scheduled alarm, Done, Snooze, repeat idempotency, physical drag gesture, moved event and week screen verified\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable error){StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));result.putString("stream","MOMENT_SMOKE_FAILED\n"+trace);finish(Activity.RESULT_CANCELED,result);}}
}
