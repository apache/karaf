/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.url.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.osgi.Constants;

/**
 * Parser for the {@code wrap:} protocol.
 * <p>
 * Syntax: {@code wrap:wrapped-jar-url[,wrapping-instructions-url][$wrapping-instructions]}
 * where the wrapping instructions are {@code &} separated {@code header=value} bnd instructions.
 */
public class WrapUrlParser {

    private static final String SYNTAX = "wrap:wrapped-jar-url[,wrapping-instructions-url][$wrapping-instructions]";

    private static final String INSTRUCTIONS_SEPARATOR = "$";

    private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

    private static final Pattern INSTRUCTION = Pattern.compile("([a-zA-Z_0-9-]+)=([^\\r\\n]*)");

    private final URL wrappedJarUrl;
    private final URL instructionsUrl;
    private final Properties instructions;

    /**
     * @param path the {@code wrap:} URL, without the protocol
     * @throws MalformedURLException if the path does not comply with the {@code wrap:} syntax
     */
    public WrapUrlParser(String path) throws MalformedURLException {
        if (path == null || path.trim().isEmpty()) {
            throw new MalformedURLException("Path cannot be null or empty. Syntax " + SYNTAX);
        }
        if (path.startsWith(INSTRUCTIONS_SEPARATOR) || path.endsWith(INSTRUCTIONS_SEPARATOR)) {
            throw new MalformedURLException("Path cannot start or end with " + INSTRUCTIONS_SEPARATOR + ". Syntax " + SYNTAX);
        }
        int instructionsSeparator = path.indexOf(INSTRUCTIONS_SEPARATOR);
        String urlPart = instructionsSeparator >= 0 ? path.substring(0, instructionsSeparator) : path;
        int comma = urlPart.indexOf(',');
        if (comma >= 0) {
            wrappedJarUrl = new URL(urlPart.substring(0, comma));
            instructionsUrl = new URL(urlPart.substring(comma + 1));
        } else {
            wrappedJarUrl = new URL(urlPart);
            instructionsUrl = null;
        }
        instructions = instructionsSeparator >= 0
                ? parseInstructions(path.substring(instructionsSeparator + 1)) : new Properties();
    }

    public URL getWrappedJarUrl() {
        return wrappedJarUrl;
    }

    /**
     * @return the URL of the properties file providing wrapping instructions, or {@code null}
     */
    public URL getInstructionsUrl() {
        return instructionsUrl;
    }

    /**
     * @return the wrapping instructions provided inline in the URL
     */
    public Properties getInstructions() {
        return instructions;
    }

    static Properties parseInstructions(String query) throws MalformedURLException {
        Properties instructions = new Properties();
        for (String segment : query.split("&")) {
            if (segment.trim().isEmpty()) {
                continue;
            }
            Matcher matcher = INSTRUCTION.matcher(segment);
            if (!matcher.matches()) {
                throw new MalformedURLException("Invalid syntax for instruction [" + segment + "]. Syntax " + SYNTAX);
            }
            String value = matcher.group(2);
            try {
                instructions.setProperty(normalizeHeader(matcher.group(1)),
                        value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : "");
            } catch (IllegalArgumentException e) {
                MalformedURLException exception = new MalformedURLException("Invalid encoding in instruction [" + segment + "]");
                exception.initCause(e);
                throw exception;
            }
        }
        return instructions;
    }

    // headers are case sensitive in bnd, so bundle-version is turned into Bundle-Version
    private static String normalizeHeader(String key) {
        if (WEB_CONTEXT_PATH.equalsIgnoreCase(key)) {
            return WEB_CONTEXT_PATH;
        }
        for (String header : Constants.headers) {
            if (header.equalsIgnoreCase(key)) {
                return header;
            }
        }
        return key;
    }

}
