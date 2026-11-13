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
 static final String CALENDAR_ACTION="com.moment.app.WIDGET_CALENDAR_ACTION";
 @Override public void onReceive(Context c,Intent i){
  super.onReceive(c,i);
  if(CALENDAR_ACTION.equals(i.getAction())){
   int widget=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
   if(widget<0)return;
   android.content.SharedPreferences prefs=c.getSharedPreferences("widget_calendar",0);
   String prefix="widget_"+widget+"_";
   boolean month=prefs.getBoolean(prefix+"month",true);
   String op=i.getStringExtra("operation");
   LocalDate base;
   try{base=LocalDate.parse(prefs.getString(prefix+"date",LocalDate.now().toString()));}
   catch(Exception ex){base=LocalDate.now();}
   if("mode".equals(op))month=!month;
   else if("previous".equals(op))base=month?base.minusMonths(1):base.minusWeeks(1);
   else if("next".equals(op))base=month?base.plusMonths(1):base.plusWeeks(1);
   prefs.edit().putBoolean(prefix+"month",month).putString(prefix+"date",base.toString()).apply();
   draw(c,AppWidgetManager.getInstance(c),widget);
   return;
  }
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
 static PendingIntent calendarAction(Context c,int widgetId,String operation){
  Intent intent=new Intent(c,MomentWidget.class).setAction(CALENDAR_ACTION)
   .setData(Uri.parse("moment://widget/"+widgetId+"/"+operation))
   .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).putExtra("operation",operation);
  return PendingIntent.getBroadcast(c,widgetId*10+("next".equals(operation)?1:"previous".equals(operation)?2:3),
   intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 @Override public void onDeleted(Context c,int[] ids){
  android.content.SharedPreferences.Editor edit=c.getSharedPreferences("widget_calendar",0).edit();
  for(int id:ids){String prefix="widget_"+id+"_";edit.remove(prefix+"month").remove(prefix+"date");}
  edit.apply();
 }
 static void draw(Context c,AppWidgetManager manager,int widgetId){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
  LocalDate today=LocalDate.now();String now=today.toString(),last=today.plusDays(7).toString();
  v.setTextViewText(R.id.title,"moment.  ✦");
  v.setTextViewText(R.id.date,today.format(DateTimeFormatter.ofPattern("MMM d",Locale.US)).toUpperCase(Locale.US));
  android.content.SharedPreferences prefs=c.getSharedPreferences("widget_calendar",0);
  String prefix="widget_"+widgetId+"_";
  boolean monthView=prefs.getBoolean(prefix+"month",false);
  LocalDate anchor;
  try{anchor=LocalDate.parse(prefs.getString(prefix+"date",today.toString()));}
  catch(Exception ex){anchor=today;}
  LocalDate monday=anchor.minusDays(anchor.getDayOfWeek().getValue()-1);
  YearMonth shownMonth=YearMonth.from(anchor);
  LocalDate first=monthView?shownMonth.atDay(1).minusDays(shownMonth.atDay(1).getDayOfWeek().getValue()-1):monday;
  String label=monthView?anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.US)):
   monday.format(DateTimeFormatter.ofPattern("MMM d",Locale.US))+" – "+monday.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d",Locale.US));
  v.setTextViewText(R.id.calendar,label);
  v.setTextViewText(R.id.cal_mode,monthView?"Week":"Month");
  v.setOnClickPendingIntent(R.id.calendar,launch(c,"com.moment.app.WIDGET_CALENDAR",401));
  v.setOnClickPendingIntent(R.id.cal_prev,calendarAction(c,widgetId,"previous"));
  v.setOnClickPendingIntent(R.id.cal_next,calendarAction(c,widgetId,"next"));
  v.setOnClickPendingIntent(R.id.cal_mode,calendarAction(c,widgetId,"mode"));
  // Display events and birthdays inside the date cells, without needing to open the app.
  Map<String,String> labels=new HashMap<>();
  try(Store scheduleStore=new Store(c);Cursor days=scheduleStore.all()){
    while(days.moveToNext()){
      String kind=days.getString(2),name=days.getString(1),date=days.getString(3);
      if(!"event".equals(kind)&&!"birthday".equals(kind))continue;
      try{
        LocalDate saved=LocalDate.parse(date);
        LocalDate shown="birthday".equals(kind)?MainActivity.birthdayThisYear(saved,shownMonth.getYear()):saved;
        if(!YearMonth.from(shown).equals(shownMonth))continue;
        String k=shown.toString();
        String display=("birthday".equals(kind)?"♥ ":"")+name;
        if(!labels.containsKey(k))labels.put(k,display);
      }catch(Exception ignored){}
    }
  }
  v.removeAllViews(R.id.calendar_grid);
  String[] names={"M","T","W","T","F","S","S"};
  RemoteViews heading=new RemoteViews(c.getPackageName(),R.layout.widget_week);
  for(String name:names){
   RemoteViews day=new RemoteViews(c.getPackageName(),R.layout.widget_day);
   day.setTextViewText(R.id.day_label,name);day.setTextViewText(R.id.day_event,"");
   day.setTextColor(R.id.day_label,0xff756889);
   heading.addView(R.id.week_row,day);
  }
  v.addView(R.id.calendar_grid,heading);
  int calendarRows=monthView?((shownMonth.lengthOfMonth()+shownMonth.atDay(1).getDayOfWeek().getValue()-2)/7)+1:1;
  // Up to six month rows, or one week row.
  for(int row=0;row<calendarRows;row++){
   RemoteViews week=new RemoteViews(c.getPackageName(),R.layout.widget_week);
   for(int col=0;col<7;col++){
    LocalDate dayDate=first.plusDays(row*7+col);
    RemoteViews day=new RemoteViews(c.getPackageName(),R.layout.widget_day);
    boolean inMonth=!monthView||YearMonth.from(dayDate).equals(shownMonth);
    day.setTextViewText(R.id.day_label,inMonth?String.valueOf(dayDate.getDayOfMonth()):"");day.setTextViewText(R.id.day_event,inMonth?labels.getOrDefault(dayDate.toString(),""):"");
    day.setTextColor(R.id.day_label,dayDate.equals(today)?0xff6d4aa8:inMonth?0xff30264b:0xffb0a7bd);
    if(inMonth){
     Intent open=new Intent(c,MainActivity.class).setAction("com.moment.app.WIDGET_DATE")
        .setData(Uri.parse("moment://widget/date/"+dayDate.toString())).putExtra("widget_day",dayDate.toString())
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
     int request=(int)(dayDate.toEpochDay()%Integer.MAX_VALUE);
     PendingIntent pending=PendingIntent.getActivity(c,request,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
     day.setOnClickPendingIntent(R.id.day_label,pending);day.setOnClickPendingIntent(R.id.day_event,pending);
    }
    week.addView(R.id.week_row,day);
   }
   v.addView(R.id.calendar_grid,week);
  }
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
