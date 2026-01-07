package com.example.connectifychattingapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
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
    ArrayList<Users> originalList = new ArrayList<>(); // To back up data
    UserAdapter adapter;
    String currentSearchQuery = "";
    FirebaseDatabase database;
    FirebaseAuth auth;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new UserAdapter(list, getContext(), new UserAdapter.OnUserLongClickListener() {
            @Override
            public void onUserLongClick(Users user, int position) {
                showDeleteDialog(user, position);
                binding.chatsRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        binding.fabGemini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), GeminiChatActivity.class);
                startActivity(intent);

            }
        });

        binding.chatsRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.chatsRecyclerView.setLayoutManager(layoutManager);

        // --- SEARCH VIEW LOGIC START
        binding.chatSearchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // This method is called every time text changes
                currentSearchQuery = newText;
                filterList(newText);
                return true;
            }
        });
        // --- SEARCH VIEW LOGIC END ---

        binding.fabGemini.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), GeminiChatActivity.class);
            startActivity(intent);
        });

        binding.chatRootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!binding.chatSearchView.isIconified()) {
                    binding.chatSearchView.clearFocus();
                    binding.chatSearchView.setIconified(true);
                }
            }
        });
        binding.chatsRecyclerView.setOnTouchListener((v, event) -> {
            binding.chatSearchView.clearFocus();
            return false;
        });

        String currentUserId = auth.getUid();

        // Listen for changes in the 'chats' node to find active conversations
        database.getReference().child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                originalList.clear();
                // Temporary list to track added IDs to avoid duplicates
                ArrayList<String> addedUserIds = new ArrayList<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String roomKey = chatSnapshot.getKey();

                    // Check if the current user is part of this specific chat room
                    if (roomKey != null && roomKey.startsWith(currentUserId)) {
                        // Extract the ID of the other person in the chat
                        String otherUserId = roomKey.substring(currentUserId.length());

                        if (!addedUserIds.contains(otherUserId)) {
                            addedUserIds.add(otherUserId);

                            // Fetch the other user's profile details to display in the list
                            database.getReference().child("Users")
                                    .child(currentUserId)
                                    .child("Contacts")
                                    .child(otherUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                            Users user = userSnapshot.getValue(Users.class);
                                            if (user != null) {
                                                binding.NoChatMsg.setVisibility(View.GONE);
                                                user.setUserId(userSnapshot.getKey());
                                                originalList.add(user);
                                                if (currentSearchQuery.isEmpty() ||
                                                        user.getusername().toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                                                    list.add(user);
                                                }
                                            }
                                            adapter.notifyDataSetChanged();
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible database errors
            }
        });
        return binding.getRoot();

    }
    private void showDeleteDialog(Users user, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Chat");
        builder.setMessage("Remove this conversation from your list?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String currentUserId = FirebaseAuth.getInstance().getUid();
                if (currentUserId == null) return;

                // 1. INSTANT UI UPDATE (The "Vanish" Logic)
                // We remove it from the lists immediately so the user sees it disappear
                Users userToRemove = list.get(position);
                list.remove(position);
                adapter.notifyItemRemoved(position);

                // Also remove from the backup list so it doesn't come back after searching
                originalList.remove(userToRemove);

                // 2. DATABASE DELETE
                String senderRoom = currentUserId + user.getUserId();
                database.getReference().child("chats")
                        .child(senderRoom)
                        .removeValue()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getContext(), "Chat Deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // If DB delete fails, put it back so the user knows
                            list.add(position, userToRemove);
                            originalList.add(userToRemove);
                            adapter.notifyItemInserted(position);
                            Toast.makeText(getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void filterList(String text) {
        list.clear();

        if (text.isEmpty()) {
            // Restore full list
            list.addAll(originalList);

            if (originalList.isEmpty()) {
                binding.NoChatMsg.setVisibility(View.VISIBLE);
                binding.NoChatMsg.setText("No chat is Available");
            } else {
                binding.NoChatMsg.setVisibility(View.GONE);
            }
        } else {
            // Filtering logic
            for (Users user : originalList) {
                if (user.getusername().toLowerCase().contains(text.toLowerCase())) {
                    list.add(user);
                }
            }
            if (list.isEmpty()) {
                binding.NoChatMsg.setVisibility(View.VISIBLE);
                binding.NoChatMsg.setText("No user found");
            } else {
                binding.NoChatMsg.setVisibility(View.GONE);
            }
        }
        adapter.notifyDataSetChanged();
    }
}