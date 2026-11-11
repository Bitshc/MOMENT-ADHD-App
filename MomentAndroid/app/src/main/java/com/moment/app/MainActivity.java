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
 LinearLayout root,items;
 String selectedDay;YearMonth calendarMonth;
 SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
 final int INK=Color.rgb(43,37,66), MUTED=Color.rgb(111,105,130), PURPLE=Color.rgb(112,83,173), LAVENDER=Color.rgb(238,229,251), BG=Color.rgb(250,248,253), WHITE=Color.WHITE, SAGE=Color.rgb(225,239,226), BORDER=Color.rgb(232,227,239);

 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);store=new Store(this);selectedDay=fmt.format(new Date());calendarMonth=YearMonth.from(LocalDate.now());show();if("com.moment.app.QUICK_ADD".equals(getIntent().getAction()))addDialog("task");}
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 GradientDrawable stroke(int color,int radius){GradientDrawable d=bg(color,radius);d.setStroke(dp(1),BORDER);return d;}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(INK);t.setPadding(0,dp(5),0,dp(5));return t;}
 TextView txt(String s,int size,int color,boolean bold){TextView t=label(s,size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 LinearLayout.LayoutParams margins(int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);p.bottomMargin=dp(bottom);return p;}
 Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setPadding(dp(10),dp(12),dp(10),dp(12));b.setBackground(stroke(WHITE,17));b.setOnClickListener(v->r.run());return b;}
 void addTile(LinearLayout parent,String emoji,String title,String subtitle,Runnable action,int marginLeft){LinearLayout card=column();card.setPadding(dp(15),dp(12),dp(12),dp(12));card.setBackground(stroke(WHITE,18));card.setMinimumHeight(dp(121));card.addView(txt(emoji,24,INK,false));card.addView(txt(title,15,INK,true));card.addView(txt(subtitle,12,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(marginLeft);parent.addView(card,p);card.setOnClickListener(v->action.run());}
 String prettyDate(){try{return new SimpleDateFormat("EEE, MMM d",Locale.US).format(fmt.parse(selectedDay));}catch(Exception e){return selectedDay;}}
 void write(Runnable work){db.execute(()->{work.run();EventReminders.reschedule(getApplicationContext());MomentWidget.refresh(getApplicationContext());runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())show();});});}
 void show(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root=column();root.setPadding(dp(20),dp(18),dp(20),dp(32));root.setBackgroundColor(BG);scroll.addView(root);setContentView(scroll);
  LinearLayout header=row();TextView logo=txt("moment.",34,INK,true);header.addView(logo,new LinearLayout.LayoutParams(0,-2,1));TextView leaf=txt("✦",28,PURPLE,true);header.addView(leaf);root.addView(header);root.addView(txt("One thing at a time  🌿",15,MUTED,false));
  LinearLayout quickBar=row();quickBar.setPadding(dp(14),dp(12),dp(14),dp(12));quickBar.setBackground(bg(LAVENDER,18));
  LinearLayout intro=column();intro.addView(txt("Your day, your pace.",19,INK,true));intro.addView(txt("Start with just one small thing.",13,MUTED,false));quickBar.addView(intro,new LinearLayout.LayoutParams(0,-2,1));
  Button quick=button("＋ Add",()->addDialog("task"));quick.setBackground(bg(PURPLE,14));quick.setTextColor(WHITE);quickBar.addView(quick,new LinearLayout.LayoutParams(dp(105),dp(51)));root.addView(quickBar,margins(14,0));
  addOverview();
  Button lockQuick=button(LockCapture.enabled(this)?"🔒  Lock-screen quick add: ON":"🔒  Enable lock-screen quick add",()->{
   boolean next=!LockCapture.enabled(this);
   if(next&&android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},301);
    Toast.makeText(this,"Allow notifications, then tap to enable quick capture.",Toast.LENGTH_LONG).show();
    return;
   }
   LockCapture.toggle(this,next);show();
  });
  root.addView(lockQuick,margins(10,3));
  LinearLayout lists=row();
  addTile(lists,"✅","To-do lists","All your tasks",()->openList("task"),0);
  addTile(lists,"🧹","Chores","Keep home on track",()->openList("chore"),10);
  root.addView(lists,margins(12,15));
  InlineCalendar.attach(this,root);
  LinearLayout heading=row();heading.addView(txt("Tasks for this day",21,INK,true),new LinearLayout.LayoutParams(0,-2,1));TextView small=txt("ONE STEP AT A TIME",10,PURPLE,true);heading.addView(small);root.addView(heading);
  items=column();root.addView(items,margins(9,4));render();
  Button focus=button("🎯  Help me choose one thing",()->focus());focus.setBackground(bg(SAGE,17));root.addView(focus,margins(10,19));
  root.addView(txt("Your tools",21,INK,true));
  LinearLayout first=row();addTile(first,"📍","Travel planning","Add time & address",()->MomentFeatures.addEvent(this),0);addTile(first,"🧩","Break it down","Tap a task above",()->{new AlertDialog.Builder(this).setMessage("Tap a task in your day to break it into small checkable steps with time estimates.").setPositiveButton("Got it",null).show();},10);root.addView(first,margins(8,9));
  LinearLayout second=row();addTile(second,"🧠","Brain dump","Clear your head",()->brainDump(),0);addTile(second,"⏳","Focus together","Your virtual body double",()->MomentFeatures.companion(this),10);root.addView(second);
  Button shared=button("💜  Shared space — requests",()->sharedSpace());shared.setTextSize(14);root.addView(shared,margins(12,13));
  root.addView(txt("Tip: Hold a task to edit, reschedule, or delete it. Add the MOMENT widget from your home screen to see tasks at a glance.",12,MUTED,false));
 }

 void addOverview(){
  LinearLayout card=column();card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(stroke(WHITE,18));
  LinearLayout title=row();title.addView(txt("🔔  Up next",19,INK,true),new LinearLayout.LayoutParams(0,-2,1));
  TextView hint=txt("YOUR REMINDERS",10,PURPLE,true);title.addView(hint);card.addView(title);
  LinearLayout contents=column();contents.addView(txt("Checking what's coming up…",13,MUTED,false));card.addView(contents);
  root.addView(card,margins(13,4));
  String today=LocalDate.now().toString();
  db.execute(()->{
   ArrayList<String> alerts=new ArrayList<>();
   try(Cursor c=store.all()){while(c.moveToNext()){
    String kind=c.getString(2),day=c.getString(3),name=c.getString(1);
    boolean done=c.getInt(4)==1;
    if(("task".equals(kind)||"chore".equals(kind))&&!done&&day.compareTo(today)<=0){
     alerts.add((day.compareTo(today)<0?"Overdue · ":"Today · ")+("chore".equals(kind)?"🧹 ":"✅ ")+name);
    }else if("event".equals(kind)&&day.compareTo(today)>=0&&day.compareTo(LocalDate.now().plusDays(3).toString())<=0){
     alerts.add("📅 "+day+" · "+name);
    }
   }}
   runOnUiThread(()->{
    if(isFinishing()||isDestroyed())return;
    contents.removeAllViews();
    if(alerts.isEmpty())contents.addView(txt("All clear for now. You can add a task or event whenever you're ready.",13,MUTED,false));
    else{
     for(int i=0;i<Math.min(4,alerts.size());i++)contents.addView(txt("•  "+alerts.get(i),14,INK,false));
     if(alerts.size()>4)contents.addView(txt("And "+(alerts.size()-4)+" more in your lists",12,MUTED,false));
    }
   });
  });
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
 void render(){
  final String date=selectedDay;items.removeAllViews();items.addView(txt("Loading your plans…",14,MUTED,false));
  db.execute(()->{ArrayList<String[]> rows=new ArrayList<>();try(Cursor cur=store.all()){while(cur.moveToNext()){String day=cur.getString(3),kind=cur.getString(2);if(kind.equals("birthday")?day.substring(5).equals(date.substring(5)):day.equals(date))rows.add(new String[]{cur.getString(0),cur.getString(1),kind,day,cur.getString(4),kind.equals("event")?store.eventDetails(cur.getLong(0))[0]:""});}}
   runOnUiThread(()->{if(!selectedDay.equals(date))return;items.removeAllViews();for(String[] x:rows){long id=Long.parseLong(x[0]);String title=x[1],kind=x[2],day=x[3];boolean done=x[4].equals("1");LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(9),dp(10),dp(9));row.setBackground(stroke(WHITE,15));LinearLayout.LayoutParams cardParams=margins(0,8);if(kind.equals("task")||kind.equals("chore")){CheckBox check=new CheckBox(this);check.setChecked(done);check.setOnCheckedChangeListener((v,b)->write(()->store.setDone(id,b)));row.addView(check);}TextView text=label((kind.equals("birthday")?"🎂 ":kind.equals("event")?"📅 ":"")+(kind.equals("event")&&!x[5].isEmpty()?x[5]+"  ":"")+title,17);if(done)text.setPaintFlags(text.getPaintFlags()|8);text.setPadding(dp(4),dp(7),dp(4),dp(7));row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.setOnClickListener(v->{if("event".equals(kind))MomentFeatures.event(this,id,title,day);else if("task".equals(kind)||"chore".equals(kind))MomentFeatures.steps(this,id,title);});row.setOnLongClickListener(v->{String[] choices=("task".equals(kind)||"chore".equals(kind))?new String[]{"Break into small steps","Edit / reschedule","Delete"}:new String[]{"Edit / reschedule","Delete"};new AlertDialog.Builder(this).setTitle(title).setItems(choices,(d,which)->{if(("task".equals(kind)||"chore".equals(kind))&&which==0)MomentFeatures.steps(this,id,title);else if(which==(("task".equals(kind)||"chore".equals(kind))?1:0)){if("event".equals(kind))MomentFeatures.editEvent(this,id,title,day);else edit(id,title,day);}else new AlertDialog.Builder(this).setMessage("Delete this item?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d2,which2)->write(()->store.delete(id))).show();}).show();return true;});items.addView(row,cardParams);}if(rows.isEmpty()){LinearLayout empty=column();empty.setGravity(Gravity.CENTER);empty.setPadding(dp(14),dp(22),dp(14),dp(22));empty.setBackground(stroke(WHITE,18));empty.addView(txt("🌱  A little breathing room",17,INK,true));empty.addView(txt("Nothing planned yet. Add one small thing whenever you’re ready.",14,MUTED,false));items.addView(empty);}});});
 }
 void edit(long id,String title,String day){LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);EditText name=new EditText(this);name.setText(title);layout.addView(name);EditText date=new EditText(this);date.setText(day);date.setHint("YYYY-MM-DD");layout.addView(date);new AlertDialog.Builder(this).setTitle("Edit or reschedule").setView(layout).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=name.getText().toString().trim(),dt=date.getText().toString().trim();try{fmt.setLenient(false);if(n.isEmpty()||dt.length()!=10||!fmt.format(fmt.parse(dt)).equals(dt))throw new Exception();write(()->store.update(id,n,dt));}catch(Exception ex){Toast.makeText(this,"Use a title and date YYYY-MM-DD",Toast.LENGTH_LONG).show();}}).show();}
 void focus(){String today=fmt.format(new Date());db.execute(()->{long id=-1;String name=null;try(Cursor c=store.todayTasks(today)){if(c.moveToFirst()){id=c.getLong(0);name=c.getString(1);}}final long taskId=id;final String taskName=name;runOnUiThread(()->{if(taskId>=0)new AlertDialog.Builder(this).setTitle("Just this one thing").setMessage(taskName+"\n\nYou can start small. You don't have to finish everything.").setNegativeButton("Later",null).setPositiveButton("Mark done",(d,w)->write(()->store.complete(taskId))).show();else new AlertDialog.Builder(this).setMessage("No unfinished tasks due today. Enjoy the breathing room.").setPositiveButton("OK",null).show();});});}
 void brainDump(){EditText input=new EditText(this);input.setMinLines(5);input.setHint("One thought or task per line. No organizing needed.");new AlertDialog.Builder(this).setTitle("Brain dump").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save to today",(d,w)->{String text=input.getText().toString(),date=selectedDay;write(()->{for(String line:text.split("\n")){if(!line.trim().isEmpty())store.add(line.trim(),"task",date);}});}).show();}
 void birthdays(){db.execute(()->{ArrayList<String> rows=new ArrayList<>();LocalDate now=LocalDate.now();try(Cursor c=store.upcomingBirthdays()){while(c.moveToNext()){String name=c.getString(0),day=c.getString(1);try{LocalDate birth=LocalDate.parse(day);LocalDate next=birthdayThisYear(birth,now.getYear());if(next.isBefore(now))next=birthdayThisYear(birth,now.getYear()+1);long days=ChronoUnit.DAYS.between(now,next);rows.add(String.format(Locale.US,"%05d|%s — in %d days",days,name,days));}catch(Exception ignored){}}}Collections.sort(rows);StringBuilder out=new StringBuilder();for(int i=0;i<Math.min(8,rows.size());i++)out.append(rows.get(i).split("\\|",2)[1]).append("\n");runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Upcoming birthdays").setMessage(out.length()==0?"No birthdays saved yet.":out.toString()).setPositiveButton("OK",null).show());});}
 static LocalDate birthdayThisYear(LocalDate birth,int year){if(birth.getMonthValue()==2&&birth.getDayOfMonth()==29&&!Year.isLeap(year))return LocalDate.of(year,2,28);return LocalDate.of(year,birth.getMonthValue(),birth.getDayOfMonth());}
 void sharedSpace(){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(18,8,18,8);panel.addView(label("Requests stay private until you accept them. This starter works on one device only; remote partner invitations are not yet available.",14));panel.addView(button("+ Add a partner request (on this phone)",()->requestDialog()));int count=0;try(Cursor c=store.pendingRequests()){while(c.moveToNext()){count++;long id=c.getLong(0);String sender=c.getString(1),title=c.getString(2),day=c.getString(3);panel.addView(label(sender+" asks: "+title+"\nSuggested date: "+day,16));LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);actions.addView(button("Accept",()->{write(()->store.resolveRequest(id,true));}),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("Decline",()->{write(()->store.resolveRequest(id,false));}),new LinearLayout.LayoutParams(0,-2,1));panel.addView(actions);}}if(count==0)panel.addView(label("No pending requests.",15));ScrollView sc=new ScrollView(this);sc.addView(panel);new AlertDialog.Builder(this).setTitle("Shared Space").setView(sc).setPositiveButton("Close",null).show();}
 void requestDialog(){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);EditText sender=new EditText(this);sender.setHint("Partner's name");panel.addView(sender);EditText title=new EditText(this);title.setHint("What are they requesting?");panel.addView(title);EditText day=new EditText(this);day.setText(selectedDay);day.setHint("YYYY-MM-DD");panel.addView(day);new AlertDialog.Builder(this).setTitle("New request").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String name=sender.getText().toString().trim(),task=title.getText().toString().trim(),date=day.getText().toString().trim();try{fmt.setLenient(false);if(name.isEmpty()||task.isEmpty()||date.length()!=10||!fmt.format(fmt.parse(date)).equals(date))throw new Exception();write(()->store.addRequest(name,task,date));Toast.makeText(this,"Request saved — awaiting your acceptance",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Enter a name, task and valid YYYY-MM-DD date",Toast.LENGTH_LONG).show();}}).show();}
 @Override public void onDestroy(){db.shutdown();super.onDestroy();}
 @Override public void onResume(){super.onResume();String eventDay=getIntent().getStringExtra("event_day");if(eventDay!=null){try{selectedDay=LocalDate.parse(eventDay).toString();calendarMonth=YearMonth.from(LocalDate.parse(eventDay));show();}catch(Exception ignored){}getIntent().removeExtra("event_day");}db.execute(()->{EventReminders.reschedule(getApplicationContext());LockCapture.publish(getApplicationContext());});}
}
