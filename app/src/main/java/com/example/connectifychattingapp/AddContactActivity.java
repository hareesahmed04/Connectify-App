package com.example.connectifychattingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.connectifychattingapp.databinding.ActivityAddContactBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class AddContactActivity extends AppCompatActivity {
    ActivityAddContactBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        binding.backCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(AddContactActivity.this,MainActivity.class);
                startActivity(intent);

            }
        });
        binding.btnSave.setOnClickListener(v -> {
            String emailInput = binding.etEmail.getText().toString().trim(); // Assuming you use this field for email
            String FirstName = binding.etFirstName.getText().toString().trim();
            String LastName = binding.etLastName.getText().toString().trim();

            if (TextUtils.isEmpty(emailInput) || TextUtils.isEmpty(FirstName) || TextUtils.isEmpty(LastName)) {
                Toast.makeText(this, "Please enter both Name and Email", Toast.LENGTH_SHORT).show();
                return;
            }

            searchAndAddUser(emailInput, FirstName , LastName);
        });
    }
    private void searchAndAddUser(String email, String firstName ,String lastName) {
        // Query to find user by email in the main "Users" node
        Query query = database.getReference().child("Users").orderByChild("mail").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Users foundUser = data.getValue(Users.class);
                        String foundUserId = data.getKey();

                        if (foundUserId.equals(auth.getUid())) {
                            Toast.makeText(AddContactActivity.this, "You cannot add yourself", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Create a new User object for YOUR contact list
                        // Using the name YOU gave, but the PIC from their profile
                        Users newContact = new Users();
                        newContact.setusername(firstName + " " + lastName);
                        newContact.setProfilePic(foundUser.getProfilePic());
                        newContact.setUserId(foundUserId);

                        // Save into Users -> CurrentUserID -> Contacts -> FoundUserID
                        database.getReference().child("Users")
                                .child(auth.getUid())
                                .child("Contacts")
                                .child(foundUserId)
                                .setValue(newContact)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(AddContactActivity.this, "Contact Added", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    }
                } else {
                    Toast.makeText(AddContactActivity.this, "No user found with this email", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}