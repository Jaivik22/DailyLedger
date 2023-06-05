package com.example.dailyledger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ItemsPage extends AppCompatActivity {
    FirebaseFirestore db;
    String userID;
    SharedPreferences sharedPreferences;
    private List<ItemModel> itemList;
    private ItemAdapter itemAdapter;
    RecyclerView recyclerView;
    private ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_page);
        recyclerView = findViewById(R.id.itemsrv);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        progressDialog = new ProgressDialog(ItemsPage.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);


        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("storedUserId", "userID");
        itemList = new ArrayList<ItemModel>();
        fetchItems();


    }
    public void fetchItems() {
        progressDialog.show();
        CollectionReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Items");

        partiesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    itemList.clear();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String ItemName = "";
                        Double ItemPrice = 0.0;
                        Double ItemStock= 0.0;
                        String ItemID = "";
                        if (!document.getString("ItemName").isEmpty()) {
                            ItemName = document.getString("ItemName");
                        }
                        if (document.getDouble("ItemPrice")!=null) {
                            ItemPrice = document.getDouble("ItemPrice");
                        }
                        if (document.getDouble("ItemStock")!=null) {
                            ItemStock = document.getDouble("ItemStock");
                        }
                        if (!document.getId().isEmpty()) {
                            ItemID = document.getId();
                        }
                        ItemModel itemModel = new ItemModel(ItemName,ItemPrice,ItemStock,ItemID);
                        itemList.add(itemModel);
                    }

                    itemAdapter = new ItemAdapter(ItemsPage.this, itemList);
                    recyclerView.setAdapter(itemAdapter);
                    itemAdapter.notifyDataSetChanged();
                    Log.d("fetchItem", itemList.toString(), task.getException());
                    progressDialog.dismiss();

                } else {
                    Log.d("MainActivity", "Error getting party documents: ", task.getException());
                    Toast.makeText(ItemsPage.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });


    }
}