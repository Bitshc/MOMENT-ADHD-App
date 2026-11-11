package com.moment.app;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Always-visible month and selected-day agenda. The DB is read off the UI thread. */
public class InlineCalendar {
 static final int INK=Color.rgb(43,37,66), MUTED=Color.rgb(110,104,130), PURPLE=Color.rgb(112,83,173), BG=Color.rgb(250,248,253), LAVENDER=Color.rgb(238,229,251), WHITE=Color.WHITE, BORDER=Color.rgb(232,227,239);
 static int dp(MainActivity a,int n){return a.dp(n);}
 static GradientDrawable round(MainActivity a,int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(a,radius));return g;}
 static TextView text(MainActivity a,String s,int size,int color,boolean bold){
  TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;
 }
 static LinearLayout vertical(MainActivity a){LinearLayout l=new LinearLayout(a);l.setOrientation(1);return l;}
 static void attach(MainActivity a,LinearLayout parent){
  final YearMonth month=a.calendarMonth;
  final boolean weekMode=a.weekView;
  final LocalDate selected=LocalDate.parse(a.selectedDay);
  final LocalDate firstVisible=weekMode?selected.minusDays(selected.getDayOfWeek().getValue()-1):month.atDay(1);
  LinearLayout card=vertical(a);card.setPadding(dp(a,14),dp(a,15),dp(a,14),dp(a,14));card.setBackground(round(a,WHITE,23));card.setTag("moment_calendar");parent.addView(card,a.margins(16,12));
  LinearLayout title=new LinearLayout(a);title.setGravity(Gravity.CENTER_VERTICAL);TextView heading=text(a,(weekMode?firstVisible.format(DateTimeFormatter.ofPattern("MMM d",Locale.US))+" – "+firstVisible.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d",Locale.US)):month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.US))),20,INK,true);title.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  Button back=button(a,"‹");Button next=button(a,"›");title.addView(back,new LinearLayout.LayoutParams(dp(a,45),dp(a,45)));title.addView(next,new LinearLayout.LayoutParams(dp(a,45),dp(a,45)));card.addView(title);
  LinearLayout controls=new LinearLayout(a);controls.setGravity(Gravity.CENTER_VERTICAL);
  Button weekToggle=button(a,weekMode?"✓ Week":"Week"),monthToggle=button(a,weekMode?"Month":"✓ Month");
  LinearLayout.LayoutParams toggle1=new LinearLayout.LayoutParams(0,dp(a,39),1);toggle1.rightMargin=dp(a,7);
  controls.addView(weekToggle,toggle1);controls.addView(monthToggle,new LinearLayout.LayoutParams(0,dp(a,39),1));
  card.addView(controls,a.margins(5,3));
  weekToggle.setOnClickListener(v->{a.weekView=true;a.getPreferences(0).edit().putBoolean("week_mode",true).apply();a.show();});
  monthToggle.setOnClickListener(v->{a.weekView=false;a.getPreferences(0).edit().putBoolean("week_mode",false).apply();a.calendarMonth=YearMonth.from(LocalDate.parse(a.selectedDay));a.show();});
  back.setOnClickListener(v->{
   if(a.weekView){a.selectedDay=LocalDate.parse(a.selectedDay).minusWeeks(1).toString();a.calendarMonth=YearMonth.from(LocalDate.parse(a.selectedDay));}
   else a.calendarMonth=a.calendarMonth.minusMonths(1);
   a.show();
  });
  next.setOnClickListener(v->{
   if(a.weekView){a.selectedDay=LocalDate.parse(a.selectedDay).plusWeeks(1).toString();a.calendarMonth=YearMonth.from(LocalDate.parse(a.selectedDay));}
   else a.calendarMonth=a.calendarMonth.plusMonths(1);
   a.show();
  });
  LinearLayout shortcuts=new LinearLayout(a);shortcuts.setGravity(Gravity.CENTER_VERTICAL);
  Button add=button(a,"＋ Event");Button birthday=button(a,"＋ Birthday");LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(a,45),1);left.rightMargin=dp(a,8);shortcuts.addView(add,left);shortcuts.addView(birthday,new LinearLayout.LayoutParams(0,dp(a,45),1));card.addView(shortcuts,a.margins(8,12));
  add.setOnClickListener(v->MomentFeatures.addEvent(a));birthday.setOnClickListener(v->a.addDialog("birthday"));
  LinearLayout weekday=new LinearLayout(a);String[] names={"M","T","W","T","F","S","S"};
  for(String name:names){TextView label=text(a,name,12,MUTED,true);label.setGravity(Gravity.CENTER);weekday.addView(label,new LinearLayout.LayoutParams(0,dp(a,32),1));}card.addView(weekday);
  LinearLayout monthRows=vertical(a);card.addView(monthRows);
  TextView legend=text(a,"● Events    ♥ Birthdays    · Tasks",12,MUTED,false);legend.setGravity(Gravity.CENTER);card.addView(legend,a.margins(9,0));
  LinearLayout agenda=vertical(a);card.addView(agenda,a.margins(14,0));agenda.addView(text(a,"Loading your day…",14,MUTED,false));
  a.db.execute(()->{
   Map<LocalDate,int[]> counts=new HashMap<>();ArrayList<String[]> daily=new ArrayList<>();
   try(Cursor c=a.store.all()){while(c.moveToNext()){
    String name=c.getString(1),kind=c.getString(2),date=c.getString(3);long id=c.getLong(0);
    LocalDate itemDate;try{itemDate=LocalDate.parse(date);}catch(Exception e){continue;}
    boolean birthdayMatch="birthday".equals(kind)&&itemDate.getMonthValue()==month.getMonthValue();
    boolean normalMatch=YearMonth.from(itemDate).equals(month);
    boolean mark="birthday".equals(kind)?birthdayMatch:normalMatch;
    if(weekMode){
     if("birthday".equals(kind)){
      for(int offset=0;offset<7;offset++){LocalDate w=firstVisible.plusDays(offset);if(monthDayMatches(itemDate,w)){int[] arr=counts.get(w);if(arr==null){arr=new int[3];counts.put(w,arr);}arr[1]++;}}
     }else if(!itemDate.isBefore(firstVisible)&&itemDate.isBefore(firstVisible.plusDays(7))){
      int[] arr=counts.get(itemDate);if(arr==null){arr=new int[3];counts.put(itemDate,arr);}arr["event".equals(kind)?0:2]++;
     }
    }else if(mark){
     int day=(birthdayMatch&&itemDate.getMonthValue()==2&&itemDate.getDayOfMonth()==29&&!month.isLeapYear())?28:itemDate.getDayOfMonth();
     LocalDate key=month.atDay(day);int[] arr=counts.get(key);if(arr==null){arr=new int[3];counts.put(key,arr);}arr["event".equals(kind)?0:"birthday".equals(kind)?1:2]++;
    }
    boolean selectedMatch="birthday".equals(kind)?monthDayMatches(itemDate,selected):date.equals(a.selectedDay);
    if(selectedMatch){String time="event".equals(kind)?a.store.eventDetails(id)[0]:"";daily.add(new String[]{String.valueOf(id),name,kind,time,date});}
   }}
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed()||!a.calendarMonth.equals(month)||a.weekView!=weekMode||!a.selectedDay.equals(selected.toString()))return;
    monthRows.removeAllViews();int offset=month.atDay(1).getDayOfWeek().getValue()-1;int days=month.lengthOfMonth();int cells=weekMode?7:((offset+days+6)/7)*7;
    for(int base=0;base<cells;base+=7){
     LinearLayout week=new LinearLayout(a);week.setOrientation(0);monthRows.addView(week);
     for(int col=0;col<7;col++){int day=base+col-offset+1;
      if(!weekMode&&(day<1||day>days)){TextView empty=text(a,"",14,INK,false);week.addView(empty,new LinearLayout.LayoutParams(0,dp(a,57),1));continue;}
      LinearLayout box=vertical(a);box.setGravity(Gravity.CENTER);box.setPadding(dp(a,1),dp(a,4),dp(a,1),dp(a,3));
      LocalDate calendarDay=weekMode?firstVisible.plusDays(col):month.atDay(day);boolean chosen=calendarDay.equals(selected),today=calendarDay.equals(LocalDate.now());
      if(chosen)box.setBackground(round(a,LAVENDER,13));
      TextView dayText=text(a,String.valueOf(calendarDay.getDayOfMonth()),15,chosen?PURPLE:INK,chosen||today);dayText.setGravity(Gravity.CENTER);box.addView(dayText);
      int[] ct=counts.get(calendarDay);StringBuilder symbols=new StringBuilder();
      if(ct!=null){if(ct[0]>0)symbols.append("●");if(ct[1]>0)symbols.append("♥");if(ct[2]>0)symbols.append("·");}
      TextView marks=text(a,symbols.toString(),12,PURPLE,true);marks.setGravity(Gravity.CENTER);box.addView(marks);
      week.addView(box,new LinearLayout.LayoutParams(0,dp(a,57),1));box.setOnClickListener(v->{a.selectedDay=calendarDay.toString();a.show();});
     }
    }
    agenda.removeAllViews();String dayTitle=selected.format(DateTimeFormatter.ofPattern("EEEE, MMMM d",Locale.US));
    agenda.addView(text(a,"Your plans · "+dayTitle,17,INK,true),a.margins(0,7));
    if(daily.isEmpty()){TextView empty=text(a,"No plans for this day. Enjoy the breathing room. 🌿",13,MUTED,false);empty.setPadding(0,dp(a,9),0,dp(a,10));agenda.addView(empty);}
    else{
     daily.sort((x,y)->{int aKey="event".equals(x[2])?0:"birthday".equals(x[2])?1:2;int bKey="event".equals(y[2])?0:"birthday".equals(y[2])?1:2;if(aKey!=bKey)return aKey-bKey;return x[3].compareTo(y[3]);});
     for(String[] entry:daily){
      long id=Long.parseLong(entry[0]);String kind=entry[2],titleText=entry[1],date=entry[4];
      String prefix="event".equals(kind)?"📅 ":"birthday".equals(kind)?"🎂 ":"☐ ";
      String suffix="event".equals(kind)&&!entry[3].isEmpty()?"  ·  "+entry[3]:"";
      TextView item=text(a,prefix+titleText+suffix,14,INK,false);item.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));item.setBackground(round(a,BG,13));agenda.addView(item,a.margins(5,0));
      if("event".equals(kind))item.setOnClickListener(v->MomentFeatures.event(a,id,titleText,date));
      else if("task".equals(kind))item.setOnClickListener(v->MomentFeatures.steps(a,id,titleText));
     }
    }
   });
  });
 }
 static boolean monthDayMatches(LocalDate birth,LocalDate target){
  if(birth.getMonthValue()==2&&birth.getDayOfMonth()==29&&!target.isLeapYear())return target.getMonthValue()==2&&target.getDayOfMonth()==28;
  return birth.getMonthValue()==target.getMonthValue()&&birth.getDayOfMonth()==target.getDayOfMonth();
 }
 static Button button(MainActivity a,String value){Button b=new Button(a);b.setAllCaps(false);b.setText(value);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(PURPLE);b.setBackground(round(a,LAVENDER,13));return b;}
}
