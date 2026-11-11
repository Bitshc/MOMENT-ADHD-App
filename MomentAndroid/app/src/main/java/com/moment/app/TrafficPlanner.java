package com.moment.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.InputType;
import android.widget.*;
import java.net.URI;
import java.time.*;

final class TrafficPlanner {
 static void attach(MainActivity a,LinearLayout panel,long id,String title,String day,String time,String address,EditText origin) {
  panel.addView(MomentFeatures.text(a,"Traffic departure updates",17,true));
  panel.addView(MomentFeatures.text(a,"Watch a driving trip for up to 2 hours before arrival. Updates check every 5 minutes, or about every minute near departure. Android may delay checks. Start each watch yourself; one trip at a time.",13,false));
  EditText buffer=new EditText(a);buffer.setHint("Arrival buffer in minutes");buffer.setInputType(InputType.TYPE_CLASS_NUMBER);buffer.setText("10");panel.addView(buffer);
  TextView state=MomentFeatures.text(a,a.getSharedPreferences("traffic_private",0).getString(TrafficService.STATUS,"Google Routes is not connected yet. Manual estimates and Google Maps are available."),13,false);panel.addView(state);
  android.content.SharedPreferences prefs=a.getSharedPreferences("traffic_private",0);
  android.content.SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(TrafficService.STATUS.equals(key))state.setText(p.getString(key,""));};
  state.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(android.view.View v){prefs.registerOnSharedPreferenceChangeListener(listener);}public void onViewDetachedFromWindow(android.view.View v){prefs.unregisterOnSharedPreferenceChangeListener(listener);}});
  Button start=new Button(a);start.setAllCaps(false);start.setText("Watch traffic for this event");panel.addView(start);
  start.setOnClickListener(v->{
   try {
    String from=origin.getText().toString().trim();
    if(from.isEmpty()||address.isEmpty()||time.isEmpty())throw new IllegalArgumentException("Add a starting address, destination and event time first.");
    int extra=Integer.parseInt(buffer.getText().toString());if(extra<0||extra>180)throw new IllegalArgumentException("Choose a buffer from 0 to 180 minutes.");
    LocalDateTime local=LocalDateTime.of(LocalDate.parse(day),LocalTime.parse(time));
    if(ZoneId.systemDefault().getRules().getValidOffsets(local).size()!=1)throw new IllegalArgumentException("This time falls in a daylight-saving clock change. Choose an unambiguous event time.");
    long at=local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),now=System.currentTimeMillis();
    if(at<=now || at-now>2*60*60*1000L)throw new IllegalArgumentException("Start watching within 2 hours of the event, before its arrival time.");
    if(prefs.getString("endpoint","").isEmpty()||prefs.getString("token","").isEmpty()){settings(a);return;}
    if(!a.getSystemService(NotificationManager.class).areNotificationsEnabled()){
     if(Build.VERSION.SDK_INT>=33 && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},87);Toast.makeText(a,"Allow notifications, then tap Watch traffic again.",Toast.LENGTH_LONG).show();}
     else Toast.makeText(a,"Enable MOMENT notifications in Android settings before starting.",Toast.LENGTH_LONG).show();
     return;
    }
    new AlertDialog.Builder(a).setTitle("Start departure updates?").setMessage("Your starting and destination addresses will be sent to your connected traffic server and Google Routes. This uses a fixed starting address, not your phone’s live location. Stop an existing watch before starting another.").setNegativeButton("Cancel",null).setPositiveButton("Start watch",(d,w)->{
     Intent i=new Intent(a,TrafficService.class).putExtra("arrival",at).putExtra("buffer",extra).putExtra("origin",from).putExtra("destination",address).putExtra("title",title);
     try{a.startForegroundService(i);}catch(RuntimeException e){Toast.makeText(a,"Android could not start traffic updates. Try again while MOMENT is open.",Toast.LENGTH_LONG).show();}
    }).show();
   }catch(Exception e){Toast.makeText(a,e instanceof IllegalArgumentException?e.getMessage():"Check the event time and arrival buffer.",Toast.LENGTH_LONG).show();}
  });
  Button stop=new Button(a);stop.setAllCaps(false);stop.setText("Stop current traffic watch");stop.setOnClickListener(v->a.stopService(new Intent(a,TrafficService.class)));panel.addView(stop);
  Button connect=new Button(a);connect.setAllCaps(false);connect.setText("Traffic connection settings");connect.setOnClickListener(v->settings(a));panel.addView(connect);
 }
 static void settings(MainActivity a) {
  android.content.SharedPreferences prefs=a.getSharedPreferences("traffic_private",0);
  LinearLayout p=MomentFeatures.vertical(a);
  p.addView(MomentFeatures.text(a,"Live traffic is disconnected until you connect your Google Routes server. Google billing and quotas are managed on that server. No Google key belongs in this app.",14,false));
  EditText endpoint=new EditText(a);endpoint.setSingleLine(true);endpoint.setHint("Traffic server HTTPS URL ending /route");endpoint.setText(prefs.getString("endpoint",""));p.addView(endpoint);
  EditText token=new EditText(a);token.setSingleLine(true);token.setHint("Private server access token");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setText(prefs.getString("token",""));p.addView(token);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Connect live traffic").setView(p).setNegativeButton("Later",null).setNeutralButton("Disconnect",(d,w)->{a.stopService(new Intent(a,TrafficService.class));prefs.edit().clear().apply();}).setPositiveButton("Save",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
   String url=endpoint.getText().toString().trim(),secret=token.getText().toString().trim();URI uri=new URI(url);
   if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getQuery()!=null||!uri.getPath().endsWith("/route")||secret.length()<32)throw new Exception();
   a.stopService(new Intent(a,TrafficService.class));prefs.edit().putString("endpoint",url).putString("token",secret).apply();dialog.dismiss();
  }catch(Exception e){Toast.makeText(a,"Use an HTTPS /route URL and an access token of at least 32 characters.",Toast.LENGTH_LONG).show();}}));dialog.show();
 }
}
