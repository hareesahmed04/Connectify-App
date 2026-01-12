package com.example.connectifychattingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.connectifychattingapp.databinding.ActivityAudioIncomingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AudioIncomingActivity extends AppCompatActivity {
    ActivityAudioIncomingBinding binding;
    String callerId, channelId, callerName;
    FirebaseAuth auth;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAudioIncomingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        callerName = getIntent().getStringExtra("callerName");
        callerId = getIntent().getStringExtra("callerId");
        channelId = getIntent().getStringExtra("channelId");

        binding.tvCallerName.setText(callerName);

        // SYNC LISTENER: If caller cancels, close this screen
        database.getReference().child("calls").child(auth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            finish(); // Caller hung up
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        binding.btnAnswer.setOnClickListener(v -> {
            database.getReference().child("calls").child(auth.getUid()).child("status").setValue("picked");
            Intent intent = new Intent(AudioIncomingActivity.this, AudioCallingActivty.class);
            intent.putExtra("channelId", channelId);
            intent.putExtra("isCaller", false);
            intent.putExtra("remoteUserId", callerId);
            intent.putExtra("remoteUserName", callerName);
            startActivity(intent);
            finish();
        });

        binding.btnDecline.setOnClickListener(v -> {
            database.getReference().child("calls").child(auth.getUid()).removeValue();
            finish();
        });
    }
}