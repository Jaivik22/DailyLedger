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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

public class LoginPage extends AppCompatActivity {
    private FirebaseAuth mAuth;
    EditText enterPhoneNo, enterOTP;
    TextView RegisterTXT;

    Button loginSubmitBtn;

    String phoneNumber,verificationCode;
    private String storedVerificationId;
    private ProgressDialog progressDialog;

    String storedUserId,storedBusinessName, storedFullName, storedPhoneNo,storedAccountTotal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);
        mAuth = FirebaseAuth.getInstance();

        enterPhoneNo = findViewById(R.id.lphoneNumber);
        enterOTP = findViewById(R.id.lpOTP);
        RegisterTXT = findViewById(R.id.lpRegister);

        loginSubmitBtn = findViewById(R.id.lpLogin);
        verificationCode = "";



        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        RegisterTXT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LoginPage.this,RegisterPage.class);
                startActivity(i);
            }
        });

        loginSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verificationCode = enterOTP.getText().toString();
                phoneNumber = "+91" + enterPhoneNo.getText().toString();
                checkPhoneNumberExists(phoneNumber);

            }
        });


    }
    private void checkPhoneNumberExists(final String phoneNumber) {
        final boolean[] flag = {false};
        CollectionReference collectionRef = FirebaseFirestore.getInstance().collection("ProfileCollection");
        Query query = collectionRef.whereEqualTo("PhoneNo", phoneNumber);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Phone number exists in the database, proceed with login
//                        Toast.makeText(LoginPage.this, "Phone number exists", Toast.LENGTH_SHORT).show();
                        // TODO: Implement your login logic here
                        flag[0] = true;

                    } else {
                        // Phone number does not exist in the database, redirect to registration page
//                        Toast.makeText(LoginPage.this, "Phone number does not exist", Toast.LENGTH_SHORT).show();

                        flag[0] = false;
                    }
                } else {
                    // Error occurred while querying Firestore
                    if (task.getException() != null) {
                        Toast.makeText(LoginPage.this, task.getException().getMessage().toString(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginPage.this, "An error occurred.", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("TAG", task.getException().getMessage());
                }
                handlePhoneNumberExists(flag[0]);
            }
        });

    }
    private void handlePhoneNumberExists(boolean phoneNumberExists) {
        if (phoneNumberExists) {
            // Phone number exists, proceed with login
            if (verificationCode.equals("")) {
                signInWithOutOTP(phoneNumber);
                progressDialog.show();
            } else {
                verificationCode = enterOTP.getText().toString();
                signInWithOTP(verificationCode, storedVerificationId);
                progressDialog.show();
            }
        } else {
            // Phone number does not exist, redirect to registration page
            Intent intent = new Intent(LoginPage.this, RegisterPage.class);
            startActivity(intent);
            Toast.makeText(LoginPage.this, "User does not exist\nRegister first", Toast.LENGTH_SHORT).show();
        }
    }

    private void signInWithOutOTP(String phoneNumber){
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // This callback will be invoked in case of instant verification.
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        // Handle verification failure
                        Toast.makeText(LoginPage.this,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                        Log.e("TAG",e.getMessage().toString());

                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        // Save the verification ID and token to use in the signInWithPhoneAuthCredential method
                        // You can store it in shared preferences or any other way that suits your app's architecture
                        Toast.makeText(LoginPage.this,"Otpsent",Toast.LENGTH_SHORT).show();
                        storedVerificationId = verificationId;
                        PhoneAuthProvider.ForceResendingToken storedToken = token;
                        progressDialog.dismiss();

                        enterOTP.setVisibility(View.VISIBLE);

                    }

                }
        );
    }

    private void signInWithOTP(String verificationCode,String storedVerificationId){

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(storedVerificationId, verificationCode);
        signInWithPhoneAuthCredential(credential);

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign-in successful
                            storeUserDetails();
                            // Proceed with your app's logic for signed-in users
                        } else {
                            // Sign-in failed
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // Handle invalid verification code
                                Toast.makeText(LoginPage.this,"Wrong OTP",Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            } else {
                                // Handle other errors
                                Toast.makeText(LoginPage.this,task.getException().getMessage().toString(),Toast.LENGTH_SHORT).show();
                                Log.e("TAG",task.getException().getMessage().toString());
                                progressDialog.dismiss();
                            }
                        }
                    }
                });
    }

    public void storeUserDetails(){
        FirebaseUser user = mAuth.getCurrentUser();
        storedUserId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection("ProfileCollection").document(storedUserId);

        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // User data found, access it here
                        storedBusinessName = document.getString("BusinessName");
                        storedFullName = document.getString("FullName");
                        storedPhoneNo = document.getString("PhoneNo");
//                        storedAccountTotal  =document.getString("AccountTotal");
                        Toast.makeText(LoginPage.this,"Login Successful",Toast.LENGTH_SHORT).show();


                        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("storedBusinessName", storedBusinessName);
                        editor.putString("storedFullName", storedFullName);
                        editor.putString("storedPhoneNo", storedPhoneNo);
//                        editor.putString("storedAccountTotal", storedAccountTotal);
                        editor.putString("storedUserId", storedUserId);
                        editor.commit();


                        Intent i = new Intent(LoginPage.this,MainActivity.class);
                        startActivity(i);
                        progressDialog.show();

                    } else {
                        // User data not found
                        Log.d("Firestore", "User data not found");
                    }
                } else {
                    // Error occurred while retrieving data
                    Log.e("Firestore", "Error: " + task.getException().getMessage());
                }
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        progressDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();

    }
}