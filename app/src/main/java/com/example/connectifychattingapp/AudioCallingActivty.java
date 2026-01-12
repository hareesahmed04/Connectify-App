package com.example.connectifychattingapp;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.Date;

public class AudioCallingActivty extends AppCompatActivity {
    ActivityAudioCallingActivtyBinding binding;
    private RtcEngine mRtcEngine;
    String channelId, remoteUserId, remoteUserName, remoteUserProfile;
    boolean isCaller, isMuted = false, isSpeakerOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAudioCallingActivtyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get Data from Intent
        channelId = getIntent().getStringExtra("channelId");
        isCaller = getIntent().getBooleanExtra("isCaller", false);
        remoteUserId = getIntent().getStringExtra("remoteUserId");
        remoteUserName = getIntent().getStringExtra("remoteUserName");
        remoteUserProfile = getIntent().getStringExtra("remoteUserProfile");

        initAgora();
        mRtcEngine.joinChannel(null, channelId, "", 0);

        String nodePath = isCaller ? remoteUserId : FirebaseAuth.getInstance().getUid();

        // SYNC: End call for both or Switch to Video
        FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            // Remote user ended the call
                            saveCallLog();
                            endCallLocally();
                        } else if ("video".equals(snapshot.child("type").getValue(String.class))) {
                            switchToVideo();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
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
            FirebaseDatabase.getInstance().getReference().child("calls")
                    .child(nodePath).child("type").setValue("video");
        });

        binding.btnEndCall.setOnClickListener(v -> {
            // User manually ended the call
            FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath).removeValue();
            saveCallLog();
            endCallLocally();
        });
    }
    private void switchToVideo() {
        if (mRtcEngine != null) { mRtcEngine.leaveChannel(); RtcEngine.destroy(); mRtcEngine = null; }
        Intent intent = new Intent(this, VideoCallActivty.class);
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);
        finish();
    }
    private void initAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = "c25ddcb31adb4cb79078662c3205f6f9";
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> {
                        saveCallLog();
                        endCallLocally();
                    });
                }
            };
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void endCallLocally() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
        finish();
    }
    private void saveCallLog() {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        CallLogModel log = new CallLogModel(
                remoteUserId,
                remoteUserName != null ? remoteUserName : "Unknown User",
                remoteUserProfile != null ? remoteUserProfile : "",
                "audio",
                "ended",
                new Date().getTime()
        );
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(myId)
                .child("CallLogs")
                .push()
                .setValue(log);
    }
}