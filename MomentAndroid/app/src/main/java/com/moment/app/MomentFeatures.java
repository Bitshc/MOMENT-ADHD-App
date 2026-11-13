package com.moment.app;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.CountDownTimer;
import android.view.*;
import android.widget.*;
import java.net.URLEncoder;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MomentFeatures {
 static final int INK=Color.rgb(43,37,66), MUTED=Color.rgb(111,105,130), PURPLE=Color.rgb(112,83,173);
 static TextView text(Context c,String s,int size,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(INK);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(8,9,8,9);return t;}
 static LinearLayout vertical(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);l.setPadding(12,8,12,8);return l;}
 static String monthKey(String day){return day.length()>=10?day.substring(5):"";}
 static void calendar(MainActivity a){calendar(a,YearMonth.from(LocalDate.parse(a.selectedDay)));}
 static void calendar(MainActivity a,YearMonth month){
  LinearLayout panel=vertical(a);TextView title=text(a,month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.US)),23,true);panel.addView(title);
  LinearLayout header=new LinearLayout(a);Button previous=new Button(a),next=new Button(a);
  previous.setText("‹  Previous");next.setText("Next  ›");header.addView(previous,new LinearLayout.LayoutParams(0,-2,1));header.addView(next,new LinearLayout.LayoutParams(0,-2,1));panel.addView(header);
  GridLayout grid=new GridLayout(a);grid.setColumnCount(7);panel.addView(grid);
  TextView help=text(a,"Events 📅  ·  Birthdays 🎂  ·  Tap any day to view plans.",13,false);panel.addView(help);
  LinearLayout shortcuts=new LinearLayout(a);Button addBirthday=new Button(a);addBirthday.setText("＋ Birthday");addBirthday.setAllCaps(false);Button addEvent=new Button(a);addEvent.setText("＋ Event");addEvent.setAllCaps(false);shortcuts.addView(addBirthday,new LinearLayout.LayoutParams(0,-2,1));shortcuts.addView(addEvent,new LinearLayout.LayoutParams(0,-2,1));panel.addView(shortcuts);ScrollView scroll=new ScrollView(a);scroll.addView(panel);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Your calendar").setView(scroll).setPositiveButton("Close",null).create();
  addBirthday.setOnClickListener(v->{dialog.dismiss();a.addDialog("birthday");});addEvent.setOnClickListener(v->{dialog.dismiss();addEvent(a);});previous.setOnClickListener(v->{dialog.dismiss();calendar(a,month.minusMonths(1));});
  next.setOnClickListener(v->{dialog.dismiss();calendar(a,month.plusMonths(1));});
  dialog.show();
  a.db.execute(()->{
   Map<Integer,List<String>> labels=new HashMap<>();
   try(Cursor c=a.store.all()){while(c.moveToNext()){
    String item=c.getString(1),kind=c.getString(2),day=c.getString(3);
    if(!"event".equals(kind)&&!"birthday".equals(kind))continue;
    try{
     LocalDate date=LocalDate.parse(day);
     boolean matches="birthday".equals(kind)?date.getMonthValue()==month.getMonthValue():YearMonth.from(date).equals(month);
     if(matches){
      int dayNum=date.getDayOfMonth();
      if(!labels.containsKey(dayNum))labels.put(dayNum,new ArrayList<>());
      labels.get(dayNum).add(("birthday".equals(kind)?"🎂 ":"📅 ")+item);
     }
    }catch(Exception ignored){}
   }}
   a.runOnUiThread(()->{
    if(a.isFinishing()||a.isDestroyed())return;
    String[] week={"M","T","W","T","F","S","S"};
    for(String w:week){TextView head=text(a,w,13,true);head.setGravity(Gravity.CENTER);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(head,p);}
    int offset=month.atDay(1).getDayOfWeek().getValue()-1;
    for(int i=0;i<offset;i++){TextView blank=text(a,"",12,false);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=54;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(blank,p);}
    for(int d=1;d<=month.lengthOfMonth();d++){
     final int day=d;LinearLayout cell=new LinearLayout(a);cell.setOrientation(1);cell.setPadding(2,3,2,3);cell.setMinimumHeight(70);
     TextView number=text(a,String.valueOf(day),14,true);number.setGravity(Gravity.CENTER);if(LocalDate.now().equals(month.atDay(day)))number.setTextColor(PURPLE);cell.addView(number);
     List<String> entries=labels.get(day);if(entries!=null){TextView notes=text(a,entries.size()>1?"● "+entries.size()+" plans":"●",11,false);notes.setTextColor(PURPLE);notes.setGravity(Gravity.CENTER);cell.addView(notes);}
     cell.setOnClickListener(v->{a.selectedDay=month.atDay(day).toString();dialog.dismiss();a.show();});
     GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(cell,p);
    }
    if(labels.isEmpty())help.setText("No events or birthdays this month. Add an event to get started.");
    else {
     StringBuilder overview=new StringBuilder("This month's dates:\n");
     for(int d=1;d<=month.lengthOfMonth();d++)if(labels.containsKey(d)){for(String e:labels.get(d))overview.append(d).append(" · ").append(e).append("\n");}
     help.setText(overview.toString());
    }
   });
  });
 }
 static void addEvent(MainActivity a){EventEditor.create(a,LocalDate.parse(a.selectedDay),InlineCalendar.suggested(LocalDate.parse(a.selectedDay)));}
 static void event(MainActivity a,long id,String title,String day){
  a.db.execute(()->{
   String[] data=a.store.eventDetails(id);
   boolean reminders=a.store.remindersEnabled(id);
   a.runOnUiThread(()->{
    if(a.isFinishing())return;
    String time=data[0],address=data[1];int mins=0;try{mins=Integer.parseInt(data[2]);}catch(Exception ignored){}
    final int travel=mins;
    LinearLayout p=vertical(a);p.addView(text(a,"📅 "+title+"\n"+day+(time.isEmpty()?"":" at "+time),18,true));
    p.addView(text(a,(time.isEmpty()?"All day / no fixed time":data[3]+" minutes")+" · "+data[4],14,true));
    if(!address.isEmpty())p.addView(text(a,"📍 "+address,14,false));
    EditText home=new EditText(a);home.setHint("Your starting address");home.setSingleLine(true);
    home.setText(a.getPreferences(0).getString("home_address",""));p.addView(home);
    if(!address.isEmpty()&&!time.isEmpty()){
     try{
      LocalDateTime eventAt=LocalDateTime.of(LocalDate.parse(day),LocalTime.parse(time));
      LocalDateTime leaveAt=eventAt.minusMinutes(travel+10);
      String estimate=travel>0?"Using your entered "+travel+"-minute trip estimate plus a 10-minute buffer.":"Enter a trip estimate when editing the event to calculate a leave time.";
      p.addView(text(a,travel>0?"Suggested departure: "+leaveAt.format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")):"Departure estimate not configured",16,true));
      p.addView(text(a,estimate+" Live traffic must be checked in Google Maps.",13,false));
     }catch(Exception ignored){}
    }
    ReminderControls.attach(a,p,id,day,time,reminders);
    ScrollView eventScroll=new ScrollView(a);eventScroll.addView(p);
    AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Event details").setView(eventScroll).setNegativeButton("Close",null).setNeutralButton("Edit",(d,w)->editEvent(a,id,title,day)).setPositiveButton("Open Maps",(d,w)->{
     String start=home.getText().toString().trim();a.getPreferences(0).edit().putString("home_address",start).apply();
     if(address.isEmpty()){Toast.makeText(a,"Add a destination address to this event first.",Toast.LENGTH_LONG).show();return;}
     try{
      a.startActivity(MapsLink.intent(address,start));
     }catch(Exception e){Toast.makeText(a,"Couldn't open maps.",Toast.LENGTH_LONG).show();}
    }).create();dialog.show();
   });
  });
 }
 static void editEvent(MainActivity a,long id,String title,String day){EventEditor.edit(a,id,title,day);}
 static void steps(MainActivity a,long id,String title){
  a.db.execute(()->{
   ArrayList<String[]> saved=new ArrayList<>();
   try(Cursor c=a.store.steps(id)){while(c.moveToNext())saved.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}
   a.runOnUiThread(()->{
    if(a.isFinishing())return;
    LinearLayout panel=vertical(a);panel.addView(text(a,title,19,true));
    panel.addView(text(a,"Each step is a starting-point estimate. Adjust the minutes to fit you.",13,false));
    int total=0,finished=0;
    for(String[] row:saved){
     int min=Integer.parseInt(row[2]);total+=min;if("1".equals(row[3]))finished++;
     CheckBox check=new CheckBox(a);check.setText(row[1]+"  ·  "+min+" min");check.setTextColor(INK);check.setChecked("1".equals(row[3]));long stepId=Long.parseLong(row[0]);
     check.setOnCheckedChangeListener((v,checked)->a.db.execute(()->a.store.setStepDone(stepId,checked)));panel.addView(check);
    }
    panel.addView(text(a,finished+" of "+saved.size()+" steps finished · about "+total+" minutes total",14,true));
    Button edit=new Button(a);edit.setAllCaps(false);edit.setText(saved.isEmpty()?"✨  Suggest starter steps":"✏️  Edit steps and estimates");panel.addView(edit);
    ScrollView scroll=new ScrollView(a);scroll.addView(panel);
    AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Break it down").setView(scroll).setPositiveButton("Done",null).create();
    edit.setOnClickListener(v->{dialog.dismiss();editSteps(a,id,title,saved);});dialog.show();
   });
  });
 }
 static String suggestions(String title){
  String t=title.toLowerCase(Locale.US);
  if(t.contains("clean")||t.contains("chore"))return "Pick one small area | 3\nGather cleaning supplies | 3\nClear the surface | 5\nWipe or clean the area | 10\nPut supplies away | 3";
  if(t.contains("laundry"))return "Gather clothes | 3\nStart one load | 5\nMove clothes to dryer | 5\nFold one small pile | 10\nPut clothes away | 5";
  if(t.contains("study")||t.contains("homework"))return "Open the materials | 2\nChoose one topic | 3\nFocus on the first section | 15\nTake a short break | 5\nReview what you finished | 5";
  return "Get what you need | 3\nDo the tiniest first step | 5\nWork on the main part | 15\nCheck what is left | 3\nPut things away or wrap up | 4";
 }
 static void editSteps(MainActivity a,long id,String title,ArrayList<String[]> saved){
  EditText input=new EditText(a);input.setMinLines(7);input.setTextSize(15);input.setHint("One step per line: Step name | minutes");
  StringBuilder lines=new StringBuilder();
  for(String[] s:saved)lines.append(s[1]).append(" | ").append(s[2]).append("\n");
  input.setText(saved.isEmpty()?suggestions(title):lines.toString());
  new AlertDialog.Builder(a).setTitle("Your small steps").setMessage("Edit anything. The suggestions are examples, not AI-generated guarantees. Format: Step | minutes").setView(input).setNegativeButton("Cancel",(d,w)->steps(a,id,title)).setPositiveButton("Save",(d,w)->{
   ArrayList<String[]> values=new ArrayList<>();
   for(String line:input.getText().toString().split("\n")){
    String[] bits=line.split("\\|",2);String name=bits[0].trim();if(name.isEmpty())continue;
    int minutes=5;try{if(bits.length>1)minutes=Math.max(1,Math.min(480,Integer.parseInt(bits[1].trim())));}catch(Exception ignored){}
    values.add(new String[]{name,String.valueOf(minutes)});
   }
   a.db.execute(()->{a.store.replaceSteps(id,values);a.runOnUiThread(()->steps(a,id,title));});
  }).show();
 }
 static void companion(MainActivity a){
  LinearLayout panel=vertical(a);TextView avatar=text(a,"🪴",48,true);avatar.setGravity(Gravity.CENTER);panel.addView(avatar);
  TextView heading=text(a,"I'll stay here while you work.",20,true);heading.setGravity(Gravity.CENTER);panel.addView(heading);
  TextView clock=text(a,"25:00",38,true);clock.setGravity(Gravity.CENTER);panel.addView(clock);
  TextView message=text(a,"A virtual focus buddy — not another person online.\nChoose one step. Starting counts.",14,false);message.setGravity(Gravity.CENTER);panel.addView(message);
  Button shortTimer=new Button(a);shortTimer.setText("Start 10 minutes");shortTimer.setAllCaps(false);panel.addView(shortTimer);
  Button timer=new Button(a);timer.setText("Start 25 minutes");timer.setAllCaps(false);panel.addView(timer);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Focus together").setView(panel).setNegativeButton("Close",null).create();
  final CountDownTimer[] running={null};
  View.OnClickListener start=v->{
   if(running[0]!=null)running[0].cancel();
   int mins=v==shortTimer?10:25;
   long duration=mins*60000L;
   shortTimer.setEnabled(false);timer.setEnabled(false);message.setText("We're doing this together. Just the next tiny step. 💜");
   running[0]=new CountDownTimer(duration,1000){
    public void onTick(long left){long s=(left+999)/1000;clock.setText(String.format(Locale.US,"%02d:%02d",s/60,s%60));}
    public void onFinish(){clock.setText("✓");message.setText("You showed up. That counts. Take a break. 🌿");shortTimer.setEnabled(true);timer.setEnabled(true);}
   };running[0].start();
  };
  shortTimer.setOnClickListener(start);timer.setOnClickListener(start);
  dialog.setOnDismissListener(d->{if(running[0]!=null)running[0].cancel();});
  dialog.show();
 }
}
