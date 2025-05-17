package com.example.jupitertheaterapp.model;

/**
 * Abstract base class for chat messages
 */
public abstract class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_SERVER = 2;  // Server message type

    protected String message;
    protected int type;

    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    // Returns true if message is from system (bot or server)
    public boolean isSystemMessage() {
        return type == TYPE_BOT || type == TYPE_SERVER;
    }
    
    /**
     * Creates appropriate message subtype based on the type
     */
    public static ChatMessage createMessage(String message, int type) {
        if (type == TYPE_USER) {
            return new UserMessage(message);
        } else {
            return new SystemMessage(message, type);
        }
    }
}

/**
 * Represents a message from the system (either bot or server)
 */
class SystemMessage extends ChatMessage {
    public SystemMessage(String message, int type) {
        super(message, type);
        // Ensure type is either BOT or SERVER
        if (type != TYPE_BOT && type != TYPE_SERVER) {
            this.type = TYPE_BOT; // Default to BOT if an invalid type is provided
        }
    }
    
    // Constructor with default BOT type
    public SystemMessage(String message) {
        super(message, TYPE_BOT);
    }
    
    /**
     * Creates a server-type system message
     */
    public static SystemMessage createServerMessage(String message) {
        return new SystemMessage(message, TYPE_SERVER);
    }
    
    /**
     * Creates a bot-type system message
     */
    public static SystemMessage createBotMessage(String message) {
        return new SystemMessage(message, TYPE_BOT);
    }
}

/**
 * Represents a message from the user
 */
class UserMessage extends ChatMessage {
    public UserMessage(String message) {
        super(message, TYPE_USER);
    }
}