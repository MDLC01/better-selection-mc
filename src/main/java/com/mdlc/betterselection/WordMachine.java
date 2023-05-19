package com.mdlc.betterselection;

/**
 * A finite state machine that detects a word's category by reading it character by character. Each state corresponds to
 * a category.
 * <p>
 * A call to {@link #read(char)} with the next character returns the new state of the machine.
 */
public enum WordMachine {
    /**
     * The word is empty or contains only underscores.
     */
    UNKNOWN,
    /**
     * The word contains {@linkplain #isDigit(char) digits} only.
     */
    NUMERIC,
    /**
     * The word consists of {@linkplain #isDigit(char) digits} and
     * {@linkplain #isCurrencySymbol(char) currency symbols}.
     */
    INTEGER,
    /**
     * The word consists of {@linkplain #isDigit(char) digits}, {@linkplain #isCurrencySymbol(char) currency symbols}
     * and a single {@linkplain #isDecimalSeparator(char) decimal separator}.
     */
    FLOAT,
    /**
     * The word contains  {@linkplain #isWordCharacter(char) word characters} only.
     */
    WORD,
    /**
     * The word contains {@linkplain #isWhitespaceCharacter(char) whitespace characters} only.
     */
    WHITESPACE,
    /**
     * The word consists of a single character that does not belong to any other category.
     */
    SPECIAL,
    /**
     * The state corresponding to a word that cannot be categorized. In other words, when this state is returned by
     * {@link #read(char)}, it means the word has ended.
     */
    DONE;

    /**
     * Tests if a character is a decimal separator.
     * <p>
     * The only decimal separator is the period.
     *
     * @param c
     *         the character to test
     * @return {@code c == '.'}
     */
    public static boolean isDecimalSeparator(char c) {
        return c == '.';
    }

    /**
     * Tests if a character is a digit or an underscore.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} is a digit or an underscore
     * @see Character#isDigit(char)
     */
    public static boolean isDigit(char c) {
        return Character.isDigit(c) || c == '_';
    }

    /**
     * Tests if a character is a currency symbol.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} is a currency symbol
     * @see <a href="https://www.unicode.org/charts/PDF/U20A0.pdf">Currency Symbols Unicode block chart</a>
     */
    public static boolean isCurrencySymbol(char c) {
        //noinspection UnnecessaryUnicodeEscape
        return c == '\u0024'
                || '\u00A2' <= c && c <= '\u00A5'
                || c == '\u0192'
                || c == '\u058F'
                || c == '\u060B'
                || c == '\u09F2'
                || c == '\u09F3'
                || c == '\u0AF1'
                || c == '\u0BF9'
                || c == '\u0E3F'
                || c == '\u17DB'
                || c == '\u2133'
                || c == '\u5143'
                || c == '\u5186'
                || c == '\u5706'
                || c == '\u5713'
                || c == '\uFDFC'
                // Currency Symbols block
                || '\u20A0' <= c && c <= '\u20CF';
    }

    /**
     * Tests if a character is a word character.
     * <p>
     * A word character is either an alphabetic character, an ideograph or an underscore.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} is a word character
     * @see Character#isAlphabetic(int)
     * @see Character#isIdeographic(int)
     */
    public static boolean isWordCharacter(char c) {
        return Character.isAlphabetic(c) || Character.isIdeographic(c) || c == '_';
    }

    /**
     * Tests if a character is a whitespace character.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} is a whitespace character
     * @see Character#isSpaceChar(char)
     */
    public static boolean isWhitespaceCharacter(char c) {
        return Character.isSpaceChar(c);
    }

    /**
     * Returns the state of the machine after reading the first character.
     *
     * @param c
     *         the character to read
     * @return the state of the machine after reading {@code c}
     */
    public static WordMachine startRead(char c) {
        return UNKNOWN.read(c);
    }

    /**
     * Returns the state of the machine after reading a character <i>x</i>. That is, the category of a word of the form
     * <i>u</i><i>x</i>, where <i>u</i> is any word in the current category.
     *
     * @param c
     *         the character to read
     * @return the state of the machine after reading {@code c}
     */
    public WordMachine read(char c) {
        return switch (this) {
            case UNKNOWN -> {
                if (c == '_') {
                    yield UNKNOWN;
                } else if (isDigit(c)) {
                    yield NUMERIC;
                } else if (isCurrencySymbol(c)) {
                    yield INTEGER;
                } else if (isDecimalSeparator(c)) {
                    yield FLOAT;
                } else if (isWordCharacter(c)) {
                    yield WORD;
                } else if (isWhitespaceCharacter(c)) {
                    yield WHITESPACE;
                } else {
                    yield SPECIAL;
                }
            }
            case NUMERIC -> {
                if (isDigit(c)) {
                    yield NUMERIC;
                } else if (isCurrencySymbol(c)) {
                    yield INTEGER;
                } else if (isDecimalSeparator(c)) {
                    yield FLOAT;
                } else if (isWordCharacter(c)) {
                    yield WORD;
                } else {
                    yield DONE;
                }
            }
            case INTEGER -> {
                if (isDigit(c) || isCurrencySymbol(c)) {
                    yield INTEGER;
                } else if (isDecimalSeparator(c)) {
                    yield FLOAT;
                } else {
                    yield DONE;
                }
            }
            case FLOAT -> {
                if (isDigit(c) || isCurrencySymbol(c)) {
                    yield FLOAT;
                } else {
                    yield DONE;
                }
            }
            case WORD -> {
                if (isWordCharacter(c) || isDigit(c)) {
                    yield WORD;
                } else {
                    yield DONE;
                }
            }
            case WHITESPACE -> {
                if (isWhitespaceCharacter(c)) {
                    yield WHITESPACE;
                } else {
                    yield DONE;
                }
            }
            case SPECIAL, DONE -> DONE;
        };
    }
}
