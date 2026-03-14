package com.example.connectifychattingapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.connectifychattingapp.databinding.ActivityChatDetailBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ChatDetailActivity extends AppCompatActivity {
    ActivityChatDetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    ArrayList<MessageModel> messageModels;
    ChatAdapter chatAdapter;
    String receiverId, senderId, receiverName, receiverProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding =ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the status bar background to black
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        // Ensure the icons are white/light so they are visible on the black background
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        // 2. Navigation Bar Setup (Black background, white icons)
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        windowInsetsController.setAppearanceLightNavigationBars(false);


        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();

        final String senderId=auth.getUid();

        receiverId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("userName");
        receiverProfile = getIntent().getStringExtra("profilePic");
        binding.userNameChats.setText(receiverName);
        Picasso.get().load(receiverProfile).placeholder(R.drawable.user1).into(binding.profileImage);

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ChatDetailActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // --- Video Call Button Logic ---
        binding.btnVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateCall("video");
            }
        });

        // --- Audio Call Button Logic ---
        binding.btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateCall("audio");
            }
        });
          messageModels=new ArrayList<>();
         chatAdapter=new ChatAdapter(messageModels,this);
        binding.chatRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        final String senderRoom= senderId + receiverId;
        final String receiverRoom= receiverId + senderId;

        database.getReference().child("chats")
                        .child(senderRoom)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        messageModels.clear();
                                    for (DataSnapshot snapshot1 : snapshot.getChildren()){
                                        MessageModel model=snapshot1.getValue(MessageModel.class);
                                        messageModels.add(model);
                                    }
                                    chatAdapter.notifyDataSetChanged();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

        binding.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = binding.etMessage.getText().toString();
                final MessageModel model = new MessageModel(senderId, message);
                model.setTimestamp(new Date().getTime());
                binding.etMessage.setText("");

                    database.getReference().child("chats")
                            .child(senderRoom)
                            .push()
                            .setValue(model)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                }
                            });

                    database.getReference().child("chats")
                            .child(receiverRoom)
                            .push()
                            .setValue(model)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {

                                }
                            });
                }
        });
        binding.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(ChatDetailActivity.this, v);
                popupMenu.getMenuInflater().inflate(R.menu.chat_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.clearChat) {
                            showClearChatDialog(senderRoom);
                            return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }
    private void showClearChatDialog(String senderRoom) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Chat")
                .setMessage("This will delete all messages in this conversation.")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 1. Delete messages from Firebase
                        database.getReference().child("chats")
                                .child(senderRoom)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        // 2. Clear the local list
                                        messageModels.clear();
                                        chatAdapter.notifyDataSetChanged();
                                        Toast.makeText(ChatDetailActivity.this, "Chat cleared", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        // 2. Show it
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.blue));

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }
    private void initiateCall(String type) {
        String senderId = FirebaseAuth.getInstance().getUid();
        String channelId = senderId + "_" + receiverId;

        // 1. Fetch CURRENT user's data (The Caller)
        database.getReference().child("Users").child(senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Get actual name and pic from the snapshot
                        String myRealName = snapshot.child("userName").getValue(String.class);
                        String myRealProfile = snapshot.child("profilePic").getValue(String.class);

                        // Default to "User" only if the database name is actually empty
                        if (myRealName == null || myRealName.isEmpty()) myRealName = "Connectify User";

                        HashMap<String, Object> callData = new HashMap<>();
                        callData.put("callerId", senderId);
                        callData.put("callerName", myRealName); // This will now be the real name
                        callData.put("callerPic", myRealProfile != null ? myRealProfile : "");
                        callData.put("type", type);
                        callData.put("channelId", channelId);
                        callData.put("status", "ringing");

                        // 2. Only after we have the name, notify the receiver
                        database.getReference().child("calls").child(receiverId).setValue(callData)
                                .addOnSuccessListener(unused -> {
                                    Intent intent;
                                    if ("video".equals(type)) {
                                        intent = new Intent(ChatDetailActivity.this, VideoCallActivty.class);
                                    } else {
                                        intent = new Intent(ChatDetailActivity.this, AudioCallingActivty.class);
                                    }

                                    // CALLER sees RECEIVER'S info
                                    intent.putExtra("channelId", channelId);
                                    intent.putExtra("isCaller", true);
                                    intent.putExtra("remoteUserId", receiverId);
                                    intent.putExtra("remoteUserName", receiverName);
                                    intent.putExtra("remoteUserProfile", receiverProfile);
                                    startActivity(intent);
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatDetailActivity.this, "Call failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}