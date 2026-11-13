package com.moment.app;

import android.app.*;
import android.widget.*;
import android.database.Cursor;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A short event form with date/time pickers and optional details. */
final class EventEditor {
 static final String[] CATEGORIES={"Personal","Work / study","Health","Rest"};
 static void create(MainActivity a,LocalDate date,LocalTime suggestion){open(a,-1,"",date,new String[]{suggestion.toString(),"","0","30","Personal"},false);}
 static void edit(MainActivity a,long id,String title,String date){a.db.execute(()->{String[] saved=a.store.eventDetails(id);boolean remind=a.store.remindersEnabled(id);a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())open(a,id,title,LocalDate.parse(date),saved,remind);});});}
 static void open(MainActivity a,long id,String title,LocalDate initial,String[] saved,boolean enabled){
  LinearLayout panel=MomentFeatures.vertical(a);EditText name=new EditText(a);name.setHint("What are you planning?");name.setText(title);name.setSingleLine(true);panel.addView(name);
  LocalDate[] date={initial};LocalTime[] time={saved[0].isEmpty()?LocalTime.of(9,0):LocalTime.parse(saved[0])};
  Button dateButton=new Button(a);dateButton.setAllCaps(false);dateButton.setText(date[0].format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy",Locale.US)));panel.addView(dateButton);
  dateButton.setOnClickListener(v->new DatePickerDialog(a,(picker,y,m,d)->{date[0]=LocalDate.of(y,m+1,d);dateButton.setText(date[0].format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy",Locale.US)));},date[0].getYear(),date[0].getMonthValue()-1,date[0].getDayOfMonth()).show());
  CheckBox allDay=new CheckBox(a);allDay.setText("All day / no fixed time");allDay.setChecked(saved[0].isEmpty());panel.addView(allDay);
  Button timeButton=new Button(a);timeButton.setAllCaps(false);timeButton.setText("Starts at "+label(time[0]));timeButton.setEnabled(!allDay.isChecked());panel.addView(timeButton);
  timeButton.setOnClickListener(v->new TimePickerDialog(a,(picker,h,m)->{time[0]=LocalTime.of(h,m);timeButton.setText("Starts at "+label(time[0]));},time[0].getHour(),time[0].getMinute(),android.text.format.DateFormat.is24HourFormat(a)).show());
  panel.addView(MomentFeatures.text(a,"How much time does it need?",14,true));
  TreeSet<Integer> minutes=new TreeSet<>(Arrays.asList(15,30,45,60,90,120,180,240));int old=30;try{old=Integer.parseInt(saved[3]);}catch(Exception ignored){}minutes.add(old);
  List<Integer> durations=new ArrayList<>(minutes);List<String> labels=new ArrayList<>();for(int n:durations)labels.add(n+" minutes");
  Spinner duration=new Spinner(a);duration.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels));duration.setSelection(durations.indexOf(old));duration.setEnabled(!allDay.isChecked());panel.addView(duration);
  panel.addView(MomentFeatures.text(a,"A little color for context",14,true));Spinner category=new Spinner(a);category.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,CATEGORIES));for(int n=0;n<CATEGORIES.length;n++)if(CATEGORIES[n].equals(saved[4]))category.setSelection(n);panel.addView(category);
  CheckBox reminders=ReminderControls.choice(a,panel);reminders.setChecked(enabled&&EventReminders.allowed(a));reminders.setEnabled(!allDay.isChecked());
  allDay.setOnCheckedChangeListener((v,on)->{timeButton.setEnabled(!on);duration.setEnabled(!on);reminders.setEnabled(!on);if(on)reminders.setChecked(false);});
  LinearLayout extras=MomentFeatures.vertical(a);extras.setVisibility(android.view.View.GONE);
  EditText destination=new EditText(a);destination.setHint("Destination address (optional)");destination.setText(saved[1]);extras.addView(destination);
  EditText trip=new EditText(a);trip.setHint("Your estimated travel minutes (optional)");trip.setInputType(2);trip.setText("0".equals(saved[2])?"":saved[2]);extras.addView(trip);
  Button more=new Button(a);more.setAllCaps(false);more.setText("Location & travel estimate");more.setOnClickListener(v->extras.setVisibility(extras.getVisibility()==android.view.View.VISIBLE?android.view.View.GONE:android.view.View.VISIBLE));panel.addView(more);panel.addView(extras);
  panel.addView(MomentFeatures.text(a,"Leave a little room between plans. Estimates can change—and that’s okay.",13,false));
  ScrollView scroll=new ScrollView(a);scroll.addView(panel);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle(id<0?"Make a little plan":"Edit your plan").setView(scroll).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
  dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{
    String titleText=name.getText().toString().trim(),day=date[0].toString(),clock=allDay.isChecked()?"":time[0].toString(),address=destination.getText().toString().trim(),tripText=trip.getText().toString().trim(),cat=CATEGORIES[category.getSelectedItemPosition()];
    int length=durations.get(duration.getSelectedItemPosition()),travel=tripText.isEmpty()?0:Integer.parseInt(tripText);boolean notify=reminders.isChecked();
    if(titleText.isEmpty()){name.setError("Give your plan a name");return;}
    if(travel<0||travel>1440)throw new IllegalArgumentException("Use a travel estimate between 0 and 1440 minutes.");
    if(!clock.isEmpty()){
     long start=ReminderPlan.arrival(day,clock,ZoneId.systemDefault());
     if(time[0].toSecondOfDay()/60+length>1440)throw new IllegalArgumentException("This plan goes past midnight. Shorten it or add a separate plan for the next day.");
     if(notify&&start<=System.currentTimeMillis())throw new IllegalArgumentException("Reminders need a future start time. Turn them off to save a past event.");
    }
    Runnable save=()->{a.selectedDay=day;a.calendarMonth=YearMonth.from(LocalDate.parse(day));a.write(()->a.store.saveCalendarEvent(id,titleText,day,clock,address,travel,notify,length,cat));dialog.dismiss();};
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    a.db.execute(()->{
     boolean overlap=false;int start=clock.isEmpty()?-1:LocalTime.parse(clock).toSecondOfDay()/60;
     try(Cursor c=a.store.calendarItems()){while(c.moveToNext())if(c.getLong(0)!=id&&"event".equals(c.getString(2))&&day.equals(c.getString(3))&&!c.getString(5).isEmpty()&&start>=0){int other=LocalTime.parse(c.getString(5)).toSecondOfDay()/60;if(start<other+c.getInt(6)&&start+length>other)overlap=true;}}
     final boolean conflict=overlap;
     a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed()||!dialog.isShowing())return;if(!conflict)save.run();else new AlertDialog.Builder(a).setTitle("Two plans share this time").setMessage("You can keep both, or adjust one to leave yourself some breathing room.").setNegativeButton("Adjust this plan",(d,w)->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true)).setPositiveButton("Keep both",(d,w)->save.run()).setOnCancelListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true)).show();});
    });
   }catch(Exception e){Toast.makeText(a,e.getMessage()==null?"Check your event details.":e.getMessage(),Toast.LENGTH_LONG).show();}
  }));dialog.show();
 }
 static String label(LocalTime time){return time.format(DateTimeFormatter.ofPattern("h:mm a",Locale.US));}
}
