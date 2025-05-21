package com.example.jupitertheaterapp.model;

/**
 * Singleton class to manage conversation states throughout the application.
 * This allows tracking the current state of the conversation flow from any part of the app.
 */
public class ConversationState {
    // Singleton instance
    private static ConversationState instance;
    
    // Available states
    public enum State {
        INITIAL,            // Initial state of the conversation
        GET_INFO,  // When the chatbot needs information from the user
        GIVE_INFO,   // When the chatbot is providing information to the user
        CONFIRMATION,        // When the chatbot is asking for confirmation
        LLM_GET_INFO,        // When using a large language model response
        EXIT                 // When exiting the conversation
    }
    
    // Current state
    private State currentState = State.INITIAL; // Default state
    
    // Private constructor to enforce singleton pattern
    private ConversationState() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized ConversationState getInstance() {
        if (instance == null) {
            instance = new ConversationState();
        }
        return instance;
    }
    
    /**
     * Get the current conversation state
     * @return The current state
     */
    public State getCurrentState() {
        return currentState;
    }
    
    /**
     * Set the conversation to a new state
     * @param state The new state to set
     */
    public void setCurrentState(State state) {
        this.currentState = state;
    }
    
    /**
     * Check if the current state matches the specified state
     * @param state The state to check against
     * @return True if states match, false otherwise
     */
    public boolean isInState(State state) {
        return currentState == state;
    }
    
    /**
     * Get the string representation of the current state
     * @return String representation of the current state
     */
    public String getCurrentStateAsString() {
        return currentState.toString();
    }
}
