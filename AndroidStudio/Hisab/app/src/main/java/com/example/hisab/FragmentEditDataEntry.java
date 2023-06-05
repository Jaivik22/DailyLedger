package com.example.hisab;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class FragmentEditDataEntry extends Fragment {
    EditText etPartyName, etItem, etWeight, etPrice, etTotal, etPaymentIn, etPaymentOut,etPurchase;
    Button editCancelBtn,editUpdateBtn;
    TextView defaultDate;
    private Switch switchButton;
    String entryID,selectedDate, entPartyName, entItem, entWeight, entPrice, entTotal, selectedPaymentStatus, entPaymentIn, entPaymentOut, entPurchase, orderStatus;
    DataEntryModel dataEntry;
    private boolean isBuyMode = true;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userID;
    Map<String, Object> updatedFields = new HashMap<>();
    public static FragmentEditDataEntry newInstance(DataEntryModel dataEntry) {
        FragmentEditDataEntry fragment = new FragmentEditDataEntry();
        Bundle args = new Bundle();
        args.putParcelable("dataEntry",dataEntry);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the data entry from the arguments
        Bundle args = getArguments();
        if (args != null) {
            dataEntry = args.getParcelable("dataEntry");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setTheme(R.style.Theme_MyApp);
        View v = inflater.inflate(R.layout.fragment_edit_data_entry, container, false);
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            v.setBackgroundColor(getResources().getColor(R.color.my_on_secondary_color));
        } else {
            v.setBackgroundColor(getResources().getColor(R.color.my_on_primary_color));
        }


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userID = mAuth.getCurrentUser().getUid();

        editUpdateBtn  = v.findViewById(R.id.editUpdateBtn);
        editCancelBtn = v.findViewById(R.id.editCancelBtn);
        etPartyName = v.findViewById(R.id.editPagePartyName);
        etItem = v.findViewById(R.id.editPageItem);
        etWeight = v.findViewById(R.id.editPageWeight);
        etPrice = v.findViewById(R.id.editPagePrice);
        etTotal = v.findViewById(R.id.editPageTotal);
        etPaymentIn = v.findViewById(R.id.editPagePaymentIn);
        etPaymentOut = v.findViewById(R.id.editPagePaymentOut);
//        etPurchase  = findViewById(R.id.editPurchase);
        switchButton = v.findViewById(R.id.switchPageButton);
        defaultDate = v.findViewById(R.id.defaultDate);
        EditText editDate = v.findViewById(R.id.editPageDate);
        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });


        if (dataEntry.getOrderStatus().equals("Buy")){
            switchButton.setChecked(true);
            switchButton.setText("Buy");
        }
        else {
            switchButton.setChecked(false);
            switchButton.setText("Sell");
        }

        String[] paymentStatusOptions = {"Paid", "HalfPaid", "Unpaid"};
        Spinner paymentStatusSpinner = v.findViewById(R.id.editspinnerPaymentStatus);
        ArrayAdapter<String> paymentStatusAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, paymentStatusOptions);
        paymentStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentStatusSpinner.setAdapter(paymentStatusAdapter);
        int unpaidIndex = Arrays.asList(paymentStatusOptions).indexOf(String.valueOf(dataEntry.getPaymentStatus()));
        paymentStatusSpinner.setSelection(unpaidIndex, false);
        paymentStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                selectedPaymentStatus = paymentStatusOptions[position];
                // Do something with the selected payment status
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

                    int unpaidIndex = Arrays.asList(paymentStatusOptions).indexOf(String.valueOf(dataEntry.getPaymentStatus()));
                    selectedPaymentStatus = paymentStatusOptions[unpaidIndex];
            }
        });



        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    // Buy mode
                    isBuyMode = true;
                    switchButton.setText("Buy");
                    // Handle buy functionality
//                    orderStatus = "Buy";
                    // ...
                } else {
                    // Sell mode
                    isBuyMode = false;
                    switchButton.setText("Sell");
//                    orderStatus = "Sell";
                    // Handle sell functionality
                    // ...
                }
            }
        });

        // Set the data entry values in the EditText fields
        if (dataEntry != null) {
            etPartyName.setText(dataEntry.getPartyName());
            etItem.setText(dataEntry.getItem());
            etWeight.setText(String.valueOf(dataEntry.getWeight()));
            etPrice.setText(String.valueOf(dataEntry.getPrice()));
            etTotal.setText(String.valueOf(dataEntry.getTotal()));
            etPaymentIn.setText(String.valueOf(dataEntry.getPaymentIn()));
            etPaymentOut.setText(String.valueOf(dataEntry.getPaymentOut()));
            defaultDate.setText(String.valueOf(setformatDate(dataEntry.getDate())));

        }


        editUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date selectedDateObj = null;
                try {
                    if (selectedDate!=null) {
                        selectedDateObj = dateFormat.parse(selectedDate);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (selectedDate!=null) {
                    if (!dataEntry.getDate().equals( new Timestamp( selectedDateObj))) {
                        updatedFields.put("Date",  new Timestamp( selectedDateObj));
                    }
                }
                if (!dataEntry.getOrderStatus().equals(switchButton.getText().toString()) && switchButton.getText().toString()!=null){
                    updatedFields.put("OrderStatus",switchButton.getText().toString());
                }
                if(!dataEntry.getPartyName().equals(etPartyName.getText().toString()) && etPartyName.getText().toString()!=null){
                    updatedFields.put("PartyName", etPartyName.getText().toString());
                }
                if (!dataEntry.getItem().equals(etItem.getText().toString()) && etItem.getText().toString()!=null){
                    updatedFields.put("Item",etItem.getText().toString());
                }
                if (!dataEntry.getWeight().equals(etWeight.getText().toString()) && etWeight.getText().toString()!=null){
                    updatedFields.put("Weight",etWeight.getText().toString());
                }
                if (!dataEntry.getPrice().equals(etPrice.getText().toString()) && etPrice.getText().toString()!=null){
                    updatedFields.put("Price",etPrice.getText().toString());
                }
                if (!dataEntry.getTotal().equals(etTotal.getText().toString()) && etTotal.getText().toString()!=null){
                    updatedFields.put("Total",etTotal.getText().toString());
                }
                if (!dataEntry.getPaymentStatus().equals(selectedPaymentStatus) && selectedPaymentStatus!=null){
                    updatedFields.put("PaymentStatus",selectedPaymentStatus);
                }
                if (!dataEntry.getPaymentIn().equals(etPaymentIn.getText().toString()) && etPaymentIn.getText().toString()!=null){
                    updatedFields.put("PaymentIn",etPaymentIn.getText().toString());
                }
                if (!dataEntry.getPaymentOut().equals(etPaymentOut.getText().toString()) && etPaymentOut.getText().toString()!=null){
                    updatedFields.put("PaymentOut",etPaymentOut.getText().toString());
                }
                Log.d("updatedFields",updatedFields.toString());

                updateDataEntryInFirestore(userID, dataEntry.getEntryId(), updatedFields);
                Toast.makeText(getActivity(),dataEntry.getPaymentStatus(),Toast.LENGTH_SHORT).show();
            }
        });
        editCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(),MainActivity.class);
                startActivity(i);
            }
        });

        return v;
    }
    private String setformatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }



    public void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), (datePicker, selectedYear, selectedMonth, selectedDayOfMonth) -> {
            selectedDate = formatDate(selectedDayOfMonth, selectedMonth + 1, selectedYear);
            EditText editDate = getView().findViewById(R.id.editPageDate);
            editDate.setText(selectedDate);
        }, year, month, dayOfMonth);
        datePickerDialog.show();
    }
    private String formatDate(int day, int month, int year) {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year);
    }

    // Method to update specific fields in a data entry in Firestore
    private void updateDataEntryInFirestore(String userID, String entryID, Map<String, Object> updatedFields) {
        DocumentReference entryRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("DataEntries")
                .document(entryID);

        entryRef.update(updatedFields)
                .addOnSuccessListener(aVoid -> {
                    // Data entry updated successfully
                    Toast.makeText(getActivity(), "Data entry updated successfully", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(getActivity(),MainActivity.class);
                    startActivity(i);
                })
                .addOnFailureListener(e -> {
                    // Error updating data entry
                    Toast.makeText(getActivity(), "Failed to update data entry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




}