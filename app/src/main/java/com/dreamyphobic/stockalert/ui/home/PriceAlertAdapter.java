package com.dreamyphobic.stockalert.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dreamyphobic.stockalert.R;
import com.dreamyphobic.stockalert.model.PriceAlert;

import java.util.ArrayList;

class PriceAlertAdapter extends RecyclerView.Adapter<PriceAlertAdapter.RateViewHolder> {


    private ArrayList<PriceAlert> alerts;
    private PriceAlertClickListener listener;

    public PriceAlertAdapter(ArrayList<PriceAlert> alerts,PriceAlertClickListener listener) {
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new RateViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RateViewHolder holder, int position) {
        holder.bind(alerts.get(position));
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public class RateViewHolder extends RecyclerView.ViewHolder {
        TextView price;
        TextView symbol;
        TextView overPrice;
        TextView belowPrice;
        private View itemView;

        public RateViewHolder(@NonNull View itemView) {
            super(itemView);
            price = itemView.findViewById(R.id.stock_price);
            symbol = itemView.findViewById(R.id.stock_name);
            overPrice = itemView.findViewById(R.id.above_price);
            belowPrice = itemView.findViewById(R.id.below_price);
            this.itemView = itemView;
        }

        public void bind(final PriceAlert priceAlert) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(priceAlert);
                }
            });
            price.setText(String.format("%.5f", priceAlert.getPrice()));
            symbol.setText(priceAlert.getCurrency());
            if(priceAlert.getOver()==-1){
                overPrice.setText("N/A");
            }
            else{
                overPrice.setText(String.format("%.5f", priceAlert.getOver()));
            }

            if(priceAlert.getBelow()==-1){
                belowPrice.setText("N/A");
            }
            else{
                belowPrice.setText(String.format("%.5f", priceAlert.getBelow()));
            }
        }
    }
}
