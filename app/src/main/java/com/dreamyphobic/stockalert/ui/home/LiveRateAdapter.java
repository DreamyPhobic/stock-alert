package com.dreamyphobic.stockalert.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dreamyphobic.stockalert.R;
import com.dreamyphobic.stockalert.model.AssertQuote;

import java.util.ArrayList;

class LiveRateAdapter extends RecyclerView.Adapter<LiveRateAdapter.RateViewHolder> {


    private ArrayList<AssertQuote> quotes;
    private LiveRateClickListener listener;

    public LiveRateAdapter(ArrayList<AssertQuote> quotes, LiveRateClickListener listener) {
        this.quotes = quotes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assert, parent, false);
        return new RateViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RateViewHolder holder, int position) {
        holder.bind(quotes.get(position));
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    public class RateViewHolder extends RecyclerView.ViewHolder {
        TextView price,symbol,dayHi,dayLow;
        View itemView;
        public RateViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            price = itemView.findViewById(R.id.stock_price);
            symbol = itemView.findViewById(R.id.stock_name);
            dayHi = itemView.findViewById(R.id.stock_day_hi);
            dayLow = itemView.findViewById(R.id.stock_day_lo);
        }

        public void bind(final AssertQuote assertQuote) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(assertQuote);
                }
            });
            price.setText(String.format("%.5f", assertQuote.getPrice()));
            symbol.setText(assertQuote.getCurrency());
            if(assertQuote.getHigh()==-1){
                dayHi.setText("N/A");
            }
            else{
                dayHi.setText(String.format("%.5f", assertQuote.getHigh()));
            }

            if(assertQuote.getLow()==-1){
                dayLow.setText("N/A");
            }
            else{
                dayLow.setText(String.format("%.5f", assertQuote.getLow()));
            }
        }
    }
}
