package com.mdlc.betterselection;


/**
 * Character classes form a partition of the set of all characters.
 */
public enum CharacterClass {
    /**
     * The Word character class contains all the {@linkplain Character#isAlphabetic(int) letters},
     * {@linkplain Character#isIdeographic(int) ideographs} and {@linkplain Character#isDigit(char) digits}, as well as
     * the underscore.
     */
    WORD,
    /**
     * The Whitespace character class contains all {@linkplain Character#isSpaceChar(char) whitespace characters}.
     */
    WHITESPACE,
    /**
     * The Punctuation character class contains any character that is not a {@linkplain #WORD word} character nor a
     * {@linkplain #WHITESPACE whitespace} character.
     */
    PUNCTUATION;

    /**
     * Tests if a character is a currency symbol.
     * <p>
     * Currency symbols belong to the {@linkplain #WORD Word} class.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} is a currency symbol
     * @see <a
     *         href="https://en.wikipedia.org/wiki/Currency_symbol">https://en.wikipedia.org/wiki/Currency_symbol</a>
     */
    private static boolean isCurrencySymbol(char c) {
        return c == '؋' || c == '฿' || c == '₿' || c == '₵' || c == '¢' || c == '₡' || c == '$' || c == '₫' || c == '֏' || c == '€' || c == '₣' || c == '₲' || c == '₴' || c == '₭' || c == '₾' || c == '₺' || c == '₼' || c == '₦' || c == '₱' || c == '£' || c == '﷼' || c == '៛' || c == '₽' || c == '₹' || c == '₨' || c == '₪' || c == '⃀' || c == '৳' || c == '₸' || c == '₮' || c == '₩' || c == '¤';
    }

    /**
     * Tests if a character belongs to the Word class.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} belongs to the Word class
     * @see #WORD
     */
    private static boolean isWordCharacter(char c) {
        return Character.isAlphabetic(c) || Character.isIdeographic(c) || Character.isDigit(c) || c == '_' || isCurrencySymbol(c);
    }

    /**
     * Tests if a character belongs to the Whitespace class.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} belongs to the Whitespace class
     * @see #WHITESPACE
     */
    private static boolean isWhitespaceCharacter(char c) {
        return Character.isSpaceChar(c);
    }

    /**
     * Tests if a character belongs to the Punctuation class.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} belongs to the Punctuation class
     * @see #PUNCTUATION
     */
    private static boolean isPunctuationCharacter(char c) {
        return !isWordCharacter(c) && !isWhitespaceCharacter(c);
    }

    /**
     * Returns the class of a specific character.
     *
     * @param c
     *         the character to get the class of
     * @return the character class of {@code c}
     */
    public static CharacterClass fromCharacter(char c) {
        if (isWordCharacter(c)) {
            return WORD;
        } else if (isWhitespaceCharacter(c)) {
            return WHITESPACE;
        } else {
            return PUNCTUATION;
        }
    }

    /**
     * Tests if a character belongs to this class.
     *
     * @param c
     *         the character to test
     * @return {@code true} if, and only if, {@code c} belongs to this class
     */
    public boolean contains(char c) {
        return switch (this) {
            case WORD -> isWordCharacter(c);
            case WHITESPACE -> isWhitespaceCharacter(c);
            case PUNCTUATION -> isPunctuationCharacter(c);
        };
    }
}
