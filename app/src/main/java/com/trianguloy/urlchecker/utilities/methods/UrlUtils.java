package com.trianguloy.urlchecker.utilities.methods;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.content.Intent;
import android.net.Uri;

import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;

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
     * Checks if a string contains mostly valid, printable characters.
     * Used to validate if a Base64 decoded result is meaningful text.
     * Supports international (non-ASCII) characters for worldwide web compatibility.
     */
    static boolean isValidText(String text) {
        if (text == null || text.isEmpty()) return false;
        int validCount = 0;
        int totalCount = text.length();
        
        for (char c : text.toCharArray()) {
            // Consider valid:
            // - Standard ASCII printable characters (space to ~)
            // - Common whitespace control chars
            // - International characters: letters, digits, marks, symbols, punctuation
            // - Common URL/text characters like spaces, currency symbols, etc.
            if ((c >= 32 && c < 127) || // ASCII printable
                c == '\n' || c == '\r' || c == '\t' || // Whitespace
                Character.isLetter(c) || // International letters (e.g., ñ, ö, 中, あ)
                Character.isDigit(c) || // International digits
                Character.isWhitespace(c) || // International whitespace
                Character.getType(c) == Character.CURRENCY_SYMBOL || // Currency symbols
                Character.getType(c) == Character.CONNECTOR_PUNCTUATION || // Connectors like _
                Character.getType(c) == Character.DASH_PUNCTUATION || // Dashes
                Character.getType(c) == Character.START_PUNCTUATION || // Opening brackets
                Character.getType(c) == Character.END_PUNCTUATION || // Closing brackets
                Character.getType(c) == Character.INITIAL_QUOTE_PUNCTUATION ||
                Character.getType(c) == Character.FINAL_QUOTE_PUNCTUATION ||
                Character.getType(c) == Character.OTHER_PUNCTUATION || // Punctuation like !, ?
                Character.getType(c) == Character.MATH_SYMBOL || // Math symbols
                Character.getType(c) == Character.OTHER_SYMBOL) { // Other symbols
                validCount++;
            }
        }
        
        // Consider valid if at least 80% of characters are valid
        return (validCount * 100.0 / totalCount) >= 80.0;
    }
}
