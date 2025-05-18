package main.java.com.example.jupitertheaterapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Booking entity class for theater reservations
 */
@Entity(tableName = "bookings")
public class Booking {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @TypeConverters(Converters.class)
    private FieldValue show_name;
    
    @TypeConverters(Converters.class)
    private FieldValue room;
    
    @TypeConverters(Converters.class)
    private FieldValue day;
    
    @TypeConverters(Converters.class)
    private FieldValue time;
    
    @TypeConverters(Converters.class)
    private Person person;
    
    @TypeConverters(Converters.class)
    private FieldValue reservation_id;
    
    public Booking() {
        // Initialize with empty field values to avoid null pointer exceptions
        this.show_name = new FieldValue();
        this.room = new FieldValue();
        this.day = new FieldValue();
        this.time = new FieldValue();
        this.person = new Person();
        this.reservation_id = new FieldValue();
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
    
    public FieldValue getRoom() {
        return room;
    }
    
    public void setRoom(FieldValue room) {
        this.room = room;
    }
    
    public FieldValue getDay() {
        return day;
    }
    
    public void setDay(FieldValue day) {
        this.day = day;
    }
    
    public FieldValue getTime() {
        return time;
    }
    
    public void setTime(FieldValue time) {
        this.time = time;
    }
    
    public Person getPerson() {
        return person;
    }
    
    public void setPerson(Person person) {
        this.person = person;
    }
    
    public FieldValue getReservationId() {
        return reservation_id;
    }
    
    public void setReservationId(FieldValue reservation_id) {
        this.reservation_id = reservation_id;
    }
}
