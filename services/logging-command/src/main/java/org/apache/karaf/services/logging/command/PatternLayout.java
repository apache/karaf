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
package org.apache.karaf.services.logging.command;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

/**
 * Log4j like pattern layout for the OSGi {@link LogEntry}, supporting the same conversions as the
 * <code>log:display</code> command of the Apache Karaf distribution: <code>%c{precision}</code> (logger),
 * <code>%C{precision}</code> (class), <code>%d{format}</code> (date: <code>ISO8601</code>, <code>ABSOLUTE</code>,
 * <code>DATE</code> or a {@link SimpleDateFormat} pattern), <code>%F</code> (file), <code>%h{pattern}{style}</code>
 * (highlight), <code>%L</code> (line), <code>%m</code> (message), <code>%M</code> (method), <code>%p</code> (level),
 * <code>%r</code> (milliseconds since the JVM start), <code>%t</code> (thread), <code>%X{key}</code> (the
 * <code>bundle.id</code>, <code>bundle.name</code> and <code>bundle.version</code> of the entry), <code>%n</code> and
 * <code>%%</code>, with the <code>-</code>, minimum width and <code>.</code> maximum width format modifiers.
 * <p>
 * A layout is not thread safe.
 */
final class PatternLayout {

    static final String ISO8601_DATE_FORMAT = "ISO8601";
    static final String ABSOLUTE_DATE_FORMAT = "ABSOLUTE";
    static final String DATE_AND_TIME_DATE_FORMAT = "DATE";

    private static final String LINE_SEP = System.lineSeparator();
    private static final Pattern ESCAPE_SEQUENCE = Pattern.compile("\u001b\\[[0-?]*[ -/]*[@-~]");

    private final List<Converter> converters = new ArrayList<>();
    private final String pattern;
    private int pos;

    PatternLayout(String pattern) {
        this.pattern = pattern != null ? pattern : "";
        parse();
    }

    /**
     * Format the entry.
     *
     * @param sb      the buffer to append the formatted entry to.
     * @param entry   the log entry.
     * @param noColor <code>true</code> to remove the escape sequences of the pattern, and not to highlight.
     */
    void format(StringBuilder sb, LogEntry entry, boolean noColor) {
        for (Converter converter : converters) {
            converter.format(sb, entry, noColor);
        }
    }

    static String stripEscapeSequences(String s) {
        return ESCAPE_SEQUENCE.matcher(s).replaceAll("");
    }

    static LogLevel level(LogEntry entry) {
        if (entry.getLogLevel() != null) {
            return entry.getLogLevel();
        }
        // legacy LogService levels: LOG_ERROR = 1 ... LOG_DEBUG = 4
        int level = entry.getLevel();
        if (level < 1) {
            return LogLevel.AUDIT;
        }
        return level > 4 ? LogLevel.TRACE : LogLevel.values()[level];
    }

    private void parse() {
        int length = pattern.length();
        StringBuilder literal = new StringBuilder();
        pos = 0;
        while (pos < length) {
            char c = pattern.charAt(pos++);
            if (c != '%' || pos == length) {
                literal.append(c);
                continue;
            }
            char next = pattern.charAt(pos);
            if (next == '%') {
                literal.append('%');
                pos++;
                continue;
            }
            if (next == 'n') {
                literal.append(LINE_SEP);
                pos++;
                continue;
            }
            int start = pos - 1;
            boolean leftAlign = false;
            int min = -1;
            int max = Integer.MAX_VALUE;
            while (pos < length && pattern.charAt(pos) == '-') {
                leftAlign = true;
                pos++;
            }
            if (pos < length && isDigit(pattern.charAt(pos))) {
                min = readNumber();
            }
            if (pos < length && pattern.charAt(pos) == '.') {
                pos++;
                if (pos < length && isDigit(pattern.charAt(pos))) {
                    max = readNumber();
                } else {
                    // not a format modifier: keep it as is, with the next character (as Log4j does)
                    pos = Math.min(pos + 1, length);
                    literal.append(pattern, start, pos);
                    continue;
                }
            }
            if (pos == length) {
                literal.append(pattern, start, length);
                break;
            }
            Converter converter = createConverter(pattern.charAt(pos++));
            if (converter == null) {
                // unknown conversion: keep it as is
                literal.append(pattern, start, pos);
                continue;
            }
            converter.leftAlign = leftAlign;
            converter.min = min;
            converter.max = max;
            if (literal.length() > 0) {
                converters.add(new LiteralConverter(literal.toString()));
                literal.setLength(0);
            }
            converters.add(converter);
        }
        if (literal.length() > 0) {
            converters.add(new LiteralConverter(literal.toString()));
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private int readNumber() {
        int value = 0;
        while (pos < pattern.length() && isDigit(pattern.charAt(pos))) {
            value = value * 10 + (pattern.charAt(pos++) - '0');
        }
        return value;
    }

    private String extractOption() {
        if (pos < pattern.length() && pattern.charAt(pos) == '{') {
            int depth = 1;
            for (int end = pos + 1; end < pattern.length(); end++) {
                char c = pattern.charAt(end);
                if (c == '{') {
                    depth++;
                } else if (c == '}' && --depth == 0) {
                    String option = pattern.substring(pos + 1, end);
                    pos = end + 1;
                    return option;
                }
            }
        }
        return null;
    }

    private int extractPrecision() {
        String option = extractOption();
        if (option != null) {
            try {
                return Math.max(Integer.parseInt(option.trim()), 0);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 0;
    }

    private Converter createConverter(char conversion) {
        switch (conversion) {
            case 'c':
                return new NameConverter(extractPrecision(), LogEntry::getLoggerName);
            case 'C':
                return new NameConverter(extractPrecision(),
                        e -> e.getLocation() != null ? e.getLocation().getClassName() : "?");
            case 'd':
                return new DateConverter(dateFormat(extractOption()));
            case 'F':
                return new SimpleConverter(e -> e.getLocation() != null && e.getLocation().getFileName() != null
                        ? e.getLocation().getFileName() : "?");
            case 'h':
                return new HighlightConverter(extractOption(), extractOption());
            case 'L':
                return new SimpleConverter(e -> e.getLocation() != null && e.getLocation().getLineNumber() >= 0
                        ? Integer.toString(e.getLocation().getLineNumber()) : "?");
            case 'm':
                return new SimpleConverter(LogEntry::getMessage);
            case 'M':
                return new SimpleConverter(e -> e.getLocation() != null ? e.getLocation().getMethodName() : "?");
            case 'p':
                return new SimpleConverter(e -> level(e).name());
            case 'r':
                return new SimpleConverter(e -> Long.toString(e.getTime() - StartTime.VALUE));
            case 't':
                return new SimpleConverter(LogEntry::getThreadInfo);
            case 'X':
                return new PropertiesConverter(extractOption());
            default:
                return null;
        }
    }

    private static DateFormat dateFormat(String option) {
        String format = option != null ? option : ISO8601_DATE_FORMAT;
        if (format.equalsIgnoreCase(ISO8601_DATE_FORMAT)) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        } else if (format.equalsIgnoreCase(ABSOLUTE_DATE_FORMAT)) {
            return new SimpleDateFormat("HH:mm:ss,SSS");
        } else if (format.equalsIgnoreCase(DATE_AND_TIME_DATE_FORMAT)) {
            return new SimpleDateFormat("dd MMM yyyy HH:mm:ss,SSS");
        }
        try {
            return new SimpleDateFormat(format);
        } catch (IllegalArgumentException e) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        }
    }

    private abstract static class Converter {

        boolean leftAlign;
        int min = -1;
        int max = Integer.MAX_VALUE;

        abstract String convert(LogEntry entry, boolean noColor);

        void format(StringBuilder sb, LogEntry entry, boolean noColor) {
            String s = convert(entry, noColor);
            if (s == null) {
                pad(sb, min);
                return;
            }
            int length = s.length();
            if (length > max) {
                // like Log4j, truncate from the beginning
                sb.append(s, length - max, length);
            } else if (length < min) {
                if (leftAlign) {
                    sb.append(s);
                    pad(sb, min - length);
                } else {
                    pad(sb, min - length);
                    sb.append(s);
                }
            } else {
                sb.append(s);
            }
        }

        private static void pad(StringBuilder sb, int length) {
            for (int i = 0; i < length; i++) {
                sb.append(' ');
            }
        }
    }

    private static final class LiteralConverter extends Converter {

        private final String literal;
        private final String strippedLiteral;

        LiteralConverter(String literal) {
            this.literal = literal;
            this.strippedLiteral = stripEscapeSequences(literal);
        }

        @Override
        String convert(LogEntry entry, boolean noColor) {
            return noColor ? strippedLiteral : literal;
        }

        @Override
        void format(StringBuilder sb, LogEntry entry, boolean noColor) {
            sb.append(convert(entry, noColor));
        }
    }

    @FunctionalInterface
    private interface Extractor {
        String extract(LogEntry entry);
    }

    private static final class SimpleConverter extends Converter {

        private final Extractor extractor;

        SimpleConverter(Extractor extractor) {
            this.extractor = extractor;
        }

        @Override
        String convert(LogEntry entry, boolean noColor) {
            return extractor.extract(entry);
        }
    }

    private static final class NameConverter extends Converter {

        private final int precision;
        private final Extractor extractor;

        NameConverter(int precision, Extractor extractor) {
            this.precision = precision;
            this.extractor = extractor;
        }

        @Override
        String convert(LogEntry entry, boolean noColor) {
            String name = extractor.extract(entry);
            if (name == null || precision <= 0) {
                return name;
            }
            // keep the last <precision> elements of the name (a trailing dot is not an element separator)
            int end = name.length() - 1;
            for (int i = precision; i > 0; i--) {
                end = name.lastIndexOf('.', end - 1);
                if (end == -1) {
                    return name;
                }
            }
            return name.substring(end + 1);
        }
    }

    private static final class DateConverter extends Converter {

        private final DateFormat dateFormat;
        private final Date date = new Date();

        DateConverter(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        String convert(LogEntry entry, boolean noColor) {
            date.setTime(entry.getTime());
            return dateFormat.format(date);
        }
    }

    private static final class PropertiesConverter extends Converter {

        private final String key;

        PropertiesConverter(String key) {
            this.key = key;
        }

        @Override
        String convert(LogEntry entry, boolean noColor) {
            Map<String, String> properties = properties(entry);
            if (key != null) {
                return properties.get(key);
            }
            StringBuilder sb = new StringBuilder("{");
            for (Map.Entry<String, String> property : properties.entrySet()) {
                sb.append('{').append(property.getKey()).append(',').append(property.getValue()).append('}');
            }
            return sb.append('}').toString();
        }

        private static Map<String, String> properties(LogEntry entry) {
            Bundle bundle = entry.getBundle();
            if (bundle == null) {
                return Collections.emptyMap();
            }
            Map<String, String> properties = new TreeMap<>();
            properties.put("bundle.id", Long.toString(bundle.getBundleId()));
            if (bundle.getSymbolicName() != null) {
                properties.put("bundle.name", bundle.getSymbolicName());
            }
            properties.put("bundle.version", bundle.getVersion().toString());
            return properties;
        }
    }

    private static final class HighlightConverter extends Converter {

        private static final String CSI = "\u001b[";
        private static final String SUFFIX = "m";
        private static final Map<String, String> SEQUENCES = new HashMap<>();

        static {
            SEQUENCES.put("normal", "0");
            SEQUENCES.put("bold", "1");
            SEQUENCES.put("bright", "1");
            SEQUENCES.put("dim", "2");
            SEQUENCES.put("underline", "3");
            SEQUENCES.put("blink", "5");
            SEQUENCES.put("reverse", "7");
            SEQUENCES.put("hidden", "8");
            String[] colors = {"black", "red", "green", "yellow", "blue", "magenta", "cyan", "white"};
            for (int i = 0; i < colors.length; i++) {
                SEQUENCES.put(colors[i], Integer.toString(30 + i));
                SEQUENCES.put("fg_" + colors[i], Integer.toString(30 + i));
                SEQUENCES.put("bg_" + colors[i], Integer.toString(40 + i));
            }
            SEQUENCES.put("default", "39");
            SEQUENCES.put("fg_default", "39");
            SEQUENCES.put("bg_default", "49");
        }

        private final PatternLayout layout;
        private final Map<String, String> styles = new HashMap<>();

        HighlightConverter(String pattern, String style) {
            this.layout = new PatternLayout(pattern);
            Map<String, String> unparsed = new HashMap<>();
            unparsed.put("trace", "cyan");
            unparsed.put("debug", "cyan");
            unparsed.put("info", "bright green");
            unparsed.put("warn", "bright yellow");
            unparsed.put("error", "bright red");
            unparsed.put("fatal", "bright red");
            if (style != null) {
                style = style.toLowerCase(Locale.ENGLISH);
                if (style.indexOf(',') < 0 && style.indexOf('=') < 0) {
                    for (String level : new ArrayList<>(unparsed.keySet())) {
                        unparsed.put(level, style.trim());
                    }
                } else {
                    for (String key : style.split("\\s*,\\s*")) {
                        String[] value = key.split("\\s*=\\s*");
                        if (value.length > 1) {
                            unparsed.put(value[0].trim(), value[1].trim());
                        }
                    }
                }
            }
            for (Map.Entry<String, String> entry : unparsed.entrySet()) {
                styles.put(entry.getKey(), sequence(entry.getValue().trim().split("\\s+")));
            }
        }

        private static String sequence(String... names) {
            StringBuilder sb = new StringBuilder(CSI);
            for (int i = 0; i < names.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                sb.append(SEQUENCES.getOrDefault(names[i], names[i]));
            }
            return sb.append(SUFFIX).toString();
        }

        @Override
        String convert(LogEntry entry, boolean noColor) {
            StringBuilder sb = new StringBuilder();
            layout.format(sb, entry, noColor);
            String style = noColor ? null : styles.get(level(entry).name().toLowerCase(Locale.ENGLISH));
            return style != null ? style + sb + CSI + SUFFIX : sb.toString();
        }
    }

    // initialized when %r is first used
    private static final class StartTime {

        static final long VALUE = startTime();

        private static long startTime() {
            try {
                return ManagementFactory.getRuntimeMXBean().getStartTime();
            } catch (Throwable t) {
                return System.currentTimeMillis();
            }
        }
    }

}
