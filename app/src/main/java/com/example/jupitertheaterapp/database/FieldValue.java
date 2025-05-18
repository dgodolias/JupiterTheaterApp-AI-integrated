package com.example.jupitertheaterapp.database;

import androidx.room.TypeConverters;
import java.util.ArrayList;
import java.util.List;

/**
 * Field value class to match JSON structure
 * Used in all entities to represent a field with its value and possible values
 */
public class FieldValue {
    private String value;
    private List<String> pvalues;
    
    public FieldValue() {
        this.pvalues = new ArrayList<>();
    }
    
    public FieldValue(String value, List<String> pvalues) {
        this.value = value;
        this.pvalues = pvalues != null ? pvalues : new ArrayList<>();
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public List<String> getPValues() {
        return pvalues;
    }
    
    public void setPValues(List<String> pvalues) {
        this.pvalues = pvalues;
    }
}
