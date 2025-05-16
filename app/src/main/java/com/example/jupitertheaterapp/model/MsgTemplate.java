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
    // Factory pattern implementation
    private static final Map<String, Supplier<MsgTemplate>> templateMap = new HashMap<>();
    
    static {
        // Register all template types with their corresponding IDs
        templateMap.put("ΠΛΗΡΟΦΟΡΙΕΣ", ShowInfoTemplate::new);
        templateMap.put("ΚΡΑΤΗΣΗ", BookingTemplate::new);
        templateMap.put("ΑΚΥΡΩΣΗ", CancellationTemplate::new);
        templateMap.put("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ", DiscountTemplate::new);
        templateMap.put("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", ReviewTemplate::new);
        // Add more mappings as needed
    }
    
    /**
     * Creates an appropriate MsgTemplate instance based on the provided node ID
     * @param id The node ID used to determine which template to create
     * @return A new instance of the appropriate MsgTemplate subclass
     * @throws IllegalArgumentException if no template is registered for the given ID
     */
    public static MsgTemplate createTemplate(String id) {
        Supplier<MsgTemplate> supplier = templateMap.get(id);
        if (supplier != null) {
            return supplier.get();
        }
        // Return a default template or throw an exception
        throw new IllegalArgumentException("Unknown template type: " + id);
    }
    
    /**
     * Fills the template fields from a JSON string
     * @param jsonString JSON string to parse
     * @return true if parsing was successful, false otherwise
     */
    public boolean valuesFromJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return populateFromJsonObject(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Populates the template fields from a JSONObject
     * @param jsonObject JSONObject to extract values from
     * @return true if population was successful, false otherwise
     */
    protected abstract boolean populateFromJsonObject(JSONObject jsonObject) throws JSONException;
    
    /**
     * Helper method to extract string value from a field object
     */
    protected String extractStringValue(JSONObject fieldObject) throws JSONException {
        if (fieldObject.has("value")) {
            return fieldObject.getString("value");
        }
        return "";
    }
    
    /**
     * Helper method to extract integer value from a field object
     */
    protected int extractIntValue(JSONObject fieldObject) throws JSONException {
        if (fieldObject.has("value")) {
            return fieldObject.getInt("value");
        }
        return 0;
    }
    
    /**
     * Helper method to extract string list from a field object with array value
     */
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

    /**
     * Helper method to extract possible values as strings
     */
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
      /**
     * Helper method to extract possible values as integers
     */
    protected List<Integer> extractPossibleIntValues(JSONObject fieldObject) throws JSONException {
        List<Integer> pValues = new ArrayList<>();
        if (fieldObject.has("pvalues")) {
            JSONArray pvaluesArray = fieldObject.getJSONArray("pvalues");
            for (int i = 0; i < pvaluesArray.length(); i++) {
                pValues.add(pvaluesArray.getInt(i));
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
    
    // Possible values lists
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
    
    // Possible values
    private List<String> possibleDays;
    
    public BookingTemplate() {
        showName = "";
        room = "";
        day = "";
        time = "";
        person = new Person();
        possibleDays = new ArrayList<>();
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
    
    // Getters and setters
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
    
    // Getters and setters
    public String getReservationNumber() {
        return reservationNumber;
    }

    public String getPasscode() {
        return passcode;
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
    
    // Possible values
    private List<String> possibleAgeCategories;
    
    public DiscountTemplate() {
        showName = new ArrayList<>();
        numberOfPeople = 0;
        age = new ArrayList<>();
        date = new ArrayList<>();
        possibleAgeCategories = new ArrayList<>();
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
    
    // Getters and setters
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
}

/**
 * Template for review requests
 */
class ReviewTemplate extends MsgTemplate {
    private String reservationNumber;
    private String passcode;
    private int stars;
    private String review;
    
    // Possible values
    private List<Integer> possibleStarRatings;
    
    public ReviewTemplate() {
        reservationNumber = "";
        passcode = "";
        stars = 0;
        review = "";
        possibleStarRatings = new ArrayList<>();
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
    
    // Getters and setters
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
}
