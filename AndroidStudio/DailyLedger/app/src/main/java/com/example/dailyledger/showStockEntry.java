package com.example.dailyledger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class showStockEntry extends AppCompatActivity {
    StockEntryModel stockEntry;
    TextView tvDate, tvOrderStatus, tvItem;
    EditText tvQuantity;
    Button editbtn,deleteBtn;

    SharedPreferences sharedPreferences;
    String userID;
    FirebaseFirestore db;
    ProgressDialog progressDialog;
    Double itemStock;
    String itemID;
    String stockFilter,stockItemPrice,stockItemID;
    DecimalFormat decimalFormat = new DecimalFormat("#.##");





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_stock_entry);

        tvDate = findViewById(R.id.stocktvDate);
        tvOrderStatus = findViewById(R.id.stocktvOrderStatus);
        tvItem = findViewById(R.id.stocktvItem);
        tvQuantity = findViewById(R.id.stocktvQty);
//        tvTotal = findViewById(R.id.stocktvTotal);
        editbtn = findViewById(R.id.stockeditBtn);
        deleteBtn = findViewById(R.id.stockdeleteBtn);
        db = FirebaseFirestore.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("storedUserId","userID");


        Intent intent = getIntent();
        stockEntry = intent.getParcelableExtra("stockEntry");
        stockFilter = intent.getStringExtra("stockFilter");
        stockItemPrice = intent.getStringExtra("stockItemPrice");
        stockItemID = intent.getStringExtra("stockItemID");

        if (stockEntry.getDate()!=null){
            tvDate.setText(" "+formatDate(stockEntry.getDate()));
        }
        if (stockEntry.getOrderStatus() != null && !stockEntry.getOrderStatus().isEmpty()) {
            tvOrderStatus.setText(" "+stockEntry.getOrderStatus());
        }
        if (!stockEntry.getItem().isEmpty()){
            tvItem.setText(" "+stockEntry.getItem());
        }
        tvQuantity.setText(stockEntry.getQty() != null ? " " + decimalFormat.format(stockEntry.getQty()) : "");
//        tvTotal.setText(stockEntry.getTotalQty() != null ? " " + String.valueOf(decimalFormat.format(stockEntry.getTotalQty())) : "");

        if (stockEntry.getOrderStatus().equals("Sell") || stockEntry.getOrderStatus().equals("Purchase")){
            editbtn.setVisibility(View.GONE);
            deleteBtn.setVisibility(View.GONE);
        }
        fetchItems();
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteStockEntry();
            }
        });





        editbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editStockEntry();
            }
        });
    }

    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }
    public void deleteStockEntry(){
        progressDialog.show();
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("StockEntry").document(stockEntry.getEntryID());
        partiesRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                updateStock();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
            }
        });

    }

    public void updateStock(){
        progressDialog.show();

        if (stockEntry.getOrderStatus().equals("Add") || stockEntry.getOrderStatus().equals("Reduce")){
            Double sqty = 0.0;
            if (itemStock!=null) {
                sqty = itemStock;
            }
            if (stockEntry.getOrderStatus().equals("Add")){
                sqty-=stockEntry.getQty();
            }
            if (stockEntry.getOrderStatus().equals("Reduce")){
                sqty+=stockEntry.getQty();
            }
            Map<String,Object> map = new HashMap<>();
            map.put("ItemStock",sqty);
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("Items").document(itemID);

            Double finalSqty = sqty;
            partiesRef.update(map)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(showStockEntry.this,"stock Updated",Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        Intent i = new Intent(showStockEntry.this,stockReport.class);
                        i.putExtra("stockFilter",stockFilter);
                        i.putExtra("stockItemTotal",String.valueOf(decimalFormat.format(finalSqty)));
                        i.putExtra("stockItemPrice",stockItemPrice);
                        i.putExtra("stockItemID",stockItemID);
                        startActivity(i);
                        finish();

                    }).addOnFailureListener(e -> {
                        Toast.makeText(showStockEntry.this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PriceUpdate",e.getMessage().toString());
                        progressDialog.dismiss();
                    });
        }

    }
    private void fetchItems() {
        CollectionReference itemsRef = db.collection("ProfileCollection").document(userID).collection("Items");
        Query query = itemsRef.whereEqualTo("ItemName",stockEntry.getItem());


        query.get().addOnSuccessListener(queryDocumentSnapshots -> {

            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                itemStock = 0.0;

                if (documentSnapshot.getDouble("ItemStock")!=null){
                    itemStock = documentSnapshot.getDouble("ItemStock");
                    itemID = documentSnapshot.getId();
                }


            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to retrieve items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("fetchItem",e.getMessage().toString());
        });

    }

    public void editStockEntry(){
        progressDialog.show();
        String newQty = tvQuantity.getText().toString().trim();
        Double dqty=0.0;
        if (!newQty.isEmpty() ){
             dqty= Double.parseDouble(newQty);
        }
        if (dqty!=stockEntry.getQty()){
            Map<String,Object> map = new HashMap<>();
            map.put("Qty",dqty);
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("StockEntry").document(stockEntry.getEntryID());
            Double finalDqty = dqty;
            partiesRef.update(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    editStockInItem(finalDqty);
                    progressDialog.dismiss();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(showStockEntry.this,"Failed to change data",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void editStockInItem(Double changeQty) {
        progressDialog.show();
        if (itemStock != null) {
            if (stockEntry.getOrderStatus().equals("Add")) {
                itemStock -= stockEntry.getQty();
                itemStock += changeQty;
            } else if (stockEntry.getOrderStatus().equals("Reduce")) {
                itemStock += stockEntry.getQty();
                itemStock -= changeQty;
            }
        }

        Map<String,Object> map = new HashMap<>();
        map.put("ItemStock",itemStock);
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Items").document(itemID);
        partiesRef.update(map).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Intent i = new Intent(showStockEntry.this,stockReport.class);
                i.putExtra("stockFilter",stockFilter);
                i.putExtra("stockItemTotal",String.valueOf(decimalFormat.format(itemStock)));
                i.putExtra("stockItemPrice",stockItemPrice);
                i.putExtra("stockItemID","stockItemID");
                startActivity(i);
                finish();
                Toast.makeText(showStockEntry.this,"Stock Changed",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(showStockEntry.this,"failed to Change",Toast.LENGTH_SHORT).show();
            }
        });

    }

}