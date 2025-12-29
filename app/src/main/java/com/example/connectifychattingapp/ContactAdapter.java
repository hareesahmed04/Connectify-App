package com.example.connectifychattingapp;

import android.content.Context;
import android.content.Intent;
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

    public ContactAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the NEW contact-specific layout
        View view = LayoutInflater.from(context).inflate(R.layout.sample_user_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = list.get(position);

        Picasso.get().load(user.getProfilePic())
                .placeholder(R.drawable.user1)
                .into(holder.image);

        holder.userName.setText(user.getusername());

        // CLICK LISTENER: Opens the same ChatDetailActivity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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