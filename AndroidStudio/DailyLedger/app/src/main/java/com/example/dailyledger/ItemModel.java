package com.example.dailyledger;

public class ItemModel {
    String ItemName;
    Double ItemPrice;
    Double ItemStock;
    String ItemID;

    public String getItemName() {
        return ItemName;
    }

    public void setItemName(String itemName) {
        ItemName = itemName;
    }

    public Double getItemPrice() {
        return ItemPrice;
    }

    public void setItemPrice(Double itemPrice) {
        ItemPrice = itemPrice;
    }

    public Double getItemStock() {
        return ItemStock;
    }

    public void setItemStock(Double itemStock) {
        ItemStock = itemStock;
    }

    public String getItemID() {
        return ItemID;
    }

    public void setItemID(String itemID) {
        ItemID = itemID;
    }

    public ItemModel(String itemName, Double itemPrice, Double itemStock, String itemID) {
        ItemName = itemName;
        ItemPrice = itemPrice;
        ItemStock = itemStock;
        ItemID = itemID;
    }

    public ItemModel() {
    }


}
