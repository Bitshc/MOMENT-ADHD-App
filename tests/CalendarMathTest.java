package com.moment.app;
import java.time.*;
import java.util.*;
public class CalendarMathTest {
 static CalendarItem event(long id,String time,int minutes){return new CalendarItem(id,"Plan "+id,"event","2026-11-11",false,time,minutes,"Personal","",0,"");}
 static void check(boolean result){if(!result)throw new AssertionError();}
 public static void main(String[] args){
  List<CalendarItem> events=Arrays.asList(event(1,"09:00",60),event(2,"09:30",60),event(3,"10:30",30));
  check(CalendarMath.overlaps(events));check(CalendarMath.occupiedMinutes(events)==120);
  check(CalendarMath.currentOrNext(events,599).id==1);check(CalendarMath.currentOrNext(events,600).id==2);check(CalendarMath.currentOrNext(events,660)==null);
  List<CalendarMath.Block> blocks=CalendarMath.layout(events);check(blocks.get(0).lane!=blocks.get(1).lane);check(blocks.get(0).lanes==2);check(blocks.get(2).lanes==1);
  check(!CalendarMath.overlaps(Arrays.asList(event(1,"09:00",30),event(2,"09:30",30))));
  check(CalendarMath.occupiedMinutes(Arrays.asList(event(1,"09:00",180),event(2,"09:30",30)))==180);
  check(CalendarMath.gapAfter(Arrays.asList(event(1,"09:00",30),event(2,"10:00",30)),event(1,"09:00",30))==30);
  check(CalendarMath.gapAfter(events,event(1,"09:00",60))==0);
  check(CalendarMath.occupiedMinutes(Arrays.asList(event(1,"23:45",30)))==15);
  check(CalendarMath.occupiedMinutes(Collections.emptyList())==0);
  CalendarItem birthday=new CalendarItem(4,"Birthday","birthday","2000-02-29",false,"",30,"Personal","",0,"");
  check(birthday.on(LocalDate.of(2027,2,28)));check(!birthday.on(LocalDate.of(2028,2,28)));check(birthday.on(LocalDate.of(2028,2,29)));
  // Short blocks receive separate visual lanes when needed, without inventing a time conflict.
  List<CalendarItem> shortPlans=Arrays.asList(event(5,"09:00",15),event(6,"09:15",15));check(!CalendarMath.overlaps(shortPlans));check(CalendarMath.layout(shortPlans).get(0).lanes==2);
  System.out.println("Calendar intervals, overlap lanes, workload, gaps and birthday checks passed");
 }
}
