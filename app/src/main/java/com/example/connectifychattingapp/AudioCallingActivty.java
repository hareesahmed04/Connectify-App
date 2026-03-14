package com.example.connectifychattingapp;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.connectifychattingapp.databinding.ActivityAudioCallingActivtyBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class AudioCallingActivty extends AppCompatActivity {
    ActivityAudioCallingActivtyBinding binding;
    private RtcEngine mRtcEngine;
    String channelId, remoteUserId, remoteUserName, nodePath;
    boolean isCaller, isMuted = false, isSpeakerOn = false;
    private MediaPlayer bellPlayer;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAudioCallingActivtyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        channelId = getIntent().getStringExtra("channelId");
        isCaller = getIntent().getBooleanExtra("isCaller", false);
        remoteUserId = getIntent().getStringExtra("remoteUserId");
        remoteUserName = getIntent().getStringExtra("remoteUserName");
        nodePath = isCaller ? remoteUserId : FirebaseAuth.getInstance().getUid();

        binding.tvName.setText(remoteUserName);
        if (isCaller) { playCallingBell(); startTimeoutTimer(); }

        initAgora();
        setupFirebaseSync();

        binding.btnEndCall.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath).removeValue();
            releaseAndFinish();
        });

        binding.btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            mRtcEngine.muteLocalAudioStream(isMuted);
            binding.btnMute.setAlpha(isMuted ? 0.5f : 1.0f);
        });

        binding.btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            mRtcEngine.setEnableSpeakerphone(isSpeakerOn);
            binding.btnSpeaker.setAlpha(isSpeakerOn ? 1.0f : 0.5f);
        });

        binding.btnVideo.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath).child("type").setValue("video");
            navigateToVideo();
        });
    }

    private void startTimeoutTimer() {
        timeoutRunnable = () -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath).removeValue();
            releaseAndFinish();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 60000);
    }

    private void initAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = "e9a90ec5b39546e4a5b41f585c42ebf4";
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() -> {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        stopRingtone();
                        binding.tvStatus.setText("Connected");
                    });
                }
                @Override
                public void onUserOffline(int uid, int reason) { runOnUiThread(() -> releaseAndFinish()); }
            };
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.enableAudio();
            mRtcEngine.joinChannel(null, channelId, "", 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupFirebaseSync() {
        FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) releaseAndFinish();
                        else if ("video".equals(snapshot.child("type").getValue(String.class))) navigateToVideo();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void navigateToVideo() {
        Intent intent = new Intent(this, VideoCallActivty.class);
        intent.putExtra("channelId", channelId);
        intent.putExtra("remoteUserId", remoteUserId);
        intent.putExtra("remoteUserName", remoteUserName);
        intent.putExtra("isCaller", isCaller);
        startActivity(intent);
        releaseAndFinish();
    }

    private void playCallingBell() { bellPlayer = MediaPlayer.create(this, R.raw.ringtone); bellPlayer.setLooping(true); bellPlayer.start(); }
    private void stopRingtone() { if (bellPlayer != null) { bellPlayer.stop(); bellPlayer.release(); bellPlayer = null; } }
    private void releaseAndFinish() { stopRingtone(); timeoutHandler.removeCallbacks(timeoutRunnable); if (mRtcEngine != null) { mRtcEngine.leaveChannel(); RtcEngine.destroy(); } finish(); }
}