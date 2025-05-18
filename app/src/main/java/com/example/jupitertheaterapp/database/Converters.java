package com.example.jupitertheaterapp.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Type converters for complex data types in Room database
 * Converts between custom objects and formats that Room can store
 */
public class Converters {
    
    private static final Gson gson = new Gson();
    
    // Date converters
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    // String List converters
    @TypeConverter
    public static List<String> fromString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return value == null ? new ArrayList<>() : gson.fromJson(value, listType);
    }
    
    @TypeConverter
    public static String fromList(List<String> list) {
        return list == null ? gson.toJson(new ArrayList<>()) : gson.toJson(list);
    }
    
    // Person converter (for Booking entity)
    @TypeConverter
    public static Person fromPersonString(String value) {
        return value == null ? new Person() : gson.fromJson(value, Person.class);
    }
    
    @TypeConverter
    public static String fromPerson(Person person) {
        return person == null ? gson.toJson(new Person()) : gson.toJson(person);
    }
    
    // Field value converter
    @TypeConverter
    public static FieldValue fromFieldValueString(String value) {
        return value == null ? new FieldValue() : gson.fromJson(value, FieldValue.class);
    }
    
    @TypeConverter
    public static String fromFieldValue(FieldValue fieldValue) {
        return fieldValue == null ? gson.toJson(new FieldValue()) : gson.toJson(fieldValue);
    }
}
