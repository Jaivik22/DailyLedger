package com.example.dailyledger;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class showDataEntry extends AppCompatActivity {
    DataEntryModel dataEntry;
    TextView valName,valOrderStatus,valItem,valQty,valPrice,valTotal,valDescription;
    EditText valDate;
    Button deleteButton, shareButton,editButton;
    SharedPreferences sharedPreferences;
    String userID;
    String PartyID = "";
    String FullName, BusinessName, phoneNo;
    Double PartyTotal;
    FirebaseFirestore db;
    File file;
    String filePath;
    ProgressDialog progressDialog;
    String itemID;
    Double itemStock;
    int selectedYear, selectedMonth, selectedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data_entry);

        db = FirebaseFirestore.getInstance();


        valDate = findViewById(R.id.valuetvDate);
        valName = findViewById(R.id.valuetvPartyName);
        valOrderStatus = findViewById(R.id.valuetvOrderStatus);
        valItem = findViewById(R.id.valuetvItem);
        valQty = findViewById(R.id.valuetvQty);
        valPrice = findViewById(R.id.valuetvPrice);
        valTotal = findViewById(R.id.valuetvTotal);
        deleteButton = findViewById(R.id.deleteBtn);
        shareButton = findViewById(R.id.shareBtn);
        valDescription = findViewById(R.id.valuetvDescription);
        editButton = findViewById(R.id.editBtn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);


        Intent intent = getIntent();
        dataEntry = intent.getParcelableExtra("dataEntry");
        PartyTotal = intent.getDoubleExtra("PartyTotal",0.00);
        PartyID = intent.getStringExtra("PartyID");

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        FullName = sharedPreferences.getString("storedFullName","FullName");
        BusinessName = sharedPreferences.getString("storedBusinessName","BusinessName");
        phoneNo = sharedPreferences.getString("storedPhoneNo","Phone no.");
        userID = sharedPreferences.getString("storedUserId","userID");
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        if (dataEntry.getDate()!=null) {
            valDate.setText(" " + formatDate(dataEntry.getDate()));
        }
        if (!dataEntry.getPartyName().isEmpty()) {
            valName.setText(" " + dataEntry.getPartyName());
        }
        if (!dataEntry.getOrderStatus().isEmpty()) {
            valOrderStatus.setText(" " + dataEntry.getOrderStatus());
        }

        valItem.setText(dataEntry.getItem() != null ? " " + dataEntry.getItem() : "");
        valQty.setText(dataEntry.getQty() != null ? " " + String.valueOf(decimalFormat.format(dataEntry.getQty())) : "");
        valPrice.setText(dataEntry.getPrice() != null ? " " + String.valueOf(decimalFormat.format(dataEntry.getPrice())) : "");
        valTotal.setText(dataEntry.getTotal() != null ? " " + String.valueOf(decimalFormat.format(dataEntry.getTotal())) : "");
        valDescription.setText(dataEntry.getDescription() != null ? " " + String.valueOf(dataEntry.getDescription()) : "");

        if (PartyID.equals("partyID")){
            deleteButton.setVisibility(View.GONE);
        }

        deleteButton.setOnClickListener(view ->
                deleteEntry()
//                Toast.makeText(showDataEntry.this,dataEntry.getStockEntryID().toString(),Toast.LENGTH_SHORT).show()
        );
        shareButton.setOnClickListener(view -> {
            generatePDFReport(dataEntry);
            SharePDF(file);
        });
        if (PartyID.equals("partyID")){
            editButton.setVisibility(View.GONE);
        }
        editButton.setOnClickListener(view -> {
            editDataEntry();
        });

        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        updateDateLabel();
        valDate.setOnClickListener(view -> {
            showDatePickerDialog();
        });

        valQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String qty = valQty.getText().toString();
                String prc = valPrice.getText().toString();
                if (!qty.isEmpty() && !prc.isEmpty()) {
                    try {
                        Double dqty = Double.parseDouble(qty);
                        Double dprc = Double.parseDouble(prc);
                        valTotal.setText(String.valueOf(decimalFormat.format(dprc * dqty)));
                    }catch (Exception e){}
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        valPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String qty = valQty.getText().toString();
                String prc = valPrice.getText().toString();
                if (!qty.isEmpty() || !prc.isEmpty()) {
                    try {
                        Double dqty = Double.parseDouble(qty);
                        Double dprc = Double.parseDouble(prc);
                        valTotal.setText(String.valueOf(decimalFormat.format(dprc * dqty)));
                    }catch (Exception e){}
                }
            }
        });


    }


    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    public void deleteEntry(){
        progressDialog.show();
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("DataEntry").document(dataEntry.getEntryID());

        partiesRef.delete().addOnSuccessListener(unused -> {
            progressDialog.dismiss();

            if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Purchase") ){
                if (dataEntry.getStockEntryID()!=null && !dataEntry.getStockEntryID().isEmpty() && !dataEntry.getStockEntryID().equals("")) {
                    fetchItems();
                    deleteStockEntry();
                }
            }
            updatePartyTotal();
        }).addOnFailureListener(e -> {
            Toast.makeText(showDataEntry.this,"Failed to Delete",Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        });
    }

    public void updatePartyTotal(){
        progressDialog.show();
        if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Payment Out")) {
            if (dataEntry.getTotal()!=null) {
                PartyTotal -= dataEntry.getTotal();
            }
        }else {
            if (dataEntry.getTotal()!=null) {
                PartyTotal += dataEntry.getTotal();
            }
        }
        Map<String,Object> map = new HashMap<>();
        map.put("PartyTotal",PartyTotal);
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Parties").document(PartyID);

        partiesRef.update(map).addOnSuccessListener(unused -> {
            Toast.makeText(showDataEntry.this,"Data Updated",Toast.LENGTH_SHORT).show();
            Intent i = new Intent(showDataEntry.this,PartyDetails.class);
            Bundle b = new Bundle();
            b.putString("PartyName",dataEntry.getPartyName());
            b.putDouble("PartyTotal",PartyTotal);
            b.putString("PartyID",PartyID);
            i.putExtras(b);
            startActivity(i);
            finish();

        }).addOnFailureListener(e -> {
            Toast.makeText(showDataEntry.this,"Failed to Delete",Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        });
    }

    public void deleteStockEntry(){
        progressDialog.show();
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("StockEntry").document(dataEntry.getStockEntryID());
        partiesRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();

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

        if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Purchase")){
            Double sqty = 0.0;
            if (itemStock!=null) {
                sqty = itemStock;
            }
            if (dataEntry.getOrderStatus().equals("Sell")){
                sqty+=dataEntry.getQty();
            }
            if (dataEntry.getOrderStatus().equals("Purchase")){
                sqty-=dataEntry.getQty();
            }
            Map<String,Object> map = new HashMap<>();
            map.put("ItemStock",sqty);
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("Items").document(itemID);

            partiesRef.update(map)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(showDataEntry.this,"stock Updated",Toast.LENGTH_SHORT).show();

                    }).addOnFailureListener(e -> {
                        Toast.makeText(showDataEntry.this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PriceUpdate",e.getMessage().toString());
                        progressDialog.dismiss();
                    });
        }

    }
    private void fetchItems() {
        CollectionReference itemsRef = db.collection("ProfileCollection").document(userID).collection("Items");
        Query query = itemsRef.whereEqualTo("ItemName",dataEntry.getItem());


        query.get().addOnSuccessListener(queryDocumentSnapshots -> {

            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                itemStock = 0.0;

                if (documentSnapshot.getDouble("ItemStock")!=null){
                    itemStock = documentSnapshot.getDouble("ItemStock");
                    itemID = documentSnapshot.getId();
                }

                updateStock();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to retrieve items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("fetchItem",e.getMessage().toString());
        });

    }
    private void generatePDFReport(DataEntryModel dataEntry) {
        // Create a new PdfDocument
        PdfDocument document = new PdfDocument();

        // Define table properties
        int numColumns = 7; // Number of columns in the table
        int numRows = 2; // Number of rows in the table (including header row)
        float tableWidth = 595f; // Width of the table
        float columnWidth = tableWidth / (float) numColumns; // Width of each column

        // Create a new page
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 page size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Set up the paint for text
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        // Set the initial y-coordinate for drawing text
        float y = 50f;


        float centerX = pageInfo.getPageWidth() / 2f;


        paint.setColor(Color.RED); // Set the text color to red
        paint.setTextSize(25f); // Increase the text size

        // Draw the "Daily Ledger" text
        String dailyLedgerText = "Daily Ledger";
        String slogan = "Simple, Fast and Secure";
        float dailyLedgerTextX = centerX - paint.measureText(dailyLedgerText) / 2f;
        canvas.drawText(dailyLedgerText, dailyLedgerTextX, y, paint);

        y += 15f;
        paint.setTextSize(18f);
        float sloganTextX = centerX - paint.measureText(slogan) / 2f;
        canvas.drawText(slogan, sloganTextX, y, paint);


        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        // Move to the next line
        y += 50f;

        // Draw the additional text, business name, logo, etc.
        canvas.drawText("Name: "+FullName, 10f, y, paint);
        y += 20f;
        canvas.drawText("Business Name: "+BusinessName, 10f, y, paint);
        y += 20f;
        canvas.drawText("Contact: "+phoneNo, 10f, y, paint);
        y += 50f;


        // Draw the table header
        String[] headers = {"Date", "Party Name", "Order Status", "Item", "Quantity", "Price", "Total"};
        for (int i = 0; i < numColumns; i++) {
            canvas.drawText(headers[i], columnWidth * i, y, paint);
        }
        y += 20f;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Draw the data entries in the table
//        for (DataEntryModel dataEntry : dataEntries) {
            // Draw each data field in the corresponding column
            String formattedDate = dateFormat.format(dataEntry.getDate());
            canvas.drawText(formattedDate, columnWidth * 0, y, paint);
            canvas.drawText(dataEntry.getPartyName(), columnWidth * 1, y, paint);
            canvas.drawText(dataEntry.getOrderStatus(), columnWidth * 2, y, paint);
            if (!dataEntry.getItem().toString().isEmpty()) {
                canvas.drawText(dataEntry.getItem(), columnWidth * 3, y, paint);
            }
            if (dataEntry.getQty()!=null) {
                canvas.drawText(dataEntry.getQty().toString(), columnWidth * 4, y, paint);
            }
            if (dataEntry.getPrice()!=null) {
                canvas.drawText(dataEntry.getPrice().toString(), columnWidth * 5, y, paint);
            }
            if (!dataEntry.getTotal().toString().isEmpty()) {
                canvas.drawText(dataEntry.getTotal().toString(), columnWidth * 6, y, paint);
            }

            y += 20f;
//        }

        // Finish the page
        document.finishPage(page);
        // Define the file path to save the PDF report
        filePath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_report.pdf";

        // Create a file object with the file path
        file = new File(filePath);
        Toast.makeText(showDataEntry.this, filePath.toString(), Toast.LENGTH_SHORT).show();
        Log.d("LocationPDF", filePath);

        try {
            Log.d("PDFGeneration", "Inside try block"); // Log statement for debugging

            // Write the PDF document content to the file
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Log.d("PDFGeneration", "PDF document written to file"); // Log statement for debugging

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GenError", e.getMessage().toString());
            Toast.makeText(getApplicationContext(), "Failed to generate PDF report", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GenError", e.getMessage().toString());
            Toast.makeText(getApplicationContext(), "An error occurred while generating the PDF report", Toast.LENGTH_SHORT).show();
        }
    }


    public  void SharePDF(File file){
        Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        // Create an intent to share the PDF file
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);

        // Check if WhatsApp is installed
//        if (isWhatsAppInstalled()) {
//            // Set WhatsApp as the package to handle the intent
//            shareIntent.setPackage("com.whatsapp");
//        } else {
        // Provide an alternative sharing option
        // You can customize this part based on the available sharing options on the device
        // For example, you can use a different messaging or file-sharing app
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share PDF Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Please share this PDF report");
//        }

        // Grant permission to the receiving app
        List<ResolveInfo> resolvedIntentActivities = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
            String packageName = resolvedIntentInfo.activityInfo.packageName;
            grantUriPermission(packageName, pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        // Start the activity for sharing the PDF report
        startActivity(shareIntent);
    }


    private void editDataEntry() {
        if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Purchase")) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            String editQty = valQty.getText().toString().trim();
            String editPrice = valPrice.getText().toString().trim();
            String editTotal = valTotal.getText().toString().trim();
            Double dQty = 0.0;
            Double dPrice = 0.0;
            Double dTotal = 0.0;
            if (!editQty.isEmpty() || !editPrice.isEmpty() || !editTotal.isEmpty()) {
                dQty = Double.parseDouble(editQty);
                dPrice = Double.parseDouble(editPrice);
                dTotal = Double.parseDouble(editTotal);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("Date",new com.google.firebase.Timestamp(calendar.getTime()));
            map.put("Qty", dQty);
            map.put("Price", dPrice);
            map.put("Total", dTotal);
            progressDialog.show();
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("DataEntry").document(dataEntry.getEntryID());
            Double finalDTotal = dTotal;
            Double finalDQty = dQty;
            partiesRef.update(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    editPartyTotal(finalDTotal);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(showDataEntry.this, "Failed to update data Entry", Toast.LENGTH_SHORT).show();
                }
            });

        }
        else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            String editTotal = valTotal.getText().toString().trim();
            Double dTotal = 0.0;
            if ( !editTotal.isEmpty()) {
                dTotal = Double.parseDouble(editTotal);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("Total", dTotal);
            map.put("Date",new com.google.firebase.Timestamp(calendar.getTime()));
            progressDialog.show();
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("DataEntry").document(dataEntry.getEntryID());
            Double finalDTotal = dTotal;
            partiesRef.update(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    editPartyTotal(finalDTotal);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(showDataEntry.this, "Failed to update data Entry", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void editPartyTotal(Double newTotal) {
        progressDialog.show();
        if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Payment Out")) {
            if (dataEntry.getTotal()!=null) {
                PartyTotal -= dataEntry.getTotal();
                PartyTotal+= newTotal;
            }
        }else {
            if (dataEntry.getTotal()!=null) {
                PartyTotal += dataEntry.getTotal();
                PartyTotal-=newTotal;
            }
        }
        Map<String,Object> map = new HashMap<>();
        map.put("PartyTotal",PartyTotal);
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Parties").document(PartyID);

        partiesRef.update(map).addOnSuccessListener(unused -> {
            if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Purchase")) {
                fetchEditItems();

            }else {
                Toast.makeText(showDataEntry.this, "Data Updated", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(showDataEntry.this,PartyDetails.class);
                Bundle b = new Bundle();
                b.putString("PartyName",dataEntry.getPartyName());
                b.putDouble("PartyTotal",PartyTotal);
                b.putString("PartyID",PartyID);
                i.putExtras(b);
                startActivity(i);
                finish();
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(showDataEntry.this,"Failed to Delete",Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        });
    }

    private void fetchEditItems() {
        String editQty = valQty.getText().toString().trim();
        Double dQty = 0.0;
        if (!editQty.isEmpty() ) {
            dQty = Double.parseDouble(editQty);

        }
        CollectionReference itemsRef = db.collection("ProfileCollection").document(userID).collection("Items");
        Query query = itemsRef.whereEqualTo("ItemName",dataEntry.getItem());


        Double finalDQty = dQty;
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {

            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                itemStock = 0.0;

                if (documentSnapshot.getDouble("ItemStock")!=null){
                    itemStock = documentSnapshot.getDouble("ItemStock");
                    itemID = documentSnapshot.getId();
                }

                EditStock(finalDQty);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to retrieve items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("fetchItem",e.getMessage().toString());
        });
    }

    private void EditStock(Double newQty) {
        progressDialog.show();

        if (dataEntry.getOrderStatus().equals("Sell") || dataEntry.getOrderStatus().equals("Purchase")){
            Double sqty = 0.0;
            if (itemStock!=null) {
                sqty = itemStock;
            }
            if (dataEntry.getOrderStatus().equals("Sell")){
                sqty+=dataEntry.getQty();
                sqty-=newQty;
            }
            if (dataEntry.getOrderStatus().equals("Purchase")){
                sqty-=dataEntry.getQty();
                sqty+=dataEntry.getQty();
            }
            Map<String,Object> map = new HashMap<>();
            map.put("ItemStock",sqty);
            DocumentReference partiesRef = db.collection("ProfileCollection")
                    .document(userID)
                    .collection("Items").document(itemID);

            partiesRef.update(map)
                    .addOnSuccessListener(unused -> {
                        if (dataEntry.getStockEntryID()!=null && !dataEntry.getStockEntryID().isEmpty() && !dataEntry.getStockEntryID().equals("")) {
                            editStockEntry(newQty);
                            Toast.makeText(showDataEntry.this, "stock Updated", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            progressDialog.dismiss();
                            Toast.makeText(showDataEntry.this, "Data Updated", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(showDataEntry.this,PartyDetails.class);
                            Bundle b = new Bundle();
                            b.putString("PartyName",dataEntry.getPartyName());
                            b.putDouble("PartyTotal",PartyTotal);
                            b.putString("PartyID",PartyID);
                            i.putExtras(b);
                            startActivity(i);
                            finish();
                        }

                    }).addOnFailureListener(e -> {
                        Toast.makeText(showDataEntry.this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PriceUpdate",e.getMessage().toString());
                        progressDialog.dismiss();
                    });
        }
    }

    private void editStockEntry(Double newQty) {
        progressDialog.show();
        Map<String,Object> map = new HashMap<>();
        map.put("Qty",newQty);
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("StockEntry").document(dataEntry.getStockEntryID());
        partiesRef.update(map).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(showDataEntry.this, "Data Updated", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(showDataEntry.this,PartyDetails.class);
                Bundle b = new Bundle();
                b.putString("PartyName",dataEntry.getPartyName());
                b.putDouble("PartyTotal",PartyTotal);
                b.putString("PartyID",PartyID);
                i.putExtras(b);
                startActivity(i);
                finish();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();

            }
        });
    }
    public void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(showDataEntry.this, new DatePickerDialog.OnDateSetListener() {
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
        valDate.setText(formattedDate);
    }


}