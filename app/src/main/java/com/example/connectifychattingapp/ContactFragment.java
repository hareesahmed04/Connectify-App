package com.example.connectifychattingapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
    ContactAdapter adapter;
    private androidx.appcompat.view.ActionMode actionMode;

    public ContactFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentContactBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize Adapter and RecyclerView
        adapter = new ContactAdapter(list, getContext(), new ContactAdapter.SelectionListener() {
            @Override
            public void onSelectionModeChange(boolean isActive) {
                if (isActive) {
                    actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
                } else if (actionMode != null) {
                    actionMode.finish();
                }
            }
            @Override
            public void onSelectionCountChange(int count) {
                if (actionMode != null) {
                    actionMode.setTitle(count + " selected");
                }
            }
        });
        binding.searchBar.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });
        binding.contactRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (binding.searchBar.hasFocus()) {
                    binding.searchBar.clearFocus();
                }
                return false; // Return false so the list still scrolls normally
            }
        });
        binding.contactRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    binding.searchBar.clearFocus();
                }
            }
        });

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
    private final androidx.appcompat.view.ActionMode.Callback actionModeCallback = new androidx.appcompat.view.ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contact_delete_menu, menu);
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete_contact) {
                showDeleteDialog();
                return true;
            }
            return false;
        }
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.isSelectionMode = false;
            for (Users u : list) {
                u.setSelected(false);
            }
            adapter.notifyDataSetChanged();
            actionMode = null;
        }
    };
    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete selected contacts?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteFromFirebase();
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                })
                .setNegativeButton("Cancel", null);

        // 1. Create the dialog object
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(getContext(), R.color.blue));

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
    }
    private void deleteFromFirebase() {
        String currentUid = auth.getUid();
        // Because we fixed the shadowing in Step 1, 'adapter' is no longer null here
        if (currentUid != null && adapter != null) {
            ArrayList<Users> selectedUsers = adapter.getSelectedItems();
            for (Users user : selectedUsers) {
                database.getReference().child("Users")
                        .child(currentUid)
                        .child("Contacts")
                        .child(user.getUserId()) // Ensure your Users model has a userId
                        .removeValue();
            }
        }
    }
    private void filterList(String text) {
        ArrayList<Users> filteredList = new ArrayList<>();

        for (Users user : list) {
            // .toLowerCase() makes it non-case sensitive
            if (user.getusername().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(user);
            }
        }
        if (filteredList.isEmpty()) {
            // Optional: show a toast or change visibility if no match found
            adapter.setFilteredList(filteredList);
        } else {
            adapter.setFilteredList(filteredList);
        }
    }
}


