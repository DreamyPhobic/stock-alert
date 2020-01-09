package com.dreamyphobic.stockalert.model;

import java.io.Serializable;

public class AssertQuote implements Serializable {
    private String currency;
    private double bid;
    private double ask;
    private double high;
    private double low;
    private double open;
    private double close;
    private long timestamp;

    public AssertQuote(String currency, double bid, double ask, double high, double low, double open, double close, long timestamp) {
        this.currency = currency;
        this.bid = bid;
        this.ask = ask;
        this.high = high;
        this.low = low;
        this.open = open;
        this.close = close;
        this.timestamp = timestamp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getPrice() {
        Double res = (bid+ask)/2;
        return res;
    }
}
