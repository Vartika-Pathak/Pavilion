package com.pavilion.api.dto;

// Shared regex constants for @Pattern-annotated DTO fields, so the same rule doesn't drift
// between the several phone-number fields that all need it.
public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    // Indian mobile numbers: exactly 10 digits, first digit 6-9. No spaces, hyphens, or letters —
    // callers normalize (strip spaces/hyphens, trim) before this runs so natural typing still works.
    public static final String PHONE_10_DIGIT = "^[6-9][0-9]{9}$";

    // Same rule, but also accepts an empty string for fields where a phone number is optional.
    public static final String OPTIONAL_PHONE_10_DIGIT = "^$|" + PHONE_10_DIGIT;
}
