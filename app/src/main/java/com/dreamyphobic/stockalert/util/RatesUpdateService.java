package com.dreamyphobic.stockalert.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.dreamyphobic.stockalert.R;
import com.dreamyphobic.stockalert.database.DBHandler;
import com.dreamyphobic.stockalert.model.AssertQuote;
import com.dreamyphobic.stockalert.model.PriceAlert;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class RatesUpdateService extends Service {
    private final IBinder mBinder = new MyBinder();
    public int counter = 0;
    private Socket socket;
    private Map<String, AssertQuote> quotes = new LinkedHashMap<>();
    public Map<String, AssertQuote> getQuotes() {
        return quotes;
    }
    private Map<String,ArrayList<PriceAlert>> stringArrayListMap = new LinkedHashMap<>();
    private DBHandler dbHandler = new DBHandler(this);
    private ArrayList<PriceAlert> alerts;
    private NotificationManager manager;
    private static String NOTIFICATION_CHANNEL_ID = "example.permanence";
    private NotificationCompat.Builder notificationBuilder;
    private NotificationCompat.Builder alertNotifBuilder;
    private RatesUpdateListener listener;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {

        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true);

        alertNotifBuilder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_EVENT)
                .setSmallIcon(R.mipmap.ic_launcher);

        Notification notification = createNotification(notificationBuilder, "Starting connection with server", "Please wait...");

        manager.notify(2, notification);
        startForeground(2, notification);
    }

    public Notification createNotification(NotificationCompat.Builder notificationBuilder, String title, String message) {
        Notification notification = notificationBuilder.setContentTitle(title)
                .setContentText(message)
                .build();
        return notification;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateAlerts();
        runService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        socket.disconnect();
        manager.cancelAll();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        runService();
        return mBinder;
    }

    public void registerListener(RatesUpdateListener listener) {
        this.listener = listener;
    }

    public class MyBinder extends Binder {
        public RatesUpdateService getService() {
            return RatesUpdateService.this;
        }
    }

    public void runService(){
        try {
            socket = IO.socket("https://satin-cadmium.glitch.me/");
            socket.connect();
            socket.emit("join", "newUser");

        } catch (URISyntaxException e) {
            e.printStackTrace();
            Notification notification = createNotification(notificationBuilder, "Connection with server failed", "Please check your internet connection...");
            manager.notify(2, notification);
        }

        socket.on("userJoined", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                try {
                    manager.notify(2, createNotification(notificationBuilder, "Connected with server", "Syncing the data..."));
                    JSONObject whole = new JSONObject((String) args[0]);
                    JSONArray array = whole.getJSONArray("rates");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        String key = item.getString("currency");
//                        Double bid =
                        AssertQuote quote = new AssertQuote(
                                item.getString("currency"),
                                String.valueOf(item.get("bid")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("bid"))),
                                String.valueOf(item.get("ask")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("ask"))),
                                String.valueOf(item.get("high")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("high"))),
                                String.valueOf(item.get("low")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("low"))),
                                String.valueOf(item.get("open")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("open"))),
                                String.valueOf(item.get("close")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("close"))),
                                item.getLong("timestamp")
                        );
                        quotes.put(key, quote);
                    }
                    for(String key:quotes.keySet()){
                        AssertQuote quote = quotes.get(key);
                        ArrayList<PriceAlert> priceAlerts = stringArrayListMap.get(key);
                        if(priceAlerts==null){
                            continue;
                        }
                        for(PriceAlert alert:priceAlerts){
                            Double over = alert.getOver();
                            Double below = alert.getBelow();
                            if(over!=-1&&quote.getPrice()>=over){
                                System.out.println("Alert ....");
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has reached above the "+over+" mark.");
                                manager.notify(3,notification);
                                dbHandler.deletePriceAlert(alert);
                            }
                            if(below!=-1&&quote.getPrice()<=below){
                                System.out.println("Alert ....");
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has fallen below the "+below +" mark.");
                                manager.notify(3,notification);
                                dbHandler.deletePriceAlert(alert);
                            }
                        }
                    }
                    if(listener!=null){
                        listener.initialUpdate(quotes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Notification notification = createNotification(notificationBuilder, "Synchronization with server failed", "Please check your internet connection...");
                    manager.notify(2, notification);
                }
            }
        });

        socket.on("data", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                try {
                    updateAlerts();
                    JSONObject item = new JSONObject(args[0].toString());
                    String key = item.getString("currency");
                    AssertQuote quote = new AssertQuote(
                            item.getString("currency"),
                            String.valueOf(item.get("bid")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("bid"))),
                            String.valueOf(item.get("ask")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("ask"))),
                            String.valueOf(item.get("high")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("high"))),
                            String.valueOf(item.get("low")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("low"))),
                            String.valueOf(item.get("open")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("open"))),
                            String.valueOf(item.get("close")).compareTo("n/a")==0?-1:Double.valueOf(String.valueOf(item.get("close"))),
                            item.getLong("timestamp")
                    );
                    quotes.put(key, quote);
                    ArrayList<PriceAlert> priceAlerts = stringArrayListMap.get(key);
                    if(priceAlerts!=null){

                        for(PriceAlert alert:priceAlerts){
                            Double over = alert.getOver();
                            Double below = alert.getBelow();
                            System.out.println(key+" "+over+" "+below);
                            if(over!=-1&&quote.getPrice()>=over){
                                System.out.println("Alert ....");
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has reached above the "+over+" mark.");
                                manager.notify(3,notification);
                                dbHandler.deletePriceAlert(alert);
                            }
                            if(below!=-1&&quote.getPrice()<=below){
                                System.out.println("Alert ....");
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has fallen below the "+below +" mark.");
                                manager.notify(3,notification);
                                dbHandler.deletePriceAlert(alert);
                            }
                        }
                    }


                    if(listener!=null){
                        listener.itemUpdate(quote);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Notification notification = createNotification(notificationBuilder, "Can't fetch the latest data from server", "Please check your internet connection or try later...");
                    manager.notify(2, notification);
                }
            }
        });

        socket.on("userDisconnect", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                Notification notification = createNotification(notificationBuilder, "Disconnected with the server", "Refresh to reconnect with the server.");
                manager.notify(2, notification);
            }
        });
    }

    public void updateAlerts(){
        alerts = (ArrayList<PriceAlert>) dbHandler.getAllPriceAlerts();
        for(PriceAlert alert:alerts){
            ArrayList<PriceAlert> prices = stringArrayListMap.get(alert.getCurrency());
            if(prices!=null){
                prices.add(alert);
            }
            else{
                prices = new ArrayList<>();
                prices.add(alert);
            }
            stringArrayListMap.put(alert.getCurrency(),prices);
        }
    }

}