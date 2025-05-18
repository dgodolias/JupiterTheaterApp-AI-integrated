package com.example.jupitertheaterapp.database;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.List;

/**
 * Database utility class demonstrating how to use the TheaterRepository
 */
public class DatabaseExample {
    private static final String TAG = "DatabaseExample";
    private final TheaterRepository repository;
    
    public DatabaseExample(Context context) {
        repository = new TheaterRepository(context);
    }
    
    /**
     * Example method to demonstrate getting all shows
     */
    public void getAllShowsExample() {
        LiveData<List<Show>> allShows = repository.getAllShows();
        
        // You would normally observe this in your Fragment/Activity
        // This is just a demo of how to use it
        Observer<List<Show>> observer = shows -> {
            if (shows != null) {
                Log.d(TAG, "Found " + shows.size() + " shows in the database");
                
                for (Show show : shows) {
                    Log.d(TAG, "Show: " + show.getName().getValue() + 
                         ", Room: " + show.getRoom().getValue() + 
                         ", Day: " + show.getDay().getValue());
                }
            }
        };
        
        // In a real app, observe this in your LifecycleOwner (Activity/Fragment)
        // allShows.observe(lifecycleOwner, observer);
    }
    
    /**
     * Example method to demonstrate getting shows by name
     */
    public void searchShowsByNameExample(String name) {
        LiveData<List<Show>> shows = repository.getShowsByName(name);
        
        // Observe logic would go here
    }
    
    /**
     * Example method to demonstrate getting bookings for a show
     */
    public void getBookingsForShowExample(String showName) {
        LiveData<List<Booking>> bookings = repository.getBookingsByShow(showName);
        
        // Observe logic would go here
    }
    
    /**
     * Example method to demonstrate creating a new booking
     */
    public void createBookingExample(String showName, String room, String day, String time,
                                    String personName, String personAge, String seat) {
        // Create a new booking
        Booking booking = new Booking();
        
        // Set booking details
        FieldValue showNameField = new FieldValue();
        showNameField.setValue(showName);
        booking.setShowName(showNameField);
        
        FieldValue roomField = new FieldValue();
        roomField.setValue(room);
        booking.setRoom(roomField);
        
        FieldValue dayField = new FieldValue();
        dayField.setValue(day);
        booking.setDay(dayField);
        
        FieldValue timeField = new FieldValue();
        timeField.setValue(time);
        booking.setTime(timeField);
        
        // Create reservation ID (in production would be generated on server)
        FieldValue reservationIdField = new FieldValue();
        reservationIdField.setValue("RES" + System.currentTimeMillis());
        booking.setReservationId(reservationIdField);
        
        // Set person details
        Person person = new Person();
        
        FieldValue personNameField = new FieldValue();
        personNameField.setValue(personName);
        person.setName(personNameField);
        
        FieldValue personAgeField = new FieldValue();
        personAgeField.setValue(personAge);
        person.setAge(personAgeField);
        
        FieldValue seatField = new FieldValue();
        seatField.setValue(seat);
        person.setSeat(seatField);
        
        booking.setPerson(person);
        
        // Save the booking
        repository.insertBooking(booking);
    }
    
    /**
     * Example method to demonstrate writing a review
     */
    public void createReviewExample(String reservationNumber, String showName, 
                                   int stars, String reviewText) {
        // Create a new review
        Review review = new Review();
        
        // Set reservation number
        FieldValue resNumberField = new FieldValue();
        resNumberField.setValue(reservationNumber);
        review.setReservationNumber(resNumberField);
        
        // Set show name
        FieldValue showNameField = new FieldValue();
        showNameField.setValue(showName);
        review.setShowName(showNameField);
        
        // Set passcode (would be generated in production)
        FieldValue passcodeField = new FieldValue();
        passcodeField.setValue("PASS" + System.currentTimeMillis());
        review.setPasscode(passcodeField);
        
        // Set stars
        FieldValue starsField = new FieldValue();
        starsField.setValue(String.valueOf(stars));
        review.setStars(starsField);
        
        // Set review text
        FieldValue reviewTextField = new FieldValue();
        reviewTextField.setValue(reviewText);
        review.setReview(reviewTextField);
        
        // Save the review
        repository.insertReview(review);
    }
}
