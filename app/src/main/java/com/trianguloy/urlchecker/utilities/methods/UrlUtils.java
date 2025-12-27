package com.trianguloy.urlchecker.utilities.methods;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

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
     * Uses android.util.Base64 for Android compatibility.
     */
    static String decodeBase64(String string) {
        if (string == null || string.isEmpty()) return null;
        try {
            byte[] decoded = Base64.decode(string, Base64.DEFAULT);
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
     * Supports emoji characters including those using surrogate pairs.
     * Properly handles Unicode code points beyond the Basic Multilingual Plane.
     */
    static boolean isValidText(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // First, try to parse as URI - if successful, it's definitely valid URL text
        // Optimize: use startsWith instead of matches for common URL patterns
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("ftp://")) {
            try {
                new URI(text);
                return true; // Valid URI, accept it
            } catch (URISyntaxException e) {
                // Continue with character validation
            }
        }
        
        // Validate using code points to properly handle surrogate pairs
        int validCount = 0;
        int totalCount = 0;
        
        // Iterate through code points instead of chars to handle surrogate pairs correctly
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            totalCount++;
            
            // Use built-in Java standards for character validation:
            // - Character.isLetterOrDigit() covers all Unicode letters and digits
            // - Character.isWhitespace() covers all Unicode whitespace
            // - Character.isISOControl() detects invalid control characters
            // - isPunctuationOrSymbol() checks common URL punctuation using Character.getType()
            // - isEmojiRelated() checks emoji-related code points
            
            if (Character.isLetterOrDigit(codePoint) || // Letters and digits (ASCII + international)
                Character.isWhitespace(codePoint) || // Whitespace (space, tab, newline, etc.)
                (isPunctuationOrSymbol(codePoint) && !Character.isISOControl(codePoint)) || // Punctuation/symbols but not control chars
                isEmojiRelated(codePoint)) { // Emoji characters (surrogate pairs, modifiers, etc.)
                validCount++;
            }
            
            // Move to next code point (handles surrogate pairs)
            i += Character.charCount(codePoint);
        }
        
        // Consider valid if at least 80% of characters are valid
        return totalCount > 0 && (validCount * 100.0 / totalCount) >= 80.0;
    }
    
    /**
     * Helper method to check if a code point is emoji-related.
     * Emojis often use code points beyond the Basic Multilingual Plane
     * and emoji modifiers (skin tone, gender, etc.).
     * Uses Character methods and getType() to check for emoji-related Unicode categories.
     */
    static boolean isEmojiRelated(int codePoint) {
        // Get the type of the code point
        int type = Character.getType(codePoint);
        
        // Emoji characters can be in these categories:
        // - FORMAT: Emoji modifiers like skin tone, ZWJ (Zero Width Joiner)
        // - NON_SPACING_MARK: Some emoji variation selectors
        // - ENCLOSING_MARK: Additional emoji modifiers
        // Note: OTHER_SYMBOL is handled by isPunctuationOrSymbol to avoid double-counting
        return type == Character.FORMAT ||
               type == Character.NON_SPACING_MARK ||
               type == Character.ENCLOSING_MARK;
    }
    
    /**
     * Helper method to check if a code point is punctuation or symbol commonly found in URLs.
     * Uses Character.getType() to check standard Unicode categories.
     */
    static boolean isPunctuationOrSymbol(int codePoint) {
        int type = Character.getType(codePoint);
        // Accept various punctuation and symbol Unicode character categories
        // Includes OTHER_SYMBOL which covers both regular symbols and many emoji
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
