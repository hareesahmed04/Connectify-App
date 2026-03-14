package com.example.connectifychattingapp;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.connectifychattingapp.databinding.ActivityAudioIncomingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AudioIncomingActivity extends AppCompatActivity {
    ActivityAudioIncomingBinding binding;
    String callerId, channelId, callerName, callerProfile;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Wake up screen logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        }
        binding = ActivityAudioIncomingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        callerName = getIntent().getStringExtra("callerName");
        callerId = getIntent().getStringExtra("callerId");
        channelId = getIntent().getStringExtra("channelId");
        callerProfile = getIntent().getStringExtra("callerPic");

        binding.tvCallerName.setText(callerName);
        Glide.with(this).load(callerProfile).placeholder(R.drawable.user1).into(binding.profileImage);

        playRingtone();

        // Auto-finish if caller cancels
        FirebaseDatabase.getInstance().getReference().child("calls").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) cleanupAndFinish();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        binding.btnAnswer.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(FirebaseAuth.getInstance().getUid()).child("status").setValue("picked");
            Intent intent = new Intent(this, AudioCallingActivty.class);
            intent.putExtra("channelId", channelId);
            intent.putExtra("remoteUserId", callerId);
            intent.putExtra("remoteUserName", callerName);
            intent.putExtra("isCaller", false);
            startActivity(intent);
            cleanupAndFinish();
        });

        binding.btnDecline.setOnClickListener(v -> {
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