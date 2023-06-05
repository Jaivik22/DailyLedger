    package com.example.dailyledger;

    import android.app.AlertDialog;
    import android.app.DatePickerDialog;
    import android.app.ProgressDialog;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.widget.Button;
    import android.widget.DatePicker;
    import android.widget.EditText;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.google.android.material.button.MaterialButton;
    import com.google.firebase.Timestamp;
    import com.google.firebase.firestore.CollectionReference;
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.Query;
    import com.google.firebase.firestore.QueryDocumentSnapshot;

    import java.text.DecimalFormat;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.Collections;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.Map;

    public class stockReport extends AppCompatActivity {
        FirebaseFirestore db;

        MaterialButton fab, fab2;
        ProgressDialog progressDialog;
        String userID;
        SharedPreferences sharedPreferences;
        List<StockEntryModel> stockEntries;
        RecyclerView recyclerView;
        EditText startDate,endDate;
        Button filterBtn;
        int selectedYear, selectedMonth, selectedDay;
        Date StartD , EndD;
        String stockFilter;
        String  stockItemTotal,stockItemPrice,stockItemID;
        LinearLayout llTitle;
        TextView TitleItemName,TitleItemStock,TitleItemPrice;
        LinearLayout llPriceStock;
        DecimalFormat decimalFormat = new DecimalFormat("#.##");




        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_stock_report);
            fab=findViewById(R.id.sfab);
            fab2 = findViewById(R.id.sfab2);
            recyclerView = findViewById(R.id.stockrv);
            startDate = findViewById(R.id.stockstartDate);
            endDate = findViewById(R.id.stockendDate);
            filterBtn = findViewById(R.id.stocksearchBtn);
            llTitle = findViewById(R.id.llstock);
            TitleItemName = findViewById(R.id.titleItem);
            TitleItemStock = findViewById(R.id.titleStock);
            TitleItemPrice = findViewById(R.id.titlePrice);
            llPriceStock = findViewById(R.id.llPriceStock);

            db = FirebaseFirestore.getInstance();

            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            userID = sharedPreferences.getString("storedUserId","userID");

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);

            Intent i = getIntent();
            stockFilter = "";
            stockFilter = i.getStringExtra("stockFilter");
            stockItemTotal = i.getStringExtra("stockItemTotal");
            stockItemPrice = i.getStringExtra("stockItemPrice");
            stockItemID = i.getStringExtra("stockItemID");

            if (stockFilter != null) {
                if (!stockFilter.isEmpty() && !stockFilter.equals("stockReport")) {
                    TitleItemName.setText(stockFilter);
                    TitleItemStock.setText(stockItemTotal);
                    TitleItemPrice.setText(stockItemPrice);
                } else {
                    llTitle.setVisibility(View.GONE);
                }
            } else {
                Log.e("stockReport", "stockStatus is null");
            }



            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);

            stockEntries = new ArrayList<>();

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(stockReport.this, stockEntryPage.class);
                    i.putExtra("stockStatus","Add");
                    i.putExtra("stockFilter",stockFilter);
                    i.putExtra("stockItemPrice",stockItemPrice);
                    i.putExtra("stockItemID",stockItemID);
                    startActivity(i);
                    finish();
                }
            });
            fab2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(stockReport.this,stockEntryPage.class);
                    i.putExtra("stockStatus","Reduce");
                    i.putExtra("stockFilter",stockFilter);
                    i.putExtra("stockItemPrice",stockItemPrice);
                    i.putExtra("stockItemID",stockItemID);
                    startActivity(i);
                    finish();
                }
            });

            startDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showStartDatePickerDialog();
                }
            });
            endDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showEndDatePickerDialog();
                }
            });

            filterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fetchDataByDate(recyclerView);
                }
            });
            llPriceStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showInputItemDialog(stockFilter,Double.parseDouble(stockItemPrice),Double.parseDouble(stockItemTotal),stockItemID);
                }
            });

            fetchData(recyclerView);


        }

        public void fetchData(RecyclerView recyclerView){
            progressDialog.show();

            Query query = null;

            CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("StockEntry");

            if (stockFilter.equals("stockReport")) {
                query = dataEntriesRef.limit(10).orderBy("Date", Query.Direction.DESCENDING);
            }
            else {
                query = dataEntriesRef.orderBy("Date", Query.Direction.DESCENDING).whereEqualTo("Item",stockFilter).limit(10);
            }
           query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    // Iterate through the query snapshot and convert documents to DataEntry objects
                    int count = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        StockEntryModel stockEntry = document.toObject(StockEntryModel.class);
                        stockEntry.setEntryID(document.getId());
                        // Get the timestamp from the Firestore document
                        Timestamp timestamp = document.getTimestamp("Date");

                        // Convert the timestamp to a Date object
                        Date date = timestamp.toDate();

                        // Add the date to the dataEntry object
                        stockEntry.setDate(date);

                        stockEntries.add(stockEntry);
    //                    Log.d("Firestore", "Data Entry: " + dataEntry.toString());
                    }

                    // Sort the dataEntries list by both date and time
                    Collections.sort(stockEntries, (entry1, entry2) -> {
                        long timestamp1 = entry1.getDate().getTime();
                        long timestamp2 = entry2.getDate().getTime();

                        if (timestamp1 == timestamp2) {
                            // If the timestamps are the same, no need to compare the time portion
                            long time1 = entry1.getDate().getTime();
                            long time2 = entry2.getDate().getTime();
                            return Long.compare(time1, time2);
                        } else if (timestamp1 > timestamp2) {
                            // entry1 should come after entry2
                            return -1;
                        } else {
                            // entry1 should come before entry2
                            return 1;
                        }
                    });
                    progressDialog.dismiss();
                    // Pass the data to the adapter and set it to the RecyclerView
                    StockEntryAdapter adapter = new StockEntryAdapter(stockReport.this,stockEntries,stockFilter,stockItemPrice,stockItemID);
                    recyclerView.setAdapter(adapter);
    //                progressDialog.dismiss();
                } else {
                    // Error fetching data
                    Toast.makeText(getApplicationContext(), "Failed to fetch data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("newError",task.getException().getMessage());
                    progressDialog.dismiss();
                }
            });
        }
        public void fetchDataByDate(RecyclerView recyclerView){
            progressDialog.show();
            // Assuming you have a reference to the Firestore collection
            stockEntries.clear();
            CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("StockEntry");
           Query query;
            if (stockFilter.equals("stockReport")) {
                query = dataEntriesRef.whereGreaterThanOrEqualTo("Date", StartD)
                        .whereLessThanOrEqualTo("Date", EndD).orderBy("Date", Query.Direction.DESCENDING);
            }
            else {
                query = dataEntriesRef.orderBy("Date", Query.Direction.DESCENDING).whereEqualTo("Item",stockFilter).whereGreaterThanOrEqualTo("Date", StartD)
                        .whereLessThanOrEqualTo("Date", EndD);
            }
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    // Iterate through the query snapshot and convert documents to DataEntry objects
                    int count = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        StockEntryModel stockEntry = document.toObject(StockEntryModel.class);
                        stockEntry.setEntryID(document.getId());
                        // Get the timestamp from the Firestore document
                        Timestamp timestamp = document.getTimestamp("Date");

                        // Convert the timestamp to a Date object
                        Date date = timestamp.toDate();

                        // Add the date to the dataEntry object
                        stockEntry.setDate(date);

                        stockEntries.add(stockEntry);

                    }
                    Log.d("count", "Data Entry: " + count);
                    // Sort the dataEntries list by both date and time
                    Collections.sort(stockEntries, (entry1, entry2) -> {
                        long timestamp1 = entry1.getDate().getTime();
                        long timestamp2 = entry2.getDate().getTime();

                        if (timestamp1 == timestamp2) {
                            // If the timestamps are the same, no need to compare the time portion
                            long time1 = entry1.getDate().getTime();
                            long time2 = entry2.getDate().getTime();
                            return Long.compare(time1, time2);
                        } else if (timestamp1 > timestamp2) {
                            // entry1 should come after entry2
                            return -1;
                        } else {
                            // entry1 should come before entry2
                            return 1;
                        }
                    });
                    progressDialog.dismiss();
                    // Pass the data to the adapter and set it to the RecyclerView
                    StockEntryAdapter adapter = new StockEntryAdapter(stockReport.this,stockEntries,stockFilter,stockItemPrice,stockItemID);
                    recyclerView.setAdapter(adapter);
    //                progressDialog.dismiss();

                } else {
                    // Error fetching data
                    Toast.makeText(getApplicationContext(), "Failed to fetch data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("filtering",task.getException().getMessage().toString());
                    progressDialog.dismiss();
                }
            });
        }

        public void showStartDatePickerDialog() {

            Calendar calendar = Calendar.getInstance();
            int initialYear = calendar.get(Calendar.YEAR);
            int initialMonth = calendar.get(Calendar.MONTH);
            int initialDay = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(stockReport.this, new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    updateDateLabel();

                    // Retrieve data within the selected date range
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    Date selectedDate = calendar.getTime();

                    // Set the start and end dates for the query (e.g., within the same day)
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.setTime(selectedDate);
                    startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    startCalendar.set(Calendar.MINUTE, 0);
                    startCalendar.set(Calendar.SECOND, 0);
                    StartD = startCalendar.getTime();
                }
            }, initialYear,initialMonth,initialDay);
            datePickerDialog.show();
        }
        public void showEndDatePickerDialog() {
            Calendar calendar = Calendar.getInstance();
            int initialYear = calendar.get(Calendar.YEAR);
            int initialMonth = calendar.get(Calendar.MONTH);
            int initialDay = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(stockReport.this, new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    updateEndDateLabel();

                    // Retrieve data within the selected date range
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    Date selectedDate = calendar.getTime();

                    // Set the start and end dates for the query (e.g., within the same day)
                    Calendar endCalendar = Calendar.getInstance();
                    endCalendar.setTime(selectedDate);
                    endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                    endCalendar.set(Calendar.MINUTE, 59);
                    endCalendar.set(Calendar.SECOND, 59);
                    EndD = endCalendar.getTime();
                }
            }, initialYear, initialMonth, initialDay);
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
            startDate.setText(formattedDate);
        }
        private void updateEndDateLabel() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            Date selectedDate = calendar.getTime();
            String formattedDate = sdf.format(selectedDate);
            endDate.setText(formattedDate);
        }

        private void showInputItemDialog(String itemName, Double itemPrice, Double itemStock,String itemID) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit Details");
            builder.setMessage("Item: " + itemName);


            // Inflate the dialog layout
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_item_page, null);
            builder.setView(dialogView);

            final EditText price = dialogView.findViewById(R.id.etItemPrice);
            final EditText stock = dialogView.findViewById(R.id.etItemStock);

            price.setText(String.valueOf(itemPrice));
            stock.setText(String.valueOf(itemStock));

            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressDialog.show();
                    String edprice = "";
                    String edStock = "";
                    if (!price.getText().toString().isEmpty()){
                        edprice = price.getText().toString();
                    }
                    if (!stock.getText().toString().isEmpty()){
                        edStock = stock.getText().toString();
                    }
                    updateItemStock(edprice,edStock,itemID);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.create();

            dialog.show();
        }
        private void updateItemStock(String price, String stock,String itemID) {
            progressDialog.show();
            SharedPreferences sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String userID = sharedPreferences.getString("storedUserId","userID");

            Map<String,Object> map = new HashMap<>();
            if (!price.isEmpty()){
                map.put("ItemPrice",Double.parseDouble(price));
            }
            if (!stock.isEmpty()){
                map.put("ItemStock",Double.parseDouble(stock));
            }
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("Items").document(itemID);

            partiesRef.update(map)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,"stock Updated",Toast.LENGTH_SHORT).show();
                        TitleItemPrice.setText(String.valueOf(decimalFormat.format(Double.parseDouble(price))));
                        TitleItemStock.setText(String.valueOf(decimalFormat.format(Double.parseDouble(stock))));
                        progressDialog.dismiss();

                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PriceUpdate",e.getMessage().toString());
                    });
        }
    }