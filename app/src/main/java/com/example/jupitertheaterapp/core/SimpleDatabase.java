package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import com.example.jupitertheaterapp.model.MsgTemplate;

public class SimpleDatabase {
    private static final String TAG = "SimpleDatabase";
    
    // Data tables stored as JSONObject for simplicity
    private Map<String, JSONArray> tables;
    
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
        }
        
        return resultArray;
    }
    
    /**
     * Check if a record matches the template criteria
     * @param record The record to check
     * @param template The template containing the criteria
     * @return True if the record matches all criteria
     */
    private boolean matchesTemplate(JSONObject record, MsgTemplate template) {
        Map<String, List<String>> criteria = template.getFieldValuesMap();
        
        for (Map.Entry<String, List<String>> entry : criteria.entrySet()) {
            String field = entry.getKey();
            List<String> values = entry.getValue();
            
            if (values == null || values.isEmpty()) {
                continue; // Skip empty criteria
            }

            // Check if record has this field
            try {
                if (!record.has(field)) {
                    // Handle special cases like 'name' vs 'show_name' in different tables
                    if (record.has("show_" + field)) {
                        field = "show_" + field;
                    } else {
                        return false; // Field doesn't exist
                    }
                }
                
                // Get field value from record
                JSONObject fieldObj = record.getJSONObject(field);
                String recordValue = fieldObj.getString("value");
                
                // Check if any of the template values match the record value
                boolean foundMatch = false;
                for (String value : values) {
                    if (caseInsensitiveMatch(recordValue, value)) {
                        foundMatch = true;
                        break;
                    }
                }
                
                // If no match found for this field, record doesn't match criteria
                if (!foundMatch) return false;
            } catch (JSONException e) {
                Log.e(TAG, "Error accessing field '" + field + "': " + e.getMessage());
                return false;
            }
        }
        
        // All criteria matched
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
     * Format results as a human-readable string
     * @param results JSONArray of query results
     * @return Formatted string for display
     */
    public String formatResults(JSONArray results) {
        if (results == null || results.length() == 0) {
            return "Δεν βρέθηκαν αποτελέσματα.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Βρέθηκαν ").append(results.length()).append(" αποτελέσματα:\n\n");
        
        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject record = results.getJSONObject(i);
                sb.append("• Αποτέλεσμα ").append(i + 1).append(":\n");
                
                // Add all fields from the record
                JSONArray names = record.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String name = names.getString(j);
                        try {
                            JSONObject fieldObj = record.getJSONObject(name);
                            String value = fieldObj.getString("value");
                            
                            // Format field name nicely
                            String displayName = name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
                            sb.append("  - ").append(displayName).append(": ").append(value).append("\n");
                        } catch (JSONException e) {
                            // Handle special case for nested objects like "person"
                            try {
                                JSONObject nestedObj = record.getJSONObject(name);
                                sb.append("  - ").append(name).append(":\n");
                                
                                JSONArray nestedNames = nestedObj.names();
                                if (nestedNames != null) {
                                    for (int k = 0; k < nestedNames.length(); k++) {
                                        String nestedName = nestedNames.getString(k);
                                        JSONObject nestedFieldObj = nestedObj.getJSONObject(nestedName);
                                        String nestedValue = nestedFieldObj.getString("value");
                                        sb.append("    * ").append(nestedName).append(": ").append(nestedValue).append("\n");
                                    }
                                }
                            } catch (JSONException e2) {
                                // Skip this field
                            }
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
}
