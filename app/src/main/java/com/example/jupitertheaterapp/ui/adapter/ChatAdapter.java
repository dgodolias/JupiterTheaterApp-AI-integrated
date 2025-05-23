package com.example.jupitertheaterapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jupitertheaterapp.R;
import com.example.jupitertheaterapp.model.ChatMessage;


import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        // Both bot and server messages use the same layout (left bubble)
        return message.getType() == ChatMessage.TYPE_USER ? ChatMessage.TYPE_USER : ChatMessage.TYPE_BOT;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == ChatMessage.TYPE_USER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_left, parent, false);
        }
        return new MessageViewHolder(view);
    }    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        // Get the message content
        String messageContent = message.getMessage();
        
        // Filter out generic or empty messages
        if (messageContent == null || messageContent.isEmpty()) {
            // Skip completely empty messages
            return;
        }
        
        // Filter out any "Information about category" or "Server response for category" messages
        if (messageContent.startsWith("Information about") || 
            messageContent.startsWith("Server response for category")) {
            // Try to extract relevant content after the colon if present
            int colonIndex = messageContent.indexOf(":");
            if (colonIndex > 0 && colonIndex < messageContent.length() - 1) {
                messageContent = messageContent.substring(colonIndex + 1).trim();
                // If the extracted content is still empty, don't show this message
                if (messageContent.isEmpty()) {
                    return;
                }
            } else {
                // Skip showing this generic message entirely
                return;
            }
        }
        
        // Log the message content for debugging
        System.out.println("DEBUG: Displaying message in adapter: " + messageContent);
        
        // Set the message text
        holder.messageTextView.setText(messageContent);
        
        // Ensure padding is consistent
        holder.messageTextView.setPadding(12, 12, 12, 12);

        // Apply animation
        Animation animation = AnimationUtils.loadAnimation(
                holder.itemView.getContext(),
                message.getType() == ChatMessage.TYPE_USER ?
                        R.anim.slide_in_right : R.anim.slide_in_left);
        holder.itemView.startAnimation(animation);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }    public void addMessage(ChatMessage message) {
        // Skip adding null messages
        if (message == null) {
            return;
        }
        
        // Get the message content
        String messageContent = message.getMessage();
        
        // Skip adding empty messages
        if (messageContent == null || messageContent.isEmpty()) {
            return;
        }
        
        // Skip adding generic messages
        if (messageContent.startsWith("Information about") || 
            messageContent.startsWith("Server response for category")) {
            // Only extract and add if there's meaningful content after the colon
            int colonIndex = messageContent.indexOf(":");
            if (colonIndex > 0 && colonIndex < messageContent.length() - 1) {
                String extractedContent = messageContent.substring(colonIndex + 1).trim();

            } else {
                // Skip the message if there's no colon to extract content after
                return;
            }
        }
        
        // Check for duplicate messages (avoid adding the same message twice in a row)
        if (!messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            if (lastMessage.getMessage().equals(messageContent)) {
                // Skip adding duplicate message
                return;
            }
        }
        
        // Add the message if it passed all the filters
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}