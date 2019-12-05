package com.project.userapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    TextView name, email, phone, pass;
    Button reg;
    ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private static final String TAG = "RegistrationActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        progressBar = findViewById(R.id.progress);
        firebaseAuth = FirebaseAuth.getInstance();
        name = findViewById(R.id.inp_Regname);
        phone = findViewById(R.id.inp_RegPhn);
        email = findViewById(R.id.inp_Regemail);
        pass = findViewById(R.id.inp_Regpass);
        reg = findViewById(R.id.register);
        reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(name.getText().toString()) || TextUtils.isEmpty(email.getText().toString()) || TextUtils.isEmpty(phone.getText().toString()) || TextUtils.isEmpty(pass.getText().toString())) {
                    Toast.makeText(RegistrationActivity.this, "No Empty Fields Allowed", Toast.LENGTH_LONG).show();
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    firebaseAuth.createUserWithEmailAndPassword(email.getText().toString(), pass.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "onComplete: Creation Success");
                                AuthResult authResult = task.getResult();
                                FirebaseUser firebaseUser = authResult.getUser();
                                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name.getText().toString()).build();
                                firebaseUser.updateProfile(userProfileChangeRequest);
                                Log.d(TAG, "onComplete: "+firebaseUser.getDisplayName());
                                Map<String,String> map = new HashMap<>();
                                map.put("user_name",name.getText().toString());
                                map.put("user_email",email.getText().toString());
                                map.put("type","User");
                                map.put("user_pass",pass.getText().toString());
                                map.put("user_phone",phone.getText().toString());
                                FirebaseFirestore.getInstance().collection("user_master").document(firebaseUser.getUid()).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            progressBar.setVisibility(View.GONE);Toast.makeText(RegistrationActivity.this, "User Registered", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegistrationActivity.this,MainActivity.class));
                                            finish();
                                        }else{
                                            Log.d(TAG, "onComplete: "+task.getException().toString());
                                        }
                                    }
                                });
                            }
                            else{
                                Log.d(TAG, "onComplete: EXECPTION" + task.getException().toString());
                            }
                        }
                    });
                }
            }
        });
    }
    /*boolean ifEmailExists(String email){

    }*/
}