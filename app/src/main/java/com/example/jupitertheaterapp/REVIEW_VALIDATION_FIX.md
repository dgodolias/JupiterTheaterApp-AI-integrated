# Review Validation Fix

## Problem Identified
The review posting system was allowing users to post reviews for non-existent reservations. When a user tried to post a review with invalid reservation details (like `reservation_number=DUMMY123` and `reservation_password=2`), the system would still show "Review posted successfully" instead of validating the reservation exists.

## Root Cause
The `addReview()` method in `SimpleDatabase.java` was directly creating and adding reviews without first checking if the provided reservation number and password combination exists in the bookings database.

## Solution Implemented

### 1. Added Reservation Validation Method
**File**: `SimpleDatabase.java`
- Added `validateReservationExists(MsgTemplate template)` method
- This method checks if a reservation matching the template criteria exists in the bookings table
- Uses the same `matchesTemplate()` logic that's used in `removeBooking()` for consistency

```java
public boolean validateReservationExists(MsgTemplate template) {
    // Iterates through bookings table
    // Uses matchesTemplate() to find matching reservation
    // Returns true if found, false otherwise
}
```

### 2. Updated Review Addition Process
**File**: `SimpleDatabase.java`
- Modified `addReview()` method to call `validateReservationExists()` first
- If validation fails, the method returns `false` without creating the review
- If validation passes, proceeds with normal review creation

### 3. Updated Error Messages
**File**: `ChatbotManager.java`
- Updated the `review_confirmation` case to provide specific error message
- Changed from generic "Υπήρξε πρόβλημα με την καταχώρηση της αξιολόγησής σας"
- To specific "Δεν βρέθηκε κράτηση με αυτόν τον συνδυασμό στοιχείων. Παρακαλώ ελέγξτε τα στοιχεία κράτησης και δοκιμάστε ξανά."

## Behavior Changes

### Before Fix:
```
User input: reservation_number=DUMMY123, reservation_password=2
System response: "Η αξιολόγησή σας καταχωρήθηκε επιτυχώς!"
```

### After Fix:
```
User input: reservation_number=DUMMY123, reservation_password=2
System response: "Δεν βρέθηκε κράτηση με αυτόν τον συνδυασμό στοιχείων. Παρακαλώ ελέγξτε τα στοιχεία κράτησης και δοκιμάστε ξανά."
```

## Consistency with Cancellation Process
The review validation now works exactly like the cancellation process:
- Both use `matchesTemplate()` to validate reservation existence
- Both return `false` when no matching reservation is found
- Both display the same error message for invalid reservations

## Testing
To test this fix:
1. Try to post a review with invalid reservation details (e.g., DUMMY123 / 2)
2. Verify the system shows the validation error message
3. Try to post a review with valid reservation details
4. Verify the review is posted successfully
