package com.example.connectifychattingapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.connectifychattingapp.databinding.ActivitySignInBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class SignInActivity extends AppCompatActivity {
    ActivitySignInBinding binding;
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth auth;
    ProgressDialog progressDialog;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(SignInActivity.this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Login to Your Account");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                if (binding.LoginEmail.getText().toString().isEmpty() || binding.LoginPassword.getText().toString().isEmpty()) {
                    Toast.makeText(SignInActivity.this, "Enter Complete User Details to Login", Toast.LENGTH_SHORT).show();
                }
                auth.signInWithEmailAndPassword(binding.LoginEmail.getText().toString(), binding.LoginPassword.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(SignInActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

        });
        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignIn();
            }
        });

        binding.ClickSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
    int RC_SIGN_IN=65;
    private void SignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is from the Google Sign-In intent
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from getSignInIntent() is always completed,
            // but we need to extract the GoogleSignInAccount from the intent.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);

                // Show progress dialog while authenticating
                progressDialog.setTitle("Google Sign-In");
                progressDialog.setMessage("Authenticating with Firebase...");
                progressDialog.show();

                // Pass the Google account's ID token to Firebase
                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                progressDialog.dismiss();
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("TAG", "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();

                        Users users=new Users();
                        users.setUserId(user.getUid());
                        users.setusername(user.getDisplayName());
                        users.setProfilePic(user.getPhotoUrl().toString());
                        database.getReference().child("Users").child(user.getUid()).setValue(user);

                        Intent intent=new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                        Toast.makeText(this, "Sign in with Google", Toast.LENGTH_SHORT).show();
//                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user
                        Log.w("TAG", "signInWithCredential:failure", task.getException());
//                        updateUI(null);
                    }
                });
    }
}
