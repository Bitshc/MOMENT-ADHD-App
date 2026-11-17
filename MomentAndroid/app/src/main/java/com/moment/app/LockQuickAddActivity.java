package com.moment.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.speech.RecognizerIntent;
import java.util.ArrayList;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Limited capture-only overlay; never reveals saved tasks or calendar while locked. */
public final class LockQuickAddActivity extends Activity {
 EditText title, date, time;
 String kind;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  setShowWhenLocked(true);
  setTurnScreenOn(true);
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
  kind=getIntent().getStringExtra("kind");
  if(!"event".equals(kind)&&!"chore".equals(kind))kind="task";
  getWindow().setStatusBarColor(Color.rgb(250,248,253));
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);int pad=dp(22);
  panel.setPadding(pad,dp(25),pad,pad);panel.setBackgroundColor(Color.rgb(250,248,253));
  TextView heading=new TextView(this);heading.setText("moment.  ✦");heading.setTextColor(Color.rgb(43,37,66));heading.setTextSize(30);panel.addView(heading);
  TextView sub=new TextView(this);sub.setText("Capture it now. Organize it later.\\nNothing private from your lists is shown here.");
  sub.setTextColor(Color.rgb(111,105,130));sub.setTextSize(15);sub.setPadding(0,dp(8),0,dp(18));panel.addView(sub);
  title=new EditText(this);title.setSingleLine(false);title.setMinLines(2);
  title.setHint("event".equals(kind)?"What's happening?":"chore".equals(kind)?"What chore?":"What's on your mind?");
  panel.addView(title,new LinearLayout.LayoutParams(-1,-2));
  Button speak=new Button(this);speak.setAllCaps(false);speak.setText("🎤 Speak instead of typing");
  panel.addView(speak);speak.setOnClickListener(v->startVoice());
  date=new EditText(this);date.setSingleLine(true);date.setHint("Date YYYY-MM-DD");date.setText(LocalDate.now().toString());panel.addView(date);
  if("event".equals(kind)){time=new EditText(this);time.setHint("Time HH:mm (optional)");time.setSingleLine(true);panel.addView(time);}
  Button save=new Button(this);save.setText("Save "+("event".equals(kind)?"event":"chore".equals(kind)?"chore":"to-do"));
  save.setAllCaps(false);panel.addView(save);
  Button close=new Button(this);close.setText("Cancel");close.setAllCaps(false);panel.addView(close);
  close.setOnClickListener(v->finish());
  save.setOnClickListener(v->save());
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);setContentView(scroll);
  title.requestFocus();
 }
 void startVoice(){
  try{
   Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say a short "+kind+" title");
   i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   startActivityForResult(i,410);
  }catch(android.content.ActivityNotFoundException ex){
   Toast.makeText(this,"Voice recognition isn't available on this phone. You can still type.",Toast.LENGTH_LONG).show();
  }catch(SecurityException ex){
   Toast.makeText(this,"Voice capture isn't available while locked. Unlock to use speech.",Toast.LENGTH_LONG).show();
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==410&&result==RESULT_OK&&data!=null){
   ArrayList<String> results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
   if(results!=null&&!results.isEmpty())title.setText(results.get(0));
  }
 }
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 void save(){
  final String text=title.getText().toString().trim();
  final String day=date.getText().toString().trim();
  final String clock=time==null?"":time.getText().toString().trim();
  if(text.isEmpty()){title.setError("Add a short title");return;}
  try{LocalDate.parse(day);if(!clock.isEmpty())LocalTime.parse(clock,DateTimeFormatter.ofPattern("HH:mm",Locale.US));}
  catch(Exception e){date.setError("Use YYYY-MM-DD; event time HH:mm");return;}
  new Thread(()->{
   boolean success=false;
   try(Store store=new Store(getApplicationContext())){
    if("event".equals(kind))store.addEvent(text,day,clock,"",0,false);
    else store.add(text,kind,day);
    MomentWidget.refresh(getApplicationContext());EventReminders.reschedule(getApplicationContext());LockGlance.refresh(getApplicationContext());
    success=true;
   }catch(Exception ignored){}
   final boolean done=success;
   runOnUiThread(()->{Toast.makeText(this,done?"Saved to MOMENT":"Couldn't save. Try again.",Toast.LENGTH_SHORT).show();if(done)finish();});
  },"moment-quick-save").start();
 }
}
