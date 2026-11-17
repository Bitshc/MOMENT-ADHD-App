package com.moment.app;
import android.content.ClipData;
import android.view.*;
import android.widget.*;
import android.database.Cursor;
import java.time.*;

final class CalendarDrag {
 static final class Move {final CalendarItem item;final float offset;Move(CalendarItem item,float offset){this.item=item;this.offset=offset;}}
 static void source(MainActivity a,View view,CalendarItem item,boolean timedSurface){
  if("birthday".equals(item.kind))return;float[] y={0};view.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN)y[0]=e.getY();return false;});
  view.setOnLongClickListener(v->v.startDragAndDrop(ClipData.newPlainText("MOMENT plan",String.valueOf(item.id)),new View.DragShadowBuilder(v),new Move(item,timedSurface?y[0]:0),0));
 }
 static void target(MainActivity a,View view,LocalDate date,Integer fromMinute){
  view.setOnDragListener((v,event)->{
   if(!(event.getLocalState() instanceof Move))return false;Move move=(Move)event.getLocalState();
   switch(event.getAction()){
    case DragEvent.ACTION_DRAG_STARTED:return true;
    case DragEvent.ACTION_DRAG_ENTERED:v.setAlpha(.75f);return true;
    case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setAlpha(1f);return true;
    case DragEvent.ACTION_DRAG_LOCATION:return true;
    case DragEvent.ACTION_DROP:v.setAlpha(1f);String time=move.item.time;
     if(fromMinute!=null){int minute=TaskPlan.snap(fromMinute+Math.round((event.getY()-move.offset)/a.getResources().getDisplayMetrics().density),move.item.duration);time=LocalTime.of(minute/60,minute%60).toString();}
     move(a,move.item,date,time);return true;
   }return false;
  });
 }
 static void open(MainActivity a,CalendarItem item){if("event".equals(item.kind))MomentFeatures.event(a,item.id,item.title,item.day);else if(item.task())TaskEditor.edit(a,item.id);}
 static void move(MainActivity a,CalendarItem item,LocalDate date,String clock){
  if(item.day.equals(date.toString())&&item.time.equals(clock))return;
  try{if(!clock.isEmpty())TaskPlan.due(date.toString(),clock);}catch(Exception e){Toast.makeText(a,"That time falls in a clock change. Choose another time.",Toast.LENGTH_LONG).show();return;}
  a.db.execute(()->{
   boolean overlap=false;
   if(!clock.isEmpty())try(Cursor rows=a.store.calendarItems()){int start=LocalTime.parse(clock).toSecondOfDay()/60;while(rows.moveToNext())if(rows.getLong(0)!=item.id&&rows.getInt(4)==0&&date.toString().equals(rows.getString(3))&&!rows.getString(5).isEmpty()){int other=LocalTime.parse(rows.getString(5)).toSecondOfDay()/60;if(start<other+rows.getInt(6)&&start+item.duration>other)overlap=true;}}
   final boolean conflict=overlap;
   a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;Runnable save=()->{a.db.execute(()->{try{a.store.movePlan(item.id,item.day,date.toString(),clock);EventReminders.reschedule(a);TaskReminders.reschedule(a);MomentWidget.refresh(a);LockGlance.refresh(a);a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;a.selectedDay=date.toString();a.calendarMonth=YearMonth.from(date);a.show();Toast.makeText(a,"Moved to "+InlineCalendar.dateLabel(date)+(clock.isEmpty()?"":" · "+EventEditor.label(LocalTime.parse(clock))),Toast.LENGTH_LONG).show();});}catch(Exception e){a.runOnUiThread(()->Toast.makeText(a,"This plan changed. Reopen the calendar and try again.",Toast.LENGTH_LONG).show());}});};if(!conflict)save.run();else new android.app.AlertDialog.Builder(a).setTitle("This time overlaps another plan").setMessage("Keep both here, or cancel this move?").setNegativeButton("Cancel move",null).setPositiveButton("Keep both",(d,w)->save.run()).show();});
  });
 }
}
