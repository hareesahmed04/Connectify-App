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
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("This will delete all messages in this conversation. The user will remain in your chat list.")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // 1. Delete messages from Firebase for the current user (senderRoom)
                        database.getReference().child("chats")
                                .child(senderRoom)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        // 2. Clear the local list so the UI updates immediately
                                        messageModels.clear();
                                        chatAdapter.notifyDataSetChanged();
                                        Toast.makeText(ChatDetailActivity.this, "Chat cleared", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void initiateCall(String type) {
        // Create a unique channel ID for Agora
        String channelId = senderId + "_" + receiverId + "_" + System.currentTimeMillis();

        // Prepare data for the receiver to listen to
        HashMap<String, Object> callData = new HashMap<>();
        callData.put("callerId", senderId);
        callData.put("callerName", "Friend"); // Ideally pass your own name from user data
        callData.put("callerPic", "");      // Pass your own profile pic URL
        callData.put("type", type);
        callData.put("channelId", channelId);
        callData.put("status", "ringing");

        // Notify receiver via Firebase "calls" node
        database.getReference().child("calls").child(receiverId).setValue(callData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Launch the caller's interface
                        Intent intent;
                        if ("video".equals(type)) {
                            intent = new Intent(ChatDetailActivity.this, VideoCallActivty.class);
                        } else {
                            intent = new Intent(ChatDetailActivity.this, AudioCallingActivty.class);
                        }

                        intent.putExtra("channelId", channelId);
                        intent.putExtra("isCaller", true);
                        intent.putExtra("remoteUserId", receiverId);
                        intent.putExtra("remoteUserName", receiverName);
                        intent.putExtra("remoteUserProfile", receiverProfile);
                        startActivity(intent);
                    }
                });
    }
}