package com.dreamyphobic.stockalert.model;

public class PriceAlert {
    public static final String TABLE_NAME = "alerts";

    public static final String COLUMN_CURRENCY = "currency";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_OVER = "over";
    public static final String COLUMN_BELOW = "below";
    public static final String COLUMN_ID = "id";
        // Create table SQL query
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_CURRENCY+ " TEXT,"
                    + COLUMN_PRICE + " REAL,"
                    + COLUMN_OVER + " REAL,"
                    + COLUMN_BELOW + " REAL"
                    + ")";

    private String currency;
    private Double price;
    private Double over;
    private Double below;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;

    public PriceAlert(String currency, Double price, Double over, Double below) {
        this.currency = currency;
        this.price = price;
        this.over = over;
        this.below = below;
    }

    public PriceAlert() {

    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getOver() {
        return over;
    }

    public void setOver(Double over) {
        this.over = over;
    }

    public Double getBelow() {
        return below;
    }

    public void setBelow(Double below) {
        this.below = below;
    }
}
