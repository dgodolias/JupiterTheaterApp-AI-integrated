package com.example.jupitertheaterapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Combined Data Access Object for all theater entities.
 * Provides access to Shows, Bookings, Reviews, and Discounts.
 */
@Dao
public interface TheaterDao {
    
    //--- SHOW OPERATIONS ---//
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertShow(Show show);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAllShows(List<Show> shows);
    
    @Update
    void updateShow(Show show);
    
    @Delete
    void deleteShow(Show show);
    
    @Query("DELETE FROM shows")
    void deleteAllShows();
    
    @Query("SELECT * FROM shows WHERE id = :id")
    LiveData<Show> getShowById(int id);
    
    @Query("SELECT * FROM shows")
    LiveData<List<Show>> getAllShows();
    
    @Query("SELECT * FROM shows")
    List<Show> getAllShowsSync();
    
    @Query("SELECT * FROM shows WHERE name LIKE '%' || :nameFragment || '%'")
    LiveData<List<Show>> getShowsByName(String nameFragment);
    
    @Query("SELECT * FROM shows WHERE day LIKE '%' || :dayFragment || '%'")
    LiveData<List<Show>> getShowsByDay(String dayFragment);
    
    @Query("SELECT * FROM shows WHERE room LIKE '%' || :roomFragment || '%'")
    LiveData<List<Show>> getShowsByRoom(String roomFragment);
    
    @Query("SELECT * FROM shows WHERE topic LIKE '%' || :topicFragment || '%'")
    LiveData<List<Show>> getShowsByTopic(String topicFragment);
    
    //--- BOOKING OPERATIONS ---//
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBooking(Booking booking);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAllBookings(List<Booking> bookings);
    
    @Update
    void updateBooking(Booking booking);
    
    @Delete
    void deleteBooking(Booking booking);
    
    @Query("DELETE FROM bookings")
    void deleteAllBookings();
    
    @Query("SELECT * FROM bookings WHERE id = :id")
    LiveData<Booking> getBookingById(int id);
    
    @Query("SELECT * FROM bookings")
    LiveData<List<Booking>> getAllBookings();
    
    @Query("SELECT * FROM bookings")
    List<Booking> getAllBookingsSync();
    
    @Query("SELECT * FROM bookings WHERE show_name LIKE '%' || :showName || '%'")
    LiveData<List<Booking>> getBookingsByShow(String showName);
    
    @Query("SELECT * FROM bookings WHERE reservation_id LIKE '%' || :reservationId || '%'")
    LiveData<List<Booking>> getBookingsByReservationId(String reservationId);
    
    @Query("SELECT * FROM bookings WHERE day LIKE '%' || :day || '%'")
    LiveData<List<Booking>> getBookingsByDay(String day);
    
    //--- REVIEW OPERATIONS ---//
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertReview(Review review);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAllReviews(List<Review> reviews);
    
    @Update
    void updateReview(Review review);
    
    @Delete
    void deleteReview(Review review);
    
    @Query("DELETE FROM reviews")
    void deleteAllReviews();
    
    @Query("SELECT * FROM reviews WHERE id = :id")
    LiveData<Review> getReviewById(int id);
    
    @Query("SELECT * FROM reviews")
    LiveData<List<Review>> getAllReviews();
    
    @Query("SELECT * FROM reviews")
    List<Review> getAllReviewsSync();
    
    @Query("SELECT * FROM reviews WHERE show_name LIKE '%' || :showName || '%'")
    LiveData<List<Review>> getReviewsByShow(String showName);
    
    @Query("SELECT * FROM reviews WHERE reservation_number LIKE '%' || :reservationNumber || '%'")
    LiveData<List<Review>> getReviewsByReservation(String reservationNumber);
    
    @Query("SELECT * FROM reviews WHERE stars LIKE '%' || :stars || '%'")
    LiveData<List<Review>> getReviewsByStars(String stars);
    
    //--- DISCOUNT OPERATIONS ---//
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDiscount(Discount discount);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAllDiscounts(List<Discount> discounts);
    
    @Update
    void updateDiscount(Discount discount);
    
    @Delete
    void deleteDiscount(Discount discount);
    
    @Query("DELETE FROM discounts")
    void deleteAllDiscounts();
    
    @Query("SELECT * FROM discounts WHERE id = :id")
    LiveData<Discount> getDiscountById(int id);
    
    @Query("SELECT * FROM discounts")
    LiveData<List<Discount>> getAllDiscounts();
    
    @Query("SELECT * FROM discounts")
    List<Discount> getAllDiscountsSync();
    
    @Query("SELECT * FROM discounts WHERE show_name LIKE '%' || :showName || '%'")
    LiveData<List<Discount>> getDiscountsByShow(String showName);
    
    @Query("SELECT * FROM discounts WHERE code LIKE '%' || :code || '%'")
    LiveData<List<Discount>> getDiscountsByCode(String code);
    
    @Query("SELECT * FROM discounts WHERE age LIKE '%' || :age || '%'")
    LiveData<List<Discount>> getDiscountsByAge(String age);
    
    @Query("SELECT * FROM discounts WHERE date LIKE '%' || :date || '%'")
    LiveData<List<Discount>> getDiscountsByDate(String date);
}
