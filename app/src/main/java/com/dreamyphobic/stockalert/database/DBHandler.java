package com.dreamyphobic.stockalert.database;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dreamyphobic.stockalert.model.PriceAlert;

import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "alerts_db";


    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        // create alerts table
        db.execSQL(PriceAlert.CREATE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + PriceAlert.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    public long insertPriceAlert(PriceAlert alert) {
        // get writable database as we want to write data
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(PriceAlert.COLUMN_CURRENCY, alert.getCurrency());
        values.put(PriceAlert.COLUMN_PRICE,alert.getPrice());
        values.put(PriceAlert.COLUMN_OVER,alert.getOver());
        values.put(PriceAlert.COLUMN_BELOW,alert.getBelow());

        // insert row
        long id = db.insert(PriceAlert.TABLE_NAME, null, values);

        // close db connection
        db.close();

        // return newly inserted row id
        return id;
    }

//    public PriceAlert getPriceAlert(long id) {
//        // get readable database as we are not inserting anything
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        Cursor cursor = db.query(PriceAlert.TABLE_NAME,
//                new String[]{PriceAlert.COLUMN_ID, PriceAlert.COLUMN_NOTE, PriceAlert.COLUMN_TIMESTAMP},
//                PriceAlert.COLUMN_ID + "=?",
//                new String[]{String.valueOf(id)}, null, null, null, null);
//
//        if (cursor != null)
//            cursor.moveToFirst();
//
//        // prepare alert object
//        PriceAlert alert = new PriceAlert(
//                cursor.getInt(cursor.getColumnIndex(PriceAlert.COLUMN_ID)),
//                cursor.getString(cursor.getColumnIndex(PriceAlert.COLUMN_NOTE)),
//                cursor.getString(cursor.getColumnIndex(PriceAlert.COLUMN_TIMESTAMP)));
//
//        // close the db connection
//        cursor.close();
//
//        return alert;
//    }

    public List<PriceAlert> getAllPriceAlerts() {
        List<PriceAlert> alerts = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + PriceAlert.TABLE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                PriceAlert alert = new PriceAlert();
                alert.setId(cursor.getInt(cursor.getColumnIndex(PriceAlert.COLUMN_ID)));
                alert.setCurrency(cursor.getString(cursor.getColumnIndex(PriceAlert.COLUMN_CURRENCY)));
                alert.setPrice(cursor.getDouble(cursor.getColumnIndex(PriceAlert.COLUMN_PRICE)));
                alert.setOver(cursor.getDouble(cursor.getColumnIndex(PriceAlert.COLUMN_OVER)));
                alert.setBelow(cursor.getDouble(cursor.getColumnIndex(PriceAlert.COLUMN_BELOW)));

                alerts.add(alert);
            } while (cursor.moveToNext());
        }

        // close db connection
        db.close();

        // return alerts list
        return alerts;
    }

//    public int getPriceAlertsCount() {
//        String countQuery = "SELECT  * FROM " + PriceAlert.TABLE_NAME;
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(countQuery, null);
//
//        int count = cursor.getCount();
//        cursor.close();
//
//
//        // return count
//        return count;
//    }

//    public int updatePriceAlert(PriceAlert alert) {
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(PriceAlert.COLUMN_NOTE, alert.getPriceAlert());
//
//        // updating row
//        return db.update(PriceAlert.TABLE_NAME, values, PriceAlert.COLUMN_ID + " = ?",
//                new String[]{String.valueOf(alert.getId())});
//    }

    public void deletePriceAlert(PriceAlert alert) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PriceAlert.TABLE_NAME, PriceAlert.COLUMN_ID + " = ?",
                new String[]{String.valueOf(alert.getId())});
        db.close();
    }
}