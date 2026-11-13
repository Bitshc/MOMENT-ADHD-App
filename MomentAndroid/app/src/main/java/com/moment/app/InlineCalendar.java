package com.moment.app;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Calendar first, with visible time and a quieter view when a whole day feels like too much. */
public class InlineCalendar {
 static final int INK=Color.rgb(43,37,66),MUTED=Color.rgb(110,104,130),PURPLE=Color.rgb(112,83,173),BG=Color.rgb(250,248,253),LAVENDER=Color.rgb(238,229,251),WHITE=Color.WHITE;
 static GradientDrawable round(MainActivity a,int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(a.dp(radius));return g;}
 static LinearLayout vertical(MainActivity a){LinearLayout l=new LinearLayout(a);l.setOrientation(1);return l;}
 static TextView text(MainActivity a,String value,int size,int color,boolean bold){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 static Button button(MainActivity a,String value){Button b=new Button(a);b.setAllCaps(false);b.setText(value);b.setTextSize(13);b.setTextColor(PURPLE);b.setBackground(round(a,LAVENDER,12));b.setMinHeight(a.dp(48));return b;}
 static void equal(MainActivity a,LinearLayout row,View view){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,a.dp(48),1);p.setMargins(a.dp(2),a.dp(3),a.dp(2),a.dp(3));row.addView(view,p);}
 static String dateLabel(LocalDate d){return d.format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.US));}
 static List<CalendarItem> day(List<CalendarItem> all,LocalDate date){List<CalendarItem> out=new ArrayList<>();for(CalendarItem i:all)if(i.on(date))out.add(i);return out;}
 static int color(CalendarItem i){return "Rest".equals(i.category)?Color.rgb(224,240,228):"Health".equals(i.category)?Color.rgb(250,230,223):"Work / study".equals(i.category)?Color.rgb(226,236,251):LAVENDER;}
 static void choose(MainActivity a,LocalDate date){a.selectedDay=date.toString();a.calendarMonth=YearMonth.from(date);a.show();}
 static void attach(MainActivity a,LinearLayout parent){
  final LocalDate selected=LocalDate.parse(a.selectedDay);final YearMonth month=a.calendarMonth;
  final int mode=a.getPreferences(0).getInt("calendar_view",2);final boolean quiet=a.getPreferences(0).getBoolean("calendar_quiet",false);
  LinearLayout card=vertical(a);card.setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(14));card.setBackground(round(a,WHITE,22));card.setTag("moment_calendar");parent.addView(card,a.margins(12,12));
  LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=text(a,mode==2?month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.US)):dateLabel(selected),21,INK,true);
  top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  Button previous=button(a,"‹"),next=button(a,"›"),add=button(a,"＋");
  top.addView(previous,new LinearLayout.LayoutParams(a.dp(43),a.dp(46)));
  top.addView(next,new LinearLayout.LayoutParams(a.dp(43),a.dp(46)));
  add.setTextSize(23);top.addView(add,new LinearLayout.LayoutParams(a.dp(52),a.dp(46)));
  card.addView(top);
  previous.setContentDescription("Previous "+(mode==2?"month":mode==1?"week":"day"));
  next.setContentDescription("Next "+(mode==2?"month":mode==1?"week":"day"));
  add.setContentDescription("Add an event, task or birthday");
  previous.setOnClickListener(v->navigate(a,selected,month,mode,-1));
  next.setOnClickListener(v->navigate(a,selected,month,mode,1));
  add.setOnClickListener(v->new android.app.AlertDialog.Builder(a).setTitle("Add to "+dateLabel(selected))
     .setItems(new String[]{"📅 Event","✅ Task","🎂 Birthday","🧹 Chore"},(dialog,choice)->{
       if(choice==0)EventEditor.create(a,selected,suggested(selected));
       if(choice==1)a.addDialog("task");
       if(choice==2)a.addDialog("birthday");
       if(choice==3)a.addDialog("chore");
     }).show());
  LinearLayout subnav=new LinearLayout(a);subnav.setGravity(Gravity.CENTER_VERTICAL);
  Button view=button(a,mode==2?"Month ▾":mode==1?"Week ▾":"Day ▾");
  Button today=button(a,"Today");subnav.addView(view,new LinearLayout.LayoutParams(0,a.dp(42),1));
  LinearLayout.LayoutParams spacer=new LinearLayout.LayoutParams(a.dp(72),a.dp(42));spacer.leftMargin=a.dp(8);subnav.addView(today,spacer);
  view.setOnClickListener(v->new android.app.AlertDialog.Builder(a).setTitle("Calendar view")
      .setItems(new String[]{"Day","Week","Month"},(dialog,choice)->{
        a.getPreferences(0).edit().putInt("calendar_view",choice).putBoolean("week_mode",choice==1).apply();
        a.weekView=choice==1;a.calendarMonth=YearMonth.from(LocalDate.parse(a.selectedDay));a.show();
      }).show());
  today.setOnClickListener(v->choose(a,LocalDate.now()));
  card.addView(subnav,a.margins(7,8));
  LinearLayout content=vertical(a);content.addView(text(a,"Gathering your plans…",14,MUTED,false));card.addView(content);
  a.db.execute(()->{
   List<CalendarItem> all=new ArrayList<>();try(Cursor c=a.store.calendarItems()){while(c.moveToNext())all.add(new CalendarItem(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1,c.getString(5),c.getInt(6),c.getString(7),c.getString(8),c.getInt(9),c.getString(10)));}
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed()||a.root!=parent||!a.selectedDay.equals(selected.toString()))return;
    content.removeAllViews();List<CalendarItem> daily=day(all,selected);if(mode!=0)grid(a,content,all,selected,month,mode);nowAndNext(a,content,daily,selected);
    int occupied=CalendarMath.occupiedMinutes(daily),unfinished=0;for(CalendarItem i:daily)if(i.task()&&!i.done)unfinished++;
    content.addView(text(a,(occupied/60>0?occupied/60+"h ":"")+occupied%60+"m of timed plans · "+unfinished+" unfinished tasks",13,MUTED,false),a.margins(8,4));
    if(CalendarMath.overlaps(daily))content.addView(text(a,"Some plans overlap. You can adjust them when you’re ready.",13,PURPLE,false),a.margins(4,8));
    if(!quiet)for(CalendarItem i:daily)if(i.task()&&!i.done){content.addView(text(a,"One small starting point",15,INK,true),a.margins(8,3));item(a,content,i);break;}
    if(mode==2){
      content.addView(text(a,"Selected day · "+dateLabel(selected),17,INK,true),a.margins(12,6));
      if(daily.isEmpty())content.addView(text(a,"Nothing scheduled. Your time is yours. 🌿",14,MUTED,false),a.margins(5,7));
      else{int shown=0;for(CalendarItem event:daily)if(!event.done||!event.task()){item(a,content,event);shown++;}if(shown==0)content.addView(text(a,"All done for this day. 🌿",14,MUTED,false));}
     }
    else if(quiet){content.addView(text(a,"Just the next small step",17,INK,true),a.margins(8,8));int shown=0;for(CalendarItem i:daily)if(i.task()&&!i.done&&shown++<3)item(a,content,i);if(shown==0)content.addView(text(a,"Nothing to tackle here. A little breathing room counts, too.",14,MUTED,false));else if(shown>3)content.addView(text(a,"Your other tasks are saved. Show the full day whenever you want.",13,MUTED,false));}
    else{List<CalendarItem> timed=CalendarMath.timed(daily);if(!timed.isEmpty()||mode==0){content.addView(text(a,"Your day in time · "+dateLabel(selected),17,INK,true),a.margins(10,4));content.addView(text(a,"Tap an empty hour to add a plan. Hold a plan to edit or remove it.",12,MUTED,false),a.margins(0,8));timeline(a,content,daily,selected);}if(!timed.isEmpty()){Button details=button(a,"Time details & breathing room");LinearLayout list=vertical(a);list.setVisibility(View.GONE);details.setOnClickListener(v->list.setVisibility(list.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));content.addView(details,a.margins(8,4));content.addView(list);for(CalendarItem i:timed){item(a,list,i);int gap=CalendarMath.gapAfter(daily,i);if(gap>0)list.addView(text(a,gap+" min between this and the next timed plan",12,MUTED,false));}}content.addView(text(a,"All-day plans & small steps",17,INK,true),a.margins(14,7));boolean any=false;for(CalendarItem i:daily)if(!i.timed()){item(a,content,i);any=true;}if(!any)content.addView(text(a,"No untimed plans here. Add one only if it helps.",13,MUTED,false));}
   });
  });
 }
 static void navigate(MainActivity a,LocalDate selected,YearMonth month,int mode,int direction){if(mode==2){YearMonth next=month.plusMonths(direction);a.calendarMonth=next;a.selectedDay=next.atDay(Math.min(selected.getDayOfMonth(),next.lengthOfMonth())).toString();a.show();}else choose(a,mode==1?selected.plusWeeks(direction):selected.plusDays(direction));}
 static LocalTime suggested(LocalDate day){if(!day.equals(LocalDate.now()))return LocalTime.of(9,0);int minute=Math.min(1410,((LocalTime.now().toSecondOfDay()/60+29)/30)*30);return LocalTime.of(minute/60,minute%60);}
 static void grid(MainActivity a,LinearLayout parent,List<CalendarItem> all,LocalDate selected,YearMonth month,int mode){
  LocalDate start=mode==1?selected.minusDays(selected.getDayOfWeek().getValue()%7):month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue()%7);int cells=mode==1?7:((month.atDay(1).getDayOfWeek().getValue()%7+month.lengthOfMonth()+6)/7)*7;
  LinearLayout labels=new LinearLayout(a);for(String label:new String[]{"S","M","T","W","T","F","S"}){TextView t=text(a,label,12,MUTED,true);t.setGravity(Gravity.CENTER);labels.addView(t,new LinearLayout.LayoutParams(0,a.dp(28),1));}parent.addView(labels);
  for(int offset=0;offset<cells;offset+=7){LinearLayout row=new LinearLayout(a);parent.addView(row);for(int col=0;col<7;col++){
   LocalDate date=start.plusDays(offset+col);LinearLayout cell=vertical(a);cell.setPadding(a.dp(2),a.dp(5),a.dp(2),a.dp(5));cell.setBackground(round(a,date.equals(selected)?LAVENDER:BG,9));TextView number=text(a,String.valueOf(date.getDayOfMonth()),15,date.equals(LocalDate.now())?PURPLE:YearMonth.from(date).equals(month)?INK:MUTED,true);number.setGravity(Gravity.CENTER);cell.addView(number);
   int count=0;StringBuilder accessible=new StringBuilder(dateLabel(date));for(CalendarItem i:day(all,date))if(!i.task()||!i.done){accessible.append(", ").append(i.title);if(count<2){TextView tiny=text(a,("birthday".equals(i.kind)?"♥ ":i.task()?"· ":"")+i.title,10,INK,false);tiny.setSingleLine(true);tiny.setEllipsize(TextUtils.TruncateAt.END);tiny.setPadding(a.dp(2),a.dp(2),a.dp(2),a.dp(2));tiny.setBackground(round(a,color(i),5));cell.addView(tiny);}count++;}TextView extra=text(a,count>2?"+"+(count-2):count==0?"":"",10,MUTED,false);extra.setGravity(Gravity.CENTER);cell.addView(extra);cell.setContentDescription(accessible.toString());cell.setOnClickListener(v->choose(a,date));cell.setOnLongClickListener(v->{EventEditor.create(a,date,LocalTime.of(9,0));return true;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,a.dp(91),1);p.setMargins(a.dp(1),a.dp(2),a.dp(1),a.dp(2));row.addView(cell,p);
  }}
 }
 static void nowAndNext(MainActivity a,LinearLayout parent,List<CalendarItem> daily,LocalDate selected){
  LinearLayout box=vertical(a);box.setPadding(a.dp(13),a.dp(12),a.dp(13),a.dp(12));box.setBackground(round(a,LAVENDER,15));parent.addView(box,a.margins(8,2));TextView label=text(a,"",16,INK,true),detail=text(a,"",13,MUTED,false);box.addView(label);box.addView(detail);
  Handler handler=new Handler(Looper.getMainLooper());Runnable tick=new Runnable(){public void run(){boolean today=selected.equals(LocalDate.now());int now=today?LocalTime.now().toSecondOfDay()/60:0;CalendarItem next=CalendarMath.currentOrNext(daily,now);if(next==null){label.setText(today?"No more timed plans today":"No timed plans on this day");detail.setText("You don’t have to fill every space.");box.setOnClickListener(null);}else{boolean current=today&&next.start()<=now;label.setText((current?"Now · ":"Next · ")+next.title);detail.setText((current?next.end()-now+" minutes left":"Starts "+EventEditor.label(LocalTime.parse(next.time))+(today?" · in "+Math.max(0,next.start()-now)+" min":""))+" · "+next.category);box.setOnClickListener(v->MomentFeatures.event(a,next.id,next.title,next.day));}handler.postDelayed(this,60000);}};
  box.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){handler.removeCallbacks(tick);tick.run();}public void onViewDetachedFromWindow(View v){handler.removeCallbacks(tick);}});tick.run();
 }
 static void item(MainActivity a,LinearLayout parent,CalendarItem i){
  LinearLayout card=vertical(a);card.setPadding(a.dp(11),a.dp(10),a.dp(11),a.dp(10));card.setMinimumHeight(a.dp(48));card.setBackground(round(a,i.task()?BG:color(i),12));parent.addView(card,a.margins(5,3));LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);if(i.task()){CheckBox done=new CheckBox(a);done.setContentDescription("Mark "+i.title+" complete");done.setChecked(i.done);done.setOnCheckedChangeListener((v,on)->a.write(()->a.store.setDone(i.id,on)));row.addView(done);}TextView title=text(a,("birthday".equals(i.kind)?"Birthday · ":"")+i.title,15,i.done?MUTED:INK,true);if(i.done)title.setPaintFlags(title.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);if(i.timed())card.addView(text(a,EventEditor.label(LocalTime.parse(i.time))+" – "+(i.end()==1440?"midnight":EventEditor.label(LocalTime.of(i.end()/60,i.end()%60)))+" · "+i.category,13,MUTED,false));if(i.task()&&!i.done)card.addView(text(a,i.nextStep.isEmpty()?"Tap to break this into smaller steps":"Start with: "+i.nextStep+(i.stepMinutes>0?" · ~"+i.stepMinutes+" min remaining":""),13,MUTED,false));card.setOnClickListener(v->{if("event".equals(i.kind))MomentFeatures.event(a,i.id,i.title,i.day);else if(i.task())MomentFeatures.steps(a,i.id,i.title);});card.setOnLongClickListener(v->{actions(a,i);return true;});
 }
 static void actions(MainActivity a,CalendarItem i){new android.app.AlertDialog.Builder(a).setTitle(i.title).setItems(new String[]{"Edit / reschedule","Delete"},(d,n)->{if(n==0){if("event".equals(i.kind))EventEditor.edit(a,i.id,i.title,i.day);else a.edit(i.id,i.title,i.day);}else new android.app.AlertDialog.Builder(a).setMessage("Remove this plan? Your other plans will stay saved.").setNegativeButton("Keep it",null).setPositiveButton("Delete",(dialog,w)->a.write(()->a.store.delete(i.id))).show();}).show();}
 static void timeline(MainActivity a,LinearLayout parent,List<CalendarItem> daily,LocalDate date){
  List<CalendarItem> events=CalendarMath.timed(daily);int first=8,last=20;for(CalendarItem i:events){first=Math.min(first,i.start()/60);last=Math.max(last,(i.end()+59)/60);}if(date.equals(LocalDate.now())){first=Math.min(first,LocalTime.now().getHour());last=Math.max(last,LocalTime.now().getHour()+1);}final int from=first*60,to=Math.min(1440,last*60);
  FrameLayout canvas=new FrameLayout(a);canvas.setBackground(round(a,BG,10));parent.addView(canvas,new LinearLayout.LayoutParams(-1,a.dp(to-from+32)));
  for(int minute=from;minute<to;minute+=60){final int slot=minute;LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.TOP);TextView hour=text(a,EventEditor.label(LocalTime.of(minute/60,0)),11,MUTED,false);line.addView(hour,new LinearLayout.LayoutParams(a.dp(58),a.dp(32)));TextView space=text(a,"＋",15,MUTED,false);space.setPadding(a.dp(4),0,0,0);line.addView(space,new LinearLayout.LayoutParams(-1,a.dp(60)));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,a.dp(60));p.topMargin=a.dp(minute-from);canvas.addView(line,p);line.setContentDescription("Add event at "+EventEditor.label(LocalTime.of(minute/60,0)));line.setOnClickListener(v->EventEditor.create(a,date,LocalTime.of(slot/60,0)));View rule=new View(a);rule.setBackgroundColor(Color.rgb(233,230,239));FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(-1,a.dp(1));rp.topMargin=p.topMargin;rp.leftMargin=a.dp(58);canvas.addView(rule,rp);}
  List<CalendarMath.Block> blocks=CalendarMath.layout(daily);List<TextView> views=new ArrayList<>();for(CalendarMath.Block block:blocks){CalendarItem i=block.item;TextView event=text(a,EventEditor.label(LocalTime.parse(i.time))+" · "+i.title+"\n"+i.duration+"m · "+i.category,12,INK,true);event.setPadding(a.dp(7),a.dp(3),a.dp(5),a.dp(3));event.setMaxLines(i.duration<45?1:3);event.setEllipsize(TextUtils.TruncateAt.END);event.setBackground(round(a,color(i),8));event.setContentDescription(i.title+", "+EventEditor.label(LocalTime.parse(i.time))+", "+i.duration+" minutes, "+i.category+". Tap for details.");event.setOnClickListener(v->MomentFeatures.event(a,i.id,i.title,i.day));event.setOnLongClickListener(v->{actions(a,i);return true;});canvas.addView(event);views.add(event);}
  canvas.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l==or-ol)return;int usable=r-l-a.dp(62);for(int n=0;n<blocks.size();n++){CalendarMath.Block block=blocks.get(n);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(Math.max(a.dp(28),usable/block.lanes-a.dp(3)),a.dp(Math.max(30,block.item.end()-block.item.start())-2));p.leftMargin=a.dp(60)+block.lane*usable/block.lanes;p.topMargin=a.dp(block.item.start()-from);views.get(n).setLayoutParams(p);}});
  if(date.equals(LocalDate.now()))liveNow(a,canvas,date,from,to);
 }
 static void liveNow(MainActivity a,FrameLayout canvas,LocalDate date,int from,int to){
  TextView marker=text(a,"NOW ━━━━━━━━━━━━━━━━━",11,PURPLE,true);canvas.addView(marker,new FrameLayout.LayoutParams(-1,a.dp(18)));marker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);marker.setClickable(false);
  Handler handler=new Handler(Looper.getMainLooper());Runnable tick=new Runnable(){public void run(){int now=LocalTime.now().toSecondOfDay()/60;boolean visible=date.equals(LocalDate.now())&&now>=from&&now<to;marker.setVisibility(visible?View.VISIBLE:View.GONE);if(visible){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)marker.getLayoutParams();p.topMargin=a.dp(now-from);marker.setLayoutParams(p);}handler.postDelayed(this,60000);}};
  marker.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){handler.removeCallbacks(tick);tick.run();}public void onViewDetachedFromWindow(View v){handler.removeCallbacks(tick);}});tick.run();
 }
 static boolean monthDayMatches(LocalDate birth,LocalDate target){return birth.withYear(target.getYear()).equals(target);}
}
