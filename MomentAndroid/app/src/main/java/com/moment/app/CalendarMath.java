package com.moment.app;

import java.util.*;

/** Pure day planning calculations, independent of device clock and Android views. */
final class CalendarMath {
 static final class Block {
  final CalendarItem item;final int lane,lanes;
  Block(CalendarItem item,int lane,int lanes){this.item=item;this.lane=lane;this.lanes=lanes;}
 }
 static List<CalendarItem> timed(List<CalendarItem> day){List<CalendarItem> out=new ArrayList<>();for(CalendarItem i:day)if(i.timed())try{i.start();out.add(i);}catch(Exception ignored){}out.sort(Comparator.comparingInt(CalendarItem::start).thenComparingLong(i->i.id));return out;}
 static int occupiedMinutes(List<CalendarItem> day){int total=0,start=-1,end=-1;for(CalendarItem i:timed(day)){if(start<0){start=i.start();end=i.end();}else if(i.start()<=end)end=Math.max(end,i.end());else{total+=end-start;start=i.start();end=i.end();}}return start<0?0:total+end-start;}
 static boolean overlaps(List<CalendarItem> day){int end=-1;for(CalendarItem i:timed(day)){if(i.start()<end)return true;end=Math.max(end,i.end());}return false;}
 static CalendarItem currentOrNext(List<CalendarItem> day,int minute){for(CalendarItem i:timed(day))if(i.end()>minute)return i;return null;}
 static List<Block> layout(List<CalendarItem> day){
  List<CalendarItem> items=timed(day);List<Block> out=new ArrayList<>();int offset=0;
  while(offset<items.size()){
   int limit=offset+1,groupEnd=visualEnd(items.get(offset));
   while(limit<items.size()&&items.get(limit).start()<groupEnd){groupEnd=Math.max(groupEnd,visualEnd(items.get(limit)));limit++;}
   List<Integer> ends=new ArrayList<>();List<Integer> lanes=new ArrayList<>();
   for(int p=offset;p<limit;p++){CalendarItem item=items.get(p);int lane=0;while(lane<ends.size()&&ends.get(lane)>item.start())lane++;if(lane==ends.size())ends.add(visualEnd(item));else ends.set(lane,visualEnd(item));lanes.add(lane);}
   for(int p=offset;p<limit;p++)out.add(new Block(items.get(p),lanes.get(p-offset),ends.size()));offset=limit;
  }
  return out;
 }
 private static int visualEnd(CalendarItem i){return Math.min(1440,Math.max(i.end(),i.start()+30));}
 static int gapAfter(List<CalendarItem> day,CalendarItem event){int end=event.end();for(CalendarItem i:timed(day)){if(i.id==event.id)continue;if(i.start()<end&&i.end()>event.start())return 0;if(i.start()>=end)return i.start()-end;}return -1;}
}
