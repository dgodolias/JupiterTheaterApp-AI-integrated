package com.example.jupitertheaterapp.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jupitertheaterapp.R;
import com.example.jupitertheaterapp.core.ChatbotManager;
import com.example.jupitertheaterapp.model.ChatMessage;
import com.example.jupitertheaterapp.ui.adapter.ChatAdapter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ChatbotManager chatbotManager;
    private RecyclerView messagesRecyclerView;
    private EditText userInputEditText;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private LinearLayout inputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        userInputEditText = findViewById(R.id.userInputEditText);
        sendButton = findViewById(R.id.sendButton);
        inputLayout = findViewById(R.id.inputLayout);

        // Set up RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(new ArrayList<>());
        messagesRecyclerView.setAdapter(chatAdapter); // Create the chatbot manager (now handles its own client)
        chatbotManager = new ChatbotManager(this);

        // Print the conversation tree structure to the log for debugging
        // chatbotManager.printTree(); // Display initial message
        addMessage(chatbotManager.getInitialMessage(), ChatMessage.TYPE_BOT);
        sendButton.setOnClickListener(v -> {
            String userMessage = userInputEditText.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                // Display user message
                addMessage(userMessage, ChatMessage.TYPE_USER);

                // Clear input field
                userInputEditText.setText("");

                // For debugging, print the current node before processing
                chatbotManager.printCurrentNode();

                // Get response from chatbot manager (handles both local and server logic
                // internally)
                chatbotManager.getResponse(userMessage, new ChatbotManager.ResponseCallback() {
                    @Override
                    public void onResponseReceived(String response, int messageType) {
                        // Add the response to the chat
                        addMessage(response, messageType);

                        // For debugging, print the node after processing
                        chatbotManager.printCurrentNode();
                    }
                });
            }
        });// Add long press listener to show debug info
        sendButton.setOnLongClickListener(v -> {
            // Show a dialog to choose what to display
            String[] options = { "Full Tree Structure", "Conversation Node List" };

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Debug Display Options")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            displayTreeStructure();
                        } else if (which == 1) {
                            displayConversationNodeList();
                        }
                    })
                    .show();

            return true;
        });

        // Properly handle keyboard visibility with WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets
                    .getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());

            // Update the padding of the content view
            v.setPadding(insets.left, insets.top, insets.right, 0);

            // Update the margin of the input layout to stay above keyboard
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) inputLayout
                    .getLayoutParams();
            params.bottomMargin = insets.bottom;
            inputLayout.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });        // Always using server responses
    }

    private void addMessage(String message, int type) {
        // Use factory method to create proper message subtype
        ChatMessage chatMessage = ChatMessage.createMessage(message, type);
        chatAdapter.addMessage(chatMessage);
        messagesRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Client class doesn't have shutdown method, so removed the call
    }

    /**
     * Displays the conversation tree structure in a dialog.
     * This is useful for debugging purposes.
     */
    public void displayTreeStructure() {
        String treeStructure = chatbotManager.getTreeAsString();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Conversation Tree Structure")
                .setMessage(treeStructure)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }

    /**
     * Displays the conversation as a list of nodes with system and user messages.
     * This shows only the conversation path that has been traversed, not the entire
     * tree.
     */
    public void displayConversationNodeList() {
        String nodeList = chatbotManager.getConversationNodeList();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Conversation Node List")
                .setMessage(nodeList)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }
}