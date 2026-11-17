package com.moment.app;

import java.time.*;

/** Arrival reminders only: no routing provider or traffic estimate. */
final class ReminderPlan {
 static final int[] MINUTES={60,30,10};
 static long arrival(String day,String time,ZoneId zone) {
  if(!time.matches("\\d{2}:\\d{2}"))throw new IllegalArgumentException("Use time HH:mm");
  LocalDateTime local=LocalDateTime.of(LocalDate.parse(day),LocalTime.parse(time));
  if(zone.getRules().getValidOffsets(local).size()!=1)
   throw new IllegalArgumentException("Choose a time outside the daylight-saving clock change");
  return local.atZone(zone).toInstant().toEpochMilli();
 }
 static long trigger(long arrival,int stage){return arrival-MINUTES[stage]*60000L;}
 static int latestDue(long arrival,long now){int stage=-1;for(int i=0;i<MINUTES.length;i++)if(trigger(arrival,i)<=now)stage=i;return now<arrival?stage:-1;}
}
