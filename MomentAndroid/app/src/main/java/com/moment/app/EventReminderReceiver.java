package com.moment.app;

import android.content.*;

public class EventReminderReceiver extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent){
  PendingResult pending=goAsync();Context app=context.getApplicationContext();
  new Thread(()->{try{
   if("com.moment.app.EVENT_REMINDER".equals(intent.getAction()))EventReminders.deliver(app,intent.getLongExtra("id",-1),intent.getIntExtra("stage",-1),intent.getLongExtra("arrival",0));
   else {EventReminders.reschedule(app);DailyReminder.schedule(app);LockGlance.refresh(app);}
  }finally{pending.finish();}},"moment-event-reminders").start();
 }
}
