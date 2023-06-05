package com.example.dailyledger;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class stockEntryPage extends AppCompatActivity implements AdapterView.OnItemClickListener {
    String orderStatus;
    int selectedYear, selectedMonth, selectedDay;
    EditText editdate,editQty;
    AutoCompleteTextView itemsSpinner;

    TextView tvItem,tvStockStatus;
    Button submitBtn;
    List<String> itemsList;
    Map<String,String> ItemId;
    Map<String,Double> ItemStock;
    List<String> filteredItems;
    FirebaseFirestore db;
    String userID;
    SharedPreferences sharedPreferences;
    ArrayAdapter<String> spinnerAdapter;
    String selectedItem = "";
    ProgressDialog progressDialog;
    DecimalFormat decimalFormat = new DecimalFormat("#.##");
    String stockFilter,stockItemPrice,stockItemID;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_entry_page);

        editdate  =findViewById(R.id.stockDate);
        editQty = findViewById(R.id.stockquantity);
        tvItem = findViewById(R.id.tvStockItemsSpinner);
        submitBtn= findViewById(R.id.stockSubmitBtn);
        itemsSpinner = findViewById(R.id.stockItemsSpinner);
        tvStockStatus = findViewById(R.id.tvStockStatus);

        Intent i = getIntent();
        orderStatus  = i.getStringExtra("stockStatus");
        stockFilter = i.getStringExtra("stockFilter");
        stockItemPrice = i.getStringExtra("stockItemPrice");
        stockItemID = i.getStringExtra("stockItemID");

        tvStockStatus.setText(orderStatus);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);


        itemsList = new ArrayList<>();
        ItemId = new HashMap<>();
        ItemStock = new HashMap<>();
        filteredItems = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("storedUserId", "userID");

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, itemsList);
        itemsSpinner.setAdapter(spinnerAdapter);
        itemsSpinner.setOnItemClickListener(stockEntryPage.this);

        editdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        updateDateLabel();

        fetchItems();
        setupSearchBar();

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadStockEntry();
            }
        });



    }

    public void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(stockEntryPage.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                selectedYear = year;
                selectedMonth = month;
                selectedDay = dayOfMonth;
                updateDateLabel();
            }
        }, selectedYear, selectedMonth, selectedDay);
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, selectedYear);
        calendar.set(Calendar.MONTH, selectedMonth);
        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
        Date selectedDate = calendar.getTime();
        String formattedDate = sdf.format(selectedDate);
        editdate.setText(formattedDate);
    }
    private void fetchItems() {
        CollectionReference itemsRef = db.collection("ProfileCollection").document(userID).collection("Items");

        itemsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            itemsList.clear();
            ItemId.clear();

            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                String itemName = "";
                Double itemPrice = 0.0;
                Double itemStock = 0.0;
                if (!documentSnapshot.getString("ItemName").isEmpty()) {
                    itemName = documentSnapshot.getString("ItemName");
                }
                if (documentSnapshot.getDouble("ItemPrice") != null) {
                    itemPrice = documentSnapshot.getDouble("ItemPrice");
                }
                if (documentSnapshot.getDouble("ItemStock") != null) {
                    itemStock = documentSnapshot.getDouble("ItemStock");
                }

                if (!itemName.isEmpty() && itemPrice != null) {
                    itemsList.add(itemName);
                    ItemId.put(itemName, documentSnapshot.getId());
                    ItemStock.put(itemName, itemStock);
                }
            }
            spinnerAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to retrieve items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void setupSearchBar() {
        itemsSpinner.setThreshold(1); // Show suggestions after typing 1 character

        itemsSpinner.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterItems(s.toString());

            }
        });

    }

    private void filterItems(String searchText) {
        filteredItems.clear();

        for (String item : itemsList) {
            if (item.toLowerCase().contains(searchText.toLowerCase())) {
                filteredItems.add(item);
            }
        }

        ArrayAdapter<String> filteredAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredItems);
        itemsSpinner.setAdapter(filteredAdapter);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        if (position >= 0 && position < filteredItems.size()) {
            selectedItem = filteredItems.get(position).trim();
        }
        else {
            selectedItem = itemsSpinner.getText().toString().trim();
        }
    }

    public  void uploadStockEntry(){

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.YEAR, selectedYear);
        calendar.set(Calendar.MONTH, selectedMonth);
        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
        if (!itemsList.contains(selectedItem)){
            Toast.makeText(stockEntryPage.this,"Select item from list",Toast.LENGTH_SHORT).show();
        }
        else if (selectedItem.isEmpty() || calendar==null || editQty.getText().toString().isEmpty()){
            Toast.makeText(stockEntryPage.this,"Some fields are empty",Toast.LENGTH_SHORT).show();
        }
        else {
            Double qty = 0.0;
            if (!editQty.getText().toString().isEmpty()){
                qty = Double.parseDouble(editQty.getText().toString().trim());
            }
            progressDialog.show();
            double totalQty = ItemStock.get(selectedItem);
            if (orderStatus.equals("Add")){
                totalQty+=qty;
            }
            else {
                totalQty-=qty;
            }
            Map<String,Object> map = new HashMap<>();
            map.put("Date",new com.google.firebase.Timestamp(calendar.getTime()));
            map.put("Item",selectedItem);
            map.put("Qty",qty);
            map.put("TotalQty",totalQty);
            map.put("OrderStatus",orderStatus);
            CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("StockEntry");
            Double finalQty = qty;
            dataEntriesRef.add(map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    updateStock(selectedItem, finalQty);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();

                }
            });

        }

    }
    public void updateStock(String selectedItem,double qty){
        progressDialog.show();
        double totalQty = ItemStock.get(selectedItem);
        if (orderStatus.equals("Add")){
            totalQty+=qty;
        }
        else {
           totalQty-=qty;
        }

        Map<String,Object> map = new HashMap<>();
        map.put("ItemStock",totalQty);
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Items").document(ItemId.get(selectedItem.toLowerCase()));

        double finalTotalQty = totalQty;
        partiesRef.update(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(stockEntryPage.this,"stock Updated",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    Intent i = new Intent(stockEntryPage.this,stockReport.class);
                    i.putExtra("stockFilter",stockFilter);
                    i.putExtra("stockItemTotal",String.valueOf(decimalFormat.format(finalTotalQty)));
                    i.putExtra("stockItemPrice",stockItemPrice);
                    i.putExtra("stockItemID",stockItemID);
                    startActivity(i);
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(stockEntryPage.this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PriceUpdate",e.getMessage().toString());
                    progressDialog.dismiss();
                });

    }

}