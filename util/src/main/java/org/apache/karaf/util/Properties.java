/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Enhancement of the standard <code>Properties</code>
 * managing the maintain of comments, etc.
 * </p>
 * 
 * @author gnodet, jbonofre
 */
public class Properties extends AbstractMap<String, String> {

    /** Constant for the supported comment characters.*/
    private static final String COMMENT_CHARS = "#!";

    /** The list of possible key/value separators */
    private static final char[] SEPARATORS = new char[] {'=', ':'};

    /** The white space characters used as key/value separators. */
    private static final char[] WHITE_SPACE = new char[] {' ', '\t', '\f'};

    /**
     * The default encoding (ISO-8859-1 as specified by
     * http://java.sun.com/j2se/1.5.0/docs/api/java/util/Properties.html)
     */
    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    /** Constant for the platform specific line separator.*/
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private Map<String,String> storage = new LinkedHashMap<String,String>();
    private Map<String,Layout> layout = new LinkedHashMap<String,Layout>();
    private List<String> header;
    private List<String> footer;
    private File location;

    public Properties() {
    }

    public Properties(File location) throws IOException {
        this.location = location;
        if(location.exists())
            load(location);
    }

    public void load(File location) throws IOException {
        InputStream is = new FileInputStream(location);
        try {
            load(is);
        } finally {
            is.close();
        }
    }

    public void load(URL location) throws IOException {
        InputStream is = location.openStream();
        try {
            load(is);
        } finally {
            is.close();
        }
    }

    public void load(InputStream is) throws IOException {
        load(new InputStreamReader(is, DEFAULT_ENCODING));
    }

    public void load(Reader reader) throws IOException {
        loadLayout(reader);
    }

    public void save() throws IOException {
        save(this.location);
    }

    public void save(File location) throws IOException {
        OutputStream os = new FileOutputStream(location);
        try {
            save(os);
        } finally {
            os.close();
        }
    }

    public void save(OutputStream os) throws IOException {
        save(new OutputStreamWriter(os, DEFAULT_ENCODING));
    }

    public void save(Writer writer) throws IOException {
        saveLayout(writer);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return storage.entrySet();
    }

    @Override
    public String put(String key, String value) {
        String old = storage.put(key, value);
        if (old == null || !old.equals(value)) {
            Layout l = layout.get(key);
            if (l != null) {
                l.clearValue();
            }
        }
        return old;
    }

    @Override
    public String remove(Object key) {
        Layout l = layout.get(key);
        if (l != null) {
            l.clearValue();
        }
        return storage.remove(key);
    }

    @Override
    public void clear() {
        for (Layout l : layout.values()) {
            l.clearValue();
        }
        storage.clear();
    }

    /**
     * Return the comment header.
     *
     * @return the comment header
     */
    public List<String> getHeader()
    {
        return header;
    }

    /**
     * Set the comment header.
     *
     * @param header the header to use
     */
    public void setHeader(List<String> header)
    {
        this.header = header;
    }

    /**
     * Return the comment footer.
     *
     * @return the comment footer
     */
    public List<String> getFooter()
    {
        return footer;
    }

    /**
     * Set the comment footer.
     *
     * @param footer the footer to use
     */
    public void setFooter(List<String> footer)
    {
        this.footer = footer;
    }

    /**
     * Reads a properties file and stores its internal structure. The found
     * properties will be added to the associated configuration object.
     *
     * @param in the reader to the properties file
     * @throws java.io.IOException if an error occurs
     */
    protected void loadLayout(Reader in) throws IOException
    {
        PropertiesReader reader = new PropertiesReader(in);
        while (reader.nextProperty())
        {
            storage.put(reader.getPropertyName(), reader.getPropertyValue());
            int idx = checkHeaderComment(reader.getCommentLines());
            layout.put(reader.getPropertyName(),
                    new Layout(idx < reader.getCommentLines().size() ?
                                    new ArrayList<String>(reader.getCommentLines().subList(idx, reader.getCommentLines().size())) :
                                    null,
                               new ArrayList<String>(reader.getValueLines())));
        }
        footer = new ArrayList<String>(reader.getCommentLines());
        performSubstitution(storage);
    }

    /**
     * Writes the properties file to the given writer, preserving as much of its
     * structure as possible.
     *
     * @param out the writer
     * @throws java.io.IOException if an error occurs
     */
    protected void saveLayout(Writer out) throws IOException
    {
        PropertiesWriter writer = new PropertiesWriter(out);
        if (header != null)
        {
            for (String s : header)
            {
                writer.writeln(s);
            }
        }

        for (String key : storage.keySet())
        {
            Layout l = layout.get(key);
            if (l != null && l.getCommentLines() != null)
            {
                for (String s : l.getCommentLines())
                {
                    writer.writeln(s);
                }
            }
            if (l != null && l.getValueLines() != null)
            {
                for (String s : l.getValueLines())
                {
                    writer.writeln(s);
                }
            }
            else
            {
                writer.writeProperty(key, storage.get(key));
            }
        }
        if (footer != null)
        {
            for (String s : footer)
            {
                writer.writeln(s);
            }
        }
        writer.flush();
    }

    /**
     * Checks if parts of the passed in comment can be used as header comment.
     * This method checks whether a header comment can be defined (i.e. whether
     * this is the first comment in the loaded file). If this is the case, it is
     * searched for the lates blank line. This line will mark the end of the
     * header comment. The return value is the index of the first line in the
     * passed in list, which does not belong to the header comment.
     *
     * @param commentLines the comment lines
     * @return the index of the next line after the header comment
     */
    private int checkHeaderComment(List<String> commentLines)
    {
        if (getHeader() == null && layout.isEmpty())
        {
            // This is the first comment. Search for blank lines.
            int index = commentLines.size() - 1;
            while (index >= 0 && commentLines.get(index).length() > 0)
            {
                index--;
            }
            setHeader(new ArrayList<String>(commentLines.subList(0, index + 1)));
            return index + 1;
        }
        else
        {
            return 0;
        }
    }

    /**
     * Tests whether a line is a comment, i.e. whether it starts with a comment
     * character.
     *
     * @param line the line
     * @return a flag if this is a comment line
     */
    static boolean isCommentLine(String line) {
        String s = line.trim();
        // blank lines are also treated as comment lines
        return s.length() < 1 || COMMENT_CHARS.indexOf(s.charAt(0)) >= 0;
    }

    /**
     * <p>Checks if the value is in the given array.</p>
     *
     * <p>The method returns <code>false</code> if a <code>null</code> array is passed in.</p>
     *
     * @param array  the array to search through
     * @param valueToFind  the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains(char[] array, char valueToFind) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return true;
            }
        }
        return false;
    }

    private static final char   ESCAPE_CHAR = '\\';
    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";

    private static final String CHECKSUM_SUFFIX = ".checksum";

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     */
    public static void performSubstitution(Map<String,String> properties)
    {
        for (String name : properties.keySet())
        {
            String value = properties.get(name);
            properties.put(name, substVars(value, name, null, properties));
        }
    }

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     **/
    public static String substVars(String val, String currentKey, Map<String,String> cycleMap, Map<String,String> configProps)
        throws IllegalArgumentException
    {
        if (cycleMap == null)
        {
            cycleMap = new HashMap<String,String>();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = val.indexOf(DELIM_STOP);
        while (stopDelim > 0 && val.charAt(stopDelim - 1) == ESCAPE_CHAR)
        {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
        }

        // Find the matching starting "${" variable delimiter
        // by looping until we find a start delimiter that is
        // greater than the stop delimiter we have found.
        int startDelim = val.indexOf(DELIM_START);
        while (stopDelim >= 0)
        {
            int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim))
            {
                break;
            }
            else if (idx < stopDelim)
            {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) || (stopDelim < 0))
        {
            return unescape(val);
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable = val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException("recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (String) ((configProps != null) ? configProps.get(variable) : null);
        if (substValue == null)
        {
            // Ignore unknown property values.
            substValue = variable.length() > 0 ? System.getProperty(variable, "") : "";
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim) + substValue + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Remove escape characters preceding {, } and \
        val = unescape(val);

        // Return the value.
        return val;
    }

    private static String unescape(String val) {
        int escape = val.indexOf(ESCAPE_CHAR);
        while (escape >= 0 && escape < val.length() - 1)
        {
            char c = val.charAt(escape + 1);
            if (c == '{' || c == '}' || c == ESCAPE_CHAR)
            {
                val = val.substring(0, escape) + val.substring(escape + 1);
            }
            escape = val.indexOf(ESCAPE_CHAR, escape + 1);
        }
        return val;
    }

    /**
     * This class is used to read properties lines. These lines do
     * not terminate with new-line chars but rather when there is no
     * backslash sign a the end of the line.  This is used to
     * concatenate multiple lines for readability.
     */
    public static class PropertiesReader extends LineNumberReader
    {
        /** Stores the comment lines for the currently processed property.*/
        private List<String> commentLines;

        /** Stores the value lines for the currently processed property.*/
        private List<String> valueLines;

        /** Stores the name of the last read property.*/
        private String propertyName;

        /** Stores the value of the last read property.*/
        private String propertyValue;

        /**
         * Creates a new instance of <code>PropertiesReader</code> and sets
         * the underlaying reader and the list delimiter.
         *
         * @param reader the reader
         */
        public PropertiesReader(Reader reader)
        {
            super(reader);
            commentLines = new ArrayList<String>();
            valueLines = new ArrayList<String>();
        }

        /**
         * Reads a property line. Returns null if Stream is
         * at EOF. Concatenates lines ending with "\".
         * Skips lines beginning with "#" or "!" and empty lines.
         * The return value is a property definition (<code>&lt;name&gt;</code>
         * = <code>&lt;value&gt;</code>)
         *
         * @return A string containing a property value or null
         *
         * @throws java.io.IOException in case of an I/O error
         */
        public String readProperty() throws IOException
        {
            commentLines.clear();
            valueLines.clear();
            StringBuffer buffer = new StringBuffer();

            while (true)
            {
                String line = readLine();
                if (line == null)
                {
                    // EOF
                    return null;
                }

                if (isCommentLine(line))
                {
                    commentLines.add(line);
                    continue;
                }

                valueLines.add(line);
                line = line.trim();

                if (checkCombineLines(line))
                {
                    line = line.substring(0, line.length() - 1);
                    buffer.append(line);
                }
                else
                {
                    buffer.append(line);
                    break;
                }
            }
            return buffer.toString();
        }

        /**
         * Parses the next property from the input stream and stores the found
         * name and value in internal fields. These fields can be obtained using
         * the provided getter methods. The return value indicates whether EOF
         * was reached (<b>false</b>) or whether further properties are
         * available (<b>true</b>).
         *
         * @return a flag if further properties are available
         * @throws java.io.IOException if an error occurs
         */
        public boolean nextProperty() throws IOException
        {
            String line = readProperty();

            if (line == null)
            {
                return false; // EOF
            }

            // parse the line
            String[] property = parseProperty(line);
            propertyName = StringEscapeUtils.unescapeJava(property[0]);
            propertyValue = StringEscapeUtils.unescapeJava(property[1]);
            return true;
        }

        /**
         * Returns the comment lines that have been read for the last property.
         *
         * @return the comment lines for the last property returned by
         * <code>readProperty()</code>
         */
        public List<String> getCommentLines()
        {
            return commentLines;
        }

        /**
         * Returns the value lines that have been read for the last property.
         *
         * @return the raw value lines for the last property returned by
         * <code>readProperty()</code>
         */
        public List<String> getValueLines()
        {
            return valueLines;
        }

        /**
         * Returns the name of the last read property. This method can be called
         * after <code>{@link #nextProperty()}</code> was invoked and its
         * return value was <b>true</b>.
         *
         * @return the name of the last read property
         */
        public String getPropertyName()
        {
            return propertyName;
        }

        /**
         * Returns the value of the last read property. This method can be
         * called after <code>{@link #nextProperty()}</code> was invoked and
         * its return value was <b>true</b>.
         *
         * @return the value of the last read property
         */
        public String getPropertyValue()
        {
            return propertyValue;
        }

        /**
         * Checks if the passed in line should be combined with the following.
         * This is true, if the line ends with an odd number of backslashes.
         *
         * @param line the line
         * @return a flag if the lines should be combined
         */
        private static boolean checkCombineLines(String line)
        {
            int bsCount = 0;
            for (int idx = line.length() - 1; idx >= 0 && line.charAt(idx) == '\\'; idx--)
            {
                bsCount++;
            }

            return bsCount % 2 != 0;
        }

        /**
         * Parse a property line and return the key and the value in an array.
         *
         * @param line the line to parse
         * @return an array with the property's key and value
         */
        private static String[] parseProperty(String line)
        {
            // sorry for this spaghetti code, please replace it as soon as
            // possible with a regexp when the Java 1.3 requirement is dropped

            String[] result = new String[2];
            StringBuffer key = new StringBuffer();
            StringBuffer value = new StringBuffer();

            // state of the automaton:
            // 0: key parsing
            // 1: antislash found while parsing the key
            // 2: separator crossing
            // 3: value parsing
            int state = 0;

            for (int pos = 0; pos < line.length(); pos++)
            {
                char c = line.charAt(pos);

                switch (state)
                {
                    case 0:
                        if (c == '\\')
                        {
                            state = 1;
                        }
                        else if (contains(WHITE_SPACE, c))
                        {
                            // switch to the separator crossing state
                            state = 2;
                        }
                        else if (contains(SEPARATORS, c))
                        {
                            // switch to the value parsing state
                            state = 3;
                        }
                        else
                        {
                            key.append(c);
                        }

                        break;

                    case 1:
                        if (contains(SEPARATORS, c) || contains(WHITE_SPACE, c))
                        {
                            // this is an escaped separator or white space
                            key.append(c);
                        }
                        else
                        {
                            // another escaped character, the '\' is preserved
                            key.append('\\');
                            key.append(c);
                        }

                        // return to the key parsing state
                        state = 0;

                        break;

                    case 2:
                        if (contains(WHITE_SPACE, c))
                        {
                            // do nothing, eat all white spaces
                            state = 2;
                        }
                        else if (contains(SEPARATORS, c))
                        {
                            // switch to the value parsing state
                            state = 3;
                        }
                        else
                        {
                            // any other character indicates we encoutered the beginning of the value
                            value.append(c);

                            // switch to the value parsing state
                            state = 3;
                        }

                        break;

                    case 3:
                        value.append(c);
                        break;
                }
            }

            result[0] = key.toString().trim();
            result[1] = value.toString().trim();

            return result;
        }
    } // class PropertiesReader

    /**
     * This class is used to write properties lines.
     */
    public static class PropertiesWriter extends FilterWriter
    {
        /**
         * Constructor.
         *
         * @param writer a Writer object providing the underlying stream
         */
        public PropertiesWriter(Writer writer)
        {
            super(writer);
        }

        /**
         * Writes the given property and its value.
         *
         * @param key the property key
         * @param value the property value
         * @throws java.io.IOException if an error occurs
         */
        public void writeProperty(String key, String value) throws IOException
        {
            write(escapeKey(key));
            write(" = ");
            write(StringEscapeUtils.escapeJava(value));
            writeln(null);
        }

        /**
         * Escape the separators in the key.
         *
         * @param key the key
         * @return the escaped key
         */
        private String escapeKey(String key)
        {
            StringBuffer newkey = new StringBuffer();

            for (int i = 0; i < key.length(); i++)
            {
                char c = key.charAt(i);

                if (contains(SEPARATORS, c) || contains(WHITE_SPACE, c))
                {
                    // escape the separator
                    newkey.append('\\');
                    newkey.append(c);
                }
                else
                {
                    newkey.append(c);
                }
            }

            return newkey.toString();
        }

        /**
         * Helper method for writing a line with the platform specific line
         * ending.
         *
         * @param s the content of the line (may be <b>null</b>)
         * @throws java.io.IOException if an error occurs
         */
        public void writeln(String s) throws IOException
        {
            if (s != null)
            {
                write(s);
            }
            write(LINE_SEPARATOR);
        }

    } // class PropertiesWriter

    /**
     * TODO
     */
    protected static class Layout {

        private List<String> commentLines;
        private List<String> valueLines;

        public Layout() {
        }

        public Layout(List<String> commentLines, List<String> valueLines) {
            this.commentLines = commentLines;
            this.valueLines = valueLines;
        }

        public List<String> getCommentLines() {
            return commentLines;
        }

        public void setCommentLines(List<String> commentLines) {
            this.commentLines = commentLines;
        }

        public List<String> getValueLines() {
            return valueLines;
        }

        public void setValueLines(List<String> valueLines) {
            this.valueLines = valueLines;
        }

        public void clearValue() {
            this.valueLines = null;
        }

    } // class Layout

}