package com.example.jupitertheaterapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChatbotNode {
    private String message;
    private List<ChatbotNode> children;
    private Random random;

    public ChatbotNode(String message) {
        this.message = message;
        this.children = new ArrayList<>();
        this.random = new Random();
    }

    public void addChild(ChatbotNode child) {
        children.add(child);
    }

    public String getMessage() {
        return message;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ChatbotNode getRandomChild() {
        if (children.isEmpty()) {
            return null;
        }
        int randomIndex = random.nextInt(children.size());
        return children.get(randomIndex);
    }
}