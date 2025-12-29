package com.example.connectifychattingapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.connectifychattingapp.databinding.FragmentChatBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatFragment extends Fragment {

    public ChatFragment() {
        // Required empty public constructor
    }

    FragmentChatBinding binding;
    ArrayList<Users> list = new ArrayList<>();
    FirebaseDatabase database;
    FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        UserAdapter adapter = new UserAdapter(list, getContext());
        binding.chatsRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.chatsRecyclerView.setLayoutManager(layoutManager);

        String currentUserId = auth.getUid();

        // Listen for changes in the 'chats' node to find active conversations
        database.getReference().child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                // Temporary list to track added IDs to avoid duplicates
                ArrayList<String> addedUserIds = new ArrayList<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String roomKey = chatSnapshot.getKey();

                    // Check if the current user is part of this specific chat room
                    if (roomKey != null && roomKey.contains(currentUserId)) {
                        // Extract the ID of the other person in the chat
                        String otherUserId = roomKey.replace(currentUserId, "");

                        if (!addedUserIds.contains(otherUserId)) {
                            addedUserIds.add(otherUserId);

                            // Fetch the other user's profile details to display in the list
                            database.getReference().child("Users").child(otherUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                            Users user = userSnapshot.getValue(Users.class);
                                            if (user != null) {
                                                user.setUserId(userSnapshot.getKey());
                                                list.add(user);
                                                adapter.notifyDataSetChanged();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                    }
                }
                // If no chats found, the list remains empty and nothing is displayed
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible database errors
            }
        });

        return binding.getRoot();
    }
}