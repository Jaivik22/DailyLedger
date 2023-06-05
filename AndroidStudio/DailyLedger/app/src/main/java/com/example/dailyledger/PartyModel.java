package com.example.dailyledger;

import java.util.Date;

public class PartyModel {
    private String partyName,partyID;
    private double PartyTotal;
    private Date Date;


    public PartyModel() {
    }
// Constructors, getters, and setters
    // ...

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public Double getPartyTotal() {
        return PartyTotal;
    }

    public void setAccountTotal(double PartyTotal) {
        this.PartyTotal = PartyTotal;
    }

    public String getPartyID() {
        return partyID;
    }

    public void setPartyID(String partyID) {
        this.partyID = partyID;
    }

    public java.util.Date getDate() {
        return Date;
    }

    public void setDate(java.util.Date date) {
        Date = date;
    }

    public PartyModel(String partyName, double accountTotal, String partyID, Date date ) {
        this.partyName = partyName;
        this.PartyTotal = accountTotal;
        this.partyID = partyID;
        this.Date = date;
    }
}
