package com.moment.app;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

final class TrafficClient {
 static long duration(String endpoint,String token,String origin,String destination) throws Exception {
  URL url=new URL(endpoint);
  if(!"https".equals(url.getProtocol()) || url.getUserInfo()!=null)throw new IOException("HTTPS required");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();
  try {
   c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(15000);
   c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+token);c.setDoOutput(true);
   byte[] body=new JSONObject().put("origin",origin).put("destination",destination).toString().getBytes(StandardCharsets.UTF_8);
   try(OutputStream out=c.getOutputStream()){out.write(body);}
   if(c.getResponseCode()!=200)throw new IOException("Traffic service unavailable");
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   try(InputStream in=c.getInputStream()) {byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){if(bytes.size()+n>8192)throw new IOException("Response too large");bytes.write(b,0,n);}}
   JSONObject result=new JSONObject(bytes.toString("UTF-8"));
   long age=System.currentTimeMillis()/1000-result.getLong("checkedAt");
   if(age>120 || age< -60)throw new IOException("Stale traffic response");
   long seconds=result.getLong("durationSeconds");
   if(seconds<=0 || seconds>86400)throw new IOException("Invalid duration");
   return seconds;
  } finally {c.disconnect();}
 }
}
