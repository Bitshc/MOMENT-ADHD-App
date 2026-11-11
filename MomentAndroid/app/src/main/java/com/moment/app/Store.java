package com.moment.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;import java.util.*;
public class Store extends SQLiteOpenHelper {
 public Store(Context c){super(c,"moment.db",null,5);}
 public void onCreate(SQLiteDatabase d){createExtras(d);d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,kind TEXT NOT NULL,day TEXT NOT NULL,done INTEGER NOT NULL DEFAULT 0)");d.execSQL("CREATE TABLE requests(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,title TEXT NOT NULL,day TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'pending')");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){if(a<3)createExtras(d);else {if(a<4)d.execSQL("ALTER TABLE event_details ADD COLUMN remind INTEGER NOT NULL DEFAULT 0");if(a<5){d.execSQL("ALTER TABLE event_details ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 30");d.execSQL("ALTER TABLE event_details ADD COLUMN category TEXT NOT NULL DEFAULT 'Personal'");}}if(a<2)d.execSQL("CREATE TABLE IF NOT EXISTS requests(id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL,title TEXT NOT NULL,day TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'pending')");}

 void createExtras(SQLiteDatabase d){
  d.execSQL("CREATE TABLE IF NOT EXISTS event_details(item_id INTEGER PRIMARY KEY,time TEXT NOT NULL DEFAULT '',address TEXT NOT NULL DEFAULT '',trip_minutes INTEGER NOT NULL DEFAULT 0,remind INTEGER NOT NULL DEFAULT 0,duration_minutes INTEGER NOT NULL DEFAULT 30,category TEXT NOT NULL DEFAULT 'Personal')");
  d.execSQL("CREATE TABLE IF NOT EXISTS steps(id INTEGER PRIMARY KEY AUTOINCREMENT,item_id INTEGER NOT NULL,title TEXT NOT NULL,minutes INTEGER NOT NULL DEFAULT 5,done INTEGER NOT NULL DEFAULT 0)");
 }
 public void addEvent(String title,String day,String time,String address,int minutes){
  addEvent(title,day,time,address,minutes,false);
 }
 public void addEvent(String title,String day,String time,String address,int minutes,boolean reminders){
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
  try{
   ContentValues v=new ContentValues();v.put("title",title);v.put("kind","event");v.put("day",day);
   long id=d.insertOrThrow("items",null,v);
   ContentValues detail=new ContentValues();detail.put("item_id",id);detail.put("time",time);detail.put("address",address);detail.put("trip_minutes",minutes);detail.put("remind",reminders?1:0);
   d.insertOrThrow("event_details",null,detail);d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
 public String[] eventDetails(long id){
  try(Cursor c=getReadableDatabase().rawQuery("SELECT time,address,trip_minutes,duration_minutes,category FROM event_details WHERE item_id=?",new String[]{String.valueOf(id)})){
   if(c.moveToFirst())return new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)};
  }
  return new String[]{"","","0","30","Personal"};
 }
 public Cursor calendarItems(){return getReadableDatabase().rawQuery("SELECT i.id,i.title,i.kind,i.day,i.done,COALESCE(e.time,''),COALESCE(e.duration_minutes,30),COALESCE(e.category,'Personal'),COALESCE(e.address,''),COALESCE((SELECT SUM(s.minutes) FROM steps s WHERE s.item_id=i.id AND s.done=0),0),COALESCE((SELECT s.title FROM steps s WHERE s.item_id=i.id AND s.done=0 ORDER BY s.id LIMIT 1),'') FROM items i LEFT JOIN event_details e ON e.item_id=i.id ORDER BY i.day,e.time,i.id",null);}
 public void saveCalendarEvent(long id,String title,String day,String time,String address,int trip,boolean remind,int duration,String category){
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
   ContentValues item=new ContentValues();item.put("title",title);item.put("day",day);item.put("kind","event");
   long target=id;if(id<0)target=d.insertOrThrow("items",null,item);else if(d.update("items",item,"id=? AND kind='event'",new String[]{String.valueOf(id)})!=1)throw new IllegalStateException("Event no longer exists");
   ContentValues detail=new ContentValues();detail.put("item_id",target);detail.put("time",time);detail.put("address",address);detail.put("trip_minutes",trip);detail.put("remind",remind?1:0);detail.put("duration_minutes",duration);detail.put("category",category);d.insertWithOnConflict("event_details",null,detail,SQLiteDatabase.CONFLICT_REPLACE);d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
 public Cursor reminderEvents(){return getReadableDatabase().rawQuery("SELECT i.id,i.title,i.day,e.time,e.address FROM items i JOIN event_details e ON e.item_id=i.id WHERE i.kind='event' AND e.remind=1",null);}
 public Cursor reminderEvent(long id){return getReadableDatabase().rawQuery("SELECT i.id,i.title,i.day,e.time,e.address FROM items i JOIN event_details e ON e.item_id=i.id WHERE i.id=? AND i.kind='event' AND e.remind=1",new String[]{String.valueOf(id)});}
 public boolean remindersEnabled(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT remind FROM event_details WHERE item_id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()&&c.getInt(0)==1;}}
 public void setReminders(long id,boolean enabled){ContentValues v=new ContentValues();v.put("remind",enabled?1:0);getWritableDatabase().update("event_details",v,"item_id=?",new String[]{String.valueOf(id)});}
 public void updateEvent(long id,String title,String day,String time,String address,int minutes){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{update(id,title,day);ContentValues v=new ContentValues();v.put("item_id",id);v.put("time",time);v.put("address",address);v.put("trip_minutes",minutes);d.insertWithOnConflict("event_details",null,v,SQLiteDatabase.CONFLICT_IGNORE);d.update("event_details",v,"item_id=?",new String[]{String.valueOf(id)});d.setTransactionSuccessful();}finally{d.endTransaction();}}
 public Cursor steps(long itemId){return getReadableDatabase().rawQuery("SELECT id,title,minutes,done FROM steps WHERE item_id=? ORDER BY id",new String[]{String.valueOf(itemId)});}
 public void replaceSteps(long id,ArrayList<String[]> rows){
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
  try{
   d.delete("steps","item_id=?",new String[]{String.valueOf(id)});
   for(String[] row:rows){ContentValues v=new ContentValues();v.put("item_id",id);v.put("title",row[0]);v.put("minutes",Integer.parseInt(row[1]));d.insertOrThrow("steps",null,v);}
   d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
 public void setStepDone(long stepId,boolean done){ContentValues v=new ContentValues();v.put("done",done?1:0);getWritableDatabase().update("steps",v,"id=?",new String[]{String.valueOf(stepId)});}
 public void add(String title,String kind,String day){ContentValues v=new ContentValues();v.put("title",title);v.put("kind",kind);v.put("day",day);getWritableDatabase().insert("items",null,v);}
 public void update(long id,String title,String day){ContentValues v=new ContentValues();v.put("title",title);v.put("day",day);getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});}
 public Cursor todayTasks(String day){return getReadableDatabase().rawQuery("SELECT id,title FROM items WHERE kind='task' AND done=0 AND day<=? ORDER BY day,id LIMIT 3",new String[]{day});}
 public Cursor upcomingBirthdays(){return getReadableDatabase().rawQuery("SELECT title,day FROM items WHERE kind='birthday' ORDER BY title",null);}
 public void complete(long id){ContentValues v=new ContentValues();v.put("done",1);getWritableDatabase().update("items",v,"id=? AND kind=? AND done=0",new String[]{String.valueOf(id),"task"});}
 public void completeChore(long id){ContentValues v=new ContentValues();v.put("done",1);getWritableDatabase().update("items",v,"id=? AND kind='chore' AND done=0",new String[]{String.valueOf(id)});}
 public void setDone(long id,boolean done){ContentValues v=new ContentValues();v.put("done",done?1:0);getWritableDatabase().update("items",v,"id=? AND kind IN ('task','chore')",new String[]{String.valueOf(id)});}
 public void delete(long id){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{String[] args={String.valueOf(id)};d.delete("steps","item_id=?",args);d.delete("event_details","item_id=?",args);d.delete("items","id=?",args);d.setTransactionSuccessful();}finally{d.endTransaction();}}
 public Cursor all(){return getReadableDatabase().rawQuery("SELECT id,title,kind,day,done FROM items ORDER BY day,id",null);}
 public Cursor tasks(){return getReadableDatabase().rawQuery("SELECT id,title FROM items WHERE kind='task' AND done=0 ORDER BY day,id LIMIT 3",null);}
 public void addRequest(String sender,String title,String day){ContentValues v=new ContentValues();v.put("sender",sender);v.put("title",title);v.put("day",day);v.put("status","pending");getWritableDatabase().insert("requests",null,v);}
 public Cursor pendingRequests(){return getReadableDatabase().rawQuery("SELECT id,sender,title,day FROM requests WHERE status='pending' ORDER BY id DESC",null);}
 public void resolveRequest(long id,boolean accept){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{if(accept){try(Cursor c=d.rawQuery("SELECT title,day FROM requests WHERE id=? AND status='pending'",new String[]{String.valueOf(id)})){if(c.moveToFirst()){ContentValues v=new ContentValues();v.put("title",c.getString(0));v.put("day",c.getString(1));v.put("kind","task");d.insertOrThrow("items",null,v);}}}ContentValues v=new ContentValues();v.put("status",accept?"accepted":"declined");d.update("requests",v,"id=? AND status='pending'",new String[]{String.valueOf(id)});d.setTransactionSuccessful();}finally{d.endTransaction();}}
}
