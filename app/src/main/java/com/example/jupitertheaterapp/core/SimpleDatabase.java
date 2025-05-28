package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

import com.example.jupitertheaterapp.model.MsgTemplate;

public class SimpleDatabase {
    private static final String TAG = "SimpleDatabase";
    
    // Data tables stored as JSONObject for simplicity
    private Map<String, JSONArray> tables;
    private Context context; // Store context for file operations
    
    // Singleton instance
    private static SimpleDatabase instance;
    
    private SimpleDatabase() {
        tables = new HashMap<>();
    }
    
    /**
     * Get the singleton instance of the database
     */
    public static synchronized SimpleDatabase getInstance() {
        if (instance == null) {
            instance = new SimpleDatabase();
        }
        return instance;
    }
    
    /**
     * Initialize the database by loading all JSON files
     * @param context The application context
     */
    public void initialize(Context context) {
        this.context = context; // Store context for later use
        loadJsonFile(context, "sample_shows.json", "shows");
        loadJsonFile(context, "sample_bookings.json", "bookings");
        loadJsonFile(context, "sample_discounts.json", "discounts");
        loadJsonFile(context, "sample_reviews.json", "reviews");
        Log.d(TAG, "Database initialized with " + tables.size() + " tables");
        for (String tableName : tables.keySet()) {
            Log.d(TAG, "Table '" + tableName + "' loaded with " + tables.get(tableName).length() + " records");
        }
    }
    
    /**
     * Load JSON file into memory
     * @param context The application context
     * @param fileName The JSON file name in assets
     * @param tableName The table name to identify the data
     */
    private void loadJsonFile(Context context, String fileName, String tableName) {
        try {
            String jsonString = readJSONFromAsset(context, fileName);
            if (jsonString != null) {
                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.has(tableName)) {
                    tables.put(tableName, jsonObject.getJSONArray(tableName));
                    Log.d(TAG, "Loaded " + fileName + " into table '" + tableName + "'");
                } else {
                    Log.e(TAG, "Table '" + tableName + "' not found in " + fileName);
                }
            } else {
                Log.e(TAG, "Failed to read " + fileName);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing " + fileName + ": " + e.getMessage());
        }
    }
    
    /**
     * Read JSON from assets
     * @param context The application context
     * @param fileName The file name to read
     * @return String content of the file
     */
    private String readJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(TAG, "Error reading file " + fileName + ": " + ex.getMessage());
            return null;
        }
        return json;
    }
    
    /**
     * Map category to table name
     * @param category The category from the conversation
     * @return The corresponding table name
     */
    public String getCategoryTableMapping(String category) {
        switch (category) {
            case "ΠΛΗΡΟΦΟΡΙΕΣ":
                return "shows";
            case "ΚΡΑΤΗΣΗ":
            case "ΑΚΥΡΩΣΗ":
                return "bookings";
            case "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
                return "discounts";
            case "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
                return "reviews";
            default:
                return null;
        }
    }
    
    /**
     * Query records from a table based on template values
     * @param tableName The name of the table to query
     * @param template The message template containing query criteria
     * @return JSON array of matching records or null if not found
     */
    public JSONArray queryRecords(String tableName, MsgTemplate template) {
        if (!tables.containsKey(tableName)) {
            Log.e(TAG, "Table '" + tableName + "' not found");
            return null;
        }
        
        JSONArray table = tables.get(tableName);
        List<JSONObject> results = new ArrayList<>();
        
        try {
            for (int i = 0; i < table.length(); i++) {
                JSONObject record = table.getJSONObject(i);
                if (matchesTemplate(record, template)) {
                    results.add(record);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error querying table '" + tableName + "': " + e.getMessage());
        }
        
        // Convert result list to JSONArray
        JSONArray resultArray = new JSONArray();
        for (JSONObject result : results) {
            resultArray.put(result);
        }        return resultArray;
    }
    
    /**
     * Check if a record matches the template criteria
     * @param record The record to check
     * @param template The template containing the criteria
     * @return True if the record matches all criteria
     */    private boolean matchesTemplate(JSONObject record, MsgTemplate template) {
        // If template is null, match all records (used for debug sampling)
        if (template == null) {
            return true;
        }
        
        Map<String, List<String>> criteria = template.getFieldValuesMap();
        Log.d(TAG, "matchesTemplate: Starting comparison with criteria: " + criteria);
          for (Map.Entry<String, List<String>> entry : criteria.entrySet()) {
            String field = entry.getKey();
            List<String> values = entry.getValue();
            
            Log.d(TAG, "matchesTemplate: Checking field '" + field + "' with values: " + values);
            
            if (values == null || values.isEmpty()) {
                Log.d(TAG, "matchesTemplate: Skipping empty field '" + field + "'");
                continue; // Skip empty criteria
            }

            // Check if record has this field
            try {
                if (!record.has(field)) {
                    // Handle special cases like 'name' vs 'show_name' in different tables
                    if (record.has("show_" + field)) {
                        field = "show_" + field;
                        Log.d(TAG, "matchesTemplate: Found field with 'show_' prefix: " + field);
                    } else {
                        Log.d(TAG, "matchesTemplate: Field '" + field + "' not found in record");
                        return false; // Field doesn't exist
                    }
                }
                  // Get field value from record - handle both string and array formats
                JSONObject fieldObj = record.getJSONObject(field);
                List<String> recordValues = getFieldValues(record, field);
                
                Log.d(TAG, "matchesTemplate: Record values for field '" + field + "': " + recordValues);
                  // Check if any of the template values match any of the record values
                boolean foundMatch = false;
                for (String value : values) {
                    for (String recordValue : recordValues) {
                        Log.d(TAG, "matchesTemplate: Comparing record value '" + recordValue + "' with template value '" + value + "'");
                        
                        // Use smart comparison for special fields
                        if (smartFieldComparison(field, recordValue, value)) {
                            Log.d(TAG, "matchesTemplate: SMART MATCH FOUND for field '" + field + "'");
                            foundMatch = true;
                            break;
                        } else if (caseInsensitiveMatch(recordValue, value)) {
                            Log.d(TAG, "matchesTemplate: EXACT MATCH FOUND for field '" + field + "'");
                            foundMatch = true;
                            break;
                        }
                    }
                    if (foundMatch) break;
                }
                
                // If no match found for this field, record doesn't match criteria
                if (!foundMatch) {
                    Log.d(TAG, "matchesTemplate: NO MATCH found for field '" + field + "' - record rejected");
                    return false;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error accessing field '" + field + "': " + e.getMessage());
                return false;
            }
        }
        
        // All criteria matched
        Log.d(TAG, "matchesTemplate: ALL criteria matched - record accepted");
        return true;
    }
      /**
     * Case-insensitive string matching
     * @param str1 First string
     * @param str2 Second string
     * @return True if strings match ignoring case
     */
    private boolean caseInsensitiveMatch(String str1, String str2) {
        if (str1 == null || str2 == null) return false;
        return str1.toLowerCase(Locale.ROOT).equals(str2.toLowerCase(Locale.ROOT));
    }
    
    /**
     * Smart field comparison for special fields with business logic
     * @param fieldName The name of the field being compared
     * @param recordValue The value from the database record
     * @param templateValue The value from the user template
     * @return True if the values match according to the field's business rules
     */
    private boolean smartFieldComparison(String fieldName, String recordValue, String templateValue) {
        // Handle age field comparisons
        if ("age".equals(fieldName)) {
            return compareAgeFields(recordValue, templateValue);
        }
        
        // Handle stars field comparisons (higher ratings include lower search values)
        if ("stars".equals(fieldName)) {
            return compareStarsFields(recordValue, templateValue);
        }
        
        // Handle number of people field comparisons (equal or fewer people)
        if ("no_of_people".equals(fieldName) || "numberOfPeople".equals(fieldName)) {
            return compareNumberOfPeopleFields(recordValue, templateValue);
        }
        
        // For other fields, use regular comparison
        return false;
    }
    
    /**
     * Compare age fields with special logic:
     * - If user searches for <18, match only <18
     * - If user searches for >18, match both >18 and >65
     * - If user searches for >65, match only >65
     */
    private boolean compareAgeFields(String recordValue, String templateValue) {
        try {
            Log.d(TAG, "compareAgeFields: Comparing record '" + recordValue + "' with template '" + templateValue + "'");
            
            // Direct match first
            if (caseInsensitiveMatch(recordValue, templateValue)) {
                Log.d(TAG, "compareAgeFields: Direct match found");
                return true;
            }
            
            // Special logic for >18 template value
            if ("> 18".equals(templateValue.trim())) {
                // Match both "> 18" and "> 65" records
                boolean matches = "> 18".equals(recordValue.trim()) || "> 65".equals(recordValue.trim());
                Log.d(TAG, "compareAgeFields: >18 template matches " + recordValue + ": " + matches);
                return matches;
            }
            
            // For <18 and >65, only exact matches (already handled above)
            Log.d(TAG, "compareAgeFields: No special match found");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in compareAgeFields: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compare stars fields with special logic:
     * - If user searches for 3 stars, match 3, 4, and 5 star records
     * - Higher user rating searches include all higher actual ratings
     */
    private boolean compareStarsFields(String recordValue, String templateValue) {
        try {
            Log.d(TAG, "compareStarsFields: Comparing record '" + recordValue + "' with template '" + templateValue + "'");
            
            int recordStars = Integer.parseInt(recordValue.trim());
            int templateStars = Integer.parseInt(templateValue.trim());
            
            // Record stars must be >= template stars (higher ratings include lower search values)
            boolean matches = recordStars >= templateStars;
            Log.d(TAG, "compareStarsFields: " + recordStars + " >= " + templateStars + ": " + matches);
            return matches;
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing stars values in compareStarsFields: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compare number of people fields with special logic:
     * - If user searches for 10 people, match 10, 9, 8, 7... (equal or fewer people discounts)
     * - User can benefit from discounts for equal or fewer people
     */
    private boolean compareNumberOfPeopleFields(String recordValue, String templateValue) {
        try {
            Log.d(TAG, "compareNumberOfPeopleFields: Comparing record '" + recordValue + "' with template '" + templateValue + "'");
            
            int recordPeople = Integer.parseInt(recordValue.trim());
            int templatePeople = Integer.parseInt(templateValue.trim());
            
            // Record people must be <= template people (user can use discounts for equal or fewer people)
            boolean matches = recordPeople <= templatePeople;
            Log.d(TAG, "compareNumberOfPeopleFields: " + recordPeople + " <= " + templatePeople + ": " + matches);
            return matches;
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing number of people values in compareNumberOfPeopleFields: " + e.getMessage());
            return false;
        }
    }
      /**
     * Format results as a human-readable string
     * @param results JSONArray of query results
     * @return Formatted string for display
     */
    public String formatResults(JSONArray results) {
        return formatResults(results, null);
    }
    
    /**
     * Format results as a human-readable string using template for Greek field names
     * @param results JSONArray of query results
     * @param template Optional template to get Greek field names from
     * @return Formatted string for display
     */
    public String formatResults(JSONArray results, MsgTemplate template) {
        if (results == null || results.length() == 0) {
            return "Δεν βρέθηκαν αποτελέσματα.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Βρέθηκαν ").append(results.length()).append(" αποτελέσματα:<sep>");
        
        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject record = results.getJSONObject(i);
                sb.append("<sep>• Αποτέλεσμα ").append(i + 1).append(":\n");
                
                // Add all fields from the record
                JSONArray names = record.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String name = names.getString(j);
                        // Get field values (handles both string and array formats)
                        List<String> fieldValues = getFieldValues(record, name);

                        if (!fieldValues.isEmpty()) {
                            // Get Greek field name from template if available
                            String displayName;
                            if (template != null) {
                                displayName = template.getGreekFieldName(name);
                            } else {
                                // Fallback to formatted English name
                                displayName = name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
                            }

                            // Join multiple values with comma and space
                            String formattedValue = String.join(", ", fieldValues);
                            sb.append("  - ").append(displayName).append(": ").append(formattedValue).append("\n");
                        }
                    }
                }
                
                sb.append("\n");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error formatting results: " + e.getMessage());
        }
        
        return sb.toString();
    }
      /**
     * Add a new booking to the database
     * @param template The message template containing booking data
     * @return True if booking was added successfully
     */
    public boolean addBooking(MsgTemplate template) {
        try {
            Log.d(TAG, "addBooking: Starting booking addition with template: " + template);
            if (template != null) {
                Log.d(TAG, "addBooking: Template field values: " + template.getFieldValuesMap());
            }
            
            JSONObject newBooking = createBookingFromTemplate(template);
            Log.d(TAG, "addBooking: Created booking JSON: " + newBooking.toString(2));
            
            // Add to in-memory table
            JSONArray bookingsTable = tables.get("bookings");
            if (bookingsTable != null) {
                bookingsTable.put(newBooking);
                Log.d(TAG, "addBooking: Added new booking to in-memory database. Total bookings: " + bookingsTable.length());
                
                // Write to internal storage for verification
                writeTableToInternalStorage("bookings", "test_bookings.json");
                return true;
            } else {
                Log.e(TAG, "addBooking: Bookings table is null!");
            }
        } catch (JSONException e) {
            Log.e(TAG, "addBooking: Error adding booking: " + e.getMessage());
        }
        return false;
    }/**
     * Remove a booking from the database
     * @param template The message template containing booking criteria to remove
     * @return True if booking was removed successfully
     */
    public boolean removeBooking(MsgTemplate template) {
        try {
            JSONArray bookingsTable = tables.get("bookings");
            if (bookingsTable == null) return false;
            
            Log.d(TAG, "removeBooking: Starting removal process with template: " + template);
            if (template != null) {
                Log.d(TAG, "removeBooking: Template field values: " + template.getFieldValuesMap());
            }
            
            // Find and remove matching booking
            for (int i = 0; i < bookingsTable.length(); i++) {
                JSONObject booking = bookingsTable.getJSONObject(i);
                Log.d(TAG, "removeBooking: Checking booking " + i + ": reservation_id=" + 
                      booking.optJSONObject("reservation_id").optString("value") + 
                      ", reservation_password=" + 
                      booking.optJSONObject("reservation_password").optString("value"));
                
                if (matchesTemplate(booking, template)) {
                    bookingsTable.remove(i);
                    Log.d(TAG, "Removed booking from in-memory database");
                    
                    // Write to internal storage for verification
                    writeTableToInternalStorage("bookings", "test_bookings.json");
                    return true;
                } else {
                    Log.d(TAG, "removeBooking: Booking " + i + " does not match criteria");
                }
            }
            Log.d(TAG, "removeBooking: No matching booking found");
        } catch (JSONException e) {
            Log.e(TAG, "Error removing booking: " + e.getMessage());
        }
        return false;
    }    /**
     * Add a new review to the database
     * @param template The message template containing review data
     * @return True if review was added successfully
     */
    public boolean addReview(MsgTemplate template) {
        try {
            Log.d(TAG, "addReview: Starting review addition with template: " + template);
            if (template != null) {
                Log.d(TAG, "addReview: Template field values: " + template.getFieldValuesMap());
            }
            
            JSONObject newReview = createReviewFromTemplate(template);
            Log.d(TAG, "addReview: Created review JSON: " + newReview.toString(2));
            
            // Add to in-memory table
            JSONArray reviewsTable = tables.get("reviews");
            if (reviewsTable != null) {
                reviewsTable.put(newReview);
                Log.d(TAG, "addReview: Added new review to in-memory database. Total reviews: " + reviewsTable.length());
                
                // Write to internal storage for verification
                writeTableToInternalStorage("reviews", "test_reviews.json");
                return true;
            } else {
                Log.e(TAG, "addReview: Reviews table is null!");
            }
        } catch (JSONException e) {
            Log.e(TAG, "addReview: Error adding review: " + e.getMessage());
        }
        return false;
    }    /**
     * Create a booking JSON object from template data
     * @param template The message template containing booking data
     * @return JSONObject representing the booking
     */
    private JSONObject createBookingFromTemplate(MsgTemplate template) throws JSONException {
        Log.d(TAG, "createBookingFromTemplate: Starting booking creation");
        JSONObject booking = new JSONObject();
        Map<String, List<String>> fields = template.getFieldValuesMap();
        
        Log.d(TAG, "createBookingFromTemplate: Template fields: " + fields);
        
        // Generate unique reservation ID and password
        String reservationId = "RES" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String reservationPassword = "pass" + (int)(Math.random() * 1000);
        
        Log.d(TAG, "createBookingFromTemplate: Generated booking with ID: " + reservationId + " and password: " + reservationPassword);
        
        // Add all fields from template
        for (Map.Entry<String, List<String>> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            List<String> values = entry.getValue();
            
            Log.d(TAG, "createBookingFromTemplate: Processing field '" + fieldName + "' with values: " + values);
            
            if (values != null && !values.isEmpty()) {
                String value = values.get(0); // Take first value
                Log.d(TAG, "createBookingFromTemplate: Setting field '" + fieldName + "' to value '" + value + "'");
                
                // Handle nested person object
                if (fieldName.equals("name") || fieldName.equals("age") || fieldName.equals("seat")) {
                    if (!booking.has("person")) {
                        booking.put("person", new JSONObject());
                        Log.d(TAG, "createBookingFromTemplate: Created person object");
                    }
                    JSONObject person = booking.getJSONObject("person");
                    JSONObject fieldObj = new JSONObject();
                    fieldObj.put("value", value);
                    fieldObj.put("pvalues", new JSONArray());
                    person.put(fieldName, fieldObj);
                    Log.d(TAG, "createBookingFromTemplate: Added person field '" + fieldName + "' = '" + value + "'");
                } else {
                    // Regular field
                    JSONObject fieldObj = new JSONObject();
                    fieldObj.put("value", value);
                    fieldObj.put("pvalues", new JSONArray());
                    booking.put(fieldName, fieldObj);
                    Log.d(TAG, "createBookingFromTemplate: Added regular field '" + fieldName + "' = '" + value + "'");
                }        }
        
        // Add reservation ID and password
        JSONObject resIdObj = new JSONObject();
        resIdObj.put("value", reservationId);
        resIdObj.put("pvalues", new JSONArray());
        booking.put("reservation_id", resIdObj);
        
        JSONObject resPassObj = new JSONObject();
        resPassObj.put("value", reservationPassword);
        resPassObj.put("pvalues", new JSONArray());
        booking.put("reservation_password", resPassObj);
        
        Log.d(TAG, "createBookingFromTemplate: Created booking structure: " + booking.toString(2));
        return booking;
    }
        JSONObject resIdObj = new JSONObject();
        resIdObj.put("value", reservationId);
        resIdObj.put("pvalues", new JSONArray());
        booking.put("reservation_id", resIdObj);
        
        JSONObject resPassObj = new JSONObject();
        resPassObj.put("value", reservationPassword);
        resPassObj.put("pvalues", new JSONArray());
        booking.put("reservation_password", resPassObj);
          return booking;
    }

    /**
     * Create a review JSON object from template data
     * @param template The message template containing review data
     * @return JSONObject representing the review
     */
    private JSONObject createReviewFromTemplate(MsgTemplate template) throws JSONException {
        Log.d(TAG, "createReviewFromTemplate: Starting review creation");
        JSONObject review = new JSONObject();
        Map<String, List<String>> fields = template.getFieldValuesMap();
        
        Log.d(TAG, "createReviewFromTemplate: Template fields: " + fields);
        
        // Add all fields from template
        for (Map.Entry<String, List<String>> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            List<String> values = entry.getValue();
            
            Log.d(TAG, "createReviewFromTemplate: Processing field '" + fieldName + "' with values: " + values);
            
            if (values != null && !values.isEmpty()) {
                String value = values.get(0); // Take first value
                Log.d(TAG, "createReviewFromTemplate: Setting field '" + fieldName + "' to value '" + value + "'");
                
                JSONObject fieldObj = new JSONObject();
                fieldObj.put("value", value);
                fieldObj.put("pvalues", new JSONArray());
                review.put(fieldName, fieldObj);
            } else {
                Log.w(TAG, "createReviewFromTemplate: Skipping empty field '" + fieldName + "'");
            }
        }
        
        Log.d(TAG, "createReviewFromTemplate: Created review structure: " + review.toString(2));
        return review;
    }

    /**
     * Write a table to internal storage for testing/verification
     * @param tableName The name of the table
     * @param filename The filename to write to
     */
    private void writeTableToInternalStorage(String tableName, String filename) {
        if (context == null) {
            Log.w(TAG, "Context is null, cannot write to internal storage");
            return;
        }
        
        try {
            JSONArray table = tables.get(tableName);
            if (table == null) {
                Log.w(TAG, "Table " + tableName + " not found");
                return;
            }
            
            // Create the JSON structure
            JSONObject root = new JSONObject();
            root.put(tableName, table);
            
            // Write to internal storage
            File file = new File(context.getFilesDir(), filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(root.toString(2).getBytes());
            fos.close();
            
            Log.d(TAG, "Successfully wrote " + tableName + " table to " + filename);
            Log.d(TAG, "File location: " + file.getAbsolutePath());
            Log.d(TAG, "Table now contains " + table.length() + " records");
            
        } catch (Exception e) {
            Log.e(TAG, "Error writing table to internal storage: " + e.getMessage());
        }
    }

    /**
     * Get the current count of records in a table - useful for testing
     * @param tableName The name of the table
     * @return The number of records in the table
     */
    public int getTableRecordCount(String tableName) {
        JSONArray table = tables.get(tableName);
        return table != null ? table.length() : 0;
    }

    /**
     * Log current database state - useful for debugging
     */
    public void logDatabaseState() {
        Log.d(TAG, "=== DATABASE STATE ===");
        for (Map.Entry<String, JSONArray> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            JSONArray table = entry.getValue();
            Log.d(TAG, "Table '" + tableName + "': " + table.length() + " records");
        }
        Log.d(TAG, "=====================");
    }
      /**
     * Validate that the booking details match an existing show
     * @param template The message template containing booking data
     * @return True if a matching show exists, false otherwise
     */
    public boolean validateShowExists(MsgTemplate template) {
        try {
            JSONArray showsTable = tables.get("shows");
            if (showsTable == null) {
                Log.e(TAG, "validateShowExists: Shows table is null!");
                return false;
            }
            
            Log.d(TAG, "validateShowExists: Starting validation with template: " + template);
            if (template != null) {
                Log.d(TAG, "validateShowExists: Template field values: " + template.getFieldValuesMap());
            }
            
            // Extract the booking details we need to validate
            Map<String, List<String>> criteria = template.getFieldValuesMap();
            String showName = getFirstValue(criteria, "show_name");
            String room = getFirstValue(criteria, "room");
            String day = getFirstValue(criteria, "day");
            String time = getFirstValue(criteria, "time");
            
            Log.d(TAG, "validateShowExists: Looking for show - name: '" + showName + 
                  "', room: '" + room + "', day: '" + day + "', time: '" + time + "'");
            
            // Check each show in the shows table
            for (int i = 0; i < showsTable.length(); i++) {
                JSONObject show = showsTable.getJSONObject(i);
                
                // Get all possible values for each field (handling arrays)
                List<String> showNames = getFieldValues(show, "name");
                List<String> showRooms = getFieldValues(show, "room");
                List<String> showDays = getFieldValues(show, "day");
                List<String> showTimes = getFieldValues(show, "time");
                
                Log.d(TAG, "validateShowExists: Checking show " + i + " - names: " + showNames + 
                      ", rooms: " + showRooms + ", days: " + showDays + ", times: " + showTimes);
                
                // Check if the booking details match any combination in this show
                boolean nameMatch = containsIgnoreCase(showNames, showName);
                boolean roomMatch = containsIgnoreCase(showRooms, room);
                boolean dayMatch = containsIgnoreCase(showDays, day);
                boolean timeMatch = containsIgnoreCase(showTimes, time);
                
                if (nameMatch && roomMatch && dayMatch && timeMatch) {
                    Log.d(TAG, "validateShowExists: MATCH FOUND! Show exists with all required details");
                    return true;
                }
            }
            
            Log.d(TAG, "validateShowExists: NO MATCHING SHOW FOUND");
            return false;
            
        } catch (JSONException e) {
            Log.e(TAG, "validateShowExists: Error validating show: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to check if a list contains a string (case-insensitive)
     * @param list The list to search in
     * @param target The target string to find
     * @return True if found (case-insensitive), false otherwise
     */
    private boolean containsIgnoreCase(List<String> list, String target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        for (String item : list) {
            if (caseInsensitiveMatch(item, target)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Helper method to get the first value from a criteria list
     * @param criteria The criteria map
     * @param field The field name
     * @return The first value or empty string if not found
     */
    private String getFirstValue(Map<String, List<String>> criteria, String field) {
        List<String> values = criteria.get(field);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return "";
    }
    
    /**
     * Helper method to get field values from a JSON object, handling both string and array formats
     * @param record The JSON record
     * @param fieldName The field name to extract
     * @return List of values from the field
     */
    private List<String> getFieldValues(JSONObject record, String fieldName) {
        List<String> values = new ArrayList<>();
        try {
            if (record.has(fieldName)) {
                JSONObject fieldObj = record.getJSONObject(fieldName);
                
                // Check if value is an array or a string
                Object valueObj = fieldObj.get("value");
                if (valueObj instanceof JSONArray) {
                    // Handle array format (like in shows)
                    JSONArray valueArray = (JSONArray) valueObj;
                    for (int i = 0; i < valueArray.length(); i++) {
                        values.add(valueArray.getString(i));
                    }
                } else if (valueObj instanceof String) {
                    // Handle string format (like in bookings)
                    values.add((String) valueObj);
                } else {
                    // Handle other types (convert to string)
                    values.add(valueObj.toString());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting field values for '" + fieldName + "': " + e.getMessage());
        }
        return values;
    }

    /**
     * Helper method to get the first field value from a JSON object, handling both string and array formats
     * @param record The JSON record
     * @param fieldName The field name to extract
     * @return The first value or empty string if not found
     */
    private String getFirstFieldValue(JSONObject record, String fieldName) {
        List<String> values = getFieldValues(record, fieldName);
        return values.isEmpty() ? "" : values.get(0);
    }
}
