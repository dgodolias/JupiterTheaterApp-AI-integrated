package com.example.jupitertheaterapp.test;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jupitertheaterapp.model.MsgTemplate;

public class SearchResultsTest {
    
    public static void main(String[] args) {
        testShowInfoTemplatePreservesSearchCriteria();
        testReviewTemplateStarsArray();
    }
    
    /**
     * Test that ShowInfoTemplate preserves search criteria when server returns empty arrays
     * This reproduces the exact scenario from the debug logs
     */
    public static void testShowInfoTemplatePreservesSearchCriteria() {
        System.out.println("=== Testing ShowInfoTemplate Search Criteria Preservation ===");
        
        try {
            // Create ShowInfoTemplate and populate with confirmed search criteria
            // This simulates what happens at info_confirmation node
            MsgTemplate template = MsgTemplate.createTemplate("ΠΛΗΡΟΦΟΡΙΕΣ");
            
            // Simulate the confirmed search criteria (name=Ο Βυσσινόκηπος, room=Απολλων)
            String confirmedSearchJson = "{"
                + "\"name\": {\"value\": [\"Ο Βυσσινόκηπος\"], \"pvalues\": []},"
                + "\"room\": {\"value\": [\"Απολλων\"], \"pvalues\": []}"
                + "}";
            
            boolean populatedInitial = template.populateFromJsonObject(new JSONObject(confirmedSearchJson));
            System.out.println("Initial population success: " + populatedInitial);
            System.out.println("Template after confirmed search: " + template.toString());
            System.out.println("Has queryable fields after confirmation: " + template.hasQueryableFields());
            
            // Now simulate what happens at info_complete node - server returns ALL EMPTY arrays
            String emptyServerResponseJson = "{"
                + "\"name\": {\"value\": [], \"pvalues\": []},"
                + "\"day\": {\"value\": [], \"pvalues\": []},"
                + "\"topic\": {\"value\": [], \"pvalues\": []},"
                + "\"time\": {\"value\": [], \"pvalues\": []},"
                + "\"cast\": {\"value\": [], \"pvalues\": []},"
                + "\"room\": {\"value\": [], \"pvalues\": []},"
                + "\"duration\": {\"value\": [], \"pvalues\": []},"
                + "\"stars\": {\"value\": [], \"pvalues\": []}"
                + "}";
            
            boolean populatedFinal = template.populateFromJsonObject(new JSONObject(emptyServerResponseJson));
            System.out.println("Final population success: " + populatedFinal);
            System.out.println("Template after empty server response: " + template.toString());
            System.out.println("Has queryable fields after empty response: " + template.hasQueryableFields());
            
            // The test should pass if the template still has queryable fields
            if (template.hasQueryableFields()) {
                System.out.println("✓ SUCCESS: Template preserved search criteria!");
            } else {
                System.out.println("✗ FAILURE: Template lost search criteria!");
            }
            
        } catch (JSONException e) {
            System.out.println("✗ JSON Exception: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Test that ReviewTemplate can handle stars as array format
     */
    public static void testReviewTemplateStarsArray() {
        System.out.println("=== Testing ReviewTemplate Stars Array Handling ===");
        
        try {
            // Create ReviewTemplate
            MsgTemplate template = MsgTemplate.createTemplate("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ");
            
            // Simulate server response with stars as array (like from debug logs)
            String serverResponseJson = "{"
                + "\"reservation_number\": {\"value\": \"RES23456\", \"pvalues\": []},"
                + "\"passcode\": {\"value\": \"pass234\", \"pvalues\": []},"
                + "\"stars\": {\"value\": [5], \"pvalues\": [1, 2, 3, 4, 5]},"
                + "\"review\": {\"value\": \"Καταπληκτική ερμηνεία\", \"pvalues\": []}"
                + "}";
            
            boolean populated = template.populateFromJsonObject(new JSONObject(serverResponseJson));
            System.out.println("Population success: " + populated);
            System.out.println("Template after population: " + template.toString());
            
            if (populated) {
                System.out.println("✓ SUCCESS: ReviewTemplate handled stars array correctly!");
            } else {
                System.out.println("✗ FAILURE: ReviewTemplate failed to handle stars array!");
            }
            
        } catch (JSONException e) {
            System.out.println("✗ JSON Exception: " + e.getMessage());
        }
        
        System.out.println();
    }
}
