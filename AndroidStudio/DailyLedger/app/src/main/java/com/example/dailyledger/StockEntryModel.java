package com.example.dailyledger;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;

public class StockEntryModel implements Parcelable {
    private String entryID;
    private Date Date;
    private String OrderStatus;
    private String Item;
    private  Double Qty;
    private Double TotalQty;

    public StockEntryModel(String entryID, java.util.Date date, String orderStatus, String item, Double qty, Double totalQty) {
        this.entryID = entryID;
        Date = date;
        OrderStatus = orderStatus;
        Item = item;
        Qty = qty;
        TotalQty = totalQty;
    }

    public StockEntryModel() {
    }

    public static final Creator<StockEntryModel> CREATOR = new Creator<StockEntryModel>() {
        @Override
        public StockEntryModel createFromParcel(Parcel in) {
            return new StockEntryModel(in);
        }

        @Override
        public StockEntryModel[] newArray(int size) {
            return new StockEntryModel[size];
        }
    };

    public String getEntryID() {
        return entryID;
    }

    public void setEntryID(String entryID) {
        this.entryID = entryID;
    }

    public java.util.Date getDate() {
        return Date;
    }

    public void setDate(java.util.Date date) {
        Date = date;
    }

    public String getOrderStatus() {
        return OrderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        OrderStatus = orderStatus;
    }

    public String getItem() {
        return Item;
    }

    public void setItem(String item) {
        Item = item;
    }

    public Double getQty() {
        return Qty;
    }

    public void setQty(Double qty) {
        Qty = qty;
    }

    public Double getTotalQty() {
        return TotalQty;
    }

    public void setTotalQty(Double totalQty) {
        TotalQty = totalQty;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected StockEntryModel(Parcel in){
        long dateMillis = in.readLong();
        Date = new Date(dateMillis);
        entryID = in.readString();
        OrderStatus = in.readString();
        Item = in.readString();
        if (in.readByte() == 0) {
            Qty = null;
        } else {
            Qty = in.readDouble();
        }
        if (in.readByte() == 0) {
            TotalQty = null;
        } else {
            TotalQty = in.readDouble();
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(Date.getTime());
        parcel.writeString(entryID);
        parcel.writeString(OrderStatus);
        parcel.writeString(Item);
        if (Qty == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeDouble(Qty);
        }
        if (TotalQty == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeDouble(TotalQty);
        }
    }
}
