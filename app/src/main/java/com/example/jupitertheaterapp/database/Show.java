package com.example.jupitertheaterapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Show entity class representing theater shows
 */
@Entity(tableName = "shows")
public class Show {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @TypeConverters(Converters.class)
    private FieldValue name;
    
    @TypeConverters(Converters.class)
    private FieldValue day;
    
    @TypeConverters(Converters.class)
    private FieldValue topic;
    
    @TypeConverters(Converters.class)
    private FieldValue time;
    
    @TypeConverters(Converters.class)
    private FieldValue cast;
    
    @TypeConverters(Converters.class)
    private FieldValue room;
    
    @TypeConverters(Converters.class)
    private FieldValue duration;
    
    @TypeConverters(Converters.class)
    private FieldValue stars;
    
    public Show() {
        // Initialize with empty field values to avoid null pointer exceptions
        this.name = new FieldValue();
        this.day = new FieldValue();
        this.topic = new FieldValue();
        this.time = new FieldValue();
        this.cast = new FieldValue();
        this.room = new FieldValue();
        this.duration = new FieldValue();
        this.stars = new FieldValue();
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public FieldValue getName() {
        return name;
    }
    
    public void setName(FieldValue name) {
        this.name = name;
    }
    
    public FieldValue getDay() {
        return day;
    }
    
    public void setDay(FieldValue day) {
        this.day = day;
    }
    
    public FieldValue getTopic() {
        return topic;
    }
    
    public void setTopic(FieldValue topic) {
        this.topic = topic;
    }
    
    public FieldValue getTime() {
        return time;
    }
    
    public void setTime(FieldValue time) {
        this.time = time;
    }
    
    public FieldValue getCast() {
        return cast;
    }
    
    public void setCast(FieldValue cast) {
        this.cast = cast;
    }
    
    public FieldValue getRoom() {
        return room;
    }
    
    public void setRoom(FieldValue room) {
        this.room = room;
    }
    
    public FieldValue getDuration() {
        return duration;
    }
    
    public void setDuration(FieldValue duration) {
        this.duration = duration;
    }
    
    public FieldValue getStars() {
        return stars;
    }
    
    public void setStars(FieldValue stars) {
        this.stars = stars;
    }
}
