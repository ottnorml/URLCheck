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

    /**
     * Decodes a URL-encoded string using UTF-8 encoding.
     * This method provides safe decoding that returns the original string if decoding fails.
     * 
     * @param string the URL-encoded string to decode
     * @return the decoded string, or the original string if decoding failed
     */
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
     * Attempts to decode a Base64-encoded string with automatic format detection.
     * This method tries multiple Base64 variants to maximize compatibility with different encoders.
     * 
     * <p>Decoding process:
     * <ol>
     *   <li>Attempts standard Base64 decoding (RFC 4648 Section 4)</li>
     *   <li>If that fails, attempts URL-safe Base64 decoding (RFC 4648 Section 5)</li>
     *   <li>Validates the decoded result contains valid text using {@link #isValidText(String)}</li>
     * </ol>
     * 
     * <p>Base64 variants supported:
     * <ul>
     *   <li><b>Standard Base64:</b> Uses characters A-Z, a-z, 0-9, +, / with = padding</li>
     *   <li><b>URL-safe Base64:</b> Uses characters A-Z, a-z, 0-9, -, _ (replaces +/ with -_)</li>
     * </ul>
     * 
     * <p>Use cases:
     * <ul>
     *   <li>Decoding tracking parameters embedded in URLs</li>
     *   <li>Extracting redirect URLs hidden in Base64</li>
     *   <li>Processing data from various web services that may use different Base64 formats</li>
     * </ul>
     * 
     * @param string the Base64-encoded string to decode (standard or URL-safe format)
     * @return the decoded UTF-8 string if valid, or null if decoding failed or result is not valid text
     * @see #isValidText(String) for validation criteria
     */
    static String decodeBase64(String string) {
        if (string == null || string.isEmpty()) return null;
        
        // Try standard Base64 first (RFC 4648 Section 4)
        // Uses: A-Z, a-z, 0-9, +, / with = padding
        try {
            byte[] decoded = Base64.decode(string, Base64.DEFAULT);
            String result = new String(decoded, sUTF_8);
            // Check if result contains mostly printable characters (basic validation)
            if (isValidText(result)) {
                return result;
            }
        } catch (Exception e) {
            // Standard Base64 decode failed, continue to try URL-safe variant
        }
        
        // Try URL-safe Base64 variant (RFC 4648 Section 5)
        // Uses: A-Z, a-z, 0-9, -, _ (replaces + and / to avoid URL encoding issues)
        try {
            byte[] decoded = Base64.decode(string, Base64.URL_SAFE);
            String result = new String(decoded, sUTF_8);
            if (isValidText(result)) {
                return result;
            }
        } catch (Exception e) {
            // URL-safe Base64 decode also failed
        }
        
        return null;
    }

    /**
     * Validates whether a string contains predominantly valid text/URL characters.
     * This method uses multiple validation strategies to handle international text,
     * emoji, and URL content while rejecting binary data and control characters.
     * 
     * <p>Validation strategy:
     * <ol>
     *   <li><b>Fast path for URLs:</b> If the text starts with http://, https://, or ftp://,
     *       attempts URI parsing. Valid URIs are immediately accepted.</li>
     *   <li><b>Character-by-character validation:</b> Iterates through Unicode code points
     *       (not char units) to properly handle surrogate pairs and emoji.</li>
     *   <li><b>80% threshold:</b> Accepts strings where at least 80% of code points are valid,
     *       allowing for some encoding artifacts or special characters.</li>
     * </ol>
     * 
     * <p>Valid character categories:
     * <ul>
     *   <li>Letters and digits from any Unicode script (via {@link Character#isLetterOrDigit(int)})</li>
     *   <li>Whitespace characters including international variants (via {@link Character#isWhitespace(int)})</li>
     *   <li>URL punctuation and symbols (via {@link #isPunctuationOrSymbol(int)})</li>
     *   <li>Emoji modifiers and formatting characters (via {@link #isEmojiRelated(int)})</li>
     * </ul>
     * 
     * <p>Rejected content:
     * <ul>
     *   <li>Binary data (non-printable bytes)</li>
     *   <li>ISO control characters (0x00-0x1F, 0x7F-0x9F) except when part of valid punctuation</li>
     *   <li>Strings with less than 80% valid characters</li>
     * </ul>
     * 
     * <p>Implementation notes:
     * <ul>
     *   <li>Uses code point iteration ({@link String#codePointAt(int)}) instead of char iteration
     *       to correctly handle characters beyond the Basic Multilingual Plane (BMP)</li>
     *   <li>Performance-optimized with startsWith() checks before expensive URI parsing</li>
     *   <li>Compliant with Unicode standards for international character support</li>
     * </ul>
     * 
     * @param text the string to validate
     * @return true if the string contains mostly valid text/URL characters, false otherwise
     * @see #isEmojiRelated(int) for emoji validation
     * @see #isPunctuationOrSymbol(int) for symbol validation
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
     * Checks if a Unicode code point represents an emoji-related character.
     * This helper method identifies emoji modifiers and formatting characters that are
     * typically part of complex emoji sequences.
     * 
     * <p>Detected emoji-related categories:
     * <ul>
     *   <li><b>FORMAT (Cf):</b> Emoji modifiers like skin tone (🏻-🏿), gender modifiers,
     *       and Zero Width Joiner (ZWJ) used in sequences like 👨‍👩‍👧‍👦</li>
     *   <li><b>NON_SPACING_MARK (Mn):</b> Combining marks including emoji variation selectors
     *       (e.g., VS-16 used to force emoji presentation: ❤ vs ❤️)</li>
     *   <li><b>ENCLOSING_MARK (Me):</b> Additional combining marks used in some emoji variants</li>
     * </ul>
     * 
     * <p>Note: Basic emoji symbols (e.g., 😀, ⭐, 🎉) are typically categorized as OTHER_SYMBOL
     * and are handled by {@link #isPunctuationOrSymbol(int)} to avoid double-counting.
     * 
     * <p>Examples of detected characters:
     * <ul>
     *   <li>Skin tone modifiers: U+1F3FB through U+1F3FF</li>
     *   <li>Zero Width Joiner (ZWJ): U+200D</li>
     *   <li>Variation Selector-16: U+FE0F</li>
     * </ul>
     * 
     * @param codePoint the Unicode code point to check
     * @return true if the code point is an emoji modifier or formatting character, false otherwise
     * @see Character#getType(int) for character type constants
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
     * Checks if a Unicode code point represents punctuation or a symbol commonly found in URLs and text.
     * This helper method validates various categories of symbols and punctuation marks using
     * Unicode character type classification.
     * 
     * <p>Validated character categories:
     * <ul>
     *   <li><b>Punctuation:</b>
     *     <ul>
     *       <li>DASH_PUNCTUATION (Pd): Hyphens and dashes (-)</li>
     *       <li>START_PUNCTUATION (Ps): Opening brackets ([, {, (</li>
     *       <li>END_PUNCTUATION (Pe): Closing brackets (], }, )</li>
     *       <li>CONNECTOR_PUNCTUATION (Pc): Connectors like underscore (_)</li>
     *       <li>OTHER_PUNCTUATION (Po): Periods, commas, semicolons, etc.</li>
     *       <li>INITIAL_QUOTE_PUNCTUATION (Pi): Opening quotes</li>
     *       <li>FINAL_QUOTE_PUNCTUATION (Pf): Closing quotes</li>
     *     </ul>
     *   </li>
     *   <li><b>Symbols:</b>
     *     <ul>
     *       <li>MATH_SYMBOL (Sm): Mathematical operators (+, =, <, >, etc.)</li>
     *       <li>CURRENCY_SYMBOL (Sc): Currency signs ($, €, ¥, etc.)</li>
     *       <li>MODIFIER_SYMBOL (Sk): Modifier symbols (^, `, ¨, etc.)</li>
     *       <li>OTHER_SYMBOL (So): Misc symbols including many emoji (★, ©, ®, 😀, etc.)</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <p>URL relevance: These characters are commonly found in:
     * <ul>
     *   <li>Query parameters: {@code ?key=value&other=data}</li>
     *   <li>URL paths: {@code /path/to/file.html}</li>
     *   <li>Fragments: {@code #section-name}</li>
     *   <li>Special characters in text content</li>
     * </ul>
     * 
     * <p>Note: This method includes OTHER_SYMBOL which covers both basic emoji symbols
     * and regular symbols. Complex emoji with modifiers are handled by {@link #isEmojiRelated(int)}.
     * 
     * @param codePoint the Unicode code point to check
     * @return true if the code point is a valid punctuation or symbol character, false otherwise
     * @see Character#getType(int) for character type constants
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
