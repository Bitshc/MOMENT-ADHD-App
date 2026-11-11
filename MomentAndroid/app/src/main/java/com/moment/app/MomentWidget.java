package com.moment.app;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MomentWidget extends AppWidgetProvider {
 static final String ACTION="com.moment.app.TOGGLE";
 @Override public void onReceive(Context context,Intent intent){
  super.onReceive(context,intent);
  if(!ACTION.equals(intent.getAction()))return;
  long id=intent.getLongExtra("id",-1);
  if(id<0)return;
  final Context app=context.getApplicationContext();final PendingResult result=goAsync();
  new Thread(()->{try{
   try(Store db=new Store(app)){db.setDone(id,true);}
   refresh(app);LockGlance.refresh(app);
  }finally{result.finish();}},"moment-widget-checkoff").start();
 }
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)draw(c,manager,id);}
 static void refresh(Context c){
  AppWidgetManager manager=AppWidgetManager.getInstance(c);
  android.content.ComponentName name=new android.content.ComponentName(c,MomentWidget.class);
  for(int id:manager.getAppWidgetIds(name))draw(c,manager,id);
 }
 static void draw(Context context,AppWidgetManager manager,int widgetId){
  RemoteViews view=new RemoteViews(context.getPackageName(),R.layout.widget);
  String day=LocalDate.now().toString();
  String readable=LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.US));
  view.setTextViewText(R.id.title,"moment.  ✦");
  view.setTextViewText(R.id.date,"YOUR DAY · "+readable.toUpperCase(Locale.US));
  int[] taskViews={R.id.task1,R.id.task2,R.id.task3};
  int count=0,remaining=0,events=0;
  String nextEvent="";
  try(Store store=new Store(context);Cursor cursor=store.all()){
   while(cursor.moveToNext()){
    String kind=cursor.getString(2),due=cursor.getString(3),name=cursor.getString(1);
    if(("task".equals(kind)||"chore".equals(kind))&&cursor.getInt(4)==0&&due.compareTo(day)<=0){
     remaining++;
     if(count<taskViews.length){
      int viewId=taskViews[count++];
      view.setTextViewText(viewId,("chore".equals(kind)?"☐  🧹 ":"☐  ")+name);
      Intent done=new Intent(context,MomentWidget.class).setAction(ACTION)
       .setData(Uri.parse("moment://widget/done/"+cursor.getLong(0)))
       .putExtra("id",cursor.getLong(0));
      PendingIntent action=PendingIntent.getBroadcast(context,(int)(cursor.getLong(0)%Integer.MAX_VALUE),done,
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      view.setOnClickPendingIntent(viewId,action);
     }
    }else if("event".equals(kind)&&due.equals(day)){
     events++;
     if(nextEvent.isEmpty())nextEvent=name;
    }
   }
  }
  for(int i=count;i<taskViews.length;i++){
   view.setTextViewText(taskViews[i],i==0?"✓  Nothing overdue or due today":"");
   view.setOnClickPendingIntent(taskViews[i],null);
  }
  String summary=remaining==0?"A little breathing room 🌿":remaining+" things to do · one step at a time";
  view.setTextViewText(R.id.summary,summary);
  view.setTextViewText(R.id.event,events>0?"📅  Today: "+nextEvent+(events>1?"  +"+(events-1)+" more":""):"📅  No events today");
  Intent add=new Intent(context,MainActivity.class).setAction("com.moment.app.QUICK_ADD");
  PendingIntent quick=PendingIntent.getActivity(context,100,add,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  view.setOnClickPendingIntent(R.id.add,quick);
  Intent open=new Intent(context,MainActivity.class).setAction("com.moment.app.OPEN");
  PendingIntent app=PendingIntent.getActivity(context,101,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  view.setOnClickPendingIntent(R.id.title,app);
  view.setOnClickPendingIntent(R.id.event,app);
  manager.updateAppWidget(widgetId,view);
 }
}
