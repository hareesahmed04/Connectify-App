package com.example.connectifychattingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.connectifychattingapp.databinding.ActivityVideoCallIncomingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class VideoCallIncomingActivity extends AppCompatActivity {
    ActivityVideoCallIncomingBinding binding;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallIncomingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // SYNC LISTENER
        FirebaseDatabase.getInstance().getReference().child("calls").child(auth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) finish();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        binding.btnAnswerVideo.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(auth.getUid()).child("status").setValue("picked");
            Intent intent = new Intent(this, VideoCallActivty.class);
            intent.putExtras(getIntent().getExtras());
            intent.putExtra("isCaller", false);
            startActivity(intent);
            finish();
        });

        binding.btnDeclineVideo.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(auth.getUid()).removeValue();
            finish();
        });
    }
}