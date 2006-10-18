/*
 * $Header: /cvshome/build/ee.foundation/src/java/lang/Character.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
 *
 * (C) Copyright 2001 Sun Microsystems, Inc.
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;
public final class Character implements java.io.Serializable, java.lang.Comparable {
	public Character(char var0) { }
	public char charValue() { return 0; }
	public int compareTo(java.lang.Character var0) { return 0; }
	public int compareTo(java.lang.Object var0) { return 0; }
	public static int digit(char var0, int var1) { return 0; }
	public boolean equals(java.lang.Object var0) { return false; }
	public static char forDigit(int var0, int var1) { return 0; }
	public static int getNumericValue(char var0) { return 0; }
	public static int getType(char var0) { return 0; }
	public int hashCode() { return 0; }
	public static boolean isDefined(char var0) { return false; }
	public static boolean isDigit(char var0) { return false; }
	public static boolean isIdentifierIgnorable(char var0) { return false; }
	public static boolean isISOControl(char var0) { return false; }
	public static boolean isJavaIdentifierPart(char var0) { return false; }
	public static boolean isJavaIdentifierStart(char var0) { return false; }
	public static boolean isLetter(char var0) { return false; }
	public static boolean isLetterOrDigit(char var0) { return false; }
	public static boolean isLowerCase(char var0) { return false; }
	public static boolean isSpaceChar(char var0) { return false; }
	public static boolean isTitleCase(char var0) { return false; }
	public static boolean isUnicodeIdentifierPart(char var0) { return false; }
	public static boolean isUnicodeIdentifierStart(char var0) { return false; }
	public static boolean isUpperCase(char var0) { return false; }
	public static boolean isWhitespace(char var0) { return false; }
	public static char toLowerCase(char var0) { return 0; }
	public java.lang.String toString() { return null; }
	public static char toTitleCase(char var0) { return 0; }
	public static char toUpperCase(char var0) { return 0; }
	public final static char MIN_VALUE = 0;
	public final static char MAX_VALUE = 65535;
	public final static int MIN_RADIX = 2;
	public final static int MAX_RADIX = 36;
	public final static java.lang.Class TYPE; static { TYPE = null; }
	public final static byte UNASSIGNED = 0;
	public final static byte UPPERCASE_LETTER = 1;
	public final static byte LOWERCASE_LETTER = 2;
	public final static byte TITLECASE_LETTER = 3;
	public final static byte MODIFIER_LETTER = 4;
	public final static byte OTHER_LETTER = 5;
	public final static byte NON_SPACING_MARK = 6;
	public final static byte ENCLOSING_MARK = 7;
	public final static byte COMBINING_SPACING_MARK = 8;
	public final static byte DECIMAL_DIGIT_NUMBER = 9;
	public final static byte LETTER_NUMBER = 10;
	public final static byte OTHER_NUMBER = 11;
	public final static byte SPACE_SEPARATOR = 12;
	public final static byte LINE_SEPARATOR = 13;
	public final static byte PARAGRAPH_SEPARATOR = 14;
	public final static byte CONTROL = 15;
	public final static byte FORMAT = 16;
	public final static byte PRIVATE_USE = 18;
	public final static byte SURROGATE = 19;
	public final static byte DASH_PUNCTUATION = 20;
	public final static byte START_PUNCTUATION = 21;
	public final static byte END_PUNCTUATION = 22;
	public final static byte CONNECTOR_PUNCTUATION = 23;
	public final static byte OTHER_PUNCTUATION = 24;
	public final static byte MATH_SYMBOL = 25;
	public final static byte CURRENCY_SYMBOL = 26;
	public final static byte MODIFIER_SYMBOL = 27;
	public final static byte OTHER_SYMBOL = 28;
	public static class Subset {
		protected Subset(java.lang.String var0) { }
		public final boolean equals(java.lang.Object var0) { return false; }
		public final int hashCode() { return 0; }
		public final java.lang.String toString() { return null; }
	}
	public static final class UnicodeBlock extends java.lang.Character.Subset {
		public static java.lang.Character.UnicodeBlock of(char var0) { return null; }
		public final static java.lang.Character.UnicodeBlock ALPHABETIC_PRESENTATION_FORMS; static { ALPHABETIC_PRESENTATION_FORMS = null; }
		public final static java.lang.Character.UnicodeBlock ARABIC; static { ARABIC = null; }
		public final static java.lang.Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_A; static { ARABIC_PRESENTATION_FORMS_A = null; }
		public final static java.lang.Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_B; static { ARABIC_PRESENTATION_FORMS_B = null; }
		public final static java.lang.Character.UnicodeBlock ARMENIAN; static { ARMENIAN = null; }
		public final static java.lang.Character.UnicodeBlock ARROWS; static { ARROWS = null; }
		public final static java.lang.Character.UnicodeBlock BASIC_LATIN; static { BASIC_LATIN = null; }
		public final static java.lang.Character.UnicodeBlock BENGALI; static { BENGALI = null; }
		public final static java.lang.Character.UnicodeBlock BLOCK_ELEMENTS; static { BLOCK_ELEMENTS = null; }
		public final static java.lang.Character.UnicodeBlock BOPOMOFO; static { BOPOMOFO = null; }
		public final static java.lang.Character.UnicodeBlock BOX_DRAWING; static { BOX_DRAWING = null; }
		public final static java.lang.Character.UnicodeBlock CJK_COMPATIBILITY; static { CJK_COMPATIBILITY = null; }
		public final static java.lang.Character.UnicodeBlock CJK_COMPATIBILITY_FORMS; static { CJK_COMPATIBILITY_FORMS = null; }
		public final static java.lang.Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS; static { CJK_COMPATIBILITY_IDEOGRAPHS = null; }
		public final static java.lang.Character.UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION; static { CJK_SYMBOLS_AND_PUNCTUATION = null; }
		public final static java.lang.Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS; static { CJK_UNIFIED_IDEOGRAPHS = null; }
		public final static java.lang.Character.UnicodeBlock COMBINING_DIACRITICAL_MARKS; static { COMBINING_DIACRITICAL_MARKS = null; }
		public final static java.lang.Character.UnicodeBlock COMBINING_HALF_MARKS; static { COMBINING_HALF_MARKS = null; }
		public final static java.lang.Character.UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS; static { COMBINING_MARKS_FOR_SYMBOLS = null; }
		public final static java.lang.Character.UnicodeBlock CONTROL_PICTURES; static { CONTROL_PICTURES = null; }
		public final static java.lang.Character.UnicodeBlock CURRENCY_SYMBOLS; static { CURRENCY_SYMBOLS = null; }
		public final static java.lang.Character.UnicodeBlock CYRILLIC; static { CYRILLIC = null; }
		public final static java.lang.Character.UnicodeBlock DEVANAGARI; static { DEVANAGARI = null; }
		public final static java.lang.Character.UnicodeBlock DINGBATS; static { DINGBATS = null; }
		public final static java.lang.Character.UnicodeBlock ENCLOSED_ALPHANUMERICS; static { ENCLOSED_ALPHANUMERICS = null; }
		public final static java.lang.Character.UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS; static { ENCLOSED_CJK_LETTERS_AND_MONTHS = null; }
		public final static java.lang.Character.UnicodeBlock GENERAL_PUNCTUATION; static { GENERAL_PUNCTUATION = null; }
		public final static java.lang.Character.UnicodeBlock GEOMETRIC_SHAPES; static { GEOMETRIC_SHAPES = null; }
		public final static java.lang.Character.UnicodeBlock GEORGIAN; static { GEORGIAN = null; }
		public final static java.lang.Character.UnicodeBlock GREEK; static { GREEK = null; }
		public final static java.lang.Character.UnicodeBlock GREEK_EXTENDED; static { GREEK_EXTENDED = null; }
		public final static java.lang.Character.UnicodeBlock GUJARATI; static { GUJARATI = null; }
		public final static java.lang.Character.UnicodeBlock GURMUKHI; static { GURMUKHI = null; }
		public final static java.lang.Character.UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS; static { HALFWIDTH_AND_FULLWIDTH_FORMS = null; }
		public final static java.lang.Character.UnicodeBlock HANGUL_COMPATIBILITY_JAMO; static { HANGUL_COMPATIBILITY_JAMO = null; }
		public final static java.lang.Character.UnicodeBlock HANGUL_JAMO; static { HANGUL_JAMO = null; }
		public final static java.lang.Character.UnicodeBlock HANGUL_SYLLABLES; static { HANGUL_SYLLABLES = null; }
		public final static java.lang.Character.UnicodeBlock HEBREW; static { HEBREW = null; }
		public final static java.lang.Character.UnicodeBlock HIRAGANA; static { HIRAGANA = null; }
		public final static java.lang.Character.UnicodeBlock IPA_EXTENSIONS; static { IPA_EXTENSIONS = null; }
		public final static java.lang.Character.UnicodeBlock KANBUN; static { KANBUN = null; }
		public final static java.lang.Character.UnicodeBlock KANNADA; static { KANNADA = null; }
		public final static java.lang.Character.UnicodeBlock KATAKANA; static { KATAKANA = null; }
		public final static java.lang.Character.UnicodeBlock LAO; static { LAO = null; }
		public final static java.lang.Character.UnicodeBlock LATIN_1_SUPPLEMENT; static { LATIN_1_SUPPLEMENT = null; }
		public final static java.lang.Character.UnicodeBlock LATIN_EXTENDED_A; static { LATIN_EXTENDED_A = null; }
		public final static java.lang.Character.UnicodeBlock LATIN_EXTENDED_ADDITIONAL; static { LATIN_EXTENDED_ADDITIONAL = null; }
		public final static java.lang.Character.UnicodeBlock LATIN_EXTENDED_B; static { LATIN_EXTENDED_B = null; }
		public final static java.lang.Character.UnicodeBlock LETTERLIKE_SYMBOLS; static { LETTERLIKE_SYMBOLS = null; }
		public final static java.lang.Character.UnicodeBlock MALAYALAM; static { MALAYALAM = null; }
		public final static java.lang.Character.UnicodeBlock MATHEMATICAL_OPERATORS; static { MATHEMATICAL_OPERATORS = null; }
		public final static java.lang.Character.UnicodeBlock MISCELLANEOUS_SYMBOLS; static { MISCELLANEOUS_SYMBOLS = null; }
		public final static java.lang.Character.UnicodeBlock MISCELLANEOUS_TECHNICAL; static { MISCELLANEOUS_TECHNICAL = null; }
		public final static java.lang.Character.UnicodeBlock NUMBER_FORMS; static { NUMBER_FORMS = null; }
		public final static java.lang.Character.UnicodeBlock OPTICAL_CHARACTER_RECOGNITION; static { OPTICAL_CHARACTER_RECOGNITION = null; }
		public final static java.lang.Character.UnicodeBlock ORIYA; static { ORIYA = null; }
		public final static java.lang.Character.UnicodeBlock PRIVATE_USE_AREA; static { PRIVATE_USE_AREA = null; }
		public final static java.lang.Character.UnicodeBlock SMALL_FORM_VARIANTS; static { SMALL_FORM_VARIANTS = null; }
		public final static java.lang.Character.UnicodeBlock SPACING_MODIFIER_LETTERS; static { SPACING_MODIFIER_LETTERS = null; }
		public final static java.lang.Character.UnicodeBlock SPECIALS; static { SPECIALS = null; }
		public final static java.lang.Character.UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS; static { SUPERSCRIPTS_AND_SUBSCRIPTS = null; }
		public final static java.lang.Character.UnicodeBlock SURROGATES_AREA; static { SURROGATES_AREA = null; }
		public final static java.lang.Character.UnicodeBlock TAMIL; static { TAMIL = null; }
		public final static java.lang.Character.UnicodeBlock TELUGU; static { TELUGU = null; }
		public final static java.lang.Character.UnicodeBlock THAI; static { THAI = null; }
		public final static java.lang.Character.UnicodeBlock TIBETAN; static { TIBETAN = null; }
		private UnicodeBlock() { super((java.lang.String) null); } /* generated constructor to prevent compiler adding default public constructor */
	}
}

