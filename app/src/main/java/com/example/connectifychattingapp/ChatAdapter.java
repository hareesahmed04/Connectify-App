package com.example.connectifychattingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter{

    ArrayList<MessageModel> messageModels;
    Context context;

    public ChatAdapter(ArrayList<MessageModel> messageModels, Context context) {
        this.messageModels = messageModels;
        this.context = context;
    }
    int SENDER_VIEW_TYPE=1;
    int RECEIVER_VIEW_TYPE=2;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==SENDER_VIEW_TYPE){
            View view= LayoutInflater.from(context).inflate(R.layout.sample_chatting_sender,parent,false);
            return new SenderViewHoler(view);
        }else{
            View view= LayoutInflater.from(context).inflate(R.layout.sample_chatting_reciever,parent,false);
            return new ReceiverViewHoler(view);

        }
    }

    @Override
    public int getItemViewType(int position) {
        String msgUserId = messageModels.get(position).getuserId();

        // 2. Check if it is null BEFORE calling .equals()
        if (msgUserId != null && msgUserId.equals(FirebaseAuth.getInstance().getUid())) {
            return SENDER_VIEW_TYPE;
        }else {
            return RECEIVER_VIEW_TYPE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel messageModel=messageModels.get(position);
        if(holder.getClass() == SenderViewHoler.class){
            ((SenderViewHoler)holder).senderText.setText(messageModel.getMessage());
        }else {
            ((ReceiverViewHoler)holder).receiverText.setText(messageModel.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageModels.size();
    }

    public class ReceiverViewHoler extends RecyclerView.ViewHolder{

        TextView receiverText ,receiverTime;

        public ReceiverViewHoler(@NonNull View itemView) {
            super(itemView);

            receiverText=itemView.findViewById(R.id.receiverText);
            receiverTime=itemView.findViewById(R.id.receiverTime);
        }
    }

    public class SenderViewHoler extends RecyclerView.ViewHolder{

        TextView senderText,senderTime;

        public SenderViewHoler(@NonNull View itemView) {
            super(itemView);

            senderText=itemView.findViewById(R.id.senderText);
            senderTime=itemView.findViewById(R.id.senderTime);
        }
    }


}
