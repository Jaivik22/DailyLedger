package com.example.dailyledger;

import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingParty extends AppCompatActivity {
    String PartyName,PartyID;
    TextView tvChangeName, tvDeleteAccount;
    FirebaseFirestore db;
    private List<String> partyNames;
    String FullName, BusinessName, phoneNo,userID;
    SharedPreferences sharedPreferences;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_party);
        tvChangeName = findViewById(R.id.tvChangeName);
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount);
        db = FirebaseFirestore.getInstance();
        partyNames = new ArrayList<>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        FullName = sharedPreferences.getString("storedFullName","FullName");
        BusinessName = sharedPreferences.getString("storedBusinessName","BusinessName");
        phoneNo = sharedPreferences.getString("storedPhoneNo","Phone no.");
        userID = sharedPreferences.getString("storedUserId","userID");

        Intent i = getIntent();
        PartyName = i.getStringExtra("PartyName");
        PartyID = i.getStringExtra("PartyID");

        tvChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputPartyDialog();
            }
        });

        tvDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowDeletePartyDialog();
            }
        });
        fetchParties();


    }
    private void showInputPartyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Party Name");
        builder.setMessage("Do you want to change the name of party? it may cause data loss");


        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_party, null);
        builder.setView(dialogView);

        final EditText input = dialogView.findViewById(R.id.editTextInput);
        final TextView warning = dialogView.findViewById(R.id.warningName);

        builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredText = input.getText().toString().trim();
                updatePartyNameInDataEntries(enteredText);
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

    public void ShowDeletePartyDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Party");
        builder.setMessage("Do you want to  delete this party? it will cause you data loss");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deletePartyFromDataEntries();
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
    private void fetchParties() {
        progressDialog.show();
        CollectionReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Parties");

        partiesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    partyNames.clear();
                    progressDialog.dismiss();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String partyName = document.getString("PartName");
                        partyNames.add(partyName.toLowerCase());
                    }
                } else {
                    Log.d("MainActivity", "Error getting party documents: ", task.getException());
                    Toast.makeText(SettingParty.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });

    }

    private void updatePartyNameInDataEntries(String newPartyName) {
        progressDialog.show();
        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("DataEntry");

        // Create a batch write operation
        WriteBatch batch = db.batch();

        // Query the data entries with the old party name
        Query query = dataEntriesRef.whereEqualTo("PartyName", PartyName);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Iterate through the query snapshot and update the PartyName field in each document
                for (QueryDocumentSnapshot document : task.getResult()) {
                    DocumentReference docRef = dataEntriesRef.document(document.getId());

                    // Update the PartyName field in the document
                    batch.update(docRef, "PartyName", newPartyName);
                }

                // Commit the batch write operation
                batch.commit().addOnCompleteListener(batchTask -> {
                    if (batchTask.isSuccessful()) {
                        Log.d("Firestore", "PartyName updated successfully in all data entries");
                        updatePartyName(newPartyName);
                    } else {
                        Log.e("Firestore", "Failed to update PartyName in data entries: " + batchTask.getException().getMessage());
                    }
                });
                progressDialog.dismiss();
            } else {
                Log.e("Firestore", "Failed to fetch data entries: " + task.getException().getMessage());
                progressDialog.dismiss();
            }
        });
    }

    public  void updatePartyName(String newPartyName){
        progressDialog.show();
        Map<String,Object> map = new HashMap<>();
        map.put("PartName",newPartyName);
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Parties").document(PartyID);

        partiesRef.update(map).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(SettingParty.this,"PartyName updated",Toast.LENGTH_SHORT).show();
                Intent i = new Intent(SettingParty.this,MainActivity.class);
                startActivity(i);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(SettingParty.this,"Failed to update PartyName",Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void deletePartyFromDataEntries(){
        progressDialog.show();
        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("DataEntry");

        // Create a batch write operation
        WriteBatch batch = db.batch();

        // Query the data entries with the old party name
        Query query = dataEntriesRef.whereEqualTo("PartyName", PartyName);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Iterate through the query snapshot and update the PartyName field in each document
                for (QueryDocumentSnapshot document : task.getResult()) {
                    DocumentReference docRef = dataEntriesRef.document(document.getId());

                    // Update the PartyName field in the document
                    batch.delete(docRef);
                }

                // Commit the batch write operation
                batch.commit().addOnCompleteListener(batchTask -> {
                    if (batchTask.isSuccessful()) {
                        Log.d("Firestore", "Party Deleted from all databases");
                        DeletePartyName();
                    } else {
                        Log.e("Firestore", "Failed to delete PartyName in data entries: " + batchTask.getException().getMessage());
                    }
                });
                progressDialog.dismiss();
            } else {
                progressDialog.dismiss();
                Log.e("Firestore", "Failed to fetch data entries: " + task.getException().getMessage());
            }
        });
    }

    public void DeletePartyName(){
        progressDialog.show();
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Parties").document(PartyID);

        partiesRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(SettingParty.this,"PartyName Deleted",Toast.LENGTH_SHORT).show();
                Intent i = new Intent(SettingParty.this,MainActivity.class);
                startActivity(i);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(SettingParty.this,"Failed to Delete PartyName",Toast.LENGTH_SHORT).show();

            }
        });
    }

}