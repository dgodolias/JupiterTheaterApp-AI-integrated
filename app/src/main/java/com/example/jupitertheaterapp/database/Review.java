package com.example.jupitertheaterapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Review entity class for show reviews
 */
@Entity(tableName = "reviews")
public class Review {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @TypeConverters(Converters.class)
    private FieldValue reservation_number;
    
    @TypeConverters(Converters.class)
    private FieldValue passcode;
    
    @TypeConverters(Converters.class)
    private FieldValue stars;
    
    @TypeConverters(Converters.class)
    private FieldValue review;
    
    @TypeConverters(Converters.class)
    private FieldValue show_name;
    
    public Review() {
        // Initialize with empty field values to avoid null pointer exceptions
        this.reservation_number = new FieldValue();
        this.passcode = new FieldValue();
        this.stars = new FieldValue();
        this.review = new FieldValue();
        this.show_name = new FieldValue();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public FieldValue getReservationNumber() {
        return reservation_number;
    }
    
    public void setReservationNumber(FieldValue reservation_number) {
        this.reservation_number = reservation_number;
    }
    
    public FieldValue getPasscode() {
        return passcode;
    }
    
    public void setPasscode(FieldValue passcode) {
        this.passcode = passcode;
    }
    
    public FieldValue getStars() {
        return stars;
    }
    
    public void setStars(FieldValue stars) {
        this.stars = stars;
    }
    
    public FieldValue getReview() {
        return review;
    }
    
    public void setReview(FieldValue review) {
        this.review = review;
    }
    
    public FieldValue getShowName() {
        return show_name;
    }
    
    public void setShowName(FieldValue show_name) {
        this.show_name = show_name;
    }
}
