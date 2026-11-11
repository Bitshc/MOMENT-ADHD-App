package com.moment.app;

import android.app.*;
import android.content.*;
import android.os.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.*;

/** Explicit, bounded watch session. No automatic restart or background location collection. */
public class TrafficService extends Service {
 static final String STATUS="traffic_status";
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private DeparturePlan last;
 private String stage="";
 private volatile boolean stopped;
 private boolean started;
 private boolean ended;
 private boolean failed;
 private long arrival, end;
 private String origin,destination,title,endpoint,token;
 private int buffer;
 public android.os.IBinder onBind(Intent i){return null;}
 public int onStartCommand(Intent i,int flags,int startId) {
  if(i==null || "STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  if(started){android.widget.Toast.makeText(this,"A traffic watch is already running. Stop it before starting another trip.",android.widget.Toast.LENGTH_LONG).show();return START_NOT_STICKY;}
  started=true;
  arrival=i.getLongExtra("arrival",0);buffer=i.getIntExtra("buffer",10);
  origin=i.getStringExtra("origin");destination=i.getStringExtra("destination");title=i.getStringExtra("title");
  android.content.SharedPreferences prefs=getSharedPreferences("traffic_private",0);
  endpoint=prefs.getString("endpoint","");token=prefs.getString("token","");
  NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel("traffic_watch","Traffic watch",NotificationManager.IMPORTANCE_LOW));
  nm.createNotificationChannel(new NotificationChannel("traffic_updates","Departure updates",NotificationManager.IMPORTANCE_DEFAULT));
  startForeground(70,notification("traffic_watch","Checking traffic…",true));
  if(origin==null || destination==null || endpoint.isEmpty() || token.isEmpty() || arrival<=System.currentTimeMillis()) {finish("Traffic watch could not start. Open the event and check your settings.");return START_NOT_STICKY;}
  end=Math.min(arrival,System.currentTimeMillis()+2*60*60*1000L);
  main.postDelayed(()->finish("Traffic watch ended. Open the event to start again."),end-System.currentTimeMillis());
  worker.execute(this::check);
  return START_NOT_STICKY;
 }
 private void check() {
  if(stopped)return;
  long delay=300000;
  try {
   if(System.currentTimeMillis()>=end)return;
   long seconds=TrafficClient.duration(endpoint,token,origin,destination);
   DeparturePlan plan=new DeparturePlan(arrival,seconds,buffer);
   long now=System.currentTimeMillis();
   if(stopped)return;
   String nextStage=plan.stage(now);
   String leave=DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(plan.leaveAt));
   String body=nextStage+" · leave by "+leave+" · "+((seconds+59)/60)+" min drive + "+buffer+" min buffer. Checked "+DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(now))+". Estimate from your starting address.";
   if(now+seconds*1000>arrival)body="You may arrive late if leaving now. "+body;
   status(body);
   NotificationManager nm=getSystemService(NotificationManager.class);
   nm.notify(70,notification("traffic_watch",body,true));
   if(plan.changedFrom(last)||!nextStage.equals(stage))nm.notify(71,notification("traffic_updates",body,false));
   last=plan;stage=nextStage;
   failed=false;
   if(plan.leaveAt-now<=30*60000L)delay=60000;
  } catch(Exception e) {
   if(stopped)return;
   String body="Traffic check failed. Previous estimates are not current. Check Google Maps before leaving. Retrying in 5 minutes.";
   status(body);getSystemService(NotificationManager.class).notify(70,notification("traffic_watch",body,true));
   if(!failed)getSystemService(NotificationManager.class).notify(71,notification("traffic_updates",body,false));
   failed=true;
  }
  if(!stopped)try{worker.schedule(this::check,delay,TimeUnit.MILLISECONDS);}catch(RejectedExecutionException ignored){}
 }
 private Notification notification(String channel,String body,boolean ongoing) {
  PendingIntent open=PendingIntent.getActivity(this,70,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,71,new Intent(this,TrafficService.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,channel).setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle(title==null?"MOMENT traffic":title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open).setOngoing(ongoing).setOnlyAlertOnce(ongoing).addAction(new Notification.Action.Builder(null,"Stop watching",stop).build()).build();
 }
 private void status(String body){getSharedPreferences("traffic_private",0).edit().putString(STATUS,body).apply();}
 private void finish(String reason){if(stopped)return;ended=true;status(reason);getSystemService(NotificationManager.class).notify(71,notification("traffic_updates",reason,false));stopSelf();}
 @Override public void onTimeout(int startId,int fgsType){finish("Traffic watch stopped by Android. Check Maps for current traffic.");}
 @Override public void onDestroy(){stopped=true;main.removeCallbacksAndMessages(null);worker.shutdownNow();if(!ended){status("Traffic watch stopped. Start a new watch from your event.");getSystemService(NotificationManager.class).cancel(71);}stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
