package com.example.jupitertheaterapp.database;

/**
 * Person class used in Booking entity for storing person details
 */
public class Person {
    private FieldValue name;
    private FieldValue age;
    private FieldValue seat;
    
    public Person() {
        this.name = new FieldValue();
        this.age = new FieldValue();
        this.seat = new FieldValue();
    }
    
    public Person(FieldValue name, FieldValue age, FieldValue seat) {
        this.name = name;
        this.age = age;
        this.seat = seat;
    }
    
    public FieldValue getName() {
        return name;
    }
    
    public void setName(FieldValue name) {
        this.name = name;
    }
    
    public FieldValue getAge() {
        return age;
    }
    
    public void setAge(FieldValue age) {
        this.age = age;
    }
    
    public FieldValue getSeat() {
        return seat;
    }
    
    public void setSeat(FieldValue seat) {
        this.seat = seat;
    }
}
