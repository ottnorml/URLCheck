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
     * Checks if a string contains mostly valid URL characters.
     * Used to validate if a Base64 decoded result is meaningful URL text.
     * Supports international characters valid in URLs per RFC 3986 and IRI (RFC 3987).
     */
    static boolean isValidText(String text) {
        if (text == null || text.isEmpty()) return false;
        int validCount = 0;
        int totalCount = text.length();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Consider valid URL characters:
            // 1. Unreserved characters (RFC 3986): A-Z, a-z, 0-9, -, ., _, ~
            // 2. Reserved characters valid in URLs: : / ? # [ ] @ ! $ & ' ( ) * + , ; =
            // 3. Percent sign (for percent-encoding like %20)
            // 4. Common whitespace characters (space, newline, tab, carriage return)
            // 5. International characters: letters and digits from any script (for IDN/IRI)
            
            if ((c >= 'A' && c <= 'Z') || // Uppercase letters
                (c >= 'a' && c <= 'z') || // Lowercase letters
                (c >= '0' && c <= '9') || // Digits
                c == '-' || c == '.' || c == '_' || c == '~' || // Unreserved
                c == ':' || c == '/' || c == '?' || c == '#' || // URL structure
                c == '[' || c == ']' || c == '@' || // URL components
                c == '!' || c == '$' || c == '&' || c == '\'' || // Sub-delimiters
                c == '(' || c == ')' || c == '*' || c == '+' || // Sub-delimiters
                c == ',' || c == ';' || c == '=' || // Sub-delimiters
                c == '%' || // Percent-encoding
                c == ' ' || c == '\n' || c == '\r' || c == '\t' || // Whitespace
                Character.isLetter(c) || // International letters (IDN/IRI support)
                Character.isDigit(c)) { // International digits
                validCount++;
            }
        }
        
        // Consider valid if at least 80% of characters are URL-valid
        return (validCount * 100.0 / totalCount) >= 80.0;
    }
}
