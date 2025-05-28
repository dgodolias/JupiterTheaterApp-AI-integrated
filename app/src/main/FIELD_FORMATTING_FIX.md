## Updated Field Formatting Test

### Before the fix:
When the server returned multiple values for a field like:
```json
{
  "topic": {
    "value": ["Δράμα", "Αρχαία Τραγωδία"],
    "pvalues": [...]
  }
}
```

The template system would show:
- In `<!missing>` placeholder: `topic: Αρχαία Τραγωδία` (only the last value)
- In `<results>` placeholder: `Topic: ["Δράμα", "Αρχαία Τραγωδία"]` (raw JSON format)

### After the fix:
Now the template system will show:
- In `<!missing>` placeholder: `θέμα: Δράμα, Αρχαία Τραγωδία` (all values with Greek field name)
- In `<results>` placeholder: `θέμα: Δράμα, Αρχαία Τραγωδία` (properly formatted with Greek field name)

### Changes made:

1. **Fixed `getExistingFieldsWithValues()` in ShowInfoTemplate and DiscountTemplate**:
   - Changed from looping and overwriting map entries to using `String.join(", ", list)`
   - Now combines all values for each field with commas

2. **Enhanced `formatResults()` in SimpleDatabase**:
   - Added overloaded method that accepts a template parameter
   - Uses template's `getGreekFieldName()` method to display Greek field names
   - Uses `getFieldValues()` helper to handle both string and array formats
   - Joins multiple values with commas

3. **Updated ChatbotManager**:
   - Modified to pass the template to `formatResults()` method
   - Ensures Greek field names are used in query results

### Result:
Users will now see properly formatted output like:
- `όνομα παράστασης: Magos tou oz` instead of `name:["Magos tou oz"]`
- `θέμα: Δράμα, Αρχαία Τραγωδία` instead of `topic: Αρχαία Τραγωδία`
