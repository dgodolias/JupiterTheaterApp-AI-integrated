package main.java.com.example.jupitertheaterapp.database;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to load sample data from JSON files into the database
 */
public class DataLoader {
    private static final String TAG = "DataLoader";
    private final Context context;
    private final Gson gson = new Gson();

    public DataLoader(Context context) {
        this.context = context;
    }    /**
     * Load all sample data into the database
     * @param db Database instance
     */
    public void loadSampleData(TheaterDatabase db) {
        loadShows(db);
        loadBookings(db);
        loadReviews(db);
        loadDiscounts(db);
    }

    /**
     * Load sample shows from JSON
     */
    private void loadShows(TheaterDatabase db) {
        try {
            String json = readJsonFile(context, "sample_shows.json");
            JsonObject rootObj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray showsArray = rootObj.getAsJsonArray("shows");
            
            List<Show> shows = new ArrayList<>();
            for (JsonElement element : showsArray) {
                JsonObject showObj = element.getAsJsonObject();
                Show show = new Show();
                
                // Parse each field
                show.setName(parseFieldValue(showObj.get("name").getAsJsonObject()));
                show.setDay(parseFieldValue(showObj.get("day").getAsJsonObject()));
                show.setTopic(parseFieldValue(showObj.get("topic").getAsJsonObject()));
                show.setTime(parseFieldValue(showObj.get("time").getAsJsonObject()));
                show.setCast(parseFieldValue(showObj.get("cast").getAsJsonObject()));
                show.setRoom(parseFieldValue(showObj.get("room").getAsJsonObject()));
                show.setDuration(parseFieldValue(showObj.get("duration").getAsJsonObject()));
                show.setStars(parseFieldValue(showObj.get("stars").getAsJsonObject()));
                
                shows.add(show);
            }
            
            db.theaterDao().insertAllShows(shows);
            Log.d(TAG, "Loaded " + shows.size() + " shows");
        } catch (Exception e) {
            Log.e(TAG, "Error loading shows", e);
        }
    }

    /**
     * Load sample bookings from JSON
     */
    private void loadBookings(TheaterDatabase db) {
        try {
            String json = readJsonFile(context, "sample_bookings.json");
            JsonObject rootObj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray bookingsArray = rootObj.getAsJsonArray("bookings");
            
            List<Booking> bookings = new ArrayList<>();
            for (JsonElement element : bookingsArray) {
                JsonObject bookingObj = element.getAsJsonObject();
                Booking booking = new Booking();
                
                // Parse each field
                booking.setShowName(parseFieldValue(bookingObj.get("show_name").getAsJsonObject()));
                booking.setRoom(parseFieldValue(bookingObj.get("room").getAsJsonObject()));
                booking.setDay(parseFieldValue(bookingObj.get("day").getAsJsonObject()));
                booking.setTime(parseFieldValue(bookingObj.get("time").getAsJsonObject()));
                booking.setReservationId(parseFieldValue(bookingObj.get("reservation_id").getAsJsonObject()));
                
                // Parse person object
                JsonObject personObj = bookingObj.get("person").getAsJsonObject();
                Person person = new Person();
                person.setName(parseFieldValue(personObj.get("name").getAsJsonObject()));
                person.setAge(parseFieldValue(personObj.get("age").getAsJsonObject()));
                person.setSeat(parseFieldValue(personObj.get("seat").getAsJsonObject()));
                booking.setPerson(person);
                
                bookings.add(booking);
            }
            
            db.theaterDao().insertAllBookings(bookings);
            Log.d(TAG, "Loaded " + bookings.size() + " bookings");
        } catch (Exception e) {
            Log.e(TAG, "Error loading bookings", e);
        }
    }

    /**
     * Load sample reviews from JSON
     */
    private void loadReviews(TheaterDatabase db) {
        try {
            String json = readJsonFile(context, "sample_reviews.json");
            JsonObject rootObj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray reviewsArray = rootObj.getAsJsonArray("reviews");
            
            List<Review> reviews = new ArrayList<>();
            for (JsonElement element : reviewsArray) {
                JsonObject reviewObj = element.getAsJsonObject();
                Review review = new Review();
                
                // Parse each field
                review.setReservationNumber(parseFieldValue(reviewObj.get("reservation_number").getAsJsonObject()));
                review.setPasscode(parseFieldValue(reviewObj.get("passcode").getAsJsonObject()));
                review.setStars(parseFieldValue(reviewObj.get("stars").getAsJsonObject()));
                review.setReview(parseFieldValue(reviewObj.get("review").getAsJsonObject()));
                review.setShowName(parseFieldValue(reviewObj.get("show_name").getAsJsonObject()));
                
                reviews.add(review);
            }
            
            db.theaterDao().insertAllReviews(reviews);
            Log.d(TAG, "Loaded " + reviews.size() + " reviews");
        } catch (Exception e) {
            Log.e(TAG, "Error loading reviews", e);
        }
    }

    /**
     * Load sample discounts from JSON
     */
    private void loadDiscounts(TheaterDatabase db) {
        try {
            String json = readJsonFile(context, "sample_discounts.json");
            JsonObject rootObj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray discountsArray = rootObj.getAsJsonArray("discounts");
            
            List<Discount> discounts = new ArrayList<>();
            for (JsonElement element : discountsArray) {
                JsonObject discountObj = element.getAsJsonObject();
                Discount discount = new Discount();
                
                // Parse each field
                discount.setShowName(parseFieldValue(discountObj.get("show_name").getAsJsonObject()));
                discount.setNoOfPeople(parseFieldValue(discountObj.get("no_of_people").getAsJsonObject()));
                discount.setAge(parseFieldValue(discountObj.get("age").getAsJsonObject()));
                discount.setDate(parseFieldValue(discountObj.get("date").getAsJsonObject()));
                discount.setDiscountPercentage(parseFieldValue(discountObj.get("discount_percentage").getAsJsonObject()));
                discount.setCode(parseFieldValue(discountObj.get("code").getAsJsonObject()));
                
                discounts.add(discount);
            }
            
            db.theaterDao().insertAllDiscounts(discounts);
            Log.d(TAG, "Loaded " + discounts.size() + " discounts");
        } catch (Exception e) {
            Log.e(TAG, "Error loading discounts", e);
        }
    }

    /**
     * Parse a JsonObject into a FieldValue
     */
    private FieldValue parseFieldValue(JsonObject jsonObject) {
        FieldValue fieldValue = new FieldValue();
        
        // Handle value (might be string, number, or array)
        JsonElement valueElement = jsonObject.get("value");
        if (valueElement.isJsonPrimitive()) {
            if (valueElement.getAsJsonPrimitive().isString()) {
                fieldValue.setValue(valueElement.getAsString());
            } else if (valueElement.getAsJsonPrimitive().isNumber()) {
                fieldValue.setValue(String.valueOf(valueElement.getAsInt()));
            }
        } else if (valueElement.isJsonArray()) {
            // For array values, we'll stringify them for now
            fieldValue.setValue(valueElement.toString());
        }
        
        // Handle possible values array
        JsonElement pvaluesElement = jsonObject.get("pvalues");
        if (pvaluesElement.isJsonArray()) {
            JsonArray pvaluesArray = pvaluesElement.getAsJsonArray();
            List<String> pvalues = new ArrayList<>();
            
            for (JsonElement pvalue : pvaluesArray) {
                if (pvalue.isJsonPrimitive()) {
                    pvalues.add(pvalue.getAsString());
                }
            }
            
            fieldValue.setPValues(pvalues);
        }
        
        return fieldValue;
    }

    /**
     * Read JSON file from assets
     */
    private String readJsonFile(Context context, String filename) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (InputStream is = context.getAssets().open(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
        }
        return buffer.toString();
    }
}
