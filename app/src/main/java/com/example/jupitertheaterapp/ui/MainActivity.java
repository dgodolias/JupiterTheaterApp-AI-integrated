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
import com.example.jupitertheaterapp.util.ServerClient;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ChatbotManager chatbotManager;
    private RecyclerView messagesRecyclerView;
    private EditText userInputEditText;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private LinearLayout inputLayout;
    private ServerClient serverClient;

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
        messagesRecyclerView.setAdapter(chatAdapter);


        chatbotManager = new ChatbotManager(this); // Pass context to constructor

        // Initialize server client
        serverClient = new ServerClient();

        // Display initial message
        addMessage(chatbotManager.getInitialMessage(), ChatMessage.TYPE_BOT);

        // Set up send button click listener
        sendButton.setOnClickListener(v -> {
            String userMessage = userInputEditText.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                // Display user message
                addMessage(userMessage, ChatMessage.TYPE_USER);

                // Clear input field
                userInputEditText.setText("");

                if (chatbotManager.shouldUseServer()) {
                    // Get response from server
                    serverClient.sendMessage(userMessage, new ServerClient.ServerResponseCallback() {
                        @Override
                        public void onResponse(String response) {
                            addMessage(response, ChatMessage.TYPE_SERVER);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            // Fallback to local response
                            String fallbackResponse = chatbotManager.getLocalResponse(userMessage);
                            addMessage(fallbackResponse, ChatMessage.TYPE_BOT);
                        }
                    });
                } else {
                    // Get local response
                    String response = chatbotManager.getLocalResponse(userMessage);
                    addMessage(response, ChatMessage.TYPE_BOT);
                }
            }
        });

        // Properly handle keyboard visibility with WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());

            // Update the padding of the content view
            v.setPadding(insets.left, insets.top, insets.right, 0);

            // Update the margin of the input layout to stay above keyboard
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) inputLayout.getLayoutParams();
            params.bottomMargin = insets.bottom;
            inputLayout.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });

        // For testing, enable server responses (remove this line to use local responses)
        // chatbotManager.setUseServerForResponses(true);
    }

    private void addMessage(String message, int type) {
        chatAdapter.addMessage(new ChatMessage(message, type));
        messagesRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverClient != null) {
            serverClient.shutdown();
        }
    }
}