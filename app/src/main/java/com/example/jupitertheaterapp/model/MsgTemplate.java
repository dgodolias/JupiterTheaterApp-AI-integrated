package com.example.jupitertheaterapp.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class MsgTemplate {
    private static final Map<String, Supplier<MsgTemplate>> templateMap = new HashMap<>();

    static {
        templateMap.put("ΠΛΗΡΟΦΟΡΙΕΣ", ShowInfoTemplate::new);
        templateMap.put("ΚΡΑΤΗΣΗ", BookingTemplate::new);
        templateMap.put("ΑΚΥΡΩΣΗ", CancellationTemplate::new);
        templateMap.put("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ", DiscountTemplate::new);
        templateMap.put("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", ReviewTemplate::new);
    }

    // Map for translating field names to Greek for display
    protected Map<String, String> fieldNameMap;
    
    /**
     * Gets the Greek display name for a field
     * @param fieldName The field name in English
     * @return The field name in Greek, or the original name if not found
     */
    public String getGreekFieldName(String fieldName) {
        if (fieldNameMap != null && fieldNameMap.containsKey(fieldName)) {
            return fieldNameMap.get(fieldName);
        }
        return fieldName;
    }    /**
     * Get a comma-separated list of missing fields in Greek
     * @param missingFields List of missing field names in English
     * @return Comma-separated list of missing fields in Greek
     */
    public String getMissingFieldsInGreek(List<String> missingFields) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < missingFields.size(); i++) {
            result.append(getGreekFieldName(missingFields.get(i)));
            if (i < missingFields.size() - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }
    
    /**
     * Merges data from another template of the same type
     * This allows template data to be preserved when navigating between nodes
     * 
     * @param otherTemplate The template to merge data from
     * @return true if merged successfully, false otherwise
     */
    public boolean mergeFrom(MsgTemplate otherTemplate) {
        // Only merge if templates are of the same type
        if (otherTemplate == null || !this.getClass().equals(otherTemplate.getClass())) {
            System.out.println("DEBUG: Cannot merge templates of different types");
            return false;
        }
        
        System.out.println("DEBUG: Merging template data from " + otherTemplate.getClass().getSimpleName());
        return true; // Base implementation, subclasses will override with actual merging logic
    }
       

    /**
     * Get a list of field names that are missing or incomplete
     * @return List of field names that are missing values
     */
    public abstract List<String> getMissingFields();
    
    /**
     * Get a comma-separated string of missing field names in Greek
     * @return Comma-separated string of missing field names in Greek
     */
    public String getMissingFieldsAsGreekString() {
        List<String> missing = getMissingFields();
        return getMissingFieldsInGreek(missing);
    }

    /**
     * Get a list of field names that have values (not missing)
     * @return List of field names that have values
     */
    public abstract List<String> getExistingFields();
    
    /**
     * Get a comma-separated string of existing field names in Greek
     * @return Comma-separated string of existing field names in Greek
     */
    public String getExistingFieldsAsGreekString() {
        List<String> existing = getExistingFields();
        return getMissingFieldsInGreek(existing); // Reusing the same method for Greek translation
    }    /**
     * Abstract method to get a map of existing field names and their values
     * @return Map with field names as keys and their values as values
     */
    public abstract Map<String, String> getExistingFieldsWithValues();
    
    /**
     * Get a map of field names to their values list for database queries
     * @return Map with field names as keys and list of values as values
     */
    public abstract Map<String, List<String>> getFieldValuesMap();
    
    /**
     * Checks if the template has any non-empty field values that can be used for queries
     * @return true if at least one field has values, false otherwise
     */
    public boolean hasQueryableFields() {
        Map<String, List<String>> fieldValues = getFieldValuesMap();
        for (List<String> values : fieldValues.values()) {
            if (values != null && !values.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get a formatted string of existing field names with their values in Greek
     * @return Formatted string of existing field names with values
     */
    public String getExistingFieldsWithValuesAsGreekString() {
        Map<String, String> fieldsWithValues = getExistingFieldsWithValues();
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, String> entry : fieldsWithValues.entrySet()) {
            String fieldName = getGreekFieldName(entry.getKey());
            String fieldValue = entry.getValue();
            result.append(fieldName).append(": ").append(fieldValue);
            if (i < fieldsWithValues.size() - 1) {
                result.append(", ");
            }
            i++;
        }
        return result.toString();
    }

    /**
     * Processes a template string by replacing variable placeholders with actual
     * values
     * 
     * @param templateString Template string with variables in <variable_name>
     *                       format
     * @return Processed string with all variables replaced with their values
     */
    public abstract String processTemplate(String templateString);    // Special handler for the <missing> placeholder in templates
    protected String processMissingFieldsPlaceholder(String templateString) {
        // Handle <missing> placeholder for missing fields
        if (templateString.contains("<missing>")) {
            String missingFieldsGreek = getMissingFieldsAsGreekString();
            templateString = templateString.replace("<missing>", missingFieldsGreek);
        }
        
        // Handle <!missing> placeholder for existing fields with their values
        if (templateString.contains("<!missing>")) {
            String existingFieldsWithValuesGreek = getExistingFieldsWithValuesAsGreekString();
            templateString = templateString.replace("<!missing>", existingFieldsWithValuesGreek);
        }
        
        return templateString;
    }

    /**
     * Helper method to replace a template variable with its value
     * 
     * @param template     The template string
     * @param variableName Variable name without brackets
     * @param value        Value to replace the variable with
     * @return Template with the variable replaced
     */
    protected String replaceTemplateVariable(String template, String variableName, String value) {
        if (value == null || value.isEmpty()) {
            value = "[unknown " + variableName + "]";
        }
        return template.replace("<" + variableName + ">", value);
    }

    /**
     * Creates an appropriate MsgTemplate instance based on the provided node ID
     * 
     * @param id The node ID used to determine which template to create
     * @return A new instance of the appropriate MsgTemplate subclass
     * @throws IllegalArgumentException if no template is registered for the given
     *                                  ID
     */
    public static MsgTemplate createTemplate(String id) {
        Supplier<MsgTemplate> supplier = templateMap.get(id);
        if (supplier != null) {
            return supplier.get();
        }
        throw new IllegalArgumentException("Unknown template type: " + id);
    }

    /**
     * Fills the template fields from a JSON string
     * 
     * @param jsonString JSON string to parse
     * @return true if parsing was successful, false otherwise
     */
    public boolean valuesFromJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            // Check for error first
            if (jsonObject.has("error") && !jsonObject.isNull("error")) {
                String error = jsonObject.getString("error");
                System.out.println("Server returned error: " + error);
                // Still return true since we might want to show the error message
                return true;
            }

            // Try to populate from details section if it exists and is not null
            if (jsonObject.has("details") && !jsonObject.isNull("details")) {
                return populateFromDetails(jsonObject);
            }

            // If there are no details or details is null, still return true
            // This allows messages without details to be shown properly
            if (jsonObject.has("details") && jsonObject.isNull("details")) {
                System.out.println("Details is null, skipping template population");
                return true;
            }

            // Fall back to populating from the entire object
            return populateFromJsonObject(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Error parsing JSON: " + e.getMessage());
            // Return true anyway so the message can still be shown
            return true;
        }
    }

    /**
     * Populates the template fields from a JSONObject
     * 
     * @param jsonObject JSONObject to extract values from
     * @return true if population was successful, false otherwise
     */
    protected abstract boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException;

    /**
     * Extracts details from a JSON response
     * 
     * @param jsonObject The JSON object containing the response
     * @return JSONObject with the extracted details or null if not found or null
     */
    protected JSONObject extractDetails(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("details") && !jsonObject.isNull("details")) {
            Object detailsObj = jsonObject.get("details");
            if (detailsObj instanceof JSONObject) {
                return (JSONObject) detailsObj;
            } else {
                System.out.println("Details is not a JSONObject: " + detailsObj);
                return null;
            }
        }
        return null;
    }

    /**
     * Populates the template from the details section of the response
     * 
     * @param jsonObject The complete JSON response object
     * @return true if successfully populated, false otherwise
     */
    protected boolean populateFromDetails(JSONObject jsonObject) throws JSONException {
        try {
            // Check if details exist and are not null
            if (jsonObject.has("details") && !jsonObject.isNull("details")) {
                JSONObject details = jsonObject.getJSONObject("details");
                return populateFromJsonObject(details);
            }

            // If no details or details is null, just return true without trying to populate
            // This allows the message to be shown even without template data
            System.out.println("No details found in JSON, skipping template population");
            return true;
        } catch (JSONException e) {
            System.out.println("Error extracting details: " + e.getMessage());
            // Just return true since we can still show a message even without details
            return true;
        }
    }

    /**
     * Helper methods for JSON extraction
     */
    protected String extractStringValue(JSONObject fieldObject) throws JSONException {
        if (fieldObject.has("value")) {
            return fieldObject.getString("value");
        }
        return "";
    }

    protected int extractIntValue(JSONObject fieldObject) throws JSONException {
        if (fieldObject.has("value")) {
            return fieldObject.getInt("value");
        }
        return 0;
    }

    protected List<String> extractStringListValue(JSONObject fieldObject) throws JSONException {
        List<String> values = new ArrayList<>();
        if (fieldObject.has("value")) {
            JSONArray valueArray = fieldObject.getJSONArray("value");
            for (int i = 0; i < valueArray.length(); i++) {
                values.add(valueArray.getString(i));
            }
        }
        return values;
    }

    protected List<String> extractPossibleStringValues(JSONObject fieldObject) throws JSONException {
        List<String> pValues = new ArrayList<>();
        if (fieldObject.has("pvalues")) {
            JSONArray pvaluesArray = fieldObject.getJSONArray("pvalues");
            for (int i = 0; i < pvaluesArray.length(); i++) {
                pValues.add(pvaluesArray.getString(i));
            }
        }
        return pValues;
    }

    protected List<Integer> extractPossibleIntValues(JSONObject fieldObject) throws JSONException {
        List<Integer> pValues = new ArrayList<>();
        if (fieldObject.has("pvalues")) {
            JSONArray pvaluesArray = fieldObject.getJSONArray("pvalues");
            for (int i = 0; i < pvaluesArray.length(); i++) {
                try {
                    // Try to parse as integer
                    pValues.add(pvaluesArray.getInt(i));
                } catch (JSONException e) {
                    // If it's not a direct integer, skip it
                    System.out.println("Skipping non-integer value in pvalues: " + pvaluesArray.getString(i));
                    // We don't add string values like ">3" or "<4" to the integer list
                }
            }
        }
        return pValues;
    }
}

/**
 * Template for show information requests
 */
class ShowInfoTemplate extends MsgTemplate {
    private List<String> name;
    private List<String> day;
    private List<String> topic;
    private List<String> time;
    private List<String> cast;
    private List<String> room;
    private List<String> duration;
    private List<String> stars;

    private List<String> possibleDays;
    private List<Integer> possibleStarRatings;

    public ShowInfoTemplate() {
        name = new ArrayList<>();
        day = new ArrayList<>();
        topic = new ArrayList<>();
        time = new ArrayList<>();
        cast = new ArrayList<>();
        room = new ArrayList<>();
        duration = new ArrayList<>();
        stars = new ArrayList<>();

        possibleDays = new ArrayList<>();
        possibleStarRatings = new ArrayList<>();
        
        // Initialize Greek field name mapping
        fieldNameMap = new HashMap<>();
        fieldNameMap.put("name", "όνομα παράστασης");
        fieldNameMap.put("day", "ημέρα");
        fieldNameMap.put("topic", "θέμα");
        fieldNameMap.put("time", "ώρα");
        fieldNameMap.put("cast", "ηθοποιοί");
        fieldNameMap.put("room", "αίθουσα");
        fieldNameMap.put("duration", "διάρκεια");
        fieldNameMap.put("stars", "βαθμολογία");
    }    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            // Only update fields that have non-empty values in the JSON response
            if (jsonObject.has("name")) {
                List<String> newName = extractStringListValue(jsonObject.getJSONObject("name"));
                if (newName != null && !newName.isEmpty()) {
                    name = newName;
                }
            }

            if (jsonObject.has("day")) {
                JSONObject dayObject = jsonObject.getJSONObject("day");
                List<String> newDay = extractStringListValue(dayObject);
                if (newDay != null && !newDay.isEmpty()) {
                    day = newDay;
                }
                // Always update possible values if provided
                List<String> newPossibleDays = extractPossibleStringValues(dayObject);
                if (!newPossibleDays.isEmpty()) {
                    possibleDays = newPossibleDays;
                }
            }

            if (jsonObject.has("topic")) {
                List<String> newTopic = extractStringListValue(jsonObject.getJSONObject("topic"));
                if (newTopic != null && !newTopic.isEmpty()) {
                    topic = newTopic;
                }
            }

            if (jsonObject.has("time")) {
                List<String> newTime = extractStringListValue(jsonObject.getJSONObject("time"));
                if (newTime != null && !newTime.isEmpty()) {
                    time = newTime;
                }
            }

            if (jsonObject.has("cast")) {
                List<String> newCast = extractStringListValue(jsonObject.getJSONObject("cast"));
                if (newCast != null && !newCast.isEmpty()) {
                    cast = newCast;
                }
            }

            if (jsonObject.has("room")) {
                List<String> newRoom = extractStringListValue(jsonObject.getJSONObject("room"));
                if (newRoom != null && !newRoom.isEmpty()) {
                    room = newRoom;
                }
            }

            if (jsonObject.has("duration")) {
                List<String> newDuration = extractStringListValue(jsonObject.getJSONObject("duration"));
                if (newDuration != null && !newDuration.isEmpty()) {
                    duration = newDuration;
                }
            }

            if (jsonObject.has("stars")) {
                JSONObject starsObject = jsonObject.getJSONObject("stars");
                List<String> newStars = extractStringListValue(starsObject);
                if (newStars != null && !newStars.isEmpty()) {
                    stars = newStars;
                }
                // Always update possible values if provided
                List<Integer> newPossibleStarRatings = extractPossibleIntValues(starsObject);
                if (!newPossibleStarRatings.isEmpty()) {
                    possibleStarRatings = newPossibleStarRatings;
                }
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getMissingFields() {
        List<String> missingFields = new ArrayList<>();
        if (name.isEmpty()) missingFields.add("name");
        if (day.isEmpty()) missingFields.add("day");
        if (topic.isEmpty()) missingFields.add("topic");
        if (time.isEmpty()) missingFields.add("time");
        if (cast.isEmpty()) missingFields.add("cast");
        if (room.isEmpty()) missingFields.add("room");
        if (duration.isEmpty()) missingFields.add("duration");
        if (stars.isEmpty()) missingFields.add("stars");
        return missingFields;
    }

    @Override
    public List<String> getExistingFields() {
        List<String> existingFields = new ArrayList<>();
        if (!name.isEmpty()) existingFields.add("name");
        if (!day.isEmpty()) existingFields.add("day");
        if (!topic.isEmpty()) existingFields.add("topic");
        if (!time.isEmpty()) existingFields.add("time");
        if (!cast.isEmpty()) existingFields.add("cast");
        if (!room.isEmpty()) existingFields.add("room");
        if (!duration.isEmpty()) existingFields.add("duration");
        if (!stars.isEmpty()) existingFields.add("stars");
        return existingFields;
    }

    @Override
    public Map<String, String> getExistingFieldsWithValues() {
        Map<String, String> fieldsWithValues = new HashMap<>();
        for (String n : name) {
            fieldsWithValues.put("name", n);
        }
        for (String d : day) {
            fieldsWithValues.put("day", d);
        }
        for (String t : topic) {
            fieldsWithValues.put("topic", t);
        }
        for (String ti : time) {
            fieldsWithValues.put("time", ti);
        }
        for (String c : cast) {
            fieldsWithValues.put("cast", c);
        }
        for (String r : room) {
            fieldsWithValues.put("room", r);
        }
        for (String du : duration) {
            fieldsWithValues.put("duration", du);
        }
        for (String s : stars) {
            fieldsWithValues.put("stars", s);
        }
        return fieldsWithValues;
    }

    @Override
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        for (String n : name) {
            templateString = replaceTemplateVariable(templateString, "name", n);
        }
        for (String d : day) {
            templateString = replaceTemplateVariable(templateString, "day", d);
        }
        for (String t : topic) {
            templateString = replaceTemplateVariable(templateString, "topic", t);
        }
        for (String ti : time) {
            templateString = replaceTemplateVariable(templateString, "time", ti);
        }
        for (String c : cast) {
            templateString = replaceTemplateVariable(templateString, "cast", c);
        }
        for (String r : room) {
            templateString = replaceTemplateVariable(templateString, "room", r);
        }
        for (String du : duration) {
            templateString = replaceTemplateVariable(templateString, "duration", du);
        }
        for (String s : stars) {
            templateString = replaceTemplateVariable(templateString, "stars", s);
        }
        return templateString;
    }    @Override
    public boolean mergeFrom(MsgTemplate otherTemplate) {
        // Call parent method to ensure same type check
        if (!super.mergeFrom(otherTemplate)) {
            return false;
        }
        
        // Cast to ShowInfoTemplate since we know it's the same type
        ShowInfoTemplate other = (ShowInfoTemplate) otherTemplate;
        
        // Merge lists, only if this template's lists are empty
        if (this.name == null || this.name.isEmpty()) {
            this.name = new ArrayList<>(other.name);
        }
        
        if (this.day == null || this.day.isEmpty()) {
            this.day = new ArrayList<>(other.day);
        }
        
        if (this.topic == null || this.topic.isEmpty()) {
            this.topic = new ArrayList<>(other.topic);
        }
        
        if (this.time == null || this.time.isEmpty()) {
            this.time = new ArrayList<>(other.time);
        }
        
        if (this.cast == null || this.cast.isEmpty()) {
            this.cast = new ArrayList<>(other.cast);
        }
        
        if (this.room == null || this.room.isEmpty()) {
            this.room = new ArrayList<>(other.room);
        }
        
        if (this.duration == null || this.duration.isEmpty()) {
            this.duration = new ArrayList<>(other.duration);
        }
        
        if (this.stars == null || this.stars.isEmpty()) {
            this.stars = new ArrayList<>(other.stars);
        }
        
        // Merge possible values
        if (this.possibleDays == null || this.possibleDays.isEmpty()) {
            this.possibleDays = new ArrayList<>(other.possibleDays);
        }
        
        if (this.possibleStarRatings == null || this.possibleStarRatings.isEmpty()) {
            this.possibleStarRatings = new ArrayList<>(other.possibleStarRatings);
        }
        
        return true;
    }
    
    @Override
    public Map<String, List<String>> getFieldValuesMap() {
        Map<String, List<String>> fieldsMap = new HashMap<>();
        fieldsMap.put("name", name);
        fieldsMap.put("day", day);
        fieldsMap.put("topic", topic);
        fieldsMap.put("time", time);
        fieldsMap.put("cast", cast);
        fieldsMap.put("room", room);
        fieldsMap.put("duration", duration);
        fieldsMap.put("stars", stars);
        return fieldsMap;
    }

    // Get the total number of fields in this template
    public int getTotalFieldCount() {
        return 8; // name, day, topic, time, cast, room, duration, stars
    }

    // Getters and setters
    public List<String> getName() {
        return name;
    }

    public List<String> getDay() {
        return day;
    }

    public List<String> getTopic() {
        return topic;
    }

    public List<String> getTime() {
        return time;
    }

    public List<String> getCast() {
        return cast;
    }

    public List<String> getRoom() {
        return room;
    }

    public List<String> getDuration() {
        return duration;
    }

    public List<String> getStars() {
        return stars;
    }

    public List<String> getPossibleDays() {
        return possibleDays;
    }

    public List<Integer> getPossibleStarRatings() {
        return possibleStarRatings;
    }

    // tostring
    @Override
    public String toString() {
        return "ShowInfoTemplate{" +
                "name=" + name +
                ", day=" + day +
                ", topic=" + topic +
                ", time=" + time +
                ", cast=" + cast +
                ", room=" + room +
                ", duration=" + duration +
                ", stars=" + stars +
                '}';
    }
}

/**
 * Template for booking requests
 */
class BookingTemplate extends MsgTemplate {
    private String showName;
    private String room;
    private String day;
    private String time;
    private Person person;

    private List<String> possibleDays;

    public BookingTemplate() {
        showName = "";
        room = "";
        day = "";
        time = "";
        person = new Person();
        possibleDays = new ArrayList<>();
        
        // Initialize Greek field name mapping
        fieldNameMap = new HashMap<>();
        fieldNameMap.put("show_name", "όνομα παράστασης");
        fieldNameMap.put("room", "αίθουσα");
        fieldNameMap.put("day", "ημέρα");
        fieldNameMap.put("time", "ώρα");
        fieldNameMap.put("person_name", "όνομα");
        fieldNameMap.put("person_age", "ηλικιακή κατηγορία");
        fieldNameMap.put("person_seat", "θέση");
    }    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            // Only update fields that have non-empty values in the JSON response
            if (jsonObject.has("show_name")) {
                String newShowName = extractStringValue(jsonObject.getJSONObject("show_name"));
                if (newShowName != null && !newShowName.isEmpty()) {
                    showName = newShowName;
                }
            }

            if (jsonObject.has("room")) {
                String newRoom = extractStringValue(jsonObject.getJSONObject("room"));
                if (newRoom != null && !newRoom.isEmpty()) {
                    room = newRoom;
                }
            }

            if (jsonObject.has("day")) {
                JSONObject dayObject = jsonObject.getJSONObject("day");
                String newDay = extractStringValue(dayObject);
                if (newDay != null && !newDay.isEmpty()) {
                    day = newDay;
                }
                // Always update possible values if provided
                List<String> newPossibleDays = extractPossibleStringValues(dayObject);
                if (!newPossibleDays.isEmpty()) {
                    possibleDays = newPossibleDays;
                }
            }

            if (jsonObject.has("time")) {
                String newTime = extractStringValue(jsonObject.getJSONObject("time"));
                if (newTime != null && !newTime.isEmpty()) {
                    time = newTime;
                }
            }

            if (jsonObject.has("person")) {
                JSONObject personObject = jsonObject.getJSONObject("person");
                // Initialize person if it doesn't exist
                if (person == null) {
                    person = new Person();
                }

                if (personObject.has("name")) {
                    String newPersonName = extractStringValue(personObject.getJSONObject("name"));
                    if (newPersonName != null && !newPersonName.isEmpty()) {
                        person.setName(newPersonName);
                    }
                }

                if (personObject.has("age")) {
                    JSONObject ageObject = personObject.getJSONObject("age");
                    String newAge = extractStringValue(ageObject);
                    if (newAge != null && !newAge.isEmpty()) {
                        person.setAge(newAge);
                    }
                    // Always update possible values if provided
                    List<String> newPossibleAgeCategories = extractPossibleStringValues(ageObject);
                    if (!newPossibleAgeCategories.isEmpty()) {
                        person.setPossibleAgeCategories(newPossibleAgeCategories);
                    }
                }

                if (personObject.has("seat")) {
                    String newSeat = extractStringValue(personObject.getJSONObject("seat"));
                    if (newSeat != null && !newSeat.isEmpty()) {
                        person.setSeat(newSeat);
                    }
                }
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getMissingFields() {
        List<String> missingFields = new ArrayList<>();
        if (showName.isEmpty()) missingFields.add("show_name");
        if (room.isEmpty()) missingFields.add("room");
        if (day.isEmpty()) missingFields.add("day");
        if (time.isEmpty()) missingFields.add("time");
        if (person.getName().isEmpty()) missingFields.add("person_name");
        if (person.getAge().isEmpty()) missingFields.add("person_age");
        if (person.getSeat().isEmpty()) missingFields.add("person_seat");
        return missingFields;
    }

    @Override
    public List<String> getExistingFields() {
        List<String> existingFields = new ArrayList<>();
        if (!showName.isEmpty()) existingFields.add("show_name");
        if (!room.isEmpty()) existingFields.add("room");
        if (!day.isEmpty()) existingFields.add("day");
        if (!time.isEmpty()) existingFields.add("time");
        if (!person.getName().isEmpty()) existingFields.add("person_name");
        if (!person.getAge().isEmpty()) existingFields.add("person_age");
        if (!person.getSeat().isEmpty()) existingFields.add("person_seat");
        return existingFields;
    }

    @Override
    public Map<String, String> getExistingFieldsWithValues() {
        Map<String, String> fieldsWithValues = new HashMap<>();
        fieldsWithValues.put("show_name", showName);
        fieldsWithValues.put("room", room);
        fieldsWithValues.put("day", day);
        fieldsWithValues.put("time", time);
        fieldsWithValues.put("person_name", person.getName());
        fieldsWithValues.put("person_age", person.getAge());
        fieldsWithValues.put("person_seat", person.getSeat());
        return fieldsWithValues;
    }

    @Override
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        templateString = replaceTemplateVariable(templateString, "show_name", showName);
        templateString = replaceTemplateVariable(templateString, "room", room);
        templateString = replaceTemplateVariable(templateString, "day", day);
        templateString = replaceTemplateVariable(templateString, "time", time);
        templateString = replaceTemplateVariable(templateString, "person_name", person.getName());
        templateString = replaceTemplateVariable(templateString, "person_age", person.getAge());
        templateString = replaceTemplateVariable(templateString, "person_seat", person.getSeat());
        return templateString;
    }    @Override
    public boolean mergeFrom(MsgTemplate otherTemplate) {
        // Call parent method to ensure same type check
        if (!super.mergeFrom(otherTemplate)) {
            return false;
        }
        
        // Cast to BookingTemplate since we know it's the same type
        BookingTemplate other = (BookingTemplate) otherTemplate;
        
        // Only merge fields that are empty in this template but have values in the other
        if (this.showName == null || this.showName.isEmpty()) {
            this.showName = other.showName;
        }
        
        if (this.room == null || this.room.isEmpty()) {
            this.room = other.room;
        }
        
        if (this.day == null || this.day.isEmpty()) {
            this.day = other.day;
        }
        
        if (this.time == null || this.time.isEmpty()) {
            this.time = other.time;
        }
        
        // Handle person object specially
        if (this.person == null) {
            this.person = other.person;
        } else if (other.person != null) {
            // Merge person fields individually
            if (this.person.getName() == null || this.person.getName().isEmpty()) {
                this.person.setName(other.person.getName());
            }
            
            if (this.person.getAge() == null || this.person.getAge().isEmpty()) {
                this.person.setAge(other.person.getAge());
            }
            
            if (this.person.getSeat() == null || this.person.getSeat().isEmpty()) {
                this.person.setSeat(other.person.getSeat());
            }
        }
        
        // Merge possible days list
        if (this.possibleDays == null || this.possibleDays.isEmpty()) {
            this.possibleDays = new ArrayList<>(other.possibleDays);
        }
        
        return true;
    }
    
    @Override
    public Map<String, List<String>> getFieldValuesMap() {
        Map<String, List<String>> fieldsMap = new HashMap<>();
        
        // Convert single string values to lists for consistent interface
        if (showName != null && !showName.isEmpty()) {
            List<String> showNameList = new ArrayList<>();
            showNameList.add(showName);
            fieldsMap.put("show_name", showNameList);
        } else {
            fieldsMap.put("show_name", new ArrayList<>());
        }
        
        if (room != null && !room.isEmpty()) {
            List<String> roomList = new ArrayList<>();
            roomList.add(room);
            fieldsMap.put("room", roomList);
        } else {
            fieldsMap.put("room", new ArrayList<>());
        }
        
        if (day != null && !day.isEmpty()) {
            List<String> dayList = new ArrayList<>();
            dayList.add(day);
            fieldsMap.put("day", dayList);
        } else {
            fieldsMap.put("day", new ArrayList<>());
        }
        
        if (time != null && !time.isEmpty()) {
            List<String> timeList = new ArrayList<>();
            timeList.add(time);
            fieldsMap.put("time", timeList);
        } else {
            fieldsMap.put("time", new ArrayList<>());
        }
        
        // Add person data if available
        if (person != null) {
            if (person.getName() != null && !person.getName().isEmpty()) {
                List<String> personNameList = new ArrayList<>();
                personNameList.add(person.getName());
                fieldsMap.put("person_name", personNameList);
            } else {
                fieldsMap.put("person_name", new ArrayList<>());
            }
            
            // Add other person fields as needed
        }
        
        return fieldsMap;
    }

    // Person inner class for booking
    public static class Person {
        private String name;
        private String age;
        private String seat;
        private List<String> possibleAgeCategories;

        public Person() {
            name = "";
            age = "";
            seat = "";
            possibleAgeCategories = new ArrayList<>();
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        public String getSeat() {
            return seat;
        }

        public void setSeat(String seat) {
            this.seat = seat;
        }

        public List<String> getPossibleAgeCategories() {
            return possibleAgeCategories;
        }

        public void setPossibleAgeCategories(List<String> possibleAgeCategories) {
            this.possibleAgeCategories = possibleAgeCategories;
        }
    }

    // Getters
    public String getShowName() {
        return showName;
    }

    public String getRoom() {
        return room;
    }

    public String getDay() {
        return day;
    }

    public String getTime() {
        return time;
    }

    public Person getPerson() {
        return person;
    }

    public List<String> getPossibleDays() {
        return possibleDays;
    }

    // tostring
    @Override

    public String toString() {
        return "BookingTemplate{" +
                "showName='" + showName + '\'' +
                ", room='" + room + '\'' +
                ", day='" + day + '\'' +
                ", time='" + time + '\'' +
                ", person=" + person +
                '}';
    }

}

/**
 * Template for cancellation requests
 */
class CancellationTemplate extends MsgTemplate {
    private String reservationNumber;
    private String passcode;    public CancellationTemplate() {
        reservationNumber = "";
        passcode = "";
        
        // Initialize Greek field name mapping
        fieldNameMap = new HashMap<>();
        fieldNameMap.put("reservation_number", "αριθμός κράτησης");
        fieldNameMap.put("reservation_password", "κωδικός επιβεβαίωσης");
    }@Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            // Only update fields that have non-empty values in the JSON response
            if (jsonObject.has("reservation_number")) {
                String newReservationNumber = extractStringValue(jsonObject.getJSONObject("reservation_number"));
                if (newReservationNumber != null && !newReservationNumber.isEmpty()) {
                    reservationNumber = newReservationNumber;
                }
            }

            if (jsonObject.has("passcode")) {
                String newPasscode = extractStringValue(jsonObject.getJSONObject("passcode"));
                if (newPasscode != null && !newPasscode.isEmpty()) {
                    passcode = newPasscode;
                }
            }
            
            // Also handle reservation_password field from database records
            if (jsonObject.has("reservation_password")) {
                String newPasscode = extractStringValue(jsonObject.getJSONObject("reservation_password"));
                if (newPasscode != null && !newPasscode.isEmpty()) {
                    passcode = newPasscode;
                }
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }@Override
    public boolean mergeFrom(MsgTemplate otherTemplate) {
        // Call parent method to ensure same type check
        if (!super.mergeFrom(otherTemplate)) {
            return false;
        }
        
        // Cast to CancellationTemplate since we know it's the same type
        CancellationTemplate other = (CancellationTemplate) otherTemplate;
        
        // Only merge fields that are empty in this template but have values in the other
        if (this.reservationNumber == null || this.reservationNumber.isEmpty()) {
            this.reservationNumber = other.getReservationNumber();
        }
        
        if (this.passcode == null || this.passcode.isEmpty()) {
            this.passcode = other.getPasscode();
        }
        
        return true;
    }

    @Override
    public List<String> getMissingFields() {
        List<String> missingFields = new ArrayList<>();
        if (reservationNumber.isEmpty()) missingFields.add("reservation_number");
        if (passcode.isEmpty()) missingFields.add("passcode");
        return missingFields;
    }

    @Override
    public List<String> getExistingFields() {
        List<String> existingFields = new ArrayList<>();
        if (!reservationNumber.isEmpty()) existingFields.add("reservation_number");
        if (!passcode.isEmpty()) existingFields.add("passcode");
        return existingFields;
    }

    @Override
    public Map<String, String> getExistingFieldsWithValues() {
        Map<String, String> fieldsWithValues = new HashMap<>();
        fieldsWithValues.put("reservation_number", reservationNumber);
        fieldsWithValues.put("passcode", passcode);
        return fieldsWithValues;
    }

    @Override
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        templateString = replaceTemplateVariable(templateString, "reservation_number", reservationNumber);
        templateString = replaceTemplateVariable(templateString, "passcode", passcode);
        return templateString;
    }    @Override
    public Map<String, List<String>> getFieldValuesMap() {
        Map<String, List<String>> fieldsMap = new HashMap<>();
        
        // Convert single string values to lists for consistency
        if (reservationNumber != null && !reservationNumber.isEmpty()) {
            List<String> reservationList = new ArrayList<>();
            reservationList.add(reservationNumber);
            fieldsMap.put("reservation_id", reservationList);  // Use "reservation_id" to match the field in JSON
        } else {
            fieldsMap.put("reservation_id", new ArrayList<>());
        }
        
        if (passcode != null && !passcode.isEmpty()) {
            List<String> passcodeList = new ArrayList<>();
            passcodeList.add(passcode);
            fieldsMap.put("reservation_password", passcodeList);  // Use "reservation_password" to match the field in JSON
        } else {
            fieldsMap.put("reservation_password", new ArrayList<>());
        }
        
        return fieldsMap;
    }

    // Getters
    public String getReservationNumber() {
        return reservationNumber;
    }

    public String getPasscode() {
        return passcode;
    }

    // tostring
    @Override
    public String toString() {
        return "CancellationTemplate{" +
                "reservationNumber='" + reservationNumber + '\'' +
                ", passcode='" + passcode + '\'' +
                '}';
    }

}

/**
 * Template for discount requests
 */
class DiscountTemplate extends MsgTemplate {
    private List<String> showName;
    private int numberOfPeople;
    private List<String> age;
    private List<String> date;
    private List<String> possibleAgeCategories;

    public DiscountTemplate() {
        showName = new ArrayList<>();
        numberOfPeople = 0;
        age = new ArrayList<>();
        date = new ArrayList<>();
        possibleAgeCategories = new ArrayList<>();
        
        // Initialize Greek field name mapping
        fieldNameMap = new HashMap<>();
        fieldNameMap.put("show_name", "όνομα παράστασης");
        fieldNameMap.put("no_of_people", "αριθμός ατόμων");
        fieldNameMap.put("age", "ηλικιακή κατηγορία");
        fieldNameMap.put("date", "ημερομηνία");
    }    @Override
    public boolean mergeFrom(MsgTemplate otherTemplate) {
        // Call parent method to ensure same type check
        if (!super.mergeFrom(otherTemplate)) {
            return false;
        }
        
        // Cast to DiscountTemplate since we know it's the same type
        DiscountTemplate other = (DiscountTemplate) otherTemplate;
        
        // Only merge fields that are empty in this template but have values in the other
        if (this.showName == null || this.showName.isEmpty()) {
            this.showName = new ArrayList<>(other.getShowName());
        }
        
        if (this.numberOfPeople == 0) {
            this.numberOfPeople = other.getNumberOfPeople();
        }
        
        if (this.age == null || this.age.isEmpty()) {
            this.age = new ArrayList<>(other.getAge());
        }
        
        if (this.date == null || this.date.isEmpty()) {
            this.date = new ArrayList<>(other.getDate());
        }
        
        if (this.possibleAgeCategories == null || this.possibleAgeCategories.isEmpty()) {
            this.possibleAgeCategories = new ArrayList<>(other.getPossibleAgeCategories());
        }
        
        return true;
    }
    
    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            // Only update fields that have non-empty values in the JSON response
            if (jsonObject.has("show_name")) {
                List<String> newShowName = extractStringListValue(jsonObject.getJSONObject("show_name"));
                if (newShowName != null && !newShowName.isEmpty()) {
                    showName = newShowName;
                }
            }

            if (jsonObject.has("no_of_people")) {
                int newNumberOfPeople = extractIntValue(jsonObject.getJSONObject("no_of_people"));
                if (newNumberOfPeople > 0) {
                    numberOfPeople = newNumberOfPeople;
                }
            }

            if (jsonObject.has("age")) {
                JSONObject ageObject = jsonObject.getJSONObject("age");
                List<String> newAge = extractStringListValue(ageObject);
                if (newAge != null && !newAge.isEmpty()) {
                    age = newAge;
                }
                // Always update possible values if provided
                List<String> newPossibleAgeCategories = extractPossibleStringValues(ageObject);
                if (!newPossibleAgeCategories.isEmpty()) {
                    possibleAgeCategories = newPossibleAgeCategories;
                }
            }

            if (jsonObject.has("date")) {
                List<String> newDate = extractStringListValue(jsonObject.getJSONObject("date"));
                if (newDate != null && !newDate.isEmpty()) {
                    date = newDate;
                }
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getMissingFields() {
        List<String> missingFields = new ArrayList<>();
        if (showName.isEmpty()) missingFields.add("show_name");
        if (numberOfPeople <= 0) missingFields.add("no_of_people");
        if (age.isEmpty()) missingFields.add("age");
        if (date.isEmpty()) missingFields.add("date");
        return missingFields;
    }

    @Override
    public List<String> getExistingFields() {
        List<String> existingFields = new ArrayList<>();
        if (!showName.isEmpty()) existingFields.add("show_name");
        if (numberOfPeople > 0) existingFields.add("no_of_people");
        if (!age.isEmpty()) existingFields.add("age");
        if (!date.isEmpty()) existingFields.add("date");
        return existingFields;
    }

    @Override
    public Map<String, String> getExistingFieldsWithValues() {
        Map<String, String> fieldsWithValues = new HashMap<>();
        for (String s : showName) {
            fieldsWithValues.put("show_name", s);
        }
        fieldsWithValues.put("no_of_people", String.valueOf(numberOfPeople));
        for (String a : age) {
            fieldsWithValues.put("age", a);
        }
        for (String d : date) {
            fieldsWithValues.put("date", d);
        }
        return fieldsWithValues;
    }

    @Override
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        for (String s : showName) {
            templateString = replaceTemplateVariable(templateString, "show_name", s);
        }
        templateString = replaceTemplateVariable(templateString, "no_of_people", String.valueOf(numberOfPeople));
        for (String a : age) {
            templateString = replaceTemplateVariable(templateString, "age", a);
        }
        for (String d : date) {
            templateString = replaceTemplateVariable(templateString, "date", d);
        }
        return templateString;
    }    // Getter methods needed for mergeFrom
    public List<String> getShowName() {
        return showName;
    }
    
    public int getNumberOfPeople() {
        return numberOfPeople;
    }
    
    public List<String> getAge() {
        return age;
    }
    
    public List<String> getDate() {
        return date;
    }
    
    public List<String> getPossibleAgeCategories() {
        return possibleAgeCategories;
    }
    
    public int getTotalFieldCount() {
        return 4; // showName, numberOfPeople, age, date
    }
    
    @Override
    public Map<String, List<String>> getFieldValuesMap() {
        Map<String, List<String>> fieldsMap = new HashMap<>();
        fieldsMap.put("show_name", showName);
        fieldsMap.put("age", age);
        fieldsMap.put("date", date);
        
        // Handle numeric value
        if (numberOfPeople > 0) {
            List<String> peopleList = new ArrayList<>();
            peopleList.add(String.valueOf(numberOfPeople));
            fieldsMap.put("no_of_people", peopleList);
        } else {
            fieldsMap.put("no_of_people", new ArrayList<>());
        }
        
        return fieldsMap;
    }    // NOTE: Removed duplicated getTotalFieldCount and getter methods

    // tostring
    @Override
    public String toString() {
        return "DiscountTemplate{" +
                "showName=" + showName +
                ", numberOfPeople=" + numberOfPeople +
                ", age=" + age +
                ", date=" + date +
                ", possibleAgeCategories=" + possibleAgeCategories +
                '}';
    }
}

/**
 * Template for review requests
 */
class ReviewTemplate extends MsgTemplate {
    private String reservationNumber;
    private String passcode;
    private int stars;
    private String review;
    private List<Integer> possibleStarRatings;

    public ReviewTemplate() {
        reservationNumber = "";
        passcode = "";
        stars = 0;
        review = "";
        possibleStarRatings = new ArrayList<>();
        
        // Initialize Greek field name mapping
        fieldNameMap = new HashMap<>();
        fieldNameMap.put("reservation_number", "αριθμός κράτησης");
        fieldNameMap.put("passcode", "κωδικός επιβεβαίωσης");
        fieldNameMap.put("stars", "βαθμολογία αστεριών");
        fieldNameMap.put("review", "σχόλιο αξιολόγησης");
    }
    
    @Override
    public boolean mergeFrom(MsgTemplate otherTemplate) {
        // Call parent method to ensure same type check
        if (!super.mergeFrom(otherTemplate)) {
            return false;
        }
        
        // Cast to ReviewTemplate since we know it's the same type
        ReviewTemplate other = (ReviewTemplate) otherTemplate;
        
        // Only merge fields that are empty in this template but have values in the other
        if (this.reservationNumber == null || this.reservationNumber.isEmpty()) {
            this.reservationNumber = other.getReservationNumber();
        }
        
        if (this.passcode == null || this.passcode.isEmpty()) {
            this.passcode = other.getPasscode();
        }
        
        if (this.stars == 0) {
            this.stars = other.getStars();
        }
        
        if (this.review == null || this.review.isEmpty()) {
            this.review = other.getReview();
        }
        
        if (this.possibleStarRatings == null || this.possibleStarRatings.isEmpty()) {
            this.possibleStarRatings = new ArrayList<>(other.getPossibleStarRatings());
        }
        
        return true;
    }
    
    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            // Only update fields that have non-empty values in the JSON response
            if (jsonObject.has("reservation_number")) {
                String newReservationNumber = extractStringValue(jsonObject.getJSONObject("reservation_number"));
                if (newReservationNumber != null && !newReservationNumber.isEmpty()) {
                    reservationNumber = newReservationNumber;
                }
            }

            if (jsonObject.has("passcode")) {
                String newPasscode = extractStringValue(jsonObject.getJSONObject("passcode"));
                if (newPasscode != null && !newPasscode.isEmpty()) {
                    passcode = newPasscode;
                }
            }

            if (jsonObject.has("stars")) {
                JSONObject starsObject = jsonObject.getJSONObject("stars");
                int newStars = extractIntValue(starsObject);
                if (newStars > 0) {
                    stars = newStars;
                }
                // Always update possible values if provided
                List<Integer> newPossibleStarRatings = extractPossibleIntValues(starsObject);
                if (!newPossibleStarRatings.isEmpty()) {
                    possibleStarRatings = newPossibleStarRatings;
                }
            }

            if (jsonObject.has("review")) {
                String newReview = extractStringValue(jsonObject.getJSONObject("review"));
                if (newReview != null && !newReview.isEmpty()) {
                    review = newReview;
                }
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getMissingFields() {
        List<String> missingFields = new ArrayList<>();
        if (reservationNumber.isEmpty()) missingFields.add("reservation_number");
        if (passcode.isEmpty()) missingFields.add("passcode");
        if (stars <= 0) missingFields.add("stars");
        // Review is optional, so we don't check for it
        return missingFields;
    }

    @Override
    public List<String> getExistingFields() {
        List<String> existingFields = new ArrayList<>();
        if (!reservationNumber.isEmpty()) existingFields.add("reservation_number");
        if (!passcode.isEmpty()) existingFields.add("passcode");
        if (stars > 0) existingFields.add("stars");
        // Review is optional, so we don't check for it
        return existingFields;
    }

    @Override
    public Map<String, String> getExistingFieldsWithValues() {
        Map<String, String> fieldsWithValues = new HashMap<>();
        fieldsWithValues.put("reservation_number", reservationNumber);
        fieldsWithValues.put("passcode", passcode);
        fieldsWithValues.put("stars", String.valueOf(stars));
        fieldsWithValues.put("review", review);
        return fieldsWithValues;
    }

    @Override
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        templateString = replaceTemplateVariable(templateString, "reservation_number", reservationNumber);
        templateString = replaceTemplateVariable(templateString, "passcode", passcode);
        templateString = replaceTemplateVariable(templateString, "stars", String.valueOf(stars));
        templateString = replaceTemplateVariable(templateString, "review", review);
        return templateString;
    }

    @Override
    public Map<String, List<String>> getFieldValuesMap() {
        Map<String, List<String>> fieldsMap = new HashMap<>();
        
        if (reservationNumber != null && !reservationNumber.isEmpty()) {
            List<String> reservationList = new ArrayList<>();
            reservationList.add(reservationNumber);
            fieldsMap.put("reservation_id", reservationList);
        } else {
            fieldsMap.put("reservation_id", new ArrayList<>());
        }
        
        if (passcode != null && !passcode.isEmpty()) {
            List<String> passcodeList = new ArrayList<>();
            passcodeList.add(passcode);
            fieldsMap.put("passcode", passcodeList);
        } else {
            fieldsMap.put("passcode", new ArrayList<>());
        }
        
        if (stars > 0) {
            List<String> starsList = new ArrayList<>();
            starsList.add(String.valueOf(stars));
            fieldsMap.put("stars", starsList);
        } else {
            fieldsMap.put("stars", new ArrayList<>());
        }
        
        if (review != null && !review.isEmpty()) {
            List<String> reviewList = new ArrayList<>();
            reviewList.add(review);
            fieldsMap.put("comment", reviewList);  // Assuming "comment" is the field name in JSON
        } else {
            fieldsMap.put("comment", new ArrayList<>());
        }
        
        return fieldsMap;
    }

    // Getters
    public String getReservationNumber() {
        return reservationNumber;
    }

    public String getPasscode() {
        return passcode;
    }

    public int getStars() {
        return stars;
    }

    public String getReview() {
        return review;
    }

    public List<Integer> getPossibleStarRatings() {
        return possibleStarRatings;
    }

    // tostring
    @Override
    public String toString() {
        return "ReviewTemplate{" +
                "reservationNumber='" + reservationNumber + '\'' +
                ", passcode='" + passcode + '\'' +
                ", stars=" + stars +
                ", review='" + review + '\'' +
                ", possibleStarRatings=" + possibleStarRatings +
                '}';
    }
}
