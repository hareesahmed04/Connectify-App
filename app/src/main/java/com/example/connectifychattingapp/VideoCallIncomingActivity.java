package com.example.connectifychattingapp;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
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
    private String channelId, callerId, callerName, callerPic;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        }
        binding = ActivityVideoCallIncomingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        callerName = getIntent().getStringExtra("callerName");
        callerId = getIntent().getStringExtra("callerId");
        channelId = getIntent().getStringExtra("channelId");
        callerPic = getIntent().getStringExtra("callerPic");

        binding.tvCallerName.setText(callerName);
        playRingtone();

        FirebaseDatabase.getInstance().getReference().child("calls").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) { if (!snapshot.exists()) cleanupAndFinish(); }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        binding.btnAnswerVideo.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(FirebaseAuth.getInstance().getUid()).child("status").setValue("picked");
            Intent intent = new Intent(this, VideoCallActivty.class);
            intent.putExtra("channelId", channelId);
            intent.putExtra("remoteUserId", callerId);
            intent.putExtra("remoteUserName", callerName);
            intent.putExtra("isCaller", false);
            startActivity(intent);
            cleanupAndFinish();
        });

        binding.btnDeclineVideo.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(FirebaseAuth.getInstance().getUid()).removeValue();
            cleanupAndFinish();
        });
    }

    private void playRingtone() {
        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void cleanupAndFinish() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.release(); mediaPlayer = null; }
        finish();
    }
}