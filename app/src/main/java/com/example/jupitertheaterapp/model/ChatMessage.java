package com.example.jupitertheaterapp.model;

    public class ChatMessage {
        public static final int TYPE_USER = 0;
        public static final int TYPE_BOT = 1;
        public static final int TYPE_SERVER = 2;  // New server message type

        private String message;
        private int type;

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
    }