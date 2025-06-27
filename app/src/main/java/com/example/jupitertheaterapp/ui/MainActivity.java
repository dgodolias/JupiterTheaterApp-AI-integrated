package com.example.jupitertheaterapp.ui;

import android.content.Context;
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
        });

        // Add long press listener to show debug info
        sendButton.setOnLongClickListener(v -> {
            // Show a dialog to choose what to display
            String[] options = {
                    "Full Tree Structure",
                    "Conversation Node List",
                    "Current Node Details",
                    "Database State",
                    "Complete Debug Info",
                    "Test Database Fix"
            };
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Debug Display Options")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                displayTreeStructure();
                                break;
                            case 1:
                                displayConversationNodeList();
                                break;
                            case 2:
                                displayCurrentNodeDetails();
                                break;
                            case 3:
                                displayDatabaseState();
                                break;
                            case 4:
                                displayCompleteDebugInfo();
                                break;
                            case 5:
                                testDatabaseFix();
                                break;
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

    /**
     * Displays detailed information about the current node.
     * This includes node properties, template state, and navigation details.
     */
    public void displayCurrentNodeDetails() {
        String nodeDetails = chatbotManager.getCurrentNodeDebugInfo();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Current Node Details")
                .setMessage(nodeDetails)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }

    /**
     * Displays the current state of the database including record counts and sample data.
     * This is useful for debugging database operations and seeing what data is stored.
     */
    public void displayDatabaseState() {
        String databaseState = chatbotManager.getDatabaseDebugInfo();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Database State")
                .setMessage(databaseState)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }

    /**
     * Displays comprehensive debug information including tree structure,
     * current node details, conversation path, and database state.
     * This provides a complete overview of the application's current state.
     */
    public void displayCompleteDebugInfo() {
        String completeDebugInfo = chatbotManager.getComprehensiveDebugInfo();

        // For the complete debug info, we'll use a scrollable dialog since it's quite long
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(completeDebugInfo);
        textView.setPadding(20, 20, 20, 20);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scrollView.addView(textView);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Complete Debug Information")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .setNegativeButton("Copy to Clipboard", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip =
                            android.content.ClipData.newPlainText("Debug Info", completeDebugInfo);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .create()
                .show();
    }

    /**
     * Tests the database debug functionality to verify the null pointer exception fix.
     * This method specifically tests the scenario that was causing the crash.
     */
    public void testDatabaseFix() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setPadding(20, 20, 20, 20);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);

        StringBuilder testResults = new StringBuilder();
        testResults.append("Database Debug Fix Test Results:\n");
        testResults.append("==========================================\n\n");
        try {
            // Import the test class
            com.example.jupitertheaterapp.test.DebugTest.testDatabaseDebugFunctionality(this);
            com.example.jupitertheaterapp.test.DebugTest.testNullTemplateScenario(this);
            com.example.jupitertheaterapp.test.DebugTest.testImprovedDebugOutput(this);

            testResults.append("✓ All tests executed successfully!\n");
            testResults.append("✓ No null pointer exceptions occurred\n");
            testResults.append("✓ Database sampling is working correctly\n");
            testResults.append("✓ Improved debug output formatting tested\n\n");

            // Test each table individually to show results
            com.example.jupitertheaterapp.core.SimpleDatabase db =
                    com.example.jupitertheaterapp.core.SimpleDatabase.getInstance();

            String[] tables = {"bookings", "reviews", "shows", "discounts"};
            for (String tableName : tables) {
                try {
                    org.json.JSONArray results = db.queryRecords(tableName, null);
                    if (results != null) {
                        testResults.append("✓ ").append(tableName).append(" table: ")
                                .append(results.length()).append(" records found\n");
                    } else {
                        testResults.append("⚠ ").append(tableName).append(" table: null results\n");
                    }
                } catch (Exception e) {
                    testResults.append("✗ ").append(tableName).append(" table error: ")
                            .append(e.getMessage()).append("\n");
                }
            }

            testResults.append("\n");
            testResults.append("Database Debug Info Test:\n");
            testResults.append("----------------------------------------\n");

            // Test the actual debug info generation
            String debugInfo = chatbotManager.getDatabaseDebugInfo();
            if (debugInfo != null && !debugInfo.isEmpty()) {
                testResults.append("✓ Database debug info generated successfully\n");
                testResults.append("Length: ").append(debugInfo.length()).append(" characters\n\n");
                testResults.append("Preview of generated debug info:\n");
                testResults.append(debugInfo.substring(0, Math.min(500, debugInfo.length())));
                if (debugInfo.length() > 500) {
                    testResults.append("\n... (truncated for display)");
                }
            } else {
                testResults.append("✗ Database debug info generation failed\n");
            }

        } catch (Exception e) {
            testResults.append("✗ Test failed with exception: ").append(e.getMessage()).append("\n");
            testResults.append("Stack trace: ").append(android.util.Log.getStackTraceString(e));
        }

        textView.setText(testResults.toString());
        scrollView.addView(textView);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Database Fix Test Results")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .setNeutralButton("Run Database State", (dialog, which) -> {
                    displayDatabaseState();
                })
                .create()
                .show();
    }
}