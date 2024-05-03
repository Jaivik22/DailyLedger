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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PartyDetails extends AppCompatActivity {
    MaterialButton fab,fab3;
    FloatingActionButton fab2;
    TextView tvPartyTotal, tvPartyName;
    String PartyName,PartyID;
    Double PartyTotal;
    private FirebaseFirestore db;
    RecyclerView prv;
    SharedPreferences sharedPreferences;
    String FullName, BusinessName, phoneNo,userID;
    EditText startDate,endDate;
    Button filterBtn;
    String startFromDate, endToDate;
    List<DataEntryModel> dataEntries;
    CardView sharetxt,viewtxt;
    File file;
    String filePath;
    ImageView settingImg;
    Date StartD , EndD;
    int selectedYear, selectedMonth, selectedDay;
    ProgressDialog progressDialog;
    DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_details);
        db = FirebaseFirestore.getInstance();
        startDate = findViewById(R.id.pstartDate);
        endDate = findViewById(R.id.pendDate);
        filterBtn = findViewById(R.id.psearchBtn);
        sharetxt = findViewById(R.id.psharetxt);
        viewtxt  =findViewById(R.id.pviewtxt);
        settingImg = findViewById(R.id.PartySetting);

        prv = findViewById(R.id.prv);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        prv.setLayoutManager(layoutManager);

        fab = findViewById(R.id.fab);
        fab2 = findViewById(R.id.fab2);
        fab3 = findViewById(R.id.fab3);
        tvPartyName = findViewById(R.id.pdPartyName);
        tvPartyTotal = findViewById(R.id.pdPartyTotal);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        dataEntries = new ArrayList<>();

        Bundle b = getIntent().getExtras();
        if (!b.isEmpty()) {
            PartyName = b.getString("PartyName");
            PartyTotal = b.getDouble("PartyTotal");
            PartyID = b.getString("PartyID");
        }

        tvPartyName.setText(PartyName);
        tvPartyTotal.setText(String.valueOf(decimalFormat.format(PartyTotal)));
        if (PartyTotal>0){
            tvPartyTotal.setTextColor(Color.parseColor("#0cba7b"));
        }
        else {
            tvPartyTotal.setTextColor(Color.parseColor("#FF0000"));
        }

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        FullName = sharedPreferences.getString("storedFullName","FullName");
        BusinessName = sharedPreferences.getString("storedBusinessName","BusinessName");
        phoneNo = sharedPreferences.getString("storedPhoneNo","Phone no.");
        userID = sharedPreferences.getString("storedUserId","userID");




        fab2.setOnClickListener(view -> showPopupMenu(fab2));

        fab.setOnClickListener(view -> {
            Intent i = new Intent(PartyDetails.this,DataEntryForm.class);
            Bundle bundle = new Bundle();
            bundle.putString("orderStatus","Purchase");
            bundle.putString("PartyName",PartyName);
            bundle.putDouble("PartyTotal",PartyTotal);
            bundle.putString("PartyID",PartyID);
            i.putExtras(bundle);
            startActivity(i);
            finish();
        });
        fab3.setOnClickListener(view -> {
            Intent i = new Intent(PartyDetails.this,DataEntryForm.class);
            Bundle bundle = new Bundle();
            bundle.putString("orderStatus","Sell");
            bundle.putString("PartyName",PartyName);
            bundle.putDouble("PartyTotal",PartyTotal);
            bundle.putString("PartyID",PartyID);
            i.putExtras(bundle);
            startActivity(i);
            finish();
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
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Date StartDate = calendar.getTime();
        Date EndDate = calendar.getTime();

        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchDataByDate(prv,userID);
            }
        });
        sharetxt.setOnClickListener(view -> {
            generatePDFReport(dataEntries);
            SharePDF(file);
        });
        viewtxt.setOnClickListener(view -> {
            generatePDFReport(dataEntries);
            Intent i = new Intent(PartyDetails.this,ViewPDF.class);
            i.putExtra("filePath",filePath);
            startActivity(i);
        });

        settingImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(PartyDetails.this,SettingParty.class);
                i.putExtra("PartyName",PartyName);
                i.putExtra("PartyID",PartyID);
                startActivity(i);
            }
        });

        fetchDataFromFirestore(prv,userID);


    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(PartyDetails.this, view);
        popupMenu.inflate(R.menu.popup_menu);


        // Set a listener for menu item clicks
        popupMenu.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.menu_payment_in) {
                // Handle payment in action
                Intent intent = new Intent(PartyDetails.this,DataEntryForm.class);
                Bundle bundle = new Bundle();
                bundle.putString("orderStatus","Payment In");
                bundle.putString("PartyName",PartyName);
                bundle.putDouble("PartyTotal",PartyTotal);
                bundle.putString("PartyID",PartyID);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                return true;
            }
            if (i == R.id.menu_payment_out) {
                // Handle payment out action
                Intent intent = new Intent(PartyDetails.this,DataEntryForm.class);
                Bundle bundle = new Bundle();
                bundle.putString("orderStatus","Payment Out");
                bundle.putString("PartyName",PartyName);
                bundle.putDouble("PartyTotal",PartyTotal);
                bundle.putString("PartyID",PartyID);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                return true;
            }
            else {
                return false;
            }
        });

        // Show the popup menu
        popupMenu.show();
    }

    private void fetchDataFromFirestore(RecyclerView recyclerView, String userID) {
        progressDialog.show();
        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("DataEntry");
        Query query = dataEntriesRef.orderBy("Date", Query.Direction.DESCENDING).whereEqualTo("PartyName",PartyName).limit(10);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dataEntries = new ArrayList<>();

                // Iterate through the query snapshot and convert documents to DataEntry objects
                int count = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    DataEntryModel dataEntry = document.toObject(DataEntryModel.class);
                    dataEntry.setEntryID(document.getId());
                    // Get the timestamp from the Firestore document
                    Timestamp timestamp = document.getTimestamp("Date");
                    Log.d("timestamp", String.valueOf(timestamp));

                    // Convert the timestamp to a Date object
                    Date date = timestamp.toDate();


                    // Add the date to the dataEntry object
                    dataEntry.setDate(date);

                    dataEntries.add(dataEntry);
                    Log.d("Firestore", "Data Entry: " + dataEntry.toString());
                }

                // Sort the dataEntries list by both date and time
                Collections.sort(dataEntries, (entry1, entry2) -> {
                    long timestamp1 = entry1.getDate().getTime();
                    long timestamp2 = entry2.getDate().getTime();
                    Log.d("timestamp", String.valueOf(timestamp1));

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
                DataEntryAdapter adapter = new DataEntryAdapter(PartyDetails.this,dataEntries,PartyTotal,PartyID,"specific");
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

    public void fetchDataByDate(RecyclerView recyclerView,String userID){
//        progressDialog.show();
        // Assuming you have a reference to the Firestore collection
        progressDialog.show();
        dataEntries.clear();
        CollectionReference dataEntriesRef = db.collection("ProfileCollection").document(userID).collection("DataEntry");
        Query query;

        // Create a query to retrieve documents within the specified date range
            query = dataEntriesRef.whereEqualTo("PartyName",PartyName).whereGreaterThanOrEqualTo("Date", StartD)
                    .whereLessThanOrEqualTo("Date", EndD).orderBy("Date", Query.Direction.DESCENDING);


        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // Iterate through the query snapshot and convert documents to DataEntry objects
                int count = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    DataEntryModel dataEntry = document.toObject(DataEntryModel.class);
                    dataEntry.setEntryID(document.getId());
                    dataEntries.add(dataEntry);
                    count++;
                    Log.d("Firestore", "Data Entry: " + dataEntry.toString());

                }
                Log.d("count", "Data Entry: " + count);
                // Sort the dataEntries list by both date and time
                Collections.sort(dataEntries, (entry1, entry2) -> {
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
                DataEntryAdapter adapter = new DataEntryAdapter(PartyDetails.this,dataEntries,PartyTotal,PartyID,"specific");
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(PartyDetails.this, new DatePickerDialog.OnDateSetListener() {

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
        DatePickerDialog datePickerDialog = new DatePickerDialog(PartyDetails.this, new DatePickerDialog.OnDateSetListener() {

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

    private void generatePDFReport(List<DataEntryModel> dataEntries) {
        // Create a new PdfDocument
        PdfDocument document = new PdfDocument();

        // Define table properties
        int numColumns = 7; // Number of columns in the table
        int numRows = dataEntries.size() + 1; // Number of rows in the table (including header row)
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
        for (DataEntryModel dataEntry : dataEntries) {
            // Draw each data field in the corresponding column
            String formattedDate = dateFormat.format(dataEntry.getDate());
            canvas.drawText(formattedDate, columnWidth * 0, y, paint);
            canvas.drawText(dataEntry.getPartyName(), columnWidth * 1, y, paint);
            canvas.drawText(dataEntry.getOrderStatus(), columnWidth * 2, y, paint);
            if (!dataEntry.getItem().toString().isEmpty()) {
                canvas.drawText(dataEntry.getItem(), columnWidth * 3, y, paint);
            }
            if (dataEntry.getQty()!=null) {
                canvas.drawText(decimalFormat.format(dataEntry.getQty()).toString(), columnWidth * 4, y, paint);
            }
            if (dataEntry.getPrice()!=null) {
                canvas.drawText(decimalFormat.format(dataEntry.getPrice()).toString(), columnWidth * 5, y, paint);
            }
            if (!dataEntry.getTotal().toString().isEmpty()) {
                canvas.drawText(decimalFormat.format(dataEntry.getTotal()).toString(), columnWidth * 6, y, paint);
            }

            y += 20f;
        }

        // Finish the page
        document.finishPage(page);
        // Define the file path to save the PDF report
        filePath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + File.separator + "data_report.pdf";

        // Create a file object with the file path
        file = new File(filePath);
        Toast.makeText(PartyDetails.this, filePath.toString(), Toast.LENGTH_SHORT).show();
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
    private boolean isWhatsAppInstalled() {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}