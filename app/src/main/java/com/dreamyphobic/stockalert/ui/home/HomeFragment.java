package com.dreamyphobic.stockalert.ui.home;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.dreamyphobic.stockalert.R;
import com.dreamyphobic.stockalert.database.DBHandler;
import com.dreamyphobic.stockalert.model.AssertQuote;
import com.dreamyphobic.stockalert.model.PriceAlert;
import com.dreamyphobic.stockalert.util.RatesUpdateListener;
import com.dreamyphobic.stockalert.util.RatesUpdateService;
import com.github.nkzawa.socketio.client.Socket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class HomeFragment extends Fragment implements ServiceConnection, RatesUpdateListener, LiveRateClickListener,PriceAlertClickListener {

    private HomeViewModel homeViewModel;
    private Socket socket;
    private RatesUpdateService mService;
    private RatesUpdateService s;
    private RecyclerView liveRateRV;
    private LiveRateAdapter liveRateAdapter;
    private boolean isActivated = false;
    private ArrayList<AssertQuote> quotes = new ArrayList<>();
    private RecyclerView priceAlertRV;
    private PriceAlertAdapter priceAlertAdapter;
    private ArrayList<PriceAlert> alerts = new ArrayList<>();
    private DBHandler dbHandler;
    private Map<String,Integer> keyMap = new LinkedHashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
//        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
//                textView.setText(s);
            }
        });

        dbHandler = new DBHandler(getContext());
        liveRateRV = root.findViewById(R.id.liveRateRV);
        liveRateRV.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        liveRateAdapter = new LiveRateAdapter(quotes,this);
        liveRateRV.setAdapter(liveRateAdapter);
        liveRateRV.setNestedScrollingEnabled(false);

        priceAlertRV = root.findViewById(R.id.alertRV);
        priceAlertRV.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL, false));
//        priceAlertRV.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        priceAlertAdapter = new PriceAlertAdapter(alerts,this);
        priceAlertRV.setAdapter(priceAlertAdapter);
        priceAlertRV.setNestedScrollingEnabled(false);
        updateAlerts();



        return root;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {

                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        RatesUpdateService.MyBinder b = (RatesUpdateService.MyBinder) service;
        s = b.getService();
        s.registerListener(this);
        Toast.makeText(getContext(), "Connected", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        s= null;
    }

    @Override
    public void initialUpdate(Map<String, AssertQuote> stringAssertQuoteMap) {
        quotes.clear();
        int i=0;
        for (String key : stringAssertQuoteMap.keySet()) {
            AssertQuote quote = stringAssertQuoteMap.get(key);
            keyMap.put(key,i);
            i++;
            quotes.add(quote);
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                liveRateAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void itemUpdate(AssertQuote assertQuote) {
        try{
            final int position = keyMap.get(assertQuote.getCurrency());
            quotes.set(position,assertQuote);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    liveRateAdapter.notifyItemChanged(position);
                }
            });
        }
        catch(Exception e){
            e.printStackTrace();
        }


    }


    @Override
    public void onPause() {
        super.onPause();
        if(isActivated){
            getActivity().unbindService(this);
            isActivated = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mService = new RatesUpdateService();
        Intent mServiceIntent = new Intent(getContext(), mService.getClass());
        if (isMyServiceRunning(mService.getClass())) {
            getActivity().stopService(mServiceIntent);
        }
        isActivated = true;
        getActivity().bindService(mServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onClick(final AssertQuote assertQuote) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_create_alert);
        dialog.setTitle("Create Alert");

        TextView text = (TextView) dialog.findViewById(R.id.stock_name);
        text.setText(assertQuote.getCurrency());

        TextView price = (TextView) dialog.findViewById(R.id.stock_price);
        price.setText(String.format("%.5f",assertQuote.getPrice()));

        final EditText overEdit = dialog.findViewById(R.id.above_price);
        final EditText belowEdit = dialog.findViewById(R.id.below_price);

        Button dialogBtn_save = (Button) dialog.findViewById(R.id.btn_save);
        dialogBtn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PriceAlert alert = new PriceAlert();
                alert.setCurrency(assertQuote.getCurrency());
                alert.setPrice(assertQuote.getPrice());
                Double over = (double) -1;
                if(!overEdit.getText().toString().isEmpty()){
                    over = Double.valueOf(overEdit.getText().toString());
                }
                Double below = (double) -1;
                if(!belowEdit.getText().toString().isEmpty()){
                    below = Double.valueOf(belowEdit.getText().toString());
                }
                alert.setOver(over);
                alert.setBelow(below);
                dbHandler.insertPriceAlert(alert);
                updateAlerts();
                s.updateAlerts();
//                    Toast.makeText(getApplicationContext(),"Cancel" ,Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void updateAlerts() {
        alerts.clear();
        alerts.addAll(dbHandler.getAllPriceAlerts());
        priceAlertAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(PriceAlert priceAlert) {

    }
}