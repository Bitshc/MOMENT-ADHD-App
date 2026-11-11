package com.moment.app;

/** Pure calculation: current driving duration is an estimate, not an arrival guarantee. */
final class DeparturePlan {
 final long leaveAt;
 final long seconds;
 DeparturePlan(long arrival, long durationSeconds, int bufferMinutes) {
  if(durationSeconds<=0 || durationSeconds>86400 || bufferMinutes<0 || bufferMinutes>180)
   throw new IllegalArgumentException("Invalid trip estimate");
  seconds=durationSeconds;
  leaveAt=arrival-durationSeconds*1000-bufferMinutes*60000L;
 }
 String stage(long now) {
  if(now>=leaveAt)return "Time to leave";
  if(leaveAt-now<=10*60000L)return "Get ready";
  return "Leave time updated";
 }
 boolean changedFrom(DeparturePlan previous) {
  return previous==null || Math.abs(leaveAt-previous.leaveAt)>=3*60000L;
 }
}
