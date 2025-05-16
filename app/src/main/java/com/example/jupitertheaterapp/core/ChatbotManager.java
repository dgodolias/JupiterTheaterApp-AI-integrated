package com.example.jupitertheaterapp.core;

import com.example.jupitertheaterapp.model.ChatbotNode;

public class ChatbotManager {
    private ChatbotNode rootNode;
    private ChatbotNode currentNode;
    private boolean useServerForResponses = false; // Flag to control response source

    public ChatbotManager() {
        // Initialize with a basic conversation tree
        setupConversationTree();
    }

    private void setupConversationTree() {
        // Root level
        rootNode = new ChatbotNode("Hello! Welcome to Jupiter Theater. How can I help you today?");

        // Level 1 options
        ChatbotNode showTimesNode = new ChatbotNode("Would you like to see our current show times?");
        ChatbotNode ticketsNode = new ChatbotNode("Are you interested in purchasing tickets?");
        ChatbotNode infoNode = new ChatbotNode("Do you want to know more about our theater?");

        rootNode.addChild(showTimesNode);
        rootNode.addChild(ticketsNode);
        rootNode.addChild(infoNode);

        // Level 2 for show times
        showTimesNode.addChild(new ChatbotNode("We have shows at 2PM, 5PM, and 8PM today."));
        showTimesNode.addChild(new ChatbotNode("Our current showtimes are: matinee at 2PM and evening shows at 6PM and 9PM."));

        // Level 2 for tickets
        ticketsNode.addChild(new ChatbotNode("Tickets are $15 for adults and $10 for children. Would you like to book now?"));
        ticketsNode.addChild(new ChatbotNode("We have standard tickets for $15 and premium seating for $25. How many would you like?"));

        // Level 2 for info
        infoNode.addChild(new ChatbotNode("Jupiter Theater has been operating since 1995 with award-winning performances."));
        infoNode.addChild(new ChatbotNode("Our theater features state-of-the-art sound systems and comfortable seating for 200 guests."));

        // Set the current node to the root to start
        currentNode = rootNode;
    }

    public String getInitialMessage() {
        return rootNode.getMessage();
    }

    public String getCurrentMessage() {
        return currentNode.getMessage();
    }

    // Local response generation
    public String getLocalResponse(String userInput) {
        // Move to a random child node if available
        if (currentNode.hasChildren()) {
            currentNode = currentNode.getRandomChild();
            return currentNode.getMessage();
        } else {
            // If no children, reset to root
            currentNode = rootNode;
            return "I'm not sure what to say next. Let's start over. " + rootNode.getMessage();
        }
    }

    // Determines whether to use server for responses
    public boolean shouldUseServer() {
        return useServerForResponses;
    }

    // Enable/disable server responses
    public void setUseServerForResponses(boolean useServer) {
        this.useServerForResponses = useServer;
    }

    public void reset() {
        currentNode = rootNode;
    }
}