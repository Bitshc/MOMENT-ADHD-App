package com.moment.app;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Interactive home-screen dashboard. Editing opens MOMENT; check-offs run in background. */
public class MomentWidget extends AppWidgetProvider {
 static final String ACTION="com.moment.app.TOGGLE";
 @Override public void onReceive(Context c,Intent i){
  super.onReceive(c,i);
  if(!ACTION.equals(i.getAction()))return;
  long id=i.getLongExtra("id",-1);if(id<0)return;
  Context app=c.getApplicationContext();PendingResult pending=goAsync();
  new Thread(()->{try{
   try(Store store=new Store(app)){store.setDone(id,true);}
   refresh(app);LockGlance.refresh(app);
  }finally{pending.finish();}},"moment-widget-check").start();
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)draw(c,m,id);}
 static void refresh(Context c){
  AppWidgetManager m=AppWidgetManager.getInstance(c);
  for(int id:m.getAppWidgetIds(new ComponentName(c,MomentWidget.class)))draw(c,m,id);
 }
 static PendingIntent launch(Context c,String action,int request){
  Intent i=new Intent(c,MainActivity.class).setAction(action).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  return PendingIntent.getActivity(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 static void draw(Context c,AppWidgetManager manager,int widgetId){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
  LocalDate today=LocalDate.now();String now=today.toString(),last=today.plusDays(7).toString();
  v.setTextViewText(R.id.title,"moment.  ✦");
  v.setTextViewText(R.id.date,today.format(DateTimeFormatter.ofPattern("MMM d",Locale.US)).toUpperCase(Locale.US));
  LocalDate monday=today.minusDays(today.getDayOfWeek().getValue()-1);
  StringBuilder week=new StringBuilder();
  String[] labels={"M","T","W","T","F","S","S"};
  for(int n=0;n<7;n++){if(n>0)week.append("   ");LocalDate d=monday.plusDays(n);week.append(labels[n]).append(" ").append(d.getDayOfMonth()).append(d.equals(today)?"•":"");}
  v.setTextViewText(R.id.calendar,week.toString());
  v.setOnClickPendingIntent(R.id.calendar,launch(c,"com.moment.app.WIDGET_CALENDAR",401));
  v.setOnClickPendingIntent(R.id.title,launch(c,"com.moment.app.WIDGET_HOME",402));
  v.setOnClickPendingIntent(R.id.add,launch(c,"com.moment.app.WIDGET_ADD_TASK",403));
  v.setOnClickPendingIntent(R.id.add_event,launch(c,"com.moment.app.WIDGET_ADD_EVENT",404));
  v.setOnClickPendingIntent(R.id.add_chore,launch(c,"com.moment.app.WIDGET_ADD_CHORE",405));
  v.setOnClickPendingIntent(R.id.focus,launch(c,"com.moment.app.WIDGET_FOCUS",406));
  v.setOnClickPendingIntent(R.id.alarm,launch(c,"com.moment.app.WIDGET_ALARM",407));
  v.setOnClickPendingIntent(R.id.reminder,launch(c,"com.moment.app.WIDGET_HOME",408));
  v.setOnClickPendingIntent(R.id.event,launch(c,"com.moment.app.WIDGET_CALENDAR",409));
  int count=0,remaining=0;String urgent=null,nextEvent=null;LocalDate nextDate=null;
  int[] items={R.id.task1,R.id.task2,R.id.task3};
  try(Store store=new Store(c);Cursor rows=store.all()){
   while(rows.moveToNext()){
    String kind=rows.getString(2),date=rows.getString(3),title=rows.getString(1);
    long id=rows.getLong(0);
    if(("task".equals(kind)||"chore".equals(kind))&&rows.getInt(4)==0&&date.compareTo(now)<=0){
     remaining++;
     if(urgent==null)urgent=(date.compareTo(now)<0?"Overdue: ":"Due today: ")+title;
     if(count<3){
      int view=items[count++];
      v.setTextViewText(view,("chore".equals(kind)?"☐  🧹 ":"☐  ")+title);
      Intent action=new Intent(c,MomentWidget.class).setAction(ACTION).setData(Uri.parse("moment://widget/check/"+id)).putExtra("id",id);
      PendingIntent pending=PendingIntent.getBroadcast(c,(int)(id%Integer.MAX_VALUE),action,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
      v.setOnClickPendingIntent(view,pending);
     }
    }else if("event".equals(kind)&&date.compareTo(now)>=0&&date.compareTo(last)<=0){
     LocalDate d=LocalDate.parse(date);
     if(nextDate==null||d.isBefore(nextDate)){nextDate=d;nextEvent=title;}
    }else if("birthday".equals(kind)){
     try{
      LocalDate birth=LocalDate.parse(date),next=MainActivity.birthdayThisYear(birth,today.getYear());
      if(next.isBefore(today))next=MainActivity.birthdayThisYear(birth,today.getYear()+1);
      if(!next.isAfter(today.plusDays(7))&&(nextDate==null||next.isBefore(nextDate))){nextDate=next;nextEvent="🎂 "+title;}
     }catch(Exception ignored){}
    }
   }
  }
  v.setTextViewText(R.id.summary,remaining==0?"⚡ REMINDERS · ALL CLEAR":"⚡ REMINDERS · "+remaining+" NEED ATTENTION");
  v.setTextViewText(R.id.reminder,urgent==null?"A little breathing room today 🌿":urgent);
  v.setTextViewText(R.id.todo_heading,"✅ TO-DOS & CHORES · "+remaining+" DUE");
  for(int j=count;j<3;j++){v.setTextViewText(items[j],j==0?"✓  Nothing due right now":"");v.setOnClickPendingIntent(items[j],null);}
  v.setTextViewText(R.id.event,nextEvent==null?"No events in the next 7 days":nextDate.format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.US))+" · "+nextEvent);
  v.setTextViewText(R.id.alarm,DailyReminder.enabled(c)?"🔔 "+String.format(Locale.US,"%02d:%02d",DailyReminder.hour(c),DailyReminder.minute(c)):"🔕 Alarm off");
  manager.updateAppWidget(widgetId,v);
 }
}
