package com.example.jupitertheaterapp.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract base class for chat messages
 */
public abstract class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_SERVER = 2;  // Server message type

    protected int type;
    protected String category; // Adding category field to base class

    public ChatMessage(int type) {
        this.type = type;
        this.category = "-"; // Default category
    }

    public ChatMessage(int type, String category) {
        this.type = type;
        this.category = category;
    }

    public abstract String getMessage();

    public int getType() {
        return type;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
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
        } else if (type == TYPE_SERVER && message.startsWith("{") && message.endsWith("}")) {
            // If it's a server message and appears to be JSON, create it as JSON
            return new SystemMessage(message, type, true);
        } else {
            return new SystemMessage(message, type);
        }
    }
}

/**
 * Represents a message from the system (either bot or server)
 * Only contains details and error fields as specified
 */
class SystemMessage extends ChatMessage {
    private JSONObject details;
    private String error;
    private String message; // Used internally for getMessage method
    
    public SystemMessage(String message, int type) {
        super(type);
        this.message = message;
        // Ensure type is either BOT or SERVER
        if (type != TYPE_BOT && type != TYPE_SERVER) {
            this.type = TYPE_BOT; // Default to BOT if an invalid type is provided
        }
    }
    
    // Constructor with default BOT type
    public SystemMessage(String message) {
        this(message, TYPE_BOT);
    }
      // Constructor for server responses with JSON
    public SystemMessage(JSONObject jsonResponse, int type) {
        super(type, jsonResponse.optString("category", "-"));
        this.error = jsonResponse.optString("error", null);
        this.details = jsonResponse.optJSONObject("details");
        // Don't set a default generic message to avoid the "Server response for category" text
        this.message = "";
    }// Constructor accepting server response as a JSON string
    public SystemMessage(String jsonString, int type, boolean isJson) {
        super(type);
        if (isJson) {
            try {
                JSONObject jsonResponse = new JSONObject(jsonString);
                this.category = jsonResponse.optString("category", "-");
                this.error = jsonResponse.optString("error", null);
                this.details = jsonResponse.optJSONObject("details");
                
                // Don't set a default message - this prevents the "Server response for category" text
                // Instead, leave message null/empty so handleConversationTurn will use message1/message2 only
                this.message = ""; 
            } catch (JSONException e) {
                this.message = "Error parsing server response";
                e.printStackTrace();
            }
        } else {
            this.message = jsonString;
        }
    }    @Override
    public String getMessage() {
        // Debug logging for message content
        System.out.println("DEBUG: SystemMessage.getMessage() returning: " + message);
        return message;
    }
      public JSONObject getDetails() {
        return details;
    }
    
    public String getError() {
        return error;
    }
    
    public void setMessage(String message) {
        // Debug logging for message updates
        System.out.println("DEBUG: SystemMessage.setMessage() setting message to: " + message);
        this.message = message;
    }
    
    public void setDetails(JSONObject details) {
        this.details = details;
    }
    
    public void setError(String error) {
        this.error = error;
    }
      /**
     * Gets the JSON representation of this message
     * @return A JSONObject containing all the message data
     */
    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("category", category != null ? category : "-");
            
            if (details != null) {
                json.put("details", details);
            }
            
            if (error != null && !error.isEmpty()) {
                json.put("error", error);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
    
    /**
     * Gets a specific field from the details JSON
     * @param fieldName The name of the field to get
     * @return The field value as a string, or null if not found
     */
    public String getDetailField(String fieldName) {
        if (details == null) return null;
        
        try {
            if (details.has(fieldName)) {
                Object value = details.get(fieldName);
                return value.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return null;
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
    }    //toString method to get the message and the super class fields
    @Override
    public String toString() {
        return "SystemMessage{" +
                "category='" + category + '\'' +
                ", details=" + details +
                ", error='" + error + '\'' +
                ", type=" + type +
                '}';
    }
    
}

/**
 * Represents a message from the user
 * Only contains type and message fields as specified
 */
class UserMessage extends ChatMessage {
    private String message;
    
    public UserMessage(String message) {
        super(TYPE_USER);
        this.message = message;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the JSON representation of this message
     * @return A JSONObject containing all the message data
     */
    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", getType() == TYPE_USER ? "USER" : "CATEGORISE");
            json.put("category", category);
            json.put("message", getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
    
    @Override
    public String toString() {
        return "UserMessage{" +
                "category='" + category + '\'' +
                "type=" + type +
                ", message='" + message + '\'' +
                '}';
    }
}
