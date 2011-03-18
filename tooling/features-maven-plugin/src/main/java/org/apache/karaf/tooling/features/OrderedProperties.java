/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.karaf.tooling.features;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//import org.apache.harmony.luni.internal.nls.Messages;
//import org.apache.harmony.luni.util.PriviAction;

/**
 * This is a copy of harmony's Properties implementation modified to extend LinkedHashMap instead of Hashtable
 * The default property stuff doesn't work.
 *
 * A {@code Properties} object is a {@code Hashtable} where the keys and values
 * must be {@code String}s. Each property can have a default
 * {@code Properties} list which specifies the default
 * values to be used when a given key is not found in this {@code Properties}
 * instance.
 *
 * @see LinkedHashMap
 * @see java.lang.System#getProperties
 */
public class OrderedProperties extends LinkedHashMap<Object, Object> {

    private static final long serialVersionUID = 4112578634029874840L;

    private transient DocumentBuilder builder = null;

    private static final String PROP_DTD_NAME = "http://java.sun.com/dtd/properties.dtd";

    private static final String PROP_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "    <!ELEMENT properties (comment?, entry*) >"
            + "    <!ATTLIST properties version CDATA #FIXED \"1.0\" >"
            + "    <!ELEMENT comment (#PCDATA) >"
            + "    <!ELEMENT entry (#PCDATA) >"
            + "    <!ATTLIST entry key CDATA #REQUIRED >";

    /**
     * The default values for keys not found in this {@code Properties}
     * instance.
     */
    protected Properties defaults;

    private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3,
            KEY_DONE = 4, IGNORE = 5;

    /**
     * Constructs a new {@code Properties} object.
     */
    public OrderedProperties() {
        super();
    }

    /**
     * Constructs a new {@code Properties} object using the specified default
     * {@code Properties}.
     * 
     * @param properties
     *            the default {@code Properties}.
     */
    public OrderedProperties(Properties properties) {
        defaults = properties;
    }

    private void dumpString(StringBuilder buffer, String string, boolean key) {
        int i = 0;
        if (!key && i < string.length() && string.charAt(i) == ' ') {
            buffer.append("\\ "); //$NON-NLS-1$
            i++;
        }

        for (; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
            case '\t':
                buffer.append("\\t"); //$NON-NLS-1$
                break;
            case '\n':
                buffer.append("\\n"); //$NON-NLS-1$
                break;
            case '\f':
                buffer.append("\\f"); //$NON-NLS-1$
                break;
            case '\r':
                buffer.append("\\r"); //$NON-NLS-1$
                break;
            default:
                if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
                    buffer.append('\\');
                }
                if (ch >= ' ' && ch <= '~') {
                    buffer.append(ch);
                } else {
                    String hex = Integer.toHexString(ch);
                    buffer.append("\\u"); //$NON-NLS-1$
                    for (int j = 0; j < 4 - hex.length(); j++) {
                        buffer.append("0"); //$NON-NLS-1$
                    }
                    buffer.append(hex);
                }
            }
        }
    }

    /**
     * Searches for the property with the specified name. If the property is not
     * found, the default {@code Properties} are checked. If the property is not
     * found in the default {@code Properties}, {@code null} is returned.
     * 
     * @param name
     *            the name of the property to find.
     * @return the named property value, or {@code null} if it can't be found.
     */
    public String getProperty(String name) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        return property;
    }

    /**
     * Searches for the property with the specified name. If the property is not
     * found, it looks in the default {@code Properties}. If the property is not
     * found in the default {@code Properties}, it returns the specified
     * default.
     * 
     * @param name
     *            the name of the property to find.
     * @param defaultValue
     *            the default value.
     * @return the named property value.
     */
    public String getProperty(String name, String defaultValue) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    /**
     * Lists the mappings in this {@code Properties} to the specified
     * {@code PrintStream} in a
     * human readable form.
     * 
     * @param out
     *            the {@code PrintStream} to write the content to in human readable
     *            form.
     */
    public void list(PrintStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        StringBuilder buffer = new StringBuilder(80);
        Iterator<?> keys = keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            buffer.append(key);
            buffer.append('=');
            String property = (String) super.get(key);
            Properties def = defaults;
            if (property == null) {
                property = def.getProperty(key);
            }
            if (property.length() > 40) {
                buffer.append(property.substring(0, 37));
                buffer.append("..."); //$NON-NLS-1$
            } else {
                buffer.append(property);
            }
            out.println(buffer.toString());
            buffer.setLength(0);
        }
    }

    /**
     * Lists the mappings in this {@code Properties} to the specified
     * {@code PrintWriter} in a
     * human readable form.
     * 
     * @param writer
     *            the {@code PrintWriter} to write the content to in human
     *            readable form.
     */
    public void list(PrintWriter writer) {
        if (writer == null) {
            throw new NullPointerException();
        }
        StringBuilder buffer = new StringBuilder(80);
        Iterator<?> keys = keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            buffer.append(key);
            buffer.append('=');
            String property = (String) super.get(key);
            Properties def = defaults;
            if (property == null) {
                property = def.getProperty(key);
            }
            if (property.length() > 40) {
                buffer.append(property.substring(0, 37));
                buffer.append("..."); //$NON-NLS-1$
            } else {
                buffer.append(property);
            }
            writer.println(buffer.toString());
            buffer.setLength(0);
        }
    }

    /**
     * Loads properties from the specified {@code InputStream}. The encoding is
     * ISO8859-1. The {@code Properties} file is interpreted according to the
     * following rules:
     * <ul>
     * <li>Empty lines are ignored.</li>
     * <li>Lines starting with either a "#" or a "!" are comment lines and are
     * ignored.</li>
     * <li>A backslash at the end of the line escapes the following newline
     * character ("\r", "\n", "\r\n"). If there's a whitespace after the
     * backslash it will just escape that whitespace instead of concatenating
     * the lines. This does not apply to comment lines.</li>
     * <li>A property line consists of the key, the space between the key and
     * the value, and the value. The key goes up to the first whitespace, "=" or
     * ":" that is not escaped. The space between the key and the value contains
     * either one whitespace, one "=" or one ":" and any number of additional
     * whitespaces before and after that character. The value starts with the
     * first character after the space between the key and the value.</li>
     * <li>Following escape sequences are recognized: "\ ", "\\", "\r", "\n",
     * "\!", "\#", "\t", "\b", "\f", and "&#92;uXXXX" (unicode character).</li>
     * </ul>
     * 
     * @param in
     *            the {@code InputStream}.
     * @throws IOException
     *             if error occurs during reading from the {@code InputStream}.
     */
    @SuppressWarnings("fallthrough")
    public synchronized void load(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        int mode = NONE, unicode = 0, count = 0;
        char nextChar, buf[] = new char[40];
        int offset = 0, keyLength = -1, intVal;
        boolean firstChar = true;
        BufferedInputStream bis = new BufferedInputStream(in);

        while (true) {
            intVal = bis.read();
            if (intVal == -1) {
                // if mode is UNICODE but has less than 4 hex digits, should
                // throw an IllegalArgumentException
                // luni.08=Invalid Unicode sequence: expected format \\uxxxx
                if (mode == UNICODE && count < 4) {
                    throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\\\uxxxx"); //$NON-NLS-1$
                }
                // if mode is SLASH and no data is read, should append '\u0000'
                // to buf
                if (mode == SLASH) {
                    buf[offset++] = '\u0000';
                }
                break;
            }
            nextChar = (char) (intVal & 0xff);

            if (offset == buf.length) {
                char[] newBuf = new char[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, offset);
                buf = newBuf;
            }
            if (mode == UNICODE) {
                int digit = Character.digit(nextChar, 16);
                if (digit >= 0) {
                    unicode = (unicode << 4) + digit;
                    if (++count < 4) {
                        continue;
                    }
                } else if (count <= 4) {
                    // luni.09=Invalid Unicode sequence: illegal character
                    throw new IllegalArgumentException("Invalid Unicode sequence: illegal character"); //$NON-NLS-1$
                }
                mode = NONE;
                buf[offset++] = (char) unicode;
                if (nextChar != '\n') {
                    continue;
                }
            }
            if (mode == SLASH) {
                mode = NONE;
                switch (nextChar) {
                case '\r':
                    mode = CONTINUE; // Look for a following \n
                    continue;
                case '\n':
                    mode = IGNORE; // Ignore whitespace on the next line
                    continue;
                case 'b':
                    nextChar = '\b';
                    break;
                case 'f':
                    nextChar = '\f';
                    break;
                case 'n':
                    nextChar = '\n';
                    break;
                case 'r':
                    nextChar = '\r';
                    break;
                case 't':
                    nextChar = '\t';
                    break;
                case 'u':
                    mode = UNICODE;
                    unicode = count = 0;
                    continue;
                }
            } else {
                switch (nextChar) {
                case '#':
                case '!':
                    if (firstChar) {
                        while (true) {
                            intVal = bis.read();
                            if (intVal == -1) {
                                break;
                            }
                            // & 0xff not required
                            nextChar = (char) intVal;
                            if (nextChar == '\r' || nextChar == '\n') {
                                break;
                            }
                        }
                        continue;
                    }
                    break;
                case '\n':
                    if (mode == CONTINUE) { // Part of a \r\n sequence
                        mode = IGNORE; // Ignore whitespace on the next line
                        continue;
                    }
                    // fall into the next case
                case '\r':
                    mode = NONE;
                    firstChar = true;
                    if (offset > 0 || (offset == 0 && keyLength == 0)) {
                        if (keyLength == -1) {
                            keyLength = offset;
                        }
                        String temp = new String(buf, 0, offset);
                        put(temp.substring(0, keyLength), temp
                                .substring(keyLength));
                    }
                    keyLength = -1;
                    offset = 0;
                    continue;
                case '\\':
                    if (mode == KEY_DONE) {
                        keyLength = offset;
                    }
                    mode = SLASH;
                    continue;
                case ':':
                case '=':
                    if (keyLength == -1) { // if parsing the key
                        mode = NONE;
                        keyLength = offset;
                        continue;
                    }
                    break;
                }
                if (Character.isWhitespace(nextChar)) {
                    if (mode == CONTINUE) {
                        mode = IGNORE;
                    }
                    // if key length == 0 or value length == 0
                    if (offset == 0 || offset == keyLength || mode == IGNORE) {
                        continue;
                    }
                    if (keyLength == -1) { // if parsing the key
                        mode = KEY_DONE;
                        continue;
                    }
                }
                if (mode == IGNORE || mode == CONTINUE) {
                    mode = NONE;
                }
            }
            firstChar = false;
            if (mode == KEY_DONE) {
                keyLength = offset;
                mode = NONE;
            }
            buf[offset++] = nextChar;
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset;
        }
        if (keyLength >= 0) {
            String temp = new String(buf, 0, offset);
            put(temp.substring(0, keyLength), temp.substring(keyLength));
        }
    }

    /**
     * Returns all of the property names that this {@code Properties} object
     * contains.
     * 
     * @return an {@code Enumeration} containing the names of all properties
     *         that this {@code Properties} object contains.
     */
//    public Enumeration<?> propertyNames() {
//        if (defaults == null) {
//            return keySet().;
//        }
//
//        Hashtable<Object, Object> set = new Hashtable<Object, Object>(defaults
//                .size()
//                + size());
//        Enumeration<?> keys = defaults.propertyNames();
//        while (keys.hasMoreElements()) {
//            set.put(keys.nextElement(), set);
//        }
//        keys = keys();
//        while (keys.hasMoreElements()) {
//            set.put(keys.nextElement(), set);
//        }
//        return set.keys();
//    }

    /**
     * Saves the mappings in this {@code Properties} to the specified {@code
     * OutputStream}, putting the specified comment at the beginning. The output
     * from this method is suitable for being read by the
     * {@link #load(InputStream)} method.
     * 
     * @param out the {@code OutputStream} to write to.
     * @param comment the comment to add at the beginning.
     * @throws ClassCastException if the key or value of a mapping is not a
     *                String.
     * @deprecated This method ignores any {@code IOException} thrown while
     *             writing -- use {@link #store} instead for better exception
     *             handling.
     */
    @Deprecated
    public void save(OutputStream out, String comment) {
        try {
            store(out, comment);
        } catch (IOException e) {
        }
    }

    /**
     * Maps the specified key to the specified value. If the key already exists,
     * the old value is replaced. The key and value cannot be {@code null}.
     * 
     * @param name
     *            the key.
     * @param value
     *            the value.
     * @return the old value mapped to the key, or {@code null}.
     */
    public Object setProperty(String name, String value) {
        return put(name, value);
    }

    private static String lineSeparator;

    /**
     * Stores the mappings in this {@code Properties} to the specified {@code
     * OutputStream}, putting the specified comment at the beginning. The output
     * from this method is suitable for being read by the
     * {@link #load(InputStream)} method.
     * 
     * @param out the {@code OutputStream} to write to.
     * @param comment the comment to put at the beginning.
     * @throws IOException if an error occurs during the write to the {@code
     *             OutputStream}.
     * @throws ClassCastException if the key or value of a mapping is not a
     *                {@code String}.
     */
    public synchronized void store(OutputStream out, String comment)
            throws IOException {
        if (lineSeparator == null) {
            lineSeparator = AccessController
                    .doPrivileged(new PrivilegedAction<String>(){

                        public String run() {
                            return System.getProperty("line.separator");
                        }
                    }); //$NON-NLS-1$
        }

        StringBuilder buffer = new StringBuilder(200);
        OutputStreamWriter writer = new OutputStreamWriter(out, "ISO8859_1"); //$NON-NLS-1$
        if (comment != null) {
            writer.write("#"); //$NON-NLS-1$
            writer.write(comment);
            writer.write(lineSeparator);
        }
        writer.write("#"); //$NON-NLS-1$
        writer.write(new Date().toString());
        writer.write(lineSeparator);

        for (Map.Entry<Object, Object> entry : entrySet()) {
            String key = (String) entry.getKey();
            dumpString(buffer, key, true);
            buffer.append('=');
            dumpString(buffer, (String) entry.getValue(), false);
            buffer.append(lineSeparator);
            writer.write(buffer.toString());
            buffer.setLength(0);
        }
        writer.flush();
    }

    /**
     * Loads the properties from an {@code InputStream} containing the
     * properties in XML form. The XML document must begin with (and conform to)
     * following DOCTYPE:
     *
     * <pre>
     * &lt;!DOCTYPE properties SYSTEM &quot;http://java.sun.com/dtd/properties.dtd&quot;&gt;
     * </pre>
     *
     * Also the content of the XML data must satisfy the DTD but the xml is not
     * validated against it. The DTD is not loaded from the SYSTEM ID. After
     * this method returns the InputStream is not closed.
     *
     * @param in the InputStream containing the XML document.
     * @throws IOException in case an error occurs during a read operation.
     * @throws InvalidPropertiesFormatException if the XML data is not a valid
     *             properties file.
     */
    public synchronized void loadFromXML(InputStream in) throws IOException,
            InvalidPropertiesFormatException {
        if (in == null) {
            throw new NullPointerException();
        }

        if (builder == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(true);

            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new Error(e);
            }

            builder.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException {
                    throw e;
                }

                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });

            builder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId,
                        String systemId) throws SAXException, IOException {
                    if (systemId.equals(PROP_DTD_NAME)) {
                        InputSource result = new InputSource(new StringReader(
                                PROP_DTD));
                        result.setSystemId(PROP_DTD_NAME);
                        return result;
                    }
                    throw new SAXException("Invalid DOCTYPE declaration: "
                            + systemId);
                }
            });
        }

        try {
            Document doc = builder.parse(in);
            NodeList entries = doc.getElementsByTagName("entry");
            if (entries == null) {
                return;
            }
            int entriesListLength = entries.getLength();

            for (int i = 0; i < entriesListLength; i++) {
                Element entry = (Element) entries.item(i);
                String key = entry.getAttribute("key");
                String value = entry.getTextContent();

                /*
                 * key != null & value != null but key or(and) value can be
                 * empty String
                 */
                put(key, value);
            }
        } catch (IOException e) {
            throw e;
        } catch (SAXException e) {
            throw new InvalidPropertiesFormatException(e);
        }
    }

    /**
     * Writes all properties stored in this instance into the {@code
     * OutputStream} in XML representation. The DOCTYPE is
     *
     * <pre>
     * &lt;!DOCTYPE properties SYSTEM &quot;http://java.sun.com/dtd/properties.dtd&quot;&gt;
     * </pre>
     *
     * If the comment is null, no comment is added to the output. UTF-8 is used
     * as the encoding. The {@code OutputStream} is not closed at the end. A
     * call to this method is the same as a call to {@code storeToXML(os,
     * comment, "UTF-8")}.
     *
     * @param os the {@code OutputStream} to write to.
     * @param comment the comment to add. If null, no comment is added.
     * @throws IOException if an error occurs during writing to the output.
     */
    public void storeToXML(OutputStream os, String comment) throws IOException {
        storeToXML(os, comment, "UTF-8"); //$NON-NLS-1$
    }

    /**
     * Writes all properties stored in this instance into the {@code
     * OutputStream} in XML representation. The DOCTYPE is
     *
     * <pre>
     * &lt;!DOCTYPE properties SYSTEM &quot;http://java.sun.com/dtd/properties.dtd&quot;&gt;
     * </pre>
     *
     * If the comment is null, no comment is added to the output. The parameter
     * {@code encoding} defines which encoding should be used. The {@code
     * OutputStream} is not closed at the end.
     *
     * @param os the {@code OutputStream} to write to.
     * @param comment the comment to add. If null, no comment is added.
     * @param encoding the code identifying the encoding that should be used to
     *            write into the {@code OutputStream}.
     * @throws IOException if an error occurs during writing to the output.
     */
    public synchronized void storeToXML(OutputStream os, String comment,
            String encoding) throws IOException {

        if (os == null || encoding == null) {
            throw new NullPointerException();
        }

        /*
         * We can write to XML file using encoding parameter but note that some
         * aliases for encodings are not supported by the XML parser. Thus we
         * have to know canonical name for encoding used to store data in XML
         * since the XML parser must recognize encoding name used to store data.
         */

        String encodingCanonicalName;
        try {
            encodingCanonicalName = Charset.forName(encoding).name();
        } catch (IllegalCharsetNameException e) {
            System.out.println("Warning: encoding name " + encoding
                    + " is illegal, using UTF-8 as default encoding");
            encodingCanonicalName = "UTF-8";
        } catch (UnsupportedCharsetException e) {
            System.out.println("Warning: encoding " + encoding
                    + " is not supported, using UTF-8 as default encoding");
            encodingCanonicalName = "UTF-8";
        }

        PrintStream printStream = new PrintStream(os, false,
                encodingCanonicalName);

        printStream.print("<?xml version=\"1.0\" encoding=\"");
        printStream.print(encodingCanonicalName);
        printStream.println("\"?>");

        printStream.print("<!DOCTYPE properties SYSTEM \"");
        printStream.print(PROP_DTD_NAME);
        printStream.println("\">");

        printStream.println("<properties>");

        if (comment != null) {
            printStream.print("<comment>");
            printStream.print(substitutePredefinedEntries(comment));
            printStream.println("</comment>");
        }

        for (Map.Entry<Object, Object> entry : entrySet()) {
            String keyValue = (String) entry.getKey();
            String entryValue = (String) entry.getValue();
            printStream.print("<entry key=\"");
            printStream.print(substitutePredefinedEntries(keyValue));
            printStream.print("\">");
            printStream.print(substitutePredefinedEntries(entryValue));
            printStream.println("</entry>");
        }
        printStream.println("</properties>");
        printStream.flush();
    }

    private String substitutePredefinedEntries(String s) {

        /*
         * substitution for predefined character entities to use them safely in
         * XML
         */
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(
                ">", "&gt;").replaceAll("\u0027", "&apos;").replaceAll("\"",
                "&quot;");
    }
}
