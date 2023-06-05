package com.example.dailyledger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private ProgressDialog progressDialog;
    RecyclerView recyclerView;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    TextView drawerFullname,drawerBusinessName,drawerPhoneNumber,tvAccountTotal;
    String FullName, BusinessName, phoneNo,userID;
    SharedPreferences sharedPreferences;
    MaterialButton fab,fab2;
    Double AccontTotal = 0.0;

    private FirebaseFirestore db;

    private List<PartyModel> partyModelList;

    private List<String> partyNames;
    private List<String> itemNames;
    private PartyAdapter partyAdapter;

    private EditText searchEditText;

    @Override
    protected void onStart() {
        super.onStart();
        progressDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.Toolbar1);
        setSupportActionBar(toolbar);
        tvAccountTotal= findViewById(R.id.tvAccountTotal);
        fab = findViewById(R.id.fab);
        fab2 = findViewById(R.id.fab2);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        partyNames = new ArrayList<>();
        itemNames = new ArrayList<>();
        partyModelList = new ArrayList<>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {

                } else {
                    // User is signed out
                    // You can navigate to the login activity
                    Intent i = new Intent(MainActivity.this, LoginPage.class);
                    startActivity(i);
//                    progressDialog.show();
                }
            }
        };
        mAuth.addAuthStateListener(mAuthListener);


        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        FullName = sharedPreferences.getString("storedFullName","FullName");
        BusinessName = sharedPreferences.getString("storedBusinessName","BusinessName");
        phoneNo = sharedPreferences.getString("storedPhoneNo","Phone no.");
        userID = sharedPreferences.getString("storedUserId","userID");


        drawerLayout = findViewById(R.id.mainDrawer);
        navigationView = findViewById(R.id.navigationView);

        recyclerView = findViewById(R.id.rv);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        View headerView = navigationView.getHeaderView(0);
        drawerBusinessName  = headerView.findViewById(R.id.drawerBusinessName);
        drawerPhoneNumber = headerView.findViewById(R.id.drawerPhoneNumber);

        drawerBusinessName.setText(BusinessName);
        drawerPhoneNumber.setText(phoneNo);
        toolbar.setTitle(FullName);

        navigationView.setNavigationItemSelectedListener(MainActivity.this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        AccontTotal = 0.0;

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputPartyDialog();;
            }
        });
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputItemDialog();
            }
        });

        searchEditText = findViewById(R.id.searchEditText);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = searchEditText.getText().toString().trim();
                searchPartyByName(searchTerm);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        fetchItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchParties();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.logout) {
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
            mAuth.signOut();
            Intent i = new Intent(MainActivity.this, LoginPage.class);
            startActivity(i);
        }
        if (id==R.id.menuReport){
            Intent i = new Intent(MainActivity.this,ReportPage.class);
            i.putExtra("Filtermode","Report");
            startActivity(i);
        }
        if (id==R.id.menuItems){
            Intent i = new Intent(MainActivity.this,ItemsPage.class);
            startActivity(i);
        }
        if (id==R.id.menuBuy){
            Intent i = new Intent(MainActivity.this,ReportPage.class);
            i.putExtra("Filtermode","Purchase");
            startActivity(i);
        }
        if (id==R.id.menuSell){
            Intent i = new Intent(MainActivity.this,ReportPage.class);
            i.putExtra("Filtermode","Sell");
            startActivity(i);
        }
        if (id==R.id.menuPaymentIn){
            Intent i = new Intent(MainActivity.this,ReportPage.class);
            i.putExtra("Filtermode","Payment In");
            startActivity(i);
        }
        if (id==R.id.menuPaymentOut){
            Intent i = new Intent(MainActivity.this,ReportPage.class);
            i.putExtra("Filtermode","Payment Out");
            startActivity(i);
        }
        if (id==R.id.menuStock){
            Intent i = new Intent(MainActivity.this,stockReport.class);
            i.putExtra("stockFilter","stockReport");
            i.putExtra("stockItemTotal","0.0");
            i.putExtra("stockItemPrice","0.0");
            i.putExtra("stockItemID","stockItemID");
            startActivity(i);
        }
        return false;
    }

    private void showInputPartyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Party");

        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_party, null);
        builder.setView(dialogView);

        final EditText input = dialogView.findViewById(R.id.editTextInput);
        final TextView warning = dialogView.findViewById(R.id.warningName);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String enteredText = input.getText().toString().trim();
                    if (!enteredText.isEmpty()) {
                        insertPartyName(enteredText);
                    }
                    progressDialog.show();
                }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                // Initially, disable the "Add" button
                Button addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                addButton.setEnabled(false);

                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String enteredText = input.getText().toString().trim();
                        if (partyNames.contains(enteredText.toLowerCase()) || enteredText.isEmpty()) {
                            warning.setText("Try a different name");
                            addButton.setEnabled(false);
                        } else {
                            warning.setText("");
                            addButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                    }
                });
            }
        });
        dialog.show();
    }


    public void insertPartyName(String partyName){
        progressDialog.show();
        Double partyTotal = 0.0;
        Map<String, Object> setFields = new HashMap<>();
        setFields.put("PartName",partyName);
        setFields.put("PartyTotal",partyTotal);
        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("Parties");

        dataEntriesRef.add(setFields)
                .addOnSuccessListener(aVoid -> {
                    // Data entry updated successfully
                    Toast.makeText(this, "Party Added successfully", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    fetchParties();
//                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Error updating data entry
                    Toast.makeText(this, "Failed to Add party: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    progressDialog.dismiss();
                });

    }

    private void showInputItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Item");

        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        final EditText itemName = dialogView.findViewById(R.id.editTextItem);
        final EditText itemPrice = dialogView.findViewById(R.id.editTextitemPrice);
        final TextView warning = dialogView.findViewById(R.id.warningItem);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredItem = itemName.getText().toString().trim();
                String enteredPrice = itemPrice.getText().toString().trim();
                if (!enteredPrice.isEmpty() && !enteredItem.isEmpty()) {
                    Double dEnteredPrice = Double.parseDouble(enteredPrice);
                    insertItem(enteredItem,dEnteredPrice);
                    progressDialog.show();
                }
                else {
                    Toast.makeText(MainActivity.this,"enter price",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                // Initially, disable the "Add" button
                Button addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                addButton.setEnabled(false);


                itemName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        addButton.setEnabled(false);
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        warning.setText("");
                        String enteredText = itemName.getText().toString().trim();
                        if (itemNames.contains(enteredText.toLowerCase()) || enteredText.isEmpty()) {
                            warning.setText("Try a different Item name");
                            addButton.setEnabled(false);
                        }else if(itemPrice.getText().toString().isEmpty()){
                            addButton.setEnabled(false);
                        }
                        else {
                            warning.setText("");
                            addButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                    }
                });

                itemPrice.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                        if (itemPrice.getText().toString().isEmpty()){
//                            addButton.setEnabled(false);
//                        }
//                        else if (itemName.getText().toString().isEmpty()){
//                            addButton.setEnabled(false);
//                        }
                         if (!itemPrice.getText().toString().isEmpty() && !itemName.getText().toString().isEmpty()
                                && !itemNames.contains(itemName.getText().toString())){
                            addButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
            }
        });
        dialog.show();
    }
    public void insertItem(String ItemName,Double ItemPrice){
        progressDialog.show();
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
                    fetchItems();
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    // Error updating data entry
                    Toast.makeText(this, "Failed to Add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
        progressDialog.dismiss();
    }

    private void fetchParties() {
        AccontTotal = 0.0;
        progressDialog.show();
        CollectionReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Parties");

        partiesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {

                    partyModelList.clear();
                    partyNames.clear();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String partyName = "";
                        double PartyTotal = 0.0;
                        Date date = null;
                        String partyID = "";
                        if (!document.getString("PartName").isEmpty()) {
                            partyName = document.getString("PartName");
                        }
                        if (document.getDouble("PartyTotal")!=null) {
                            PartyTotal = document.getDouble("PartyTotal");
                        }
                        if (document.getDate("Date")!=null) {
                            date = document.getDate("Date");
                        }
                        if (!document.getId().isEmpty()) {
                            partyID = document.getId();
                        }
                        AccontTotal+=PartyTotal;

                        partyNames.add(partyName.toLowerCase());
                        PartyModel partyModel = new PartyModel(partyName, PartyTotal,partyID,date);
                        partyModelList.add(partyModel);
                    }

                    DecimalFormat decimalFormat = new DecimalFormat("#.##");
                    tvAccountTotal.setText(String.valueOf(decimalFormat.format(AccontTotal)));
                    if (AccontTotal>0){
                        tvAccountTotal.setTextColor(Color.parseColor("#0cba7b"));
                    }
                    else{
                        tvAccountTotal.setTextColor(Color.parseColor("#FF0000"));
                    }

                    // Sort the dataEntries list by both date and time
                    Collections.sort(partyModelList, (entry1, entry2) -> {
                        Date date1 = entry1.getDate();
                        Date date2 = entry2.getDate();

                        if (date1 == null && date2 == null) {
                            return 0; // Both dates are null, consider them equal
                        } else if (date1 == null) {
                            return 1; // date1 is null, so it should come after date2
                        } else if (date2 == null) {
                            return -1; // date2 is null, so it should come after date1
                        } else {
                            long timestamp1 = date1.getTime();
                            long timestamp2 = date2.getTime();

                            if (timestamp1 == timestamp2) {
                                // If the timestamps are the same, no need to compare the time portion
                                long time1 = date1.getTime();
                                long time2 = date2.getTime();
                                return Long.compare(time1, time2);
                            } else if (timestamp1 > timestamp2) {
                                // entry1 should come after entry2
                                return -1;
                            } else {
                                // entry1 should come before entry2
                                return 1;
                            }
                        }
                    });
                    partyAdapter = new PartyAdapter(MainActivity.this, partyModelList);
                    recyclerView.setAdapter(partyAdapter);
                    partyAdapter.notifyDataSetChanged();

                } else {
                    Log.d("MainActivity", "Error getting party documents: ", task.getException());
                    Toast.makeText(MainActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }
        });


    }
    private void searchPartyByName(String searchTerm) {
        List<PartyModel> filteredPartyList = new ArrayList<>();

        // Perform the search operation based on the searchTerm
        for (PartyModel party : partyModelList) {
            if (party.getPartyName().toLowerCase().contains(searchTerm.toLowerCase())) {
                filteredPartyList.add(party);
            }
        }

        // Update the RecyclerView with the filtered party list
        partyAdapter = new PartyAdapter(MainActivity.this, filteredPartyList);
        recyclerView.setAdapter(partyAdapter);
        partyAdapter.notifyDataSetChanged();
    }


    public void fetchItems(){
        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("Items");
        dataEntriesRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                itemNames.clear();
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    String itemName = "";
                    if (!documentSnapshot.getString("ItemName").toLowerCase().isEmpty()) {
                        itemName = documentSnapshot.getString("ItemName").toLowerCase();
                    }
                    if (!itemName.isEmpty()) {
                        itemNames.add(itemName);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to retrieve items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



}
