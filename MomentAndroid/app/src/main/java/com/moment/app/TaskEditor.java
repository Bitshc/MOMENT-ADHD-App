package com.moment.app;
import android.app.*;
import android.database.Cursor;
import android.widget.*;
import java.time.*;
import java.util.*;

final class TaskEditor {
 static void create(MainActivity a,String kind){show(a,-1,"",kind,a.selectedDay,new String[]{"","0","none","30","0","0"});}
 static void createReminder(MainActivity a){if(!ReminderControls.permission(a))return;LocalDateTime next=LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0);show(a,-1,"","task",next.toLocalDate().toString(),new String[]{next.toLocalTime().toString(),"1","none","30","0","0"});}
 static void edit(MainActivity a,long id){a.db.execute(()->{try(Cursor rows=a.store.all()){while(rows.moveToNext())if(rows.getLong(0)==id){String name=rows.getString(1),kind=rows.getString(2),date=rows.getString(3);String[] details=a.store.taskDetails(id);a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())show(a,id,name,kind,date,details);});break;}}});}
 static void show(MainActivity a,long id,String initial,String kind,String initialDay,String[] details){
  LinearLayout p=MomentFeatures.vertical(a);EditText title=new EditText(a);title.setHint("chore".equals(kind)?"What chore needs doing?":"What needs doing?");title.setText(initial);p.addView(title);
  LocalDate[] day={LocalDate.parse(initialDay)};LocalTime[] time={details[0].isEmpty()?LocalTime.of(9,0):LocalTime.parse(details[0])};
  Button date=new Button(a);date.setAllCaps(false);date.setText(InlineCalendar.dateLabel(day[0]));p.addView(date);date.setOnClickListener(v->new DatePickerDialog(a,(picker,y,m,d)->{day[0]=LocalDate.of(y,m+1,d);date.setText(InlineCalendar.dateLabel(day[0]));},day[0].getYear(),day[0].getMonthValue()-1,day[0].getDayOfMonth()).show());
  CheckBox timed=new CheckBox(a);timed.setText("Give it a time on my calendar");timed.setChecked(!details[0].isEmpty());p.addView(timed);
  Button clock=new Button(a);clock.setAllCaps(false);clock.setText(EventEditor.label(time[0]));clock.setEnabled(timed.isChecked());clock.setOnClickListener(v->new TimePickerDialog(a,(picker,h,m)->{time[0]=LocalTime.of(h,m);clock.setText(EventEditor.label(time[0]));},time[0].getHour(),time[0].getMinute(),android.text.format.DateFormat.is24HourFormat(a)).show());p.addView(clock);
  p.addView(MomentFeatures.text(a,"About how long?",14,true));Integer[] mins={15,30,45,60,90,120};Spinner duration=new Spinner(a);List<String> labels=new ArrayList<>();for(int n:mins)labels.add(n+" minutes");duration.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels));int selected=Arrays.asList(mins).indexOf(Integer.parseInt(details[3]));duration.setSelection(selected<0?1:selected);p.addView(duration);
  CheckBox remind=new CheckBox(a);remind.setText("Remind me at this time");remind.setChecked("1".equals(details[1]));remind.setEnabled(timed.isChecked());p.addView(remind);
  remind.setOnCheckedChangeListener((v,on)->{if(on&&!ReminderControls.permission(a))remind.setChecked(false);});timed.setOnCheckedChangeListener((v,on)->{clock.setEnabled(on);remind.setEnabled(on);if(!on)remind.setChecked(false);});
  p.addView(MomentFeatures.text(a,"Repeat",14,true));Spinner repeat=new Spinner(a);repeat.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"Does not repeat","Every day","Every week"}));repeat.setSelection("daily".equals(details[2])?1:"weekly".equals(details[2])?2:0);p.addView(repeat);
  p.addView(MomentFeatures.text(a,"Done completes this occurrence. Repeating routines then move to their next day. Snooze delays a reminder by 10 minutes. A reminder saved for a past time will notify once now.",13,false));
  ScrollView sc=new ScrollView(a);sc.addView(p);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("chore".equals(kind)?"Your chore":"Your to-do").setView(sc).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
   String name=title.getText().toString().trim(),dateText=day[0].toString(),clockText=timed.isChecked()?time[0].toString():"",repeats=repeat.getSelectedItemPosition()==1?"daily":repeat.getSelectedItemPosition()==2?"weekly":"none";boolean notify=remind.isChecked();int length=mins[duration.getSelectedItemPosition()];
   if(name.isEmpty()){title.setError("Give this a name");return;}if(!clockText.isEmpty()){TaskPlan.due(dateText,clockText);if(time[0].toSecondOfDay()/60+length>1440)throw new IllegalArgumentException("Shorten this block so it ends before midnight.");}
   if(notify&&!TaskReminders.allowed(a))throw new IllegalArgumentException("Enable To-do and chore reminders in Android notification settings first.");
   a.selectedDay=dateText;a.calendarMonth=YearMonth.from(day[0]);a.write(()->a.store.saveTask(id,name,kind,dateText,clockText,notify,repeats,length));dialog.dismiss();
  }catch(Exception e){Toast.makeText(a,e.getMessage()==null?"Check the date and time.":e.getMessage(),Toast.LENGTH_LONG).show();}}));dialog.show();
 }
}
