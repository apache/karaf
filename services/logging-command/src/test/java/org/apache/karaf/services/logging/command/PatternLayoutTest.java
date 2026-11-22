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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.log.LogLevel;

import static org.junit.Assert.assertEquals;

public class PatternLayoutTest {

    private static final String NL = System.lineSeparator();
    private static final String ESC = "\u001b[";

    private static String format(String pattern, TestEntry entry, boolean noColor) {
        StringBuilder sb = new StringBuilder();
        new PatternLayout(pattern).format(sb, entry, noColor);
        return sb.toString();
    }

    private static String format(String pattern, TestEntry entry) {
        return format(pattern, entry, false);
    }

    private static String date(String format) {
        return new SimpleDateFormat(format).format(new Date(TestEntry.TIME));
    }

    @Test
    public void testDefaultPattern() {
        String expected = date("HH:mm:ss,SSS") + " | " + String.format("%-5s", "INFO") + " | " + String.format("%-16s", "main")
                + " | " + String.format("%-32s", "Foo") + " | " + String.format("%-32s", "org.apache.karaf.test.Foo")
                + " " + String.format("%4s", "42") + " | hello" + NL;
        assertEquals(expected, format(LogEntryFormatter.DEFAULT_PATTERN, new TestEntry()));
    }

    @Test
    public void testLightFeaturePattern() {
        // the pattern of the org.apache.karaf.log configuration, once the color.* placeholders are replaced
        String pattern = "\u001b[90m%d{HH:mm:ss.SSS}\u001b[0m %h{%p}{FATAL=bright red, ERROR=bright red, WARN=bright yellow, "
                + "INFO=bright green, DEBUG=cyan, TRACE=cyan} \u001b[90m[%t]\u001b[0m %m%n";
        TestEntry entry = new TestEntry(LogLevel.WARN, "foo", "careful");
        assertEquals(ESC + "90m" + date("HH:mm:ss.SSS") + ESC + "0m " + ESC + "1;33mWARN" + ESC + "m "
                + ESC + "90m[main]" + ESC + "0m careful" + NL, format(pattern, entry));
        assertEquals(date("HH:mm:ss.SSS") + " WARN [main] careful" + NL, format(pattern, entry, true));
    }

    @Test
    public void testFormatModifiers() {
        TestEntry entry = new TestEntry(LogLevel.INFO, "abcdef", "m");
        assertEquals(" INFO|", format("%5p|", entry));
        assertEquals("INFO |", format("%-5p|", entry));
        assertEquals("def|", format("%.3c|", entry));
        // like Log4j, a truncated value is not padded
        assertEquals("def|", format("%5.3c|", entry));
        assertEquals("abcdef|", format("%3c|", entry));
        entry.message = null;
        assertEquals("[   ]", format("[%3m]", entry));
    }

    @Test
    public void testPrecision() {
        TestEntry entry = new TestEntry(LogLevel.INFO, "a.b.c", "m");
        assertEquals("c", format("%c{1}", entry));
        assertEquals("b.c", format("%c{2}", entry));
        assertEquals("a.b.c", format("%c{5}", entry));
        assertEquals("a.b.c", format("%c{x}", entry));
        assertEquals("test.Foo", format("%C{2}", entry));
    }

    @Test
    public void testLocation() {
        TestEntry entry = new TestEntry();
        assertEquals("org.apache.karaf.test.Foo bar Foo.java 42", format("%C %M %F %L", entry));
        entry.location = new StackTraceElement("Foo", "bar", null, -1);
        assertEquals("Foo bar ? ?", format("%C %M %F %L", entry));
        entry.location = null;
        assertEquals("? ? ? ?", format("%C %M %F %L", entry));
    }

    @Test
    public void testDates() {
        TestEntry entry = new TestEntry();
        assertEquals(date("yyyy-MM-dd HH:mm:ss,SSS"), format("%d", entry));
        assertEquals(date("yyyy-MM-dd HH:mm:ss,SSS"), format("%d{ISO8601}", entry));
        assertEquals(date("HH:mm:ss,SSS"), format("%d{ABSOLUTE}", entry));
        assertEquals(date("dd MMM yyyy HH:mm:ss,SSS"), format("%d{DATE}", entry));
        assertEquals(date("yyyy/MM/dd"), format("%d{yyyy/MM/dd}", entry));
        // invalid pattern: ISO8601
        assertEquals(date("yyyy-MM-dd HH:mm:ss,SSS"), format("%d{bbb}", entry));
    }

    @Test
    public void testLiterals() {
        TestEntry entry = new TestEntry();
        assertEquals("100% INFO" + NL, format("100%% %p%n", entry));
        assertEquals("%k INFO", format("%k %p", entry));
        assertEquals("%-5k INFO", format("%-5k %p", entry));
        assertEquals("%5.x INFO", format("%5.x %p", entry));
        assertEquals("INFO %", format("%p %", entry));
        assertEquals("INFO %5", format("%p %5", entry));
        assertEquals("main hello", format("%t %m", entry));
    }

    @Test
    public void testProperties() {
        TestEntry entry = new TestEntry();
        assertEquals("{}|", format("%X|", entry));
        assertEquals("|", format("%X{bundle.id}|", entry));

        Bundle bundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(7L).anyTimes();
        EasyMock.expect(bundle.getSymbolicName()).andReturn("foo").anyTimes();
        EasyMock.expect(bundle.getVersion()).andReturn(new Version(1, 2, 3)).anyTimes();
        EasyMock.replay(bundle);
        entry.bundle = bundle;
        assertEquals("{{bundle.id,7}{bundle.name,foo}{bundle.version,1.2.3}}", format("%X", entry));
        assertEquals("7 - foo - 1.2.3", format("%X{bundle.id} - %X{bundle.name} - %X{bundle.version}", entry));
    }

    @Test
    public void testHighlight() {
        TestEntry entry = new TestEntry(LogLevel.ERROR, "foo", "boom");
        // default styles
        assertEquals(ESC + "1;31mERROR" + ESC + "m", format("%h{%p}", entry));
        // the whole highlighted pattern is formatted
        assertEquals(ESC + "1;31mERROR foo" + ESC + "m", format("%h{%-5p %c}", entry));
        assertEquals(ESC + "32mERROR" + ESC + "m", format("%h{%p}{green}", entry));
        assertEquals(ESC + "4;45mERROR" + ESC + "m", format("%h{%p}{ERROR=4 bg_magenta, INFO=green}", entry));
        assertEquals("ERROR", format("%h{%p}{green}", entry, true));
        // no default style for AUDIT
        entry.level = LogLevel.AUDIT;
        assertEquals("AUDIT", format("%h{%p}", entry));
    }

}
