package com.moment.app;
import android.content.*;
public class TaskReminderReceiver extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent){Context app=context.getApplicationContext();PendingResult pending=goAsync();new Thread(()->{try{TaskReminders.receive(app,intent);}finally{pending.finish();}},"moment-task-alarm").start();}
}
