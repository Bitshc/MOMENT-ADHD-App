package com.moment.app;
public class DeparturePlanTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  long arrival=1800000000000L;
  DeparturePlan normal=new DeparturePlan(arrival,1800,10);
  check(normal.leaveAt==arrival-40*60000L);
  check(normal.stage(normal.leaveAt).equals("Time to leave"));
  check(normal.stage(normal.leaveAt-10*60000L).equals("Get ready"));
  check(normal.stage(normal.leaveAt-11*60000L).equals("Leave time updated"));
  check(new DeparturePlan(arrival,2100,10).changedFrom(normal));
  check(!new DeparturePlan(arrival,1860,10).changedFrom(normal));
  check(new DeparturePlan(arrival,1500,10).changedFrom(normal));
  check(new DeparturePlan(arrival,3600,0).leaveAt==arrival-3600000);
  for(long bad:new long[]{0,-1,86401}){try{new DeparturePlan(arrival,bad,10);throw new AssertionError();}catch(IllegalArgumentException expected){}}
  System.out.println("Departure calculation checks passed");
 }
}
