package com.example.connectifychattingapp;

import android.os.Bundle;
import android.view.SurfaceView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.connectifychattingapp.databinding.ActivityVideoCallActivtyBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import java.util.Date;

public class VideoCallActivty extends AppCompatActivity {
    ActivityVideoCallActivtyBinding binding;
    private RtcEngine mRtcEngine;
    String channelId, remoteUserId, remoteUserName, remoteUserProfile;
    boolean isCaller, isMuted = false, isSpeakerOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallActivtyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        channelId = getIntent().getStringExtra("channelId");
        isCaller = getIntent().getBooleanExtra("isCaller", false);
        remoteUserId = getIntent().getStringExtra("remoteUserId");
        remoteUserName = getIntent().getStringExtra("remoteUserName");
        remoteUserProfile = getIntent().getStringExtra("remoteUserProfile");

        initAgora();
        setupLocalVideo();
        mRtcEngine.joinChannel(null, channelId, "", 0);

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

        String nodePath = isCaller ? remoteUserId : FirebaseAuth.getInstance().getUid();

        // SYNC LISTENER
        FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            saveCallLog();
                            releaseAndFinish();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        binding.btnEndCall.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath).removeValue();
            saveCallLog();
            releaseAndFinish();
        });
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        binding.videoBackground.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void initAgora() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), "c25ddcb31adb4cb79078662c3205f6f9", new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() -> {
                        SurfaceView surfaceView = new SurfaceView(getBaseContext());
                        binding.videoBackground.addView(surfaceView);
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                    });
                }
                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> {
                        saveCallLog();
                        releaseAndFinish();
                    });
                }
            });
            mRtcEngine.enableVideo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseAndFinish() {
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
                "video",
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