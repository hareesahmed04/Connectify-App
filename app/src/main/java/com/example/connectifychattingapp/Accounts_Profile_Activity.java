package com.example.connectifychattingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.connectifychattingapp.databinding.ActivityAccountsProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.HashMap;

public class Accounts_Profile_Activity extends AppCompatActivity {
    ActivityAccountsProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    String dbPassword; // Stores current DB password for verification

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountsProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // 1. Load Data from Firebase initially
        loadUserData();
        // 2. Pencil Click: Enable First Name field
        binding.editFirstName.setOnClickListener(v -> {
            binding.etFirstName.setEnabled(true);
            binding.etFirstName.requestFocus();
            showKeyboard(binding.etFirstName);
        });
        // 3. Password Pencil: Trigger secure Dialog
        binding.editPassword.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showPasswordDialog();
            }
        });
        // 4. Save Button: Update Firebase
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
            }
        });
        binding.btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAccountDialog();
            }
        });
        // Back Navigation
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    private void loadUserData() {
        database.getReference().child("Users").child(auth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users user = snapshot.getValue(Users.class);
                        if (user != null) {
                            // Map database 'username' to the First Name field
                            binding.etFirstName.setText(user.getusername());
                            binding.etEmail.setText(user.getMail());
                            binding.etEmail.setEnabled(false);

                            // Mask the password display
                            binding.etPassword.setText("********");
                            dbPassword = user.getPassword(); // Store actual password

                            if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                                Picasso.get().load(user.getProfilePic())
                                        .placeholder(R.drawable.user1)
                                        .into(binding.editProfileImage);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Accounts_Profile_Activity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateProfile() {
        String updatedName = binding.etFirstName.getText().toString();
        if (updatedName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("username", updatedName);

        database.getReference().child("Users").child(auth.getUid())
                .updateChildren(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    binding.etFirstName.setEnabled(false); // Lock the field again
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
    }
    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_password_dialog, null);
        builder.setView(dialogView);

        EditText etOldPass = dialogView.findViewById(R.id.oldPass);
        EditText etNewPass = dialogView.findViewById(R.id.newPass);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String inputOld = etOldPass.getText().toString();
            String inputNew = etNewPass.getText().toString();

            if (inputOld.equals(dbPassword)) {
                if (!inputNew.isEmpty()) {
                    database.getReference().child("Users").child(auth.getUid())
                            .child("password").setValue(inputNew)
                            .addOnSuccessListener(unused -> {
                                dbPassword = inputNew;
                                Toast.makeText(this, "Password Changed", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Enter a new password", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Incorrect Current Password", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }
    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    // Method to show verification dialog for deletion
    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_password_dialog, null);
        builder.setView(dialogView);

        // Customize the dialog UI for deletion
        EditText etConfirmPass = dialogView.findViewById(R.id.oldPass);
        EditText etNewPass = dialogView.findViewById(R.id.newPass);

        // We don't need the "New Password" field for deletion
        etNewPass.setVisibility(View.GONE);
        etConfirmPass.setHint("Enter Password to Confirm Deletion");

        builder.setTitle("Delete Account Permanently?")
                .setMessage("This action cannot be undone. All your data will be erased.")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    String inputPass = etConfirmPass.getText().toString();

                    if (inputPass.equals(dbPassword)) {
                        deleteUserPermanently();
                    } else {
                        Toast.makeText(this, "Incorrect Password!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void deleteUserPermanently() {
        String uid = auth.getUid();

        // 1. Delete from Realtime Database
        database.getReference().child("Users").child(uid)
                .removeValue()
                .addOnSuccessListener(unused -> {

                    // 2. Delete from Firebase Authentication
                    if (auth.getCurrentUser() != null) {
                        auth.getCurrentUser().delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(Accounts_Profile_Activity.this, "Account Deleted", Toast.LENGTH_SHORT).show();

                                        // 3. Move to SignUpActivity
                                        Intent intent = new Intent(Accounts_Profile_Activity.this, SignUpActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(Accounts_Profile_Activity.this, "Auth Deletion Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}