package com.example.connectifychattingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.connectifychattingapp.databinding.FragmentContactBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ContactFragment extends Fragment {

    FragmentContactBinding binding;
    ArrayList<Users> list = new ArrayList<>();
    FirebaseDatabase database;
    FirebaseAuth auth;

    public ContactFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentContactBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize Adapter and RecyclerView
        ContactAdapter adapter = new ContactAdapter(list, getContext());
        binding.contactRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.contactRecyclerView.setLayoutManager(layoutManager);

        // Floating Action Button to open AddContactActivity
        binding.fabAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AddContactActivity.class);
                startActivity(intent);
            }
        });

        // Fetch contacts specifically for the logged-in user
        String currentUid = auth.getUid();
        if (currentUid != null) {
            database.getReference().child("Users")
                    .child(currentUid)
                    .child("Contacts")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            list.clear();
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Users contact = dataSnapshot.getValue(Users.class);
                                if (contact != null) {
                                    // Set the ID manually if it's not in the object
                                    contact.setUserId(dataSnapshot.getKey());
                                    list.add(contact);
                                }
                            }
                            adapter.notifyDataSetChanged();

                            // Handle visibility if the list is empty
                            if (list.isEmpty()) {
                                binding.contactRecyclerView.setVisibility(View.GONE);
                                binding.tvEmptyMessage.setVisibility(View.VISIBLE);
                            } else {
                                binding.contactRecyclerView.setVisibility(View.VISIBLE);
                                binding.tvEmptyMessage.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle potential database errors
                        }
                    });
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}