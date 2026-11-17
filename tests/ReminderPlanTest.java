package com.moment.app;
import java.time.*;
public class ReminderPlanTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static void invalid(String day,String time){try{ReminderPlan.arrival(day,time,ZoneId.of("America/New_York"));throw new AssertionError();}catch(RuntimeException expected){}}
 public static void main(String[] args){
  ZoneId zone=ZoneId.of("America/New_York");
  long at=ReminderPlan.arrival("2026-11-12","00:15",zone);
  check(Instant.ofEpochMilli(ReminderPlan.trigger(at,0)).atZone(zone).toLocalDate().equals(LocalDate.of(2026,11,11)));
  check(ReminderPlan.latestDue(at,at-61*60000L)==-1);
  check(ReminderPlan.latestDue(at,at-60*60000L)==0);
  check(ReminderPlan.latestDue(at,at-30*60000L)==1);
  check(ReminderPlan.latestDue(at,at-10*60000L)==2);
  check(ReminderPlan.latestDue(at,at-1)==2);
  check(ReminderPlan.latestDue(at,at)==-1);
  check(ReminderPlan.latestDue(at,at+1000)==-1);
  invalid("2026-02-30","12:00");invalid("2026-11-12","24:00");invalid("2026-11-12","");
  invalid("2026-03-08","02:30");invalid("2026-11-01","01:30");
  long utc=ReminderPlan.arrival("2026-11-12","12:00",ZoneOffset.UTC);
  check(ReminderPlan.arrival("2026-11-12","12:00",zone)-utc==5*3600000L);
  System.out.println("Reminder boundaries, late delivery, midnight and clock-change checks passed");
 }
}
