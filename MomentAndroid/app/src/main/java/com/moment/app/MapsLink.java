package com.moment.app;
import android.content.Intent;
import android.net.Uri;

final class MapsLink {
 static Intent intent(String destination,String origin){
  Uri.Builder url=Uri.parse("https://www.google.com/maps/dir/").buildUpon().appendQueryParameter("api","1").appendQueryParameter("destination",destination).appendQueryParameter("travelmode","driving");
  if(origin!=null&&!origin.trim().isEmpty())url.appendQueryParameter("origin",origin.trim());
  return new Intent(Intent.ACTION_VIEW,url.build());
 }
}
