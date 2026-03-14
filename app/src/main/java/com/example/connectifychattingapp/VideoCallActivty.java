package com.example.connectifychattingapp;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
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
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivty extends AppCompatActivity {
    ActivityVideoCallActivtyBinding binding;
    private RtcEngine mRtcEngine;
    String channelId, remoteUserId, remoteUserName, nodePath;
    boolean isCaller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallActivtyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        channelId = getIntent().getStringExtra("channelId");
        remoteUserId = getIntent().getStringExtra("remoteUserId");
        remoteUserName = getIntent().getStringExtra("remoteUserName");
        isCaller = getIntent().getBooleanExtra("isCaller", false);
        nodePath = isCaller ? remoteUserId : FirebaseAuth.getInstance().getUid();

        binding.tvName.setText(remoteUserName);
        initAgoraAndJoin();

        binding.btnEndCall.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath).removeValue();
            releaseAndFinish();
        });

        binding.btnSwitchCamera.setOnClickListener(v -> {
            if (mRtcEngine != null) mRtcEngine.switchCamera();
        });

        FirebaseDatabase.getInstance().getReference().child("calls").child(nodePath)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) { if (!snapshot.exists()) releaseAndFinish(); }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void initAgoraAndJoin() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = "e9a90ec5b39546e4a5b41f585c42ebf4";
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() -> setupRemoteVideo(uid));
                }
                @Override
                public void onUserOffline(int uid, int reason) { runOnUiThread(() -> releaseAndFinish()); }
            };
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.enableVideo();
            mRtcEngine.setEnableSpeakerphone(true);
            setupLocalVideo();
            mRtcEngine.joinChannel(null, channelId, "", 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        binding.localVideoViewContainer.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        if (binding.remoteVideoViewContainer.getChildCount() > 0) binding.remoteVideoViewContainer.removeAllViews();
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        binding.remoteVideoViewContainer.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void releaseAndFinish() {
        if (mRtcEngine != null) { mRtcEngine.leaveChannel(); RtcEngine.destroy(); }
        finish();
    }
}