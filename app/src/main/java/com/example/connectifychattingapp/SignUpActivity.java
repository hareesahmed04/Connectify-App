package com.example.connectifychattingapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.connectifychattingapp.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    ActivitySignUpBinding binding;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding=ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();

        progressDialog=new ProgressDialog(SignUpActivity.this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("We're Creating Your Account");

        binding.btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(binding.userName.getText().toString().isEmpty() ||
                        binding.userEmail.getText().toString().isEmpty() ||
                        binding.userPassword.getText().toString().isEmpty()){
                    Toast.makeText(SignUpActivity.this, "Enter Complete User Details for Sign Up", Toast.LENGTH_SHORT).show();
                }
                progressDialog.show();
                auth.createUserWithEmailAndPassword
                                (binding.userEmail.getText().toString(),binding.userPassword.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressDialog.dismiss();
                                if (task.isSuccessful()){
                                    Users users=new Users(binding.userName.getText().toString(),binding.userEmail.getText().toString()
                                            ,binding.userPassword.getText().toString());

                                    String id=task.getResult().getUser().getUid();
                                    database.getReference().child("Users").child(id).setValue(users);

                                    Toast.makeText(SignUpActivity.this, "User Created Successfully", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(SignUpActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
            }
        });
        binding.ClickSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SignUpActivity.this,SignInActivity.class);
                startActivity(intent);
            }
        });
        if(auth.getCurrentUser()!=null){
            Intent intent=new Intent(SignUpActivity.this,MainActivity.class);
            startActivity(intent);
        }

    }
}