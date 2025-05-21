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
    }
    
    /**
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
     * Processes a template string by replacing variable placeholders with actual
     * values
     * 
     * @param templateString Template string with variables in <variable_name>
     *                       format
     * @return Processed string with all variables replaced with their values
     */
    public abstract String processTemplate(String templateString);

    // Special handler for the <missing> placeholder in templates
    protected String processMissingFieldsPlaceholder(String templateString) {
        if (templateString.contains("<missing>")) {
            String missingFieldsGreek = getMissingFieldsAsGreekString();
            return templateString.replace("<missing>", missingFieldsGreek);
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
    }

    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            if (jsonObject.has("name")) {
                name = extractStringListValue(jsonObject.getJSONObject("name"));
            }

            if (jsonObject.has("day")) {
                JSONObject dayObject = jsonObject.getJSONObject("day");
                day = extractStringListValue(dayObject);
                possibleDays = extractPossibleStringValues(dayObject);
            }

            if (jsonObject.has("topic")) {
                topic = extractStringListValue(jsonObject.getJSONObject("topic"));
            }

            if (jsonObject.has("time")) {
                time = extractStringListValue(jsonObject.getJSONObject("time"));
            }

            if (jsonObject.has("cast")) {
                cast = extractStringListValue(jsonObject.getJSONObject("cast"));
            }

            if (jsonObject.has("room")) {
                room = extractStringListValue(jsonObject.getJSONObject("room"));
            }

            if (jsonObject.has("duration")) {
                duration = extractStringListValue(jsonObject.getJSONObject("duration"));
            }

            if (jsonObject.has("stars")) {
                JSONObject starsObject = jsonObject.getJSONObject("stars");
                stars = extractStringListValue(starsObject);
                possibleStarRatings = extractPossibleIntValues(starsObject);
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
    }

    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            if (jsonObject.has("show_name")) {
                showName = extractStringValue(jsonObject.getJSONObject("show_name"));
            }

            if (jsonObject.has("room")) {
                room = extractStringValue(jsonObject.getJSONObject("room"));
            }

            if (jsonObject.has("day")) {
                JSONObject dayObject = jsonObject.getJSONObject("day");
                day = extractStringValue(dayObject);
                possibleDays = extractPossibleStringValues(dayObject);
            }

            if (jsonObject.has("time")) {
                time = extractStringValue(jsonObject.getJSONObject("time"));
            }

            if (jsonObject.has("person")) {
                JSONObject personObject = jsonObject.getJSONObject("person");
                person = new Person();

                if (personObject.has("name")) {
                    person.setName(extractStringValue(personObject.getJSONObject("name")));
                }

                if (personObject.has("age")) {
                    JSONObject ageObject = personObject.getJSONObject("age");
                    person.setAge(extractStringValue(ageObject));
                    person.setPossibleAgeCategories(extractPossibleStringValues(ageObject));
                }

                if (personObject.has("seat")) {
                    person.setSeat(extractStringValue(personObject.getJSONObject("seat")));
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
    private String passcode;

    public CancellationTemplate() {
        reservationNumber = "";
        passcode = "";
        
        // Initialize Greek field name mapping
        fieldNameMap = new HashMap<>();
        fieldNameMap.put("reservation_number", "αριθμός κράτησης");
        fieldNameMap.put("passcode", "κωδικός επιβεβαίωσης");
    }

    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            if (jsonObject.has("reservation_number")) {
                reservationNumber = extractStringValue(jsonObject.getJSONObject("reservation_number"));
            }

            if (jsonObject.has("passcode")) {
                passcode = extractStringValue(jsonObject.getJSONObject("passcode"));
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
        return missingFields;
    }

    @Override
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        templateString = replaceTemplateVariable(templateString, "reservation_number", reservationNumber);
        templateString = replaceTemplateVariable(templateString, "passcode", passcode);
        return templateString;
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
    }

    @Override
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            if (jsonObject.has("show_name")) {
                showName = extractStringListValue(jsonObject.getJSONObject("show_name"));
            }

            if (jsonObject.has("no_of_people")) {
                numberOfPeople = extractIntValue(jsonObject.getJSONObject("no_of_people"));
            }

            if (jsonObject.has("age")) {
                JSONObject ageObject = jsonObject.getJSONObject("age");
                age = extractStringListValue(ageObject);
                possibleAgeCategories = extractPossibleStringValues(ageObject);
            }

            if (jsonObject.has("date")) {
                date = extractStringListValue(jsonObject.getJSONObject("date"));
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
    }

    // Getters
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
    protected boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException {
        try {
            if (jsonObject.has("reservation_number")) {
                reservationNumber = extractStringValue(jsonObject.getJSONObject("reservation_number"));
            }

            if (jsonObject.has("passcode")) {
                passcode = extractStringValue(jsonObject.getJSONObject("passcode"));
            }

            if (jsonObject.has("stars")) {
                JSONObject starsObject = jsonObject.getJSONObject("stars");
                stars = extractIntValue(starsObject);
                possibleStarRatings = extractPossibleIntValues(starsObject);
            }

            if (jsonObject.has("review")) {
                review = extractStringValue(jsonObject.getJSONObject("review"));
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
    public String processTemplate(String templateString) {
        // Handle <missing> placeholder first
        templateString = processMissingFieldsPlaceholder(templateString);
        
        templateString = replaceTemplateVariable(templateString, "reservation_number", reservationNumber);
        templateString = replaceTemplateVariable(templateString, "passcode", passcode);
        templateString = replaceTemplateVariable(templateString, "stars", String.valueOf(stars));
        templateString = replaceTemplateVariable(templateString, "review", review);
        return templateString;
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
