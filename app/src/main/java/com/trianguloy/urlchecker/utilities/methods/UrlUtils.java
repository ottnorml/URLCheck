package com.trianguloy.urlchecker.utilities.methods;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.content.Intent;
import android.net.Uri;

import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Base64;

/** Static utilities related to urls */
public interface UrlUtils {

    /** Returns an intent that will open the given [url], with an optional [intentApp] */
    static Intent getViewIntent(String url, IntentApp intentApp) {
        var intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intentApp != null) intent.setComponent(intentApp.getComponent());
        return intent;
    }

    /** Calls URLDecoder.decode but returns the input string if the decoding failed */
    static String decode(String string) {
        try {
            return URLDecoder.decode(string, sUTF_8);
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to decode string", e);
            // can't decode, just leave it
            return string;
        }
    }

    /** 
     * Attempts to decode a Base64 string. Returns the decoded string if successful,
     * or null if decoding failed or result is not valid text.
     */
    static String decodeBase64(String string) {
        if (string == null || string.isEmpty()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(string);
            String result = new String(decoded, sUTF_8);
            // Check if result contains mostly printable characters (basic validation)
            if (isValidText(result)) {
                return result;
            }
            return null;
        } catch (Exception e) {
            // Not valid base64 or decoding failed
            return null;
        }
    }

    /**
     * Checks if a string contains mostly valid text/URL characters using Java built-in standards.
     * Uses Character class methods and URI parsing to validate content.
     * Rejects strings with too many control characters or invalid byte sequences.
     */
    static boolean isValidText(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // First, try to parse as URI - if successful, it's definitely valid URL text
        try {
            new URI(text);
            return true; // Valid URI, accept it
        } catch (java.net.URISyntaxException e) {
            // Not a complete URI, continue with character validation
        }
        
        // Validate character by character using standard Java Character class
        int validCount = 0;
        int totalCount = text.length();
        
        for (char c : text.toCharArray()) {
            // Use built-in Java standards for character validation:
            // - Character.isLetterOrDigit() covers all Unicode letters and digits
            // - Character.isWhitespace() covers all Unicode whitespace
            // - Character.isISOControl() detects invalid control characters
            // - isPunctuationOrSymbol() checks common URL punctuation using Character.getType()
            
            if (Character.isLetterOrDigit(c) || // Letters and digits (ASCII + international)
                Character.isWhitespace(c) || // Whitespace (space, tab, newline, etc.)
                (isPunctuationOrSymbol(c) && !Character.isISOControl(c))) { // Punctuation/symbols but not control chars
                validCount++;
            }
        }
        
        // Consider valid if at least 80% of characters are valid
        return (validCount * 100.0 / totalCount) >= 80.0;
    }
    
    /**
     * Helper method to check if a character is punctuation or symbol commonly found in URLs.
     * Uses Character.getType() to check standard Unicode categories.
     */
    static boolean isPunctuationOrSymbol(char c) {
        int type = Character.getType(c);
        // Accept various punctuation and symbol Unicode character categories
        return type == Character.DASH_PUNCTUATION ||
               type == Character.START_PUNCTUATION ||
               type == Character.END_PUNCTUATION ||
               type == Character.CONNECTOR_PUNCTUATION ||
               type == Character.OTHER_PUNCTUATION ||
               type == Character.MATH_SYMBOL ||
               type == Character.CURRENCY_SYMBOL ||
               type == Character.MODIFIER_SYMBOL ||
               type == Character.OTHER_SYMBOL ||
               type == Character.INITIAL_QUOTE_PUNCTUATION ||
               type == Character.FINAL_QUOTE_PUNCTUATION;
    }
}
