package com.moment.app;

import android.content.*;

/** One-tap completion for an existing chore; never opens the app or reveals records. */
public final class LockChoreReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  if(!LockGlance.DONE.equals(intent.getAction())||!LockCapture.enabled(context))return;
  long id=intent.getLongExtra("id",-1);if(id<0)return;
  Context app=context.getApplicationContext();PendingResult result=goAsync();
  new Thread(()->{try{
   try(Store store=new Store(app)){store.completeOccurrence(id,intent.getStringExtra("day"));}
   MomentWidget.refresh(app);LockGlance.refresh(app);TaskReminders.reschedule(app);
  }finally{result.finish();}},"moment-chore-action").start();
 }
}
