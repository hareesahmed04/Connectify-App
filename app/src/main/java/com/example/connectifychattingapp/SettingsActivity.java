package com.example.connectifychattingapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.connectifychattingapp.databinding.ActivitySettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class SettingsActivity extends AppCompatActivity {
    ActivitySettingsBinding binding;
    private SwitchCompat darkModeSwitch;
    FirebaseAuth auth;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        windowInsetsController.setAppearanceLightNavigationBars(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // 1. Fetch and Show User Data
        database.getReference().child("Users").child(auth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users user = snapshot.getValue(Users.class);
                        if (user != null) {
                            // Set Name (Ensure the ID matches your XML)
                            binding.userNameSettings.setText(user.getusername());
                            // Set Image using Picasso
                            if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                                Picasso.get()
                                        .load(user.getProfilePic())
                                        .placeholder(R.drawable.user1)
                                        .into(binding.profileImage);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });

        binding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        binding.accountsSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, Accounts_Profile_Activity.class);
                startActivity(intent);
            }
        });
    }
}
