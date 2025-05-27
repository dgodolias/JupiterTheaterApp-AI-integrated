package com.example.jupitertheaterapp.test;

import android.content.Context;
import android.util.Log;
import com.example.jupitertheaterapp.core.ChatbotManager;
import com.example.jupitertheaterapp.core.SimpleDatabase;

/**
 * Test class to verify that the database debug functionality is working correctly
 * This specifically tests the fix for the null pointer exception in getDatabaseTableSample
 */
public class DebugTest {
    private static final String TAG = "DebugTest";
    
    /**
     * Test the database debug functionality to ensure no null pointer exceptions
     * @param context The application context
     */
    public static void testDatabaseDebugFunctionality(Context context) {
        Log.d(TAG, "Starting database debug functionality test...");
        
        try {
            // Initialize the database
            SimpleDatabase db = SimpleDatabase.getInstance();
            db.initialize(context);
              // Initialize the chatbot manager
            ChatbotManager chatbotManager = new ChatbotManager(context);
            
            // Test 1: Test direct database query with null template
            Log.d(TAG, "Test 1: Testing queryRecords with null template...");
            
            String[] tables = {"bookings", "reviews", "shows", "discounts"};
            for (String tableName : tables) {
                try {
                    Log.d(TAG, "Testing table: " + tableName);
                    org.json.JSONArray results = db.queryRecords(tableName, null);
                    if (results != null) {
                        Log.d(TAG, "✓ " + tableName + " table query successful - " + results.length() + " records");
                    } else {
                        Log.w(TAG, "⚠ " + tableName + " table returned null results");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "✗ Error querying " + tableName + " table: " + e.getMessage(), e);
                }
            }
            
            // Test 2: Test the comprehensive debug info method
            Log.d(TAG, "Test 2: Testing getDatabaseDebugInfo...");
            try {
                String debugInfo = chatbotManager.getDatabaseDebugInfo();
                if (debugInfo != null && !debugInfo.isEmpty()) {
                    Log.d(TAG, "✓ Database debug info generated successfully");
                    Log.d(TAG, "Debug info preview (first 200 chars): " + 
                          debugInfo.substring(0, Math.min(200, debugInfo.length())) + "...");
                } else {
                    Log.w(TAG, "⚠ Database debug info is empty or null");
                }
            } catch (Exception e) {
                Log.e(TAG, "✗ Error generating database debug info: " + e.getMessage(), e);
            }
            
            // Test 3: Test individual table record counts
            Log.d(TAG, "Test 3: Testing table record counts...");
            for (String tableName : tables) {
                try {
                    int count = db.getTableRecordCount(tableName);
                    Log.d(TAG, "✓ " + tableName + " table has " + count + " records");
                } catch (Exception e) {
                    Log.e(TAG, "✗ Error getting record count for " + tableName + ": " + e.getMessage(), e);
                }
            }
            
            Log.d(TAG, "Database debug functionality test completed successfully!");
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Fatal error during debug test: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test method specifically for the null template scenario that was causing issues
     * @param context The application context
     */
    public static void testNullTemplateScenario(Context context) {
        Log.d(TAG, "Testing null template scenario specifically...");
        
        try {
            SimpleDatabase db = SimpleDatabase.getInstance();
            db.initialize(context);
            
            // This was the exact call that was causing the null pointer exception
            org.json.JSONArray results = db.queryRecords("bookings", null);
            
            if (results != null) {
                Log.d(TAG, "✓ Null template query successful - returned " + results.length() + " records");
            } else {
                Log.w(TAG, "⚠ Null template query returned null (but no exception)");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Null template test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test method to verify the improved database debug output formatting
     * @param context The application context
     */
    public static void testImprovedDebugOutput(Context context) {
        Log.d(TAG, "Testing improved database debug output formatting...");
        
        try {
            ChatbotManager chatbotManager = new ChatbotManager(context);
            
            // Get the improved database debug info
            String debugInfo = chatbotManager.getDatabaseDebugInfo();
            
            if (debugInfo != null && !debugInfo.isEmpty()) {
                Log.d(TAG, "✓ Improved database debug info generated successfully");
                
                // Check if the output contains actual field values instead of "N/A"
                boolean hasActualData = debugInfo.contains("Ο Μάγος του Οζ") || 
                                      debugInfo.contains("Αντιγόνη") ||
                                      debugInfo.contains("19:30") ||
                                      debugInfo.contains("Παρασκευή");
                
                if (hasActualData) {
                    Log.d(TAG, "✓ Debug output contains actual field values (not just N/A)");
                } else {
                    Log.w(TAG, "⚠ Debug output still shows N/A values - field extraction may need adjustment");
                }
                
                // Log a sample of the debug output
                Log.d(TAG, "Sample debug output:\n" + 
                      debugInfo.substring(0, Math.min(500, debugInfo.length())) + "...");
                      
            } else {
                Log.w(TAG, "⚠ Database debug info is empty or null");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Error testing improved debug output: " + e.getMessage(), e);
        }
    }
}
