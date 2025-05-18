package com.example.jupitertheaterapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Discount entity class for special offers
 */
@Entity(tableName = "discounts")
public class Discount {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @TypeConverters(Converters.class)
    private FieldValue show_name;
    
    @TypeConverters(Converters.class)
    private FieldValue no_of_people;
    
    @TypeConverters(Converters.class)
    private FieldValue age;
    
    @TypeConverters(Converters.class)
    private FieldValue date;
    
    @TypeConverters(Converters.class)
    private FieldValue discount_percentage;
    
    @TypeConverters(Converters.class)
    private FieldValue code;
    
    public Discount() {
        // Initialize with empty field values to avoid null pointer exceptions
        this.show_name = new FieldValue();
        this.no_of_people = new FieldValue();
        this.age = new FieldValue();
        this.date = new FieldValue();
        this.discount_percentage = new FieldValue();
        this.code = new FieldValue();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public FieldValue getShowName() {
        return show_name;
    }
    
    public void setShowName(FieldValue show_name) {
        this.show_name = show_name;
    }
    
    public FieldValue getNoOfPeople() {
        return no_of_people;
    }
    
    public void setNoOfPeople(FieldValue no_of_people) {
        this.no_of_people = no_of_people;
    }
    
    public FieldValue getAge() {
        return age;
    }
    
    public void setAge(FieldValue age) {
        this.age = age;
    }
    
    public FieldValue getDate() {
        return date;
    }
    
    public void setDate(FieldValue date) {
        this.date = date;
    }
    
    public FieldValue getDiscountPercentage() {
        return discount_percentage;
    }
    
    public void setDiscountPercentage(FieldValue discount_percentage) {
        this.discount_percentage = discount_percentage;
    }
    
    public FieldValue getCode() {
        return code;
    }
    
    public void setCode(FieldValue code) {
        this.code = code;
    }
}
