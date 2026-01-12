package com.example.connectifychattingapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectifychattingapp.databinding.FragmentCallLogsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CallLogsFragment extends Fragment {
    FragmentCallLogsBinding binding;
    ArrayList<CallLogModel> list;
    FirebaseDatabase database;
    FirebaseAuth auth;
    CallLogAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This is crucial: it tells the fragment to participate in the Options Menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallLogsBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        // 1. Initialize the list
        list = new ArrayList<>();
        // 2. Setup RecyclerView
        binding.callLogsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CallLogAdapter(list);
        binding.callLogsRecyclerView.setAdapter(adapter);
        // 3. Fetch Data
        fetchCallLogs();

        return binding.getRoot();
    }
    private void fetchCallLogs() {
        if (auth.getUid() == null) return;

        database.getReference().child("Users")
                .child(auth.getUid())
                .child("CallLogs")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        list.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            CallLogModel model = dataSnapshot.getValue(CallLogModel.class);
                            if (model != null) {
                                // Add to top (newest first)
                                list.add(0, model);
                            }
                        }

                        if (list.isEmpty()) {
                            binding.tvEmptyMessage.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvEmptyMessage.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    // --- MENU HANDLING ---
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate your res/menu/call_log_menu.xml
        inflater.inflate(R.menu.call_log_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clear_logs) {
            showClearConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Logs")
                .setMessage("Delete all call history?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    deleteAllLogs();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void deleteAllLogs() {
        if (auth.getUid() != null) {
            database.getReference().child("Users")
                    .child(auth.getUid())
                    .child("CallLogs")
                    .removeValue()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Call logs cleared", Toast.LENGTH_SHORT).show();
                    });
        }
    }
    // --- ADAPTER CLASS ---
    public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {
        ArrayList<CallLogModel> list;
        public CallLogAdapter(ArrayList<CallLogModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_call_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CallLogModel model = list.get(position);

            holder.tvLogName.setText(model.getUserName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            String dateString = sdf.format(new Date(model.getTimestamp()));
            holder.tvLogDetails.setText(dateString);

            if ("video".equals(model.getCallType())) {
                holder.ivVideoIcon.setVisibility(View.VISIBLE);
                holder.ivCallIcon.setVisibility(View.GONE);
            } else {
                holder.ivCallIcon.setVisibility(View.VISIBLE);
                holder.ivVideoIcon.setVisibility(View.GONE);
            }

            if (model.getProfilePic() != null && !model.getProfilePic().isEmpty()) {
                Picasso.get().load(model.getProfilePic())
                        .placeholder(R.drawable.user1)
                        .into(holder.ivProfile);
            } else {
                holder.ivProfile.setImageResource(R.drawable.user1);
            }
        }
        @Override
        public int getItemCount() {
            return list.size();
        }
        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProfile, ivCallIcon, ivVideoIcon;
            TextView tvLogName, tvLogDetails;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvLogName = itemView.findViewById(R.id.tvLogName);
                tvLogDetails = itemView.findViewById(R.id.tvLogDetails);
                ivCallIcon = itemView.findViewById(R.id.ivCallIcon);
                ivVideoIcon = itemView.findViewById(R.id.ivVideoIcon);
            }
        }
    }
}