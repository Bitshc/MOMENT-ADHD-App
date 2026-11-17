package com.moment.app;
import java.time.*;
public class TaskPlanTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(TaskPlan.sunday(LocalDate.of(2026,11,16)).equals(LocalDate.of(2026,11,15)));
  check(TaskPlan.sunday(LocalDate.of(2026,11,15)).equals(LocalDate.of(2026,11,15)));
  check(TaskPlan.sunday(LocalDate.of(2026,8,1)).equals(LocalDate.of(2026,7,26)));
  LocalDate day=LocalDate.of(2026,11,16);
  check(TaskPlan.nextDate(day,day,"daily").equals(day.plusDays(1)));
  check(TaskPlan.nextDate(day,day.plusDays(10),"daily").equals(day.plusDays(11)));
  check(TaskPlan.nextDate(day,day.plusDays(10),"weekly").equals(day.plusDays(14)));
  check(TaskPlan.nextDate(day,day.minusDays(1),"weekly").equals(day.plusDays(7)));
  check(TaskPlan.nextDate(day,day,"none")==null);
  check(TaskPlan.snap(-20,30)==0);check(TaskPlan.snap(68,30)==75);check(TaskPlan.snap(1439,60)==1380);check(TaskPlan.snap(1439,30)==1410);
  System.out.println("Sunday-first weeks, drag snapping and recurring task advancement passed");
 }
}
