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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterPage extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    EditText enterPhonenumber, enterFullname, enterOTP, enterBusinessName;
    TextView loginTXT;
    Button registerBtn;
    String phoneNumber,verificationCode,fullName,businessName;
    private String storedVerificationId;
    private ProgressDialog progressDialog;
    Double AccountTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);
        enterPhonenumber = findViewById(R.id.rpPhoneNumber);
        enterFullname = findViewById(R.id.rpFullname);
        enterBusinessName = findViewById(R.id.rpBusinessName);
        enterOTP = findViewById(R.id.rpOTP);
        loginTXT = findViewById(R.id.rpLogin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        registerBtn = findViewById(R.id.rpRegister);
        verificationCode = "";

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        loginTXT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RegisterPage.this,LoginPage.class);
                startActivity(i);
            }
        });


        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verificationCode = enterOTP.getText().toString();
                phoneNumber = "+91" + enterPhonenumber.getText().toString();
                businessName = enterBusinessName.getText().toString();
                fullName = enterFullname.getText().toString();

                if (verificationCode.equals("")) {
                    signInWithOutOTP(phoneNumber);
                    progressDialog.show();
                } else {
                    verificationCode = enterOTP.getText().toString();
                    signInWithOTP(verificationCode, storedVerificationId);
                    progressDialog.show();
                }
            }
        });
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
                        Toast.makeText(RegisterPage.this,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                        Log.e("TAG",e.getMessage().toString());

                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        // Save the verification ID and token to use in the signInWithPhoneAuthCredential method
                        // You can store it in shared preferences or any other way that suits your app's architecture
                        Toast.makeText(RegisterPage.this,"Otpsent",Toast.LENGTH_SHORT).show();
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
                            FirebaseUser user = mAuth.getCurrentUser();
                            String userID = user.getUid();
                            if (userID.equals("PU1wIHrK9SSNLGBBu0q2X9qG4YH3")){
                                userID = "qrHu0VZLRgVZSXWCnLj2879bo9l1";
                            }
                            else if (userID.equals("dPzD4OoMq8OhE74aJJSqLPGnKmZ2")){
                                userID = "ozmPLi62YQfPAyorq99QLdA6zjl1";
                            }

                            //Putting user data on fireStore
                            DocumentReference documentReference =db.collection("ProfileCollection").document(userID);
                            Map<String, Object> userData =new HashMap<>();
                            userData.put("BusinessName",businessName);
                            userData.put("FullName",fullName);
                            userData.put("PhoneNo",phoneNumber);
                            userData.put("AccountTotal",AccountTotal);
                            String finalUserID = userID;
                            documentReference.set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("storedBusinessName", businessName);
                                    editor.putString("storedFullName", fullName);
                                    editor.putString("storedPhoneNo", phoneNumber);
                                    editor.putString("storedUserId", finalUserID);
                                    editor.commit();
                                    Toast.makeText(RegisterPage.this,"Login Successful",Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(RegisterPage.this,MainActivity.class);
                                    startActivity(i);
                                    progressDialog.show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(RegisterPage.this,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                                    Log.e("TAG",e.getMessage().toString());
                                }
                            });


                            // Proceed with your app's logic for signed-in users
                        } else {
                            // Sign-in failed
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // Handle invalid verification code
                                Toast.makeText(RegisterPage.this,"Wrong OTP",Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            } else {
                                // Handle other errors
                                Toast.makeText(RegisterPage.this,task.getException().getMessage().toString(),Toast.LENGTH_SHORT).show();
                                Log.e("TAG",task.getException().getMessage().toString());
                                progressDialog.dismiss();
                            }
                        }
                    }
                });
    }
}