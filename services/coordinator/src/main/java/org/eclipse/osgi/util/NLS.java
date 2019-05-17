/*
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
package org.eclipse.osgi.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;


public class NLS {

    public static void initializeMessages(final String bundleName, final Class<?> clazz) {
        String resource = bundleName.replace('.', '/') + ".properties";
        try (InputStream input = clazz.getClassLoader().getResourceAsStream(resource)) {
            Properties properties = new Properties();
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);

                try {
                    Field field = clazz.getDeclaredField(key);
                    field.setAccessible(true);
                    field.set(null, value);
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static String bind(String message, Object binding) {
        return bind(message, new Object[] { binding });
    }

    public static String bind(String message, Object binding1, Object binding2) {
        return bind(message, new Object[] { binding1, binding2 });
    }

    public static String bind(String message, Object[] bindings) {
        int length = message.length();
        //estimate correct size of string buffer to avoid growth
        StringBuilder buffer = new StringBuilder(message.length()
                                    + (bindings != null ? bindings.length * 5 : 0));
        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);
            switch (c) {
                case '{' :
                    int index = message.indexOf('}', i);
                    // if we don't have a matching closing brace then...
                    if (index == -1) {
                        buffer.append(c);
                        break;
                    }
                    i++;
                    if (i >= length) {
                        buffer.append(c);
                        break;
                    }
                    // look for a substitution
                    int number = -1;
                    try {
                        number = Integer.parseInt(message.substring(i, index));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(e);
                    }
                    if (bindings == null || number >= bindings.length || number < 0) {
                        buffer.append("<missing argument>"); //$NON-NLS-1$
                        i = index;
                        break;
                    }
                    buffer.append(bindings[number]);
                    i = index;
                    break;
                case '\'' :
                    // if a single quote is the last char on the line then skip it
                    int nextIndex = i + 1;
                    if (nextIndex >= length) {
                        buffer.append(c);
                        break;
                    }
                    char next = message.charAt(nextIndex);
                    // if the next char is another single quote then write out one
                    if (next == '\'') {
                        i++;
                        buffer.append(c);
                        break;
                    }
                    // otherwise we want to read until we get to the next single quote
                    index = message.indexOf('\'', nextIndex);
                    // if there are no more in the string, then skip it
                    if (index == -1) {
                        buffer.append(c);
                        break;
                    }
                    // otherwise write out the chars inside the quotes
                    buffer.append(message, nextIndex, index);
                    i = index;
                    break;
                default :
                    buffer.append(c);
            }
        }
        return buffer.toString();
    }

}
