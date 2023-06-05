package com.example.dailyledger;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;

public class DataEntryModel implements Parcelable {
    private String entryID;
    private Date Date;
    private String Item;
    private String OrderStatus;
    private String PartyName;
    private Double Price;
    private Double Qty;
    private Double Total;
    private String Description;
    private  String StockEntryID;

    public DataEntryModel(String entryID, java.util.Date date, String item, String orderStatus, String partyName, Double price, Double qty, Double total,String description,String stockEntryID) {
        this.entryID = entryID;
        Date = date;
        Item = item;
        OrderStatus = orderStatus;
        PartyName = partyName;
        Price = price;
        Qty = qty;
        Total = total;
        Description = description;
        StockEntryID = stockEntryID;
    }



    public DataEntryModel() {
    }



    protected DataEntryModel(Parcel in) {
        entryID = in.readString();
        long dateMillis = in.readLong();
        Date = new Date(dateMillis);
        OrderStatus = in.readString();
        PartyName = in.readString();
        Item = in.readString();
        String qtyString = in.readString();
        Qty = qtyString != null && !qtyString.equals("null") ? Double.valueOf(qtyString) : null;
        String priceString = in.readString();
        Price = priceString != null && !priceString.equals("null") ? Double.valueOf(priceString) : null;
        String totalString = in.readString();
        Total = totalString != null && !totalString.equals("null") ? Double.valueOf(totalString) : null;
        Description = in.readString();
        StockEntryID = in.readString();

    }

    public static final Parcelable.Creator<DataEntryModel> CREATOR = new Parcelable.Creator<DataEntryModel>() {
        @Override
        public DataEntryModel createFromParcel(Parcel in) {
            return new DataEntryModel(in);
        }

        @Override
        public DataEntryModel[] newArray(int size) {
            return new DataEntryModel[size];
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

    public String getItem() {
        return Item;
    }

    public void setItem(String item) {
        Item = item;
    }

    public String getOrderStatus() {
        return OrderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        OrderStatus = orderStatus;
    }

    public String getPartyName() {
        return PartyName;
    }

    public void setPartyName(String partyName) {
        PartyName = partyName;
    }

    public Double getPrice() {
        return Price;
    }

    public void setPrice(Double price) {
        Price = price;
    }

    public Double getQty() {
        return Qty;
    }

    public void setQty(Double qty) {
        Qty = qty;
    }

    public Double getTotal() {
        return Total;
    }

    public void setTotal(Double total) {
        Total = total;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getStockEntryID() {
        return StockEntryID;
    }

    public void setStockEntryID(String stockEntryID) {
        StockEntryID = stockEntryID;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(entryID);
        parcel.writeLong(Date.getTime());
        parcel.writeString(OrderStatus);
        parcel.writeString(PartyName);
        parcel.writeString(Item);
        parcel.writeString(String.valueOf(Qty));
        parcel.writeString(String.valueOf(Price));
        parcel.writeString(String.valueOf(Total));
        parcel.writeString(Description);
        parcel.writeString(StockEntryID);

    }
}
