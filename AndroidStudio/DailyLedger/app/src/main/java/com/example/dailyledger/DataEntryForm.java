        package com.example.dailyledger;

        import android.app.AlertDialog;
        import android.app.DatePickerDialog;
        import android.app.ProgressDialog;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.text.Editable;
        import android.text.TextWatcher;
        import android.util.Log;
        import android.view.LayoutInflater;
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
        import com.google.firebase.Timestamp;
        import com.google.firebase.firestore.CollectionReference;
        import com.google.firebase.firestore.DocumentReference;
        import com.google.firebase.firestore.FirebaseFirestore;
        import com.google.firebase.firestore.QueryDocumentSnapshot;

        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.Date;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Locale;
        import java.util.Map;

        public class DataEntryForm extends AppCompatActivity implements AdapterView.OnItemClickListener {

            EditText editDate, editQuantity, editPrice, editTotal,editDescription;
            TextView tvPartyName, tvOrderStatus,tvItemsSpinner, tvquantity,tvPrice;
            AutoCompleteTextView itemsSpinner;
            Button dataSubmitBtn;
            String userID, partyName, orderStatus,partyID;
            SharedPreferences sharedPreferences;
            FirebaseFirestore db;
            ArrayAdapter<String> spinnerAdapter;
            List<String> itemsList;
            Map<String, Double> priceMap;
            Map<String,String> ItemId;
            Map<String,Double> ItemStock;
            String selectedDate;
            Double  PartyTotal;
            int selectedYear, selectedMonth, selectedDay;
            private ProgressDialog progressDialog;
            List<String> filteredItems;





            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_data_entry_form);

                db = FirebaseFirestore.getInstance();

                editDate = findViewById(R.id.editDate);
                editQuantity = findViewById(R.id.editquantity);
                editPrice = findViewById(R.id.editPrice);
                editTotal = findViewById(R.id.editTotal);
                tvPartyName = findViewById(R.id.tvPartyName);
                tvOrderStatus = findViewById(R.id.tvOrderStatus);
                dataSubmitBtn = findViewById(R.id.dataSubmitBtn);
                itemsSpinner = findViewById(R.id.ItemsSpinner);
                tvquantity = findViewById(R.id.tvquantity);
                tvItemsSpinner = findViewById(R.id.tvItemsSpinner);
                tvPrice = findViewById(R.id.tvPrice);
                editDescription = findViewById(R.id.editDescription);

                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);

                Bundle b = getIntent().getExtras();
                if (b != null) {
                    partyName = b.getString("PartyName", "");
                    orderStatus = b.getString("orderStatus", "");
                    PartyTotal = b.getDouble("PartyTotal", 0.0);
                    partyID = b.getString("PartyID", "");


                }

                sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                userID = sharedPreferences.getString("storedUserId", "userID");

                tvPartyName.setText(partyName);
                tvOrderStatus.setText(orderStatus);

                itemsList = new ArrayList<>();
                priceMap = new HashMap<>();
                ItemId = new HashMap<>();
                ItemStock = new HashMap<>();
                filteredItems = new ArrayList<>();
                spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, itemsList);
                itemsSpinner.setAdapter(spinnerAdapter);
                itemsSpinner.setOnItemClickListener(this);

                // Fetch items and populate the spinner

                if (orderStatus.equals("Sell") || orderStatus.equals("Purchase")) {
                    fetchItems();
                    setupSearchBar();
                }else {
                    itemsSpinner.setVisibility(View.GONE);
                    editQuantity.setVisibility(View.GONE);
                    editPrice.setVisibility(View.GONE);
                    tvPrice.setVisibility(View.GONE);
                    tvItemsSpinner.setVisibility(View.GONE);
                    tvquantity.setVisibility(View.GONE);
                }

                dataSubmitBtn.setOnClickListener(view -> {
                        UploadDataEntry();
                });

                editQuantity.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        String qty = editQuantity.getText().toString();
                        String prc = editPrice.getText().toString();
                        if (!qty.isEmpty() && !prc.isEmpty()) {
                            Double dqty = Double.parseDouble(qty);
                            Double dprc = Double.parseDouble(prc);
                            editTotal.setText(String.valueOf(dprc * dqty));
                        }
                    }
                });

                editPrice.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        String qty = editQuantity.getText().toString();
                        String prc = editPrice.getText().toString();
                        if (!qty.isEmpty() && !prc.isEmpty()) {
                            Double dqty = Double.parseDouble(qty);
                            Double dprc = Double.parseDouble(prc);
                            editTotal.setText(String.valueOf(dprc * dqty));
                        }
                    }
                });

                Calendar calendar = Calendar.getInstance();
                selectedYear = calendar.get(Calendar.YEAR);
                selectedMonth = calendar.get(Calendar.MONTH);
                selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
                updateDateLabel();

                editDate.setOnClickListener(view -> {
                    showDatePickerDialog();
                });
            }

            private void fetchItems() {
                CollectionReference itemsRef = db.collection("ProfileCollection").document(userID).collection("Items");

                itemsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    itemsList.clear();
                    priceMap.clear();
                    ItemId.clear();

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String itemName = "";
                        Double itemPrice = 0.0;
                        Double itemStock = 0.0;
                        if (!documentSnapshot.getString("ItemName").isEmpty()) {
                            itemName = documentSnapshot.getString("ItemName");
                        }
                        if (documentSnapshot.getDouble("ItemPrice")!=null) {
                            itemPrice = documentSnapshot.getDouble("ItemPrice");
                        }
                        if (documentSnapshot.getDouble("ItemStock")!=null){
                            itemStock = documentSnapshot.getDouble("ItemStock");
                        }

                        if (!itemName.isEmpty() && itemPrice != null) {
                            itemsList.add(itemName);
                            priceMap.put(itemName, itemPrice);
                            ItemId.put(itemName,documentSnapshot.getId());
                            ItemStock.put(itemName,itemStock);
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
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = "";
                if (position >= 0 && position < filteredItems.size()) {
                    selectedItem = filteredItems.get(position);
                    Double selectedPrice = priceMap.get(selectedItem);
                    if (selectedPrice != null) {
                        editPrice.setText(String.valueOf(selectedPrice));
                    }
                }
                else {
                    String item = itemsSpinner.getText().toString();
                    Double selectedPrice = priceMap.get(item.toLowerCase());
                    if (selectedPrice != null) {
                        editPrice.setText(String.valueOf(selectedPrice));
                    }
                }
            }

            public void UploadDataEntry(){
                progressDialog.show();
                Double qty = null;
                Double prc = null ;
                Double Total = null;
                String item = itemsSpinner.getText().toString().trim();
                String quantity = editQuantity.getText().toString().trim();
                String price = editPrice.getText().toString().trim();
                String total = editTotal.getText().toString().trim();
                String Description = editDescription.getText().toString().trim();
                if (price != null && !price.isEmpty()) {
                    prc = Double.parseDouble(price);
                }
                if (quantity != null && !quantity.isEmpty()) {
                    qty = Double.parseDouble(quantity);
                }



                if (!item.isEmpty() && !itemsList.contains(item.toLowerCase().trim()) && prc!=null) {
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("New Item");
                    builder.setMessage("The item you entered is not in the list. Do you want to add it as a new item?");
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_add_item2, null);
                    builder.setView(dialogView);
                    final TextView editem = dialogView.findViewById(R.id.addtvItem);
                    final TextView edprice = dialogView.findViewById(R.id.addtvPrice);
                    editem.setText("Item: "+item.toLowerCase());
                    edprice.setText("Price: "+price);
                    Double finalPrc1 = prc;
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Call a method to add the new item to the list and update the priceMap
                            insertItem(item, finalPrc1);
                            fetchItems();
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }


                else if ( prc!=null && priceMap.containsKey(item.toLowerCase().trim()) && !prc.equals(priceMap.get(item.toLowerCase().trim()))){
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Change Price");
                    builder.setMessage("Do you want to change price of this item?");
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_change_item_price, null);
                    builder.setView(dialogView);
                    final TextView tvitem = dialogView.findViewById(R.id.changetvItem);
                    final TextView tvprice = dialogView.findViewById(R.id.changetvPrice);
                    tvitem.setText("Item: "+item);
                    tvprice.setText("Price: "+price);

                    Double finalPrc = prc;
                    builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            updatePrice(item, finalPrc);
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
                } else if (total.isEmpty()) {
                    Toast.makeText(DataEntryForm.this,"Empty fields",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();

                }else if(orderStatus=="Sell" && qty>ItemStock.get(item)){
                    Toast.makeText(DataEntryForm.this,"Out of stock",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
                else {
                    if (orderStatus.equals("Sell") || orderStatus.equals("Purchase")) {
                        uploadStockEntry(item, qty);
                    }
                    else {
                        uploadData("");
                    }
                }

            }

            private void updateItemStock(String item, Double qty) {

                Double sqty = 0.0;
                if (ItemStock.get(item.toLowerCase())!=null) {
                   sqty = ItemStock.get(item.toLowerCase());
                }
                if (orderStatus.equals("Sell")){
                    sqty-=qty;
                }
                if (orderStatus.equals("Purchase")){
                    sqty+=qty;
                }
                Map<String,Object> map = new HashMap<>();
                map.put("ItemStock",sqty);
                DocumentReference partiesRef = db.collection("ProfileCollection")
                        .document(userID)
                        .collection("Items").document(ItemId.get(item.toLowerCase()));

                partiesRef.update(map)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(DataEntryForm.this,"stock Updated",Toast.LENGTH_SHORT).show();
                            fetchItems();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(DataEntryForm.this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("PriceUpdate",e.getMessage().toString());
                        });
            }

            public void updatePartyTotal(Double Total,Calendar calendar){
                if (orderStatus.equals("Sell") || orderStatus.equals("Payment Out")) {
                    PartyTotal += Total;
                }else {
                    PartyTotal-=Total;
                }
                Map<String,Object> map = new HashMap<>();
                map.put("PartyTotal",PartyTotal);
                map.put("Date",new Timestamp(calendar.getTime()));
                    DocumentReference partiesRef = db.collection("ProfileCollection")
                            .document(userID)
                            .collection("Parties").document(partyID);
                    partiesRef.update(map)
                            .addOnSuccessListener(unused -> {
                                progressDialog.dismiss();
                                Toast.makeText(DataEntryForm.this,"Data Updated",Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(DataEntryForm.this,PartyDetails.class);
                                Bundle b = new Bundle();
                                b.putString("PartyName",partyName);
                                b.putDouble("PartyTotal",PartyTotal);
                                b.putString("PartyID",partyID);
                                i.putExtras(b);
                                startActivity(i);
                                finish();
                            }).addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(DataEntryForm.this, "Failed to update data entry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });

            }

            public void showDatePickerDialog() {
                DatePickerDialog datePickerDialog = new DatePickerDialog(DataEntryForm.this, new DatePickerDialog.OnDateSetListener() {
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
                editDate.setText(formattedDate);
            }

            public void updatePrice(String item,Double prc){
                Map<String,Object> map = new HashMap<>();
                map.put("ItemPrice",prc);
                DocumentReference partiesRef = db.collection("ProfileCollection")
                        .document(userID)
                        .collection("Items").document(ItemId.get(item.toLowerCase()));

                partiesRef.update(map)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(DataEntryForm.this,"Price Updated",Toast.LENGTH_SHORT).show();
                            fetchItems();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(DataEntryForm.this, "Failed to update Price: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("PriceUpdate",e.getMessage().toString());
                        });

            }
            public void insertItem(String ItemName,Double ItemPrice){
        //        progressDialog.show();
                Double ItemStock = 0.0;
                Map<String, Object> setFields = new HashMap<>();
                setFields.put("ItemName",ItemName.toLowerCase());
                setFields.put("ItemPrice",ItemPrice);
                setFields.put("ItemStock",ItemStock);
                CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("Items");


                dataEntriesRef.add(setFields)
                        .addOnSuccessListener(aVoid -> {
                            // Data entry updated successfully
                            Toast.makeText(this, "Item Added successfully", Toast.LENGTH_SHORT).show();
        //                    progressDialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            // Error updating data entry
                            Toast.makeText(this, "Failed to Add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        //                    progressDialog.dismiss();
                        });
        //        progressDialog.dismiss();
            }

            public  void uploadStockEntry(String item,Double Quantity){
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.YEAR, selectedYear);
                calendar.set(Calendar.MONTH, selectedMonth);
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                if (!itemsList.contains(item)){
                    Toast.makeText(DataEntryForm.this,"Select item from list",Toast.LENGTH_SHORT).show();
                }
                else {
                    progressDialog.show();
                    Double sqty = 0.0;
                    if (ItemStock.get(item.toLowerCase())!=null) {
                        sqty = ItemStock.get(item.toLowerCase());
                    }
                    if (orderStatus.equals("Sell")){
                        sqty-=Quantity;
                    }
                    if (orderStatus.equals("Purchase")){
                        sqty+=Quantity;
                    }
                    Map<String,Object> map = new HashMap<>();
                    map.put("Date",new com.google.firebase.Timestamp(calendar.getTime()));
                    map.put("Item",item);
                    map.put("Qty",Quantity);
                    map.put("TotalQty",sqty);
                    map.put("OrderStatus",orderStatus);
                    CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("StockEntry");
                    Double finalQty = Quantity;
                    dataEntriesRef.add(map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            String stockEntryID = documentReference.getId();
                            uploadData(stockEntryID);

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();


                        }
                    });

                }
            }

            public void uploadData(String stockEntryID){
                Double qty = null;
                Double prc = null ;
                Double Total = null;
                String item = itemsSpinner.getText().toString().trim();
                String quantity = editQuantity.getText().toString().trim();
                String price = editPrice.getText().toString().trim();
                String total = editTotal.getText().toString().trim();
                String Description = editDescription.getText().toString().trim();
                if (price != null && !price.isEmpty()) {
                    prc = Double.parseDouble(price);
                }
                if (quantity != null && !quantity.isEmpty()) {
                    qty = Double.parseDouble(quantity);
                }
                if (orderStatus.equals("Sell") || orderStatus.equals("Purchase")){
                    if (item.isEmpty() || quantity.isEmpty() || price.isEmpty() || total.isEmpty()){
                        Toast.makeText(DataEntryForm.this,"Some fields are empty",Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                    else {
                        Total = Double.parseDouble(total);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.set(Calendar.YEAR, selectedYear);
                        calendar.set(Calendar.MONTH, selectedMonth);
                        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                        Map<String, Object> map = new HashMap<>();
                        map.put("Date", new com.google.firebase.Timestamp(calendar.getTime()));
                        map.put("PartyName", partyName);
                        map.put("OrderStatus", orderStatus);
                        map.put("Item", item);
                        map.put("Qty", qty);
                        map.put("Price", prc);
                        map.put("Total", Total);
                        map.put("Description",Description);
                        map.put("StockEntryID",stockEntryID);
                        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("DataEntry");
                        Double finalTotal = Total;
                        Double finalQty = qty;
                        dataEntriesRef
                                .add(map)
                                .addOnSuccessListener(documentReference -> {
                                    String entryId = documentReference.getId();
                                    Toast.makeText(getApplicationContext(), "Data uploaded to Firestore ", Toast.LENGTH_SHORT).show();
                                    updatePartyTotal(finalTotal,calendar);
                                    if(!item.isEmpty() && finalQty!=null) {
                                        updateItemStock(item, finalQty);
                                    }

                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Failed to upload data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                }
                else {
                    if (!total.isEmpty()) {
                        Total = Double.parseDouble(total);
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    if (Total != null ) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("Date", new com.google.firebase.Timestamp(calendar.getTime()));
                        map.put("PartyName", partyName);
                        map.put("OrderStatus", orderStatus);
                        map.put("Item", item);
                        map.put("Qty", qty);
                        map.put("Price", prc);
                        map.put("Total", Total);
                        map.put("Description",Description);
                        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("DataEntry");
                        Double finalTotal = Total;
                        Double finalQty = qty;
                        dataEntriesRef
                                .add(map)
                                .addOnSuccessListener(documentReference -> {
                                    String entryId = documentReference.getId();
                                    Toast.makeText(getApplicationContext(), "Data uploaded to Firestore ", Toast.LENGTH_SHORT).show();
                                    updatePartyTotal(finalTotal,calendar);
                                    if(!item.isEmpty() && finalQty!=null) {
                                        updateItemStock(item, finalQty);
                                    }

                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Failed to upload data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getApplicationContext(), "Some fields are empty", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
