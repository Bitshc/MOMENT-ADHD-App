package com.moment.app;
import java.time.*;
final class TaskPlan {
 static LocalDate nextDate(LocalDate previous,LocalDate today,String repeat){
  if(!"daily".equals(repeat)&&!"weekly".equals(repeat))return null;
  int days="daily".equals(repeat)?1:7;long passed=Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(previous,today));
  return previous.plusDays((passed/days+1)*days);
 }
 static long due(String day,String time){return ReminderPlan.arrival(day,time,ZoneId.systemDefault());}
 static int snap(int minute,int duration){int max=1440-Math.max(1,Math.min(1440,duration));return Math.max(0,Math.min(max,Math.round(minute/15f)*15));}
 static LocalDate sunday(LocalDate date){return date.minusDays(date.getDayOfWeek().getValue()%7);}
}
