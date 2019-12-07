package com.project.userapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    EditText txt_email, txt_password;
    TextView txt_error;
    Button btn_login;
    ProgressBar progressBar;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        txt_email = findViewById(R.id.email);
        txt_error = findViewById(R.id.error);
        txt_password = findViewById(R.id.pass);
        btn_login = findViewById(R.id.login);
        progressBar = findViewById(R.id.loading);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                btn_login.setEnabled(false);
                final String email = txt_email.getText().toString();
                final String password = txt_password.getText().toString();
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "Logged In Successfully",Toast.LENGTH_LONG);
                    user = mAuth.getCurrentUser();
                    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
                    firebaseFirestore.collection("user_master").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()){
                                DocumentSnapshot data = task.getResult();
                                if (data.get("type").toString().equals("Driver")){
                                    mAuth.signOut();
                                    btn_login.setEnabled(true);
                                    txt_error.setText("WRONG APP.. Please use driver app for this email.");
                                    progressBar.setVisibility(View.GONE);
                                }else{
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                            else{
                                Log.d("Tag", task.getException().toString());
                            }
                        }
                    });
                }else{
                    Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_LONG).show();
                    txt_error.setText("Invalid Credentials.. Please LogIn Again");
                    Log.d("Login",task.getException().toString());
                    btn_login.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    });
    }
}
