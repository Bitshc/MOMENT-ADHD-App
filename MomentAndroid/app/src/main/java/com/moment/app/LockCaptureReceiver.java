package com.moment.app;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.Toast;
import java.time.LocalDate;

/** Saves direct replies without launching the app or unlocking protected data. */
public class LockCaptureReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  if(!LockCapture.ACTION.equals(intent.getAction())||!LockCapture.enabled(context))return;
  Bundle input=RemoteInput.getResultsFromIntent(intent);
  if(input==null)return;
  CharSequence value=input.getCharSequence(LockCapture.KEY);
  String title=value==null?"":value.toString().trim();
  if(title.isEmpty())return;
  String kind="event".equals(intent.getStringExtra("kind"))?"event":"task";
  PendingResult result=goAsync();
  Context app=context.getApplicationContext();
  new Thread(()->{
   try(Store store=new Store(app)){
    // Quick-captured events are untimed drafts, dated today. Edit them in MOMENT to set actual times.
    store.add(title,kind,LocalDate.now().toString());
    MomentWidget.refresh(app);
    EventReminders.reschedule(app);
    LockCapture.publish(app);
   }finally{result.finish();}
  },"moment-lock-capture").start();
 }
}
