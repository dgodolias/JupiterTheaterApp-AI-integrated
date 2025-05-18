package com.example.jupitertheaterapp.database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Consolidated repository for all entities in the theater database
 * Provides methods to access shows, bookings, reviews, and discounts
 */
public class TheaterRepository {
    private final TheaterDao theaterDao;
    
    // LiveData for all entity lists
    private final LiveData<List<Show>> allShows;
    private final LiveData<List<Booking>> allBookings;
    private final LiveData<List<Review>> allReviews;
    private final LiveData<List<Discount>> allDiscounts;

    public TheaterRepository(Context context) {
        TheaterDatabase db = TheaterDatabase.getDatabase(context);
        theaterDao = db.theaterDao();
        
        // Initialize LiveData
        allShows = theaterDao.getAllShows();
        allBookings = theaterDao.getAllBookings();
        allReviews = theaterDao.getAllReviews();
        allDiscounts = theaterDao.getAllDiscounts();
    }
    
    //--- SHOW OPERATIONS ---//
    
    public LiveData<List<Show>> getAllShows() {
        return allShows;
    }

    public void insertShow(Show show) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.insertShow(show);
        });
    }

    public void updateShow(Show show) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.updateShow(show);
        });
    }

    public void deleteShow(Show show) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.deleteShow(show);
        });
    }

    public LiveData<Show> getShowById(int id) {
        return theaterDao.getShowById(id);
    }

    public LiveData<List<Show>> getShowsByName(String nameFragment) {
        return theaterDao.getShowsByName(nameFragment);
    }

    public LiveData<List<Show>> getShowsByDay(String day) {
        return theaterDao.getShowsByDay(day);
    }

    public LiveData<List<Show>> getShowsByRoom(String room) {
        return theaterDao.getShowsByRoom(room);
    }

    public LiveData<List<Show>> getShowsByTopic(String topic) {
        return theaterDao.getShowsByTopic(topic);
    }
    
    //--- BOOKING OPERATIONS ---//
    
    public LiveData<List<Booking>> getAllBookings() {
        return allBookings;
    }

    public void insertBooking(Booking booking) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.insertBooking(booking);
        });
    }

    public void updateBooking(Booking booking) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.updateBooking(booking);
        });
    }

    public void deleteBooking(Booking booking) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.deleteBooking(booking);
        });
    }

    public LiveData<Booking> getBookingById(int id) {
        return theaterDao.getBookingById(id);
    }

    public LiveData<List<Booking>> getBookingsByShow(String showName) {
        return theaterDao.getBookingsByShow(showName);
    }

    public LiveData<List<Booking>> getBookingsByReservationId(String reservationId) {
        return theaterDao.getBookingsByReservationId(reservationId);
    }

    public LiveData<List<Booking>> getBookingsByDay(String day) {
        return theaterDao.getBookingsByDay(day);
    }
    
    //--- REVIEW OPERATIONS ---//
    
    public LiveData<List<Review>> getAllReviews() {
        return allReviews;
    }

    public void insertReview(Review review) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.insertReview(review);
        });
    }

    public void updateReview(Review review) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.updateReview(review);
        });
    }

    public void deleteReview(Review review) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.deleteReview(review);
        });
    }

    public LiveData<Review> getReviewById(int id) {
        return theaterDao.getReviewById(id);
    }

    public LiveData<List<Review>> getReviewsByShow(String showName) {
        return theaterDao.getReviewsByShow(showName);
    }

    public LiveData<List<Review>> getReviewsByReservation(String reservationNumber) {
        return theaterDao.getReviewsByReservation(reservationNumber);
    }

    public LiveData<List<Review>> getReviewsByStars(String stars) {
        return theaterDao.getReviewsByStars(stars);
    }
    
    //--- DISCOUNT OPERATIONS ---//
    
    public LiveData<List<Discount>> getAllDiscounts() {
        return allDiscounts;
    }

    public void insertDiscount(Discount discount) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.insertDiscount(discount);
        });
    }

    public void updateDiscount(Discount discount) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.updateDiscount(discount);
        });
    }

    public void deleteDiscount(Discount discount) {
        TheaterDatabase.databaseWriteExecutor.execute(() -> {
            theaterDao.deleteDiscount(discount);
        });
    }

    public LiveData<Discount> getDiscountById(int id) {
        return theaterDao.getDiscountById(id);
    }

    public LiveData<List<Discount>> getDiscountsByShow(String showName) {
        return theaterDao.getDiscountsByShow(showName);
    }

    public LiveData<List<Discount>> getDiscountsByCode(String code) {
        return theaterDao.getDiscountsByCode(code);
    }

    public LiveData<List<Discount>> getDiscountsByAge(String age) {
        return theaterDao.getDiscountsByAge(age);
    }

    public LiveData<List<Discount>> getDiscountsByDate(String date) {
        return theaterDao.getDiscountsByDate(date);
    }
}
