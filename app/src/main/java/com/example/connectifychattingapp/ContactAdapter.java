package com.example.connectifychattingapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    ArrayList<Users> list;
    Context context;
    boolean isSelectionMode = false;
    private SelectionListener listener;

    public interface SelectionListener {
        void onSelectionModeChange(boolean isActive);
        void onSelectionCountChange(int count);
    }
    public ContactAdapter(ArrayList<Users> list, Context context, SelectionListener listener) {
        this.list = list;
        this.context = context;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the NEW contact-specific layout
        View view = LayoutInflater.from(context).inflate(R.layout.sample_user_contact, parent, false);
        return new ViewHolder(view);
    }
    public void setFilteredList(ArrayList<Users> filteredList) {
        this.list = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = list.get(position);

        // Highlight background if selected
        holder.itemView.setBackgroundColor(user.isSelected() ?
                Color.parseColor("#D3D3D3") : Color.TRANSPARENT);

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                user.setSelected(true);
                listener.onSelectionModeChange(true);
                listener.onSelectionCountChange(1);
                notifyDataSetChanged();
            }
            return true;
        });

        Picasso.get().load(user.getProfilePic())
                .placeholder(R.drawable.user1)
                .into(holder.image);

        holder.userName.setText(user.getusername());

        // CLICK LISTENER: Opens the same ChatDetailActivity
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                user.setSelected(!user.isSelected());
                int count = getSelectedItemsCount();

                if (count == 0) {
                    isSelectionMode = false;
                    if (listener != null) listener.onSelectionModeChange(false);
                } else {
                    if (listener != null) listener.onSelectionCountChange(count);
                }
                notifyItemChanged(position);
            } else {
                // Open ChatDetailActivity
                Intent intent = new Intent(context, ChatDetailActivity.class);
                intent.putExtra("userId", user.getUserId());
                intent.putExtra("profilePic", user.getProfilePic());
                intent.putExtra("userName", user.getusername());
                context.startActivity(intent);
            }
        });
    }
    @Override
    public int getItemCount() {
        return list.size();
    }

    // Helper to count selected items
    private int getSelectedItemsCount() {
        int count = 0;
        for (Users u : list) {
            if (u.isSelected()) count++;
        }
        return count;
    }
    // Helper to get selected list for the Delete action
    public ArrayList<Users> getSelectedItems() {
        ArrayList<Users> selected = new ArrayList<>();
        for (Users u : list) {
            if (u.isSelected()) selected.add(u);
        }
        return selected;
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView userName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profileImage);
            userName = itemView.findViewById(R.id.UserNameList);
        }
    }
}