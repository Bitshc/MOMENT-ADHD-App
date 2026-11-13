package com.moment.app;
import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 Store store;
 final ExecutorService db=Executors.newSingleThreadExecutor();
 LinearLayout root;
 String selectedDay;YearMonth calendarMonth;boolean weekView=false;
 SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
 final int INK=Color.rgb(43,37,66), MUTED=Color.rgb(111,105,130), PURPLE=Color.rgb(112,83,173), LAVENDER=Color.rgb(238,229,251), BG=Color.rgb(250,248,253), WHITE=Color.WHITE, SAGE=Color.rgb(225,239,226), BORDER=Color.rgb(232,227,239);

 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);store=new Store(this);selectedDay=fmt.format(new Date());calendarMonth=YearMonth.from(LocalDate.now());weekView=getPreferences(0).getBoolean("week_mode",false);show();handleWidgetAction(getIntent());}
 @Override protected void onNewIntent(android.content.Intent intent){
  super.onNewIntent(intent);setIntent(intent);handleWidgetAction(intent);
 }
 void handleWidgetAction(android.content.Intent intent){
  if(intent==null)return;
  String action=intent.getAction();
  if(action==null)return;
  // Clear the action so returning to MOMENT does not re-open a dialog.
  intent.setAction("com.moment.app.OPEN");
  switch(action){
   case "com.moment.app.QUICK_ADD":
   case "com.moment.app.WIDGET_ADD_TASK":addDialog("task");break;
   case "com.moment.app.WIDGET_ADD_CHORE":addDialog("chore");break;
   case "com.moment.app.WIDGET_ADD_EVENT":
    selectedDay=LocalDate.now().toString();calendarMonth=YearMonth.from(LocalDate.now());
    MomentFeatures.addEvent(this);break;
   case "com.moment.app.WIDGET_DATE":
    try{
     String day=intent.getStringExtra("widget_day");
     LocalDate date=LocalDate.parse(day);
     selectedDay=date.toString();calendarMonth=YearMonth.from(date);show();
     View selectedCalendar=root.findViewWithTag("moment_calendar");
     if(selectedCalendar!=null&&root.getParent() instanceof ScrollView){
      ScrollView sc=(ScrollView)root.getParent();
      sc.post(()->sc.smoothScrollTo(0,selectedCalendar.getTop()));
     }
    }catch(Exception ignored){}
    break;
   case "com.moment.app.WIDGET_CALENDAR":
    selectedDay=LocalDate.now().toString();calendarMonth=YearMonth.from(LocalDate.now());
    show();
    View calendar=root.findViewWithTag("moment_calendar");
    if(calendar!=null&&root.getParent() instanceof ScrollView){
     ScrollView sc=(ScrollView)root.getParent();
     sc.post(()->sc.smoothScrollTo(0,calendar.getTop()));
    }
    break;
   case "com.moment.app.WIDGET_FOCUS":MomentFeatures.companion(this);break;
   case "com.moment.app.WIDGET_ALARM":showAlarmSettings();break;
  }
 }
 void showAlarmSettings(){
  int hour=DailyReminder.hour(this),min=DailyReminder.minute(this);
  new TimePickerDialog(this,(picker,h,m)->{
   if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},301);
    Toast.makeText(this,"Allow notifications, then set the reminder time again.",Toast.LENGTH_LONG).show();return;
   }
   DailyReminder.setTime(this,h,m);
   Toast.makeText(this,"Daily reminder enabled · "+String.format(Locale.US,"%02d:%02d",h,m),Toast.LENGTH_LONG).show();
   show();
  },hour,min,false).show();
 }
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 GradientDrawable stroke(int color,int radius){GradientDrawable d=bg(color,radius);d.setStroke(dp(1),BORDER);return d;}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(INK);t.setPadding(0,dp(5),0,dp(5));return t;}
 TextView txt(String s,int size,int color,boolean bold){TextView t=label(s,size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 LinearLayout.LayoutParams margins(int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);p.bottomMargin=dp(bottom);return p;}
 Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setPadding(dp(10),dp(12),dp(10),dp(12));b.setBackground(stroke(WHITE,17));b.setOnClickListener(v->r.run());return b;}
 void addTile(LinearLayout parent,String emoji,String title,String subtitle,Runnable action,int marginLeft){LinearLayout card=column();card.setPadding(dp(13),dp(11),dp(11),dp(10));card.setBackground(stroke(WHITE,20));card.setMinimumHeight(dp(104));card.addView(txt(emoji,24,INK,false));card.addView(txt(title,15,INK,true));card.addView(txt(subtitle,12,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(marginLeft);parent.addView(card,p);card.setOnClickListener(v->action.run());}
 String prettyDate(){try{return new SimpleDateFormat("EEE, MMM d",Locale.US).format(fmt.parse(selectedDay));}catch(Exception e){return selectedDay;}}
 void write(Runnable work){db.execute(()->{work.run();EventReminders.reschedule(getApplicationContext());MomentWidget.refresh(getApplicationContext());LockGlance.refresh(getApplicationContext());DailyReminder.schedule(getApplicationContext());runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())show();});});}
 void show(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=column();root.setPadding(dp(13),dp(10),dp(13),dp(24));root.setBackgroundColor(BG);scroll.addView(root);setContentView(scroll);
  LinearLayout header=row();
  TextView logo=txt("moment.",29,INK,true);header.addView(logo,new LinearLayout.LayoutParams(0,-2,1));
  Button reminders=button("🔔",()->showReminderMenu());reminders.setContentDescription("Notification settings");
  reminders.setTextSize(20);header.addView(reminders,new LinearLayout.LayoutParams(dp(52),dp(48)));
  root.addView(header);
  InlineCalendar.attach(this,root);
  Button more=button("☰  More tools & settings",()->showMoreTools());
  more.setBackground(bg(LAVENDER,17));root.addView(more,margins(10,6));
 }
 void showReminderMenu(){
  String status=android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED?
    "Notifications permission: not granted":"Notifications permission: allowed or not required";
  String[] options={"Set daily reminder time","Daily task reminders: "+(DailyReminder.enabled(this)?"On":"Off"),"Open Android notification settings"};
  new AlertDialog.Builder(this).setTitle("Reminders").setMessage(status+"\nEvent reminders can be configured when creating or editing events.").setItems(options,(d,n)->{
   if(n==0)showAlarmSettings();
   if(n==1){
    if(!DailyReminder.enabled(this)&&android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},301);
      Toast.makeText(this,"Allow notifications, then enable reminders again.",Toast.LENGTH_LONG).show();
    }else{DailyReminder.setEnabled(this,!DailyReminder.enabled(this));show();}
   }
   if(n==2){
    android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(intent);
   }
  }).show();
 }
 void showMoreTools(){
  String[] labels={"✅ To-do lists","🧹 Chores","🧠 Brain dump","⏳ Focus together","🎯 Help me start","🎂 Add a birthday","📍 Plan an event & travel","💜 Shared Space","🏡 Home-screen widget help","🔔 Reminder settings","🎤 Voice capture"};
  new AlertDialog.Builder(this).setTitle("More, whenever you need it").setItems(labels,(d,index)->{
   switch(index){
    case 0:openList("task");break;
    case 1:openList("chore");break;
    case 2:brainDump();break;
    case 3:MomentFeatures.companion(this);break;
    case 4:focus();break;
    case 5:addDialog("birthday");break;
    case 6:EventEditor.create(this,LocalDate.parse(selectedDay),InlineCalendar.suggested(LocalDate.parse(selectedDay)));break;
    case 7:sharedSpace();break;
    case 8:new AlertDialog.Builder(this).setTitle("Add the MOMENT widget").setMessage("Hold an empty spot on your Samsung home screen, tap Widgets, search MOMENT, then drag the widget onto your home screen. Resize it to show the full month.").setPositiveButton("Got it",null).show();break;
    case 9:showReminderMenu();break;
    case 10:startActivity(new android.content.Intent(this,LockQuickAddActivity.class).putExtra("kind","task"));break;
   }
  }).show();
 }

 void addOverview(){
  LinearLayout card=column();card.setPadding(dp(12),dp(9),dp(12),dp(9));card.setBackground(stroke(WHITE,18));
  card.addView(txt("🔔  What matters today",18,INK,true));

  LinearLayout contents=column();contents.addView(txt("Loading reminders…",13,MUTED,false));card.addView(contents);
  root.addView(card,margins(13,5));
  final LocalDate today=LocalDate.now();final String now=today.toString(),last=today.plusDays(7).toString();
  db.execute(()->{
   ArrayList<String[]> critical=new ArrayList<>(),chores=new ArrayList<>(),todos=new ArrayList<>(),events=new ArrayList<>();
   try(Cursor c=store.all()){
    while(c.moveToNext()){
     long id=c.getLong(0);String title=c.getString(1),kind=c.getString(2),day=c.getString(3);
     if(("task".equals(kind)||"chore".equals(kind))&&c.getInt(4)==0&&day.compareTo(last)<=0){
      String state=day.compareTo(now)<0?"Overdue":day.equals(now)?"Due today":"Due "+day;
      String[] item={String.valueOf(id),kind,title,day,state};
      ("chore".equals(kind)?chores:todos).add(item);
      if(day.compareTo(now)<=0)critical.add(item);
     }else if("event".equals(kind)&&day.compareTo(now)>=0&&day.compareTo(last)<=0){
      String time=store.eventDetails(id)[0];
      String[] item={String.valueOf(id),kind,title,day,time.isEmpty()?day:day+" · "+time};
      events.add(item);if(day.equals(now))critical.add(item);
     }else if("birthday".equals(kind)){
      try{
       LocalDate birth=LocalDate.parse(day),next=birthdayThisYear(birth,today.getYear());
       if(next.isBefore(today))next=birthdayThisYear(birth,today.getYear()+1);
       if(!next.isAfter(today.plusDays(7))){
        String[] item={String.valueOf(id),kind,title,next.toString(),"Birthday · "+next};
        events.add(item);if(next.equals(today))critical.add(item);
       }
      }catch(Exception ignored){}
     }
    }
   }
   java.util.Comparator<String[]> byDate=(x,y)->x[3].compareTo(y[3]);
   Collections.sort(critical,byDate);Collections.sort(chores,byDate);Collections.sort(todos,byDate);Collections.sort(events,byDate);
   runOnUiThread(()->{
    if(isFinishing()||isDestroyed()||contents.getParent()==null)return;
    contents.removeAllViews();
    // Critical items are repeated in their category intentionally, for easy discovery.
    addReminderSection(contents,"⚡  Needs attention",critical,"Nothing urgent right now.",3,true);
    addReminderSection(contents,"🧹  Chores",chores,"No chores due this week.",4,false);
    addReminderSection(contents,"✅  To-dos",todos,"No to-dos due this week.",4,false);
    addReminderSection(contents,"📅  Events & birthdays",events,"No events in the next 7 days.",4,false);
   });
  });
 }
 void addReminderSection(LinearLayout parent,String heading,ArrayList<String[]> rows,String empty,int cap,boolean urgent){
  LinearLayout section=column();section.setPadding(dp(10),dp(6),dp(10),dp(6));
  section.setBackground(bg(urgent?LAVENDER:BG,15));
  LinearLayout headingRow=row();
  String key="section_"+(urgent?"urgent":heading.contains("Chores")?"chores":heading.contains("To-dos")?"todos":"events");
  boolean expanded=getPreferences(0).getBoolean(key,urgent);
  TextView label=txt(heading+" ("+rows.size()+")",14,INK,true);
  headingRow.addView(label,new LinearLayout.LayoutParams(0,-2,1));
  TextView arrow=txt(expanded?"⌃":"⌄",18,PURPLE,true);headingRow.addView(arrow);
  section.addView(headingRow);
  LinearLayout body=column();section.addView(body);
  Runnable fill=()->{
   body.removeAllViews();
   if(rows.isEmpty())body.addView(txt(empty,12,MUTED,false));
   for(int i=0;i<Math.min(cap,rows.size());i++){
    String[] item=rows.get(i);LinearLayout line=row();
    if("task".equals(item[1])||"chore".equals(item[1])){
     CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(PURPLE));
     check.setOnCheckedChangeListener((v,done)->{if(done)write(()->store.setDone(Long.parseLong(item[0]),true));});
     line.addView(check);
    }
    TextView name=txt(item[2]+" · "+item[4],13,INK,false);
    name.setMaxLines(2);line.addView(name,new LinearLayout.LayoutParams(0,-2,1));
    if("event".equals(item[1]))line.setOnClickListener(v->MomentFeatures.event(this,Long.parseLong(item[0]),item[2],item[3]));
    body.addView(line);
   }
   if(rows.size()>cap)body.addView(txt("+"+(rows.size()-cap)+" more",12,MUTED,false));
  };
  if(expanded)fill.run();
  headingRow.setOnClickListener(v->{
   boolean open=body.getChildCount()==0;
   if(open)fill.run();else body.removeAllViews();
   arrow.setText(open?"⌃":"⌄");
   getPreferences(0).edit().putBoolean(key,open).apply();
  });
  parent.addView(section,margins(5,0));
 }
 void openList(String kind){
  LinearLayout pane=column();pane.setPadding(dp(12),dp(8),dp(12),dp(8));
  ScrollView scroll=new ScrollView(this);scroll.addView(pane);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("chore".equals(kind)?"🧹  My chores":"✅  My to-dos")
    .setView(scroll).setNegativeButton("Close",null).setPositiveButton("＋ Add",null).create();
  dialog.setOnShowListener(x->{
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    dialog.dismiss();addDialog(kind);
   });
  });
  dialog.show();
  db.execute(()->{
   ArrayList<String[]> rows=new ArrayList<>();
   try(Cursor c=store.all()){while(c.moveToNext())if(kind.equals(c.getString(2)))
    rows.add(new String[]{c.getString(0),c.getString(1),c.getString(3),c.getString(4)});
   }
   runOnUiThread(()->{
    if(isFinishing()||isDestroyed()||!dialog.isShowing())return;
    pane.removeAllViews();
    pane.addView(txt("Small steps count. Tap to check off, or hold to edit.",13,MUTED,false));
    if(rows.isEmpty())pane.addView(txt("No items yet. Tap + Add to get started.",15,MUTED,false));
    for(String[] item:rows){
     long id=Long.parseLong(item[0]);boolean done="1".equals(item[3]);
     CheckBox check=new CheckBox(this);
     check.setText(item[1]+"\n"+item[2]);check.setTextSize(16);check.setTextColor(INK);check.setChecked(done);
     check.setPadding(dp(5),dp(8),dp(5),dp(8));
     check.setOnCheckedChangeListener((v,b)->{
      dialog.dismiss();write(()->store.setDone(id,b));
     });
     check.setOnLongClickListener(v->{dialog.dismiss();edit(id,item[1],item[2]);return true;});
     pane.addView(check);
    }
   });
  });
 }
 void move(int n){Calendar c=Calendar.getInstance();try{c.setTime(fmt.parse(selectedDay));}catch(Exception e){}c.add(Calendar.DATE,n);selectedDay=fmt.format(c.getTime());calendarMonth=YearMonth.from(LocalDate.parse(selectedDay));show();}
 void pickDate(){Calendar c=Calendar.getInstance();try{c.setTime(fmt.parse(selectedDay));}catch(Exception e){}new DatePickerDialog(this,(v,y,m,d)->{selectedDay=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);calendarMonth=YearMonth.of(y,m+1);show();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
 void addDialog(String kind){EditText input=new EditText(this);input.setSingleLine(false);input.setHint(kind.equals("task")?"What needs doing?":kind.equals("chore")?"What chore needs doing?":kind.equals("birthday")?"Whose birthday?":"What's happening? (include time in title)");new AlertDialog.Builder(this).setTitle("Add "+kind).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String s=input.getText().toString().trim();if(!s.isEmpty()){write(()->store.add(s,kind,selectedDay));}}).show();}
 void edit(long id,String title,String day){LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);EditText name=new EditText(this);name.setText(title);layout.addView(name);EditText date=new EditText(this);date.setText(day);date.setHint("YYYY-MM-DD");layout.addView(date);new AlertDialog.Builder(this).setTitle("Edit or reschedule").setView(layout).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=name.getText().toString().trim(),dt=date.getText().toString().trim();try{fmt.setLenient(false);if(n.isEmpty()||dt.length()!=10||!fmt.format(fmt.parse(dt)).equals(dt))throw new Exception();write(()->store.update(id,n,dt));}catch(Exception ex){Toast.makeText(this,"Use a title and date YYYY-MM-DD",Toast.LENGTH_LONG).show();}}).show();}
 void focus(){String today=fmt.format(new Date());db.execute(()->{long id=-1;String name=null;try(Cursor c=store.todayTasks(today)){if(c.moveToFirst()){id=c.getLong(0);name=c.getString(1);}}final long taskId=id;final String taskName=name;runOnUiThread(()->{if(taskId>=0)new AlertDialog.Builder(this).setTitle("Just this one thing").setMessage(taskName+"\n\nYou can start small. You don't have to finish everything.").setNegativeButton("Later",null).setPositiveButton("Mark done",(d,w)->write(()->store.complete(taskId))).show();else new AlertDialog.Builder(this).setMessage("No unfinished tasks due today. Enjoy the breathing room.").setPositiveButton("OK",null).show();});});}
 void brainDump(){EditText input=new EditText(this);input.setMinLines(5);input.setHint("One thought or task per line. No organizing needed.");new AlertDialog.Builder(this).setTitle("Brain dump").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save to today",(d,w)->{String text=input.getText().toString(),date=selectedDay;write(()->{for(String line:text.split("\n")){if(!line.trim().isEmpty())store.add(line.trim(),"task",date);}});}).show();}
 void birthdays(){db.execute(()->{ArrayList<String> rows=new ArrayList<>();LocalDate now=LocalDate.now();try(Cursor c=store.upcomingBirthdays()){while(c.moveToNext()){String name=c.getString(0),day=c.getString(1);try{LocalDate birth=LocalDate.parse(day);LocalDate next=birthdayThisYear(birth,now.getYear());if(next.isBefore(now))next=birthdayThisYear(birth,now.getYear()+1);long days=ChronoUnit.DAYS.between(now,next);rows.add(String.format(Locale.US,"%05d|%s — in %d days",days,name,days));}catch(Exception ignored){}}}Collections.sort(rows);StringBuilder out=new StringBuilder();for(int i=0;i<Math.min(8,rows.size());i++)out.append(rows.get(i).split("\\|",2)[1]).append("\n");runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Upcoming birthdays").setMessage(out.length()==0?"No birthdays saved yet.":out.toString()).setPositiveButton("OK",null).show());});}
 static LocalDate birthdayThisYear(LocalDate birth,int year){if(birth.getMonthValue()==2&&birth.getDayOfMonth()==29&&!Year.isLeap(year))return LocalDate.of(year,2,28);return LocalDate.of(year,birth.getMonthValue(),birth.getDayOfMonth());}
 void sharedSpace(){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(18,8,18,8);panel.addView(label("Requests stay private until you accept them. This starter works on one device only; remote partner invitations are not yet available.",14));panel.addView(button("+ Add a partner request (on this phone)",()->requestDialog()));int count=0;try(Cursor c=store.pendingRequests()){while(c.moveToNext()){count++;long id=c.getLong(0);String sender=c.getString(1),title=c.getString(2),day=c.getString(3);panel.addView(label(sender+" asks: "+title+"\nSuggested date: "+day,16));LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);actions.addView(button("Accept",()->{write(()->store.resolveRequest(id,true));}),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("Decline",()->{write(()->store.resolveRequest(id,false));}),new LinearLayout.LayoutParams(0,-2,1));panel.addView(actions);}}if(count==0)panel.addView(label("No pending requests.",15));ScrollView sc=new ScrollView(this);sc.addView(panel);new AlertDialog.Builder(this).setTitle("Shared Space").setView(sc).setPositiveButton("Close",null).show();}
 void requestDialog(){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);EditText sender=new EditText(this);sender.setHint("Partner's name");panel.addView(sender);EditText title=new EditText(this);title.setHint("What are they requesting?");panel.addView(title);EditText day=new EditText(this);day.setText(selectedDay);day.setHint("YYYY-MM-DD");panel.addView(day);new AlertDialog.Builder(this).setTitle("New request").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String name=sender.getText().toString().trim(),task=title.getText().toString().trim(),date=day.getText().toString().trim();try{fmt.setLenient(false);if(name.isEmpty()||task.isEmpty()||date.length()!=10||!fmt.format(fmt.parse(date)).equals(date))throw new Exception();write(()->store.addRequest(name,task,date));Toast.makeText(this,"Request saved — awaiting your acceptance",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Enter a name, task and valid YYYY-MM-DD date",Toast.LENGTH_LONG).show();}}).show();}
 @Override public void onDestroy(){db.shutdown();super.onDestroy();}
 @Override public void onResume(){super.onResume();db.execute(()->MomentWidget.refresh(getApplicationContext()));String eventDay=getIntent().getStringExtra("event_day");if(eventDay!=null){try{selectedDay=LocalDate.parse(eventDay).toString();calendarMonth=YearMonth.from(LocalDate.parse(eventDay));show();}catch(Exception ignored){}getIntent().removeExtra("event_day");}db.execute(()->{EventReminders.reschedule(getApplicationContext());LockCapture.publish(getApplicationContext());LockGlance.refresh(getApplicationContext());DailyReminder.schedule(getApplicationContext());});}
}
