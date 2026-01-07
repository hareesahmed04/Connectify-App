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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder>{
    public interface OnUserLongClickListener {
        void onUserLongClick(Users user, int position);
    }
    ArrayList<Users> list;
    Context context;
    private OnUserLongClickListener longClickListener;
    public UserAdapter(ArrayList<Users> list, Context context, OnUserLongClickListener longClickListener) {
        this.list = list;
        this.context = context;
        this.longClickListener = longClickListener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.sample_user,parent,false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users users =list.get(position);
        Picasso.get().load(users.getProfilePic()).placeholder(R.drawable.user1).into(holder.image);
        holder.UserNameList.setText(users.getusername());

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onUserLongClick(users, position);
            }
            return true;
        });

        //Showing last Messgae of User
        FirebaseDatabase.getInstance().getReference().child("chats")
                        .child(FirebaseAuth.getInstance().getUid() + users.getUserId())
                                .orderByChild("timestamp")
                                        .limitToLast(1)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if(snapshot.hasChildren()){
                                                            for(DataSnapshot snapshot1 : snapshot.getChildren()){
                                                                Object messageValue = snapshot1.child("message").getValue();
                                                                if (messageValue != null) {
                                                                    holder.lastmsg.setText(messageValue.toString());
                                                                } else {
                                                                    holder.lastmsg.setText("Tap to chat");
                                                                }
                                                            }
                                                        } else {
                                                            // Handle case where there are no messages at all
                                                            holder.lastmsg.setText("Tap to chat");
                                                        }
                                                    }
                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context, ChatDetailActivity.class);
                intent.putExtra("userId",users.getUserId());
                intent.putExtra("profilePic",users.getProfilePic());
                intent.putExtra("userName",users.getusername());
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
    TextView UserNameList,lastmsg;
    public ViewHolder(@NonNull View itemView) {
        super(itemView);

        image=itemView.findViewById(R.id.profileImage);
        UserNameList=itemView.findViewById(R.id.UserNameList);
        lastmsg=itemView.findViewById(R.id.lastmsg);

    }
}
}
