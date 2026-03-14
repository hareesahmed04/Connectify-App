package com.example.connectifychattingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
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
import java.util.HashMap;
import java.util.Locale;

public class CallLogsFragment extends Fragment {

    private FragmentCallLogsBinding binding;
    private ArrayList<CallLogModel> list;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private CallLogAdapter adapter;

    public CallLogsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallLogsBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        list = new ArrayList<>();

        adapter = new CallLogAdapter(list);
        binding.callLogsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.callLogsRecyclerView.setAdapter(adapter);

        fetchCallLogs();

        return binding.getRoot();
    }

    // Keep your existing Fragment code, but ensure this specific method is used:
    private void fetchCallLogs() {
        if (auth.getUid() == null) return;

        database.getReference().child("Users").child(auth.getUid()).child("CallLogs")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            CallLogModel model = dataSnapshot.getValue(CallLogModel.class);
                            if (model != null) {
                                model.setCallId(dataSnapshot.getKey());
                                list.add(0, model); // Newest at top
                            }
                        }
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyMessage.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void initiateCallFromLog(CallLogModel model, String type) {
        if (getContext() == null || model.getUserId() == null) return;

        String senderId = auth.getUid();
        String channelId = senderId + "_" + model.getUserId();

        // Fetch current user (Caller) info first to save log for Receiver
        database.getReference().child("Users").child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String myRealName = snapshot.child("userName").getValue(String.class);
                String myRealPic = snapshot.child("profilePic").getValue(String.class);
                long timestamp = System.currentTimeMillis();

                // 1. Prepare signaling data for the "ringing" screen
                HashMap<String, Object> callData = new HashMap<>();
                callData.put("callerId", senderId);
                callData.put("callerName", myRealName != null ? myRealName : "Connectify User");
                callData.put("callerPic", myRealPic != null ? myRealPic : "");
                callData.put("type", type);
                callData.put("channelId", channelId);
                callData.put("status", "ringing");

                // 2. Write to Signaling node to make receiver's phone ring
                database.getReference().child("calls").child(model.getUserId()).setValue(callData)
                        .addOnSuccessListener(unused -> {

                            // 3. SAVE LOG FOR CALLER
                            CallLogModel callerLog = new CallLogModel(model.getUserId(), model.getUserName(),
                                    model.getProfilePic(), type, "Outgoing", timestamp);
                            database.getReference().child("Users").child(senderId).child("CallLogs").push().setValue(callerLog);

                            // 4. SAVE LOG FOR RECEIVER
                            CallLogModel receiverLog = new CallLogModel(senderId, myRealName,
                                    myRealPic, type, "Incoming", timestamp);
                            database.getReference().child("Users").child(model.getUserId()).child("CallLogs").push().setValue(receiverLog);

                            // 5. Open Call Activity
                            Intent intent = new Intent(getContext(), type.equals("video") ? VideoCallActivty.class : AudioCallingActivty.class);
                            intent.putExtra("channelId", channelId);
                            intent.putExtra("isCaller", true);
                            intent.putExtra("remoteUserId", model.getUserId());
                            intent.putExtra("remoteUserName", model.getUserName());
                            intent.putExtra("remoteUserProfile", model.getProfilePic());
                            startActivity(intent);
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- ADAPTER ---
    public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {
        ArrayList<CallLogModel> mList;

        public CallLogAdapter(ArrayList<CallLogModel> mList) {
            this.mList = mList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_call_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CallLogModel model = mList.get(position);

            holder.tvLogName.setText(model.getUserName() != null ? model.getUserName() : "Unknown User");

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                holder.tvLogDetails.setText(sdf.format(new Date(model.getTimestamp())));
            } catch (Exception e) {
                holder.tvLogDetails.setText("Unknown time");
            }

            // Icons
            if ("video".equals(model.getCallType())) {
                holder.ivVideoIcon.setVisibility(View.VISIBLE);
                holder.ivCallIcon.setVisibility(View.GONE);
            } else {
                holder.ivCallIcon.setVisibility(View.VISIBLE);
                holder.ivVideoIcon.setVisibility(View.GONE);
            }

            // Profile Pic
            if (model.getProfilePic() != null && !model.getProfilePic().isEmpty()) {
                Picasso.get().load(model.getProfilePic()).placeholder(R.drawable.user1).into(holder.ivProfile);
            } else {
                holder.ivProfile.setImageResource(R.drawable.user1);
            }

            // Click Handlers
            holder.ivCallIcon.setOnClickListener(v -> initiateCallFromLog(model, "audio"));
            holder.ivVideoIcon.setOnClickListener(v -> initiateCallFromLog(model, "video"));
        }

        @Override
        public int getItemCount() { return mList.size(); }

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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle("Clear All Logs")
                .setMessage("Delete your entire call history?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    database.getReference().child("Users")
                            .child(auth.getUid())
                            .child("CallLogs")
                            .removeValue();
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}