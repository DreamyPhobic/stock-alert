package com.dreamyphobic.stockalert.util;

import android.app.IntentService;
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
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dreamyphobic.stockalert.R;
import com.dreamyphobic.stockalert.database.DBHandler;
import com.dreamyphobic.stockalert.model.AssertQuote;
import com.dreamyphobic.stockalert.model.PriceAlert;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RatesUpdateService extends IntentService {
    public static final String INITIAL_DATA = "initial_data";
    public static final String LIVE_DATA = "live_data";
    public static final String EXTRA_INITIAL_DATA = "extra_initial_data";
    public static final String EXTRA_LIVE_DATA_QUOTE = "extra_live_data_quote";
    public static final String EXTRA_LIVE_DATA_KEY = "extra_live_data_key";
    public static final String REFRESH_ALERTS = "refresh_alerts";
    public int counter = 0;
    private Socket socket;
    public static Map<String, AssertQuote> quotes = new LinkedHashMap<>();


    public RatesUpdateService() {
        super("RatesUpdateService");
    }


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
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true);

        alertNotifBuilder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.mipmap.ic_launcher);

        Notification notification = createNotification(notificationBuilder, "Starting connection with server", "Please wait...",false);

        manager.notify(2, notification);
        startForeground(2, notification);
    }

    public Notification createNotification(NotificationCompat.Builder notificationBuilder, String title, String message,boolean isBigData) {
        if(isBigData){
            Notification notification = notificationBuilder.setContentTitle(title)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .build();
            return notification;
        }
        else{
            Notification notification = notificationBuilder.setContentTitle(title)
                    .setContentText(message)
                    .build();
            return notification;
        }

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
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction("restartservice");
//        broadcastIntent.setClass(this, Restarter.class);
//        this.sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }


//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//
//    }





    public void runService(){
        try {
            socket = IO.socket("https://satin-cadmium.glitch.me/");
            socket.connect();
            socket.emit("join", "newUser");

        } catch (URISyntaxException e) {
            e.printStackTrace();
            Notification notification = createNotification(notificationBuilder, "Connection with server failed", "Please check your internet connection...",false);
            manager.notify(2, notification);
        }

        socket.on("userJoined", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                try {
                    manager.notify(2, createNotification(notificationBuilder, "Connected with server", "Syncing the data...",false));
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
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has reached above the "+over+" mark.",false);
                                manager.notify(alert.getId(),notification);
                                dbHandler.deletePriceAlert(alert);
                                refreshAlertList(RatesUpdateService.this);
                            }
                            if(below!=-1&&quote.getPrice()<=below){
                                System.out.println("Alert ....");
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has fallen below the "+below +" mark.",false);
                                manager.notify(alert.getId(),notification);
                                dbHandler.deletePriceAlert(alert);
                                refreshAlertList(RatesUpdateService.this);
                            }
                        }
                    }
                    sendInitialData(RatesUpdateService.this);
                    updateNotification();
                    if(listener!=null){
                        listener.initialUpdate(quotes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Notification notification = createNotification(notificationBuilder, "Synchronization with server failed", "Please check your internet connection...",false);
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
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has reached above the "+over+" mark.",false);
                                manager.notify(alert.getId(),notification);
                                dbHandler.deletePriceAlert(alert);
                                refreshAlertList(RatesUpdateService.this);

                            }
                            if(below!=-1&&quote.getPrice()<=below){
                                System.out.println("Alert ....");
                                Notification notification = createNotification(alertNotifBuilder,key+" Price Alert","Price has fallen below the "+below +" mark.",false);
                                manager.notify(alert.getId(),notification);
                                dbHandler.deletePriceAlert(alert);
                                refreshAlertList(RatesUpdateService.this);
                            }
                        }
                    }



                    sendLiveData(RatesUpdateService.this,quote);

                    updateNotification();
                    if(listener!=null){
                        listener.itemUpdate(quote);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Notification notification = createNotification(notificationBuilder, "Can't fetch the latest data from server", "Please check your internet connection or try later...",false);
                    manager.notify(2, notification);
                }
            }
        });

        socket.on("userDisconnect", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                Notification notification = createNotification(notificationBuilder, "Disconnected with the server", "Refresh to reconnect with the server.",false);
                manager.notify(2, notification);
            }
        });
    }

    private void updateNotification() {
        String message = "";
        for(String currency:stringArrayListMap.keySet()){
            AssertQuote quote = quotes.get(currency);
            if(quote==null){
                continue;
            }
            message+=(currency + " - " + String.format("%.5f",quote.getPrice())+" \n");
        }
        if(message.equals("")){
            message = "No Alerts set yet.";
        }
        Notification notification = createNotification(notificationBuilder,"Syncing Data with server...",message,true);
        manager.notify(2,notification);
    }

    public void updateAlerts(){
        alerts = (ArrayList<PriceAlert>) dbHandler.getAllPriceAlerts();
        stringArrayListMap = new LinkedHashMap<>();
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

    public static void sendInitialData(Context context) {
        Intent intent = new Intent(INITIAL_DATA);
        intent.putExtra(EXTRA_INITIAL_DATA, (Serializable) quotes);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }
    public static void sendLiveData(Context context,AssertQuote quote) {
        Intent intent = new Intent(LIVE_DATA);
        intent.putExtra(EXTRA_LIVE_DATA_QUOTE,quote);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }

    public static void refreshAlertList(Context context){
        Intent intent = new Intent(REFRESH_ALERTS);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.sendBroadcast(intent);
    }

//    public int generateID(String key){
//        int res = 0;
//        int i =1;
//        for(char c:key.toCharArray()){
//            int a = c;
//            res+=i*a;
//            i++;
//        }
//        res = res%1013;
//        return res;
//    }

}