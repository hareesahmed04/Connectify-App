package com.example.connectifychattingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GeminiChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChatMessage> chatMessages;

    public GeminiChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isUser() ? 1 : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) { // User
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user_gemini, parent, false);
            return new UserViewHolder(view);
        } else { // Gemini
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_gemini, parent, false);
            return new GeminiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String message = chatMessages.get(position).getMessage();
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).txtMsg.setText(message);
        } else {
            ((GeminiViewHolder) holder).txtMsg.setText(message);
        }
    }
    @Override
    public int getItemCount() { return chatMessages.size(); }
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtMsg;
        UserViewHolder(View itemView) { super(itemView); txtMsg = itemView.findViewById(R.id.textMessage); }
    }
    static class GeminiViewHolder extends RecyclerView.ViewHolder {
        TextView txtMsg;
        GeminiViewHolder(View itemView) { super(itemView); txtMsg = itemView.findViewById(R.id.textMessage); }
    }
}