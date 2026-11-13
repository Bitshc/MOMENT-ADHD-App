package com.moment.app;

import java.time.*;

final class CalendarItem {
 final long id;
 final String title,kind,day,time,category,address,nextStep;
 final boolean done;
 final int duration,stepMinutes;
 CalendarItem(long id,String title,String kind,String day,boolean done,String time,int duration,String category,String address,int stepMinutes,String nextStep){
  this.id=id;this.title=title;this.kind=kind;this.day=day;this.done=done;this.time=time;this.duration=duration;this.category=category;this.address=address;this.stepMinutes=stepMinutes;this.nextStep=nextStep;
 }
 boolean timed(){return "event".equals(kind)&&!time.isEmpty();}
 boolean task(){return "task".equals(kind)||"chore".equals(kind);}
 int start(){return LocalTime.parse(time).toSecondOfDay()/60;}
 int end(){return Math.min(1440,start()+duration);}
 boolean on(LocalDate date){try{LocalDate original=LocalDate.parse(day);return "birthday".equals(kind)?original.withYear(date.getYear()).equals(date):original.equals(date);}catch(DateTimeException ex){return false;}}
}
