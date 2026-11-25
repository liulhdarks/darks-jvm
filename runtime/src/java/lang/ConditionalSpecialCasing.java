package java.lang;

import java.util.Locale;

/*
 * darks-jvm: JDK 11 delegates the locale-sensitive special casing rules to
 * this class (Turkish/Azeri/Lithuanian dotless i, German sharp s and the
 * ligature expansions).  This runtime has no full Unicode case tables, so the
 * helpers fall back to Character's simple one-to-one mappings.  The public
 * String case-conversion methods keep working for the common cases; the
 * multi-character expansions that JDK produces for a few code points are not
 * performed.
 */
final class ConditionalSpecialCasing {

    private ConditionalSpecialCasing() {
    }

    static int toUpperCaseEx(String str, int index, Locale locale) {
        return Character.toUpperCaseEx(str.charAt(index));
    }

    static char[] toUpperCaseCharArray(String str, int index, Locale locale) {
        return null;
    }

    static int toLowerCaseEx(String str, int index, Locale locale) {
        return Character.toLowerCase(str.charAt(index));
    }

    static char[] toLowerCaseCharArray(String str, int index, Locale locale) {
        return null;
    }
}
