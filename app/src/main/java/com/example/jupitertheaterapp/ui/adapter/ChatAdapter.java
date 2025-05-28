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
        
        // Set the message text
        holder.messageTextView.setText(message.getMessage());
        
        // Ensure padding is consistent
        //holder.messageTextView.setPadding(12, 12, 12, 12);

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
        // Check if the message contains newlines and split if necessary
        String messageText = message.getMessage();
        if (messageText != null && messageText.contains("\n")) {
            // Split the message by newlines
            String[] parts = messageText.split("\n");
            
            // Add each part as a separate message bubble
            for (String part : parts) {
                // Skip empty parts (which can occur from multiple consecutive newlines)
                if (part.trim().isEmpty()) {
                    continue;
                }
                
                // Create a new ChatMessage for each part with the same type as the original
                ChatMessage splitMessage = ChatMessage.createMessage(part.trim(), message.getType());
                splitMessage.setCategory(message.getCategory());
                
                messages.add(splitMessage);
                notifyItemInserted(messages.size() - 1);
            }
        } else {
            // Single message without newlines - add normally
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }
    }

    /**
     * Add a message with manual splitting by newlines
     * @param messageText The combined message text
     * @param messageType The type of message (USER, BOT, SERVER)
     * @param category The message category
     */
    public void addSplitMessage(String messageText, int messageType, String category) {
        if (messageText != null && messageText.contains("\n")) {
            // Split the message by newlines
            String[] parts = messageText.split("\n");
            
            // Add each part as a separate message bubble
            for (String part : parts) {
                // Skip empty parts (which can occur from multiple consecutive newlines)
                if (part.trim().isEmpty()) {
                    continue;
                }
                
                // Create a new ChatMessage for each part
                ChatMessage splitMessage = ChatMessage.createMessage(part.trim(), messageType);
                splitMessage.setCategory(category);
                
                messages.add(splitMessage);
                notifyItemInserted(messages.size() - 1);
            }
        } else {
            // Single message without newlines - add normally
            ChatMessage singleMessage = ChatMessage.createMessage(messageText, messageType);
            singleMessage.setCategory(category);
            
            messages.add(singleMessage);
            notifyItemInserted(messages.size() - 1);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}